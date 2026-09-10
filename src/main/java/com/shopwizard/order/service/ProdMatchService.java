package com.shopwizard.order.service;

import com.shopwizard.adjust.mapper.AdjustDetailMapper;
import com.shopwizard.order.mapper.OrderProdMapper;
import com.shopwizard.order.mapper.ProdCodeMatchMapper;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.model.ProdCodeMatch;
import com.shopwizard.order.model.ProdMatchRequest;
import com.shopwizard.order.model.ProdMatchResult;
import com.shopwizard.product.model.Prod;
import com.shopwizard.product.service.ProdService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 샵링커/엑셀 수집 주문 중 상품이 매칭되지 않은(ProdCode NULL) 주문라인을 실제 상품에 연결한다.
 * 레거시 {@code OrderServiceImpl#runUpdatePriceFromExcel} 의 backfill 로직을 이식하되,
 * 주문 원본 금액(SalePrice/NvPrice/ProdQty)은 건드리지 않고 상품 기준 정보만 채운다.
 */
@Service
@RequiredArgsConstructor
public class ProdMatchService {

    private static final Logger log = LoggerFactory.getLogger(ProdMatchService.class);

    private final OrderProdMapper orderProdMapper;
    private final ProdCodeMatchMapper prodCodeMatchMapper;
    private final AdjustDetailMapper adjustDetailMapper;
    private final ProdService productProdService;   // com.shopwizard.product.service.ProdService (bean "prodService")

    public List<Map<String, Object>> selectUnmatched(Map<String, Object> params) {
        return orderProdMapper.selectListUnmatched(params);
    }

    public int selectUnmatchedCount(Map<String, Object> params) {
        return orderProdMapper.selectListUnmatchedCount(params);
    }

    /**
     * (상품명, 옵션) 이 같은 미매칭 주문라인 전체를 상품에 연결한다. 건별 커밋이 아닌 한 트랜잭션
     * (한 상품명 묶음은 전부 성공하거나 전부 롤백). 정산마감된 라인만 건너뛴다.
     */
    @Transactional(rollbackFor = Exception.class)
    public ProdMatchResult apply(ProdMatchRequest req) {
        String prodName = nz(req.getProdName());
        String prodOption = nz(req.getProdOption());
        String svcCode = (req.getSvcCode() == null || req.getSvcCode().isBlank()) ? "SHP001" : req.getSvcCode();

        Prod prod = resolveProd(req);
        if (prod == null) {
            return ProdMatchResult.fail(prodName, prodOption,
                    "연결할 상품을 찾을 수 없습니다. (prodCode=" + req.getProdCode() + ", shopProdCode=" + req.getShopProdCode() + ")");
        }

        Map<String, Object> lineParam = new HashMap<>();
        lineParam.put("pSvcCode", svcCode);
        lineParam.put("pProdName", prodName);
        lineParam.put("pProdOption", prodOption);
        lineParam.put("pStartDate", req.getPStartDate());
        lineParam.put("pEndDate", req.getPEndDate());
        lineParam.put("pChnlCode", req.getPChnlCode());
        List<OrderProd> lines = orderProdMapper.selectUnmatchedLinesByName(lineParam);

        ProdMatchResult result = new ProdMatchResult();
        result.setProdName(prodName);
        result.setProdOption(prodOption);

        for (OrderProd line : lines) {
            if (isAdjustClosed(line.getOrderNo(), line.getOrderProdNo())) {
                result.setSkipped(result.getSkipped() + 1);
                result.getSkipDetails().add("주문 " + line.getOrderNo() + "-" + line.getOrderProdNo() + " : 정산마감되어 수정 불가");
                continue;
            }
            Map<String, Object> raw = new HashMap<>();
            raw.put("orderNo", line.getOrderNo());
            raw.put("orderProdNo", line.getOrderProdNo());
            OrderProd op = orderProdMapper.selectLineRaw(raw);
            if (op == null) continue;

            backfillLine(op, prod);
            op.setChangeId(nz(req.getRegistId()));
            op.setChangeName(nz(req.getRegistName()));
            orderProdMapper.updatePrice(op);

            decreaseStock(prod.getProdCode(), op.getProdQty());
            result.setApplied(result.getApplied() + 1);
        }

        if (req.isSaveRule()) {
            ProdCodeMatch rule = new ProdCodeMatch();
            rule.setProdName(prodName);
            rule.setProdOption(prodOption);
            rule.setProdCode(prod.getProdCode());
            rule.setRegistId(nz(req.getRegistId()));
            rule.setRegistName(nz(req.getRegistName()));
            prodCodeMatchMapper.upsert(rule);
        }

        result.setMessage(String.format("상품 [%s] 연결 완료 — 적용 %d건%s",
                prod.getProdCode(), result.getApplied(),
                result.getSkipped() > 0 ? ", 건너뜀 " + result.getSkipped() + "건" : ""));
        return result;
    }

    // ── 재사용 헬퍼 (수집 자동매칭에서도 사용) ─────────────────────────

    /**
     * 주문라인에 상품 기준 정보를 채우고 마진을 다시 계산한다.
     * SalePrice/NvPrice/ProdQty/DeliFeeType/PromotFeeAmt 등 주문 원본 값은 그대로 둔다.
     */
    public static void backfillLine(OrderProd op, Prod prod) {
        op.setProdCode(prod.getProdCode());
        op.setSupplyCode(prod.getSupplyCode());
        op.setSupplyName(prod.getSupplyName());
        op.setShopCode(prod.getShopCode());
        op.setShopName(prod.getShopName());
        op.setStoreCode(prod.getCateCode());
        op.setMakerName(prod.getMakerName());
        op.setModelName(prod.getModelName());
        op.setProdUnit(prod.getProdUnit());
        op.setShopProdCode(prod.getShopProdCode());
        op.setProdImg(prod.getImgUrl());

        int salePrice = nvl(op.getSalePrice());
        int nvPrice   = nvl(op.getNvPrice());
        // 채널이 실제 거래가(공급가/원가)를 이미 실어줬으면 그 값을 신뢰하고, 0/누락일 때만 상품마스터로 채운다.
        int supplyPrice = nvl(op.getSupplyPrice()) > 0 ? nvl(op.getSupplyPrice()) : nvl(prod.getSupplyPrice());
        int buyPrice    = nvl(op.getBuyPrice())    > 0 ? nvl(op.getBuyPrice())    : nvl(prod.getBuyPrice());
        int promotFee   = nvl(op.getPromotFeeAmt());

        op.setSupplyPrice(supplyPrice);
        op.setBuyPrice(buyPrice);

        int vatRate = nvl(prod.getVatRate());
        op.setVatRate(vatRate);
        op.setVatAmt(vatRate > 0 ? (int) ((long) salePrice * 100 / (100 + vatRate)) : 0);

        op.setProdMargin(salePrice + nvPrice - buyPrice);
        op.setChnlMargin(salePrice + nvPrice - supplyPrice);
        op.setNetMargin(supplyPrice - buyPrice - promotFee);
    }

    private Prod resolveProd(ProdMatchRequest req) {
        if (req.getProdCode() != null && !req.getProdCode().isBlank()) {
            Map<String, Object> p = new HashMap<>();
            p.put("pProdCode", req.getProdCode());
            List<Prod> list = productProdService.selectList(p);
            if (!list.isEmpty()) return list.get(0);
        }
        if (req.getShopProdCode() != null && !req.getShopProdCode().isBlank()) {
            Map<String, Object> p = new HashMap<>();
            p.put("pShopProdCode", req.getShopProdCode());
            return productProdService.selectByShopProdCode(p);
        }
        return null;
    }

    private boolean isAdjustClosed(Integer orderNo, Integer orderProdNo) {
        Map<String, Object> p = new HashMap<>();
        p.put("pOrderNo", orderNo);
        p.put("pOrderProdNo", orderProdNo);
        p.put("pAdjustCloseFlag", "YES");
        return adjustDetailMapper.selectCount(p) > 0;
    }

    private void decreaseStock(String prodCode, Integer qty) {
        if (prodCode == null || prodCode.isBlank() || qty == null || qty <= 0) return;
        try {
            Map<String, Object> d = new HashMap<>();
            d.put("prodCode", prodCode);
            d.put("qty", qty);
            productProdService.updateSupplyQty(d);
        } catch (Exception e) {
            log.warn("상품 매칭 재고차감 실패 prodCode={} : {}", prodCode, e.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static int nvl(Integer i) { return i == null ? 0 : i; }
}

package com.shopwizard.ship.service;

import com.shopwizard.ship.mapper.ShipCheckMapper;
import com.shopwizard.ship.model.ShipCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 업체(상점·창고) 계정이 자기 발주 목록을 보고 확정된 발주서(.xls)를 받는 서비스.
 * 관리자용 {@code OrderMasterGuard}/{@code DataScope} 는 MNGR 세션만 읽기 때문에 업체 세션에는 쓸 수 없다 — 여기서는
 * 컨트롤러가 세션에서 계산한 "허용 상점 집합"을 인자로 받아 모든 조회·다운로드를 그 안으로 제한한다.
 *
 * <ul>
 *   <li>목록: 허용 집합 밖의 상점을 지정하면 403, 지정하지 않으면 허용 집합 전체.</li>
 *   <li>다운로드: 발주 번호가 필수(없으면 400 — 발주확인 전 대기 행·개인정보가 새지 않게), 번호가 없거나 허용 밖 상점의 것이면
 *       똑같이 404(남의 번호 존재 여부를 알려 주지 않는다). 출력 횟수는 올리지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorShipCheckService {

    static final int MAX_PAGE = 100;

    private final ShipCheckMapper shipCheckMapper;
    private final ShipCheckService shipCheckService;
    private final ShipAutoOrderService shipAutoOrderService;

    /** 창고에 연결된 사용 중인 상점. */
    public List<Map<String, Object>> shopsOfWarehs(String warehsCode) {
        if (warehsCode == null || warehsCode.isBlank()) return List.of();
        return shipCheckMapper.selectShopsByWarehs(warehsCode.trim());
    }

    /** 창고의 허용 상점 코드 집합. */
    public Set<String> allowedShopsOfWarehs(String warehsCode) {
        return shopsOfWarehs(warehsCode).stream().map(m -> String.valueOf(m.get("shopCode"))).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public Map<String, Object> list(Collection<String> allowed, String requestedShop, String startDate, String endDate,
                                    Integer offset, Integer limit) {
        Map<String, Object> out = new LinkedHashMap<>();
        String shop = requestedShop == null ? "" : requestedShop.trim();
        if (!shop.isEmpty() && (allowed == null || !allowed.contains(shop))) throw new ResponseStatusException(FORBIDDEN, "다른 업체의 발주 목록은 볼 수 없습니다.");
        if (allowed == null || allowed.isEmpty()) {
            out.put("total", 0);
            out.put("rows", List.of());
            return out;
        }
        int off = offset == null || offset < 0 ? 0 : offset;
        int lim = limit == null || limit < 1 ? 20 : Math.min(limit, MAX_PAGE);
        out.put("total", shipCheckMapper.selectCountByShops(allowed, shop, startDate, endDate));
        out.put("rows", shipCheckMapper.selectListByShops(allowed, shop, startDate, endDate, off, lim));
        return out;
    }

    /** 확정된 발주서 다운로드. 번호 필수, 허용 밖이면 없는 번호와 똑같이 404. */
    @Transactional(rollbackFor = Exception.class)
    public ShipAutoOrderService.Download download(Collection<String> allowed, Integer shipCheckNo, String loginId, String name) {
        if (shipCheckNo == null) throw new ResponseStatusException(BAD_REQUEST, "발주 번호가 필요합니다.");
        ShipCheck header = shipCheckService.find(shipCheckNo);
        if (header == null || allowed == null || !allowed.contains(header.getShopCode())) {
            throw new ResponseStatusException(NOT_FOUND, "발주서를 찾을 수 없습니다.");
        }
        log.info("업체 발주서 다운로드: loginId={}, shipCheckNo={}, shopCode={}", loginId, shipCheckNo, header.getShopCode());
        return shipAutoOrderService.download(null, shipCheckNo, loginId, name, false);
    }
}

package com.shopwizard.order.web;

import com.shopwizard.order.model.OrderDashBoard;
import com.shopwizard.order.model.OrderDetlStats;
import com.shopwizard.order.model.OrderProd;
import com.shopwizard.order.service.OrderProdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order/orderprod")
public class OrderProdController {
    private final OrderProdService orderProdService;

    @GetMapping("/list")
    public List<OrderProd> selectList(@RequestParam Map<String, Object> params) { return orderProdService.selectList(params); }

    @GetMapping("/list/count")
    public int selectListCount(@RequestParam Map<String, Object> params) { return orderProdService.selectListCount(params); }

    @GetMapping
    public OrderProd select(@RequestParam Map<String, Object> params) { return orderProdService.select(params); }

    @PostMapping
    public void insert(@RequestBody OrderProd orderProd) { orderProdService.insert(orderProd); }

    @PutMapping
    public void update(@RequestBody OrderProd orderProd) { orderProdService.update(orderProd); }

    @PutMapping("/price")
    public void updatePrice(@RequestBody OrderProd orderProd) { orderProdService.updatePrice(orderProd); }

    @PutMapping("/price/shipdirect")
    public void updatePriceShipDirect(@RequestBody OrderProd orderProd) { orderProdService.updatePriceShipDirect(orderProd); }

    @PutMapping("/price/shipdirectprint")
    public void updatePriceShipDirectPrint(@RequestBody OrderProd orderProd) { orderProdService.updatePriceShipDirectPrint(orderProd); }

    @PutMapping("/price/adjustdetail")
    public void updatePriceAdjustDetail(@RequestBody OrderProd orderProd) { orderProdService.updatePriceAdjustDetail(orderProd); }

    @DeleteMapping
    public void delete(@RequestBody Map<String, Object> params) { orderProdService.delete(params); }

    @GetMapping("/mypage/list")
    public List<OrderProd> mypageSelectList(@RequestParam Map<String, Object> params) { return orderProdService.mypageSelectList(params); }

    @PutMapping("/mypage")
    public void mypageUpdate(@RequestBody Map<String, Object> params) { orderProdService.mypageUpdate(params); }

    @GetMapping("/count/cust/{custId}")
    public int countByCustId(@PathVariable Integer custId) { return orderProdService.countByCustId(custId); }

    @GetMapping("/monthly/amt/{custId}")
    public int selectMonthlyTotalAmt(@PathVariable Integer custId) { return orderProdService.selectMonthlyTotalAmt(custId); }

    @GetMapping("/monthly/amt/dept")
    public int selectMonthlyTotalAmtDeptAccnt(@RequestParam Map<String, Object> params) { return orderProdService.selectMonthlyTotalAmtDeptAccnt(params); }

    @GetMapping("/dashboard")
    public List<OrderDashBoard> selectListDashboardAnyb(@RequestParam Map<String, Object> params) { return orderProdService.selectListDashboardAnyb(params); }

    @PutMapping("/returnreason")
    public void updateReturnReason(@RequestBody OrderProd orderProd) { orderProdService.updateReturnReason(orderProd); }

    @PutMapping("/state")
    public void updateOrderState(@RequestBody Map<String, Object> params) { orderProdService.updateOrderState(params); }

    @PutMapping("/state/error")
    public void updateOrderStateError(@RequestBody Map<String, Object> params) { orderProdService.updateOrderStateError(params); }

    @PutMapping("/refundcancel")
    public void updateRefundCancel(@RequestBody Map<String, Object> params) { orderProdService.updateRefundCancel(params); }

    @PutMapping("/shopinfo")
    public void updateShopInfo(@RequestBody Map<String, Object> params) { orderProdService.updateShopInfo(params); }

    @PutMapping("/invoice")
    public void updateInvoice(@RequestBody Map<String, Object> params) { orderProdService.updateInvoice(params); }

    @PutMapping("/custconsltyn")
    public void updateCustConsltYn(@RequestBody Map<String, Object> params) { orderProdService.updateCustConsltYn(params); }

    @GetMapping("/count/state")
    public int countOrderStateDiffer(@RequestParam Map<String, Object> params) { return orderProdService.countOrderStateDiffer(params); }

    @GetMapping("/statistics/prod")
    public List<OrderDetlStats> selectListStatisticsProd(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsProd(params); }

    @GetMapping("/statistics/shop")
    public List<OrderDetlStats> selectListStatisticsShop(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsShop(params); }

    @GetMapping("/statistics/chnl")
    public List<OrderDetlStats> selectListStatisticsChnl(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnl(params); }

    @GetMapping("/statistics/chnlshop")
    public List<OrderDetlStats> selectListStatisticsChnlShop(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnlShop(params); }

    @GetMapping("/statistics/chnlprod")
    public List<OrderDetlStats> selectListStatisticsChnlProd(@RequestParam Map<String, Object> params) { return orderProdService.selectListStatisticsChnlProd(params); }
}

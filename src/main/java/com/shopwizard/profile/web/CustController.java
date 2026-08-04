package com.shopwizard.profile.web;

import com.shopwizard.profile.model.Cust;
import com.shopwizard.profile.service.CustService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/cust")
public class CustController {

    private final CustService custService;

    @GetMapping("/list")
    public List<Cust> selectList(@RequestParam Map<String, Object> params) { return custService.selectList(params); }

    @GetMapping("/count")
    public int selectCount(@RequestParam Map<String, Object> params) { return custService.selectCount(params); }

    @GetMapping("/listEmp")
    public List<Cust> selectListEmp(@RequestParam Map<String, Object> params) { return custService.selectListEmp(params); }

    @GetMapping("/{custId}")
    public Cust select(@PathVariable Integer custId) { return custService.select(custId); }

    @GetMapping("/byLoginId")
    public Cust selectByLoginId(@RequestParam Map<String, Object> params) { return custService.selectByLoginId(params); }

    @GetMapping("/forIdpw")
    public Cust selectForIdpw(@RequestParam Map<String, Object> params) { return custService.selectForIdpw(params); }

    @PostMapping
    public int insert(@RequestBody Cust cust) { return custService.insert(cust); }

    @PutMapping
    public int update(@RequestBody Cust cust) { return custService.update(cust); }

    @PutMapping("/passwd")
    public int updatePasswd(@RequestBody Cust cust) { return custService.updatePasswd(cust); }

    @PutMapping("/lastLoginTime")
    public int updateLastLoginTime(@RequestBody Cust cust) { return custService.updateLastLoginTime(cust); }

    @PutMapping("/infoOrder")
    public int updateInfoOrder(@RequestBody Map<String, Object> params) { return custService.updateInfoOrder(params); }

    @PutMapping("/infoOrderExcel")
    public int updateInfoOrderExcel(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderExcel(params); }

    @PutMapping("/infoOrderShoplinker")
    public int updateInfoOrderShoplinker(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderShoplinker(params); }

    @PutMapping("/infoShipDirect")
    public int updateInfoShipDirect(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirect(params); }

    @PutMapping("/infoShipDirectPrint")
    public int updateInfoShipDirectPrint(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectPrint(params); }

    @PutMapping("/infoReturnDirect")
    public int updateInfoReturnDirect(@RequestBody Map<String, Object> params) { return custService.updateInfoReturnDirect(params); }

    @PutMapping("/infoOrderAll")
    public int updateInfoOrderAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderAll(params); }

    @PutMapping("/infoOrderExcelAll")
    public int updateInfoOrderExcelAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderExcelAll(params); }

    @PutMapping("/infoOrderShoplinkerAll")
    public int updateInfoOrderShoplinkerAll(@RequestBody Map<String, Object> params) { return custService.updateInfoOrderShoplinkerAll(params); }

    @PutMapping("/infoShipDirectAll")
    public int updateInfoShipDirectAll(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectAll(params); }

    @PutMapping("/infoShipDirectPrintAll")
    public int updateInfoShipDirectPrintAll(@RequestBody Map<String, Object> params) { return custService.updateInfoShipDirectPrintAll(params); }

    @PutMapping("/infoReturnDirectAll")
    public int updateInfoReturnDirectAll(@RequestBody Map<String, Object> params) { return custService.updateInfoReturnDirectAll(params); }

    @DeleteMapping("/{custId}")
    public int delete(@PathVariable Integer custId) { return custService.delete(custId); }
}

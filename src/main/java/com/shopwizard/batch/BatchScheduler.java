package com.shopwizard.batch;

import com.shopwizard.adjust.service.AdjustDetailService;
import com.shopwizard.order.service.OrderBackupService;
import com.shopwizard.profile.service.CustPointDetailService;
import com.shopwizard.profile.service.CustService;
import com.shopwizard.stock.model.DailyStockState;
import com.shopwizard.stock.service.DailyStockStateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private final CustPointDetailService custPointDetailService;
    private final CustService custService;
    private final AdjustDetailService adjustDetailService;
    private final DailyStockStateService dailyStockStateService;
    private final OrderBackupService orderBackupService;

    /** 매일 02:10 — 고객 포인트 상세 스케줄 적립 */
    @Scheduled(cron = "0 10 2 * * ?")
    public void insertCustPointDetail() {
        log.info("insertCustPointDetail schedule start");
        try {
            custPointDetailService.insertSchedule(new HashMap<>());
            log.info("insertCustPointDetail schedule finished");
        } catch (Exception e) {
            log.error("insertCustPointDetail schedule error: {}", e.getMessage(), e);
        }
    }

    /** 매일 03:10 — 고객 개인정보 파기 (전체 채널) */
    @Scheduled(cron = "0 10 3 * * ?")
    public void updateCustInfoExpireAll() {
        log.info("updateCustInfoExpireAll schedule start");
        try {
            Map<String, Object> params = new HashMap<>();
            custService.updateInfoOrderAll(params);
            custService.updateInfoOrderExcelAll(params);
            custService.updateInfoOrderShoplinkerAll(params);
            custService.updateInfoShipDirectAll(params);
            custService.updateInfoShipDirectPrintAll(params);
            custService.updateInfoReturnDirectAll(params);
            log.info("updateCustInfoExpireAll schedule finished");
        } catch (Exception e) {
            log.error("updateCustInfoExpireAll schedule error: {}", e.getMessage(), e);
        }
    }

    /** 매일 04:10 — 일별 재고 출고 수량 적재 */
    @Scheduled(cron = "0 10 4 * * ?")
    public void insertDailyStockStateShipQty() {
        log.info("insertDailyStockStateShipQty schedule start");
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date today = new Date();
            String date = formatter.format(today);
            Date setDate = formatter.parse(date);

            Calendar cal = new GregorianCalendar(Locale.KOREA);
            cal.setTime(setDate);
            cal.add(Calendar.DATE, -1);
            String insertDate = formatter.format(cal.getTime());
            log.info("insertDailyStockStateShipQty insertDate={}", insertDate);

            String[] shopCodeArr = {
                "3018508386A1",
                "1258100201A1",
                "4038516353A1"
            };

            Map<String, Object> map = new HashMap<>();
            map.put("pOrderStartDate", insertDate);
            map.put("pOrderEndDate", insertDate);

            for (String shopCode : shopCodeArr) {
                map.put("pShopCode", shopCode);
                List<DailyStockState> list = dailyStockStateService.selectListShipDirect(map);
                log.info("pShopCode={} list.size={}", shopCode, list.size());
                for (DailyStockState state : list) {
                    state.setShipQty(state.getShipCheckQty());
                    state.setReEnterQty(state.getReEnterCheckQty());
                    state.setRegistId("system");
                    state.setRegistName("schedule");
                    try {
                        dailyStockStateService.insertShipQty(state);
                    } catch (Exception e) {
                        log.error("insertShipQty error: {}", e.getMessage(), e);
                    }
                }
            }
            log.info("insertDailyStockStateShipQty schedule finished ({})", insertDate);
        } catch (Exception e) {
            log.error("insertDailyStockStateShipQty schedule error: {}", e.getMessage(), e);
        }
    }

    /** 매월 1일 05:10 — 고객 개인정보 파기 (특정 채널) */
    @Scheduled(cron = "0 10 5 1 * ?")
    public void updateCustInfoExpire() {
        log.info("updateCustInfoExpire schedule start");
        try {
            Map<String, Object> params = new HashMap<>();
            custService.updateInfoOrder(params);
            custService.updateInfoOrderExcel(params);
            custService.updateInfoOrderShoplinker(params);
            custService.updateInfoShipDirect(params);
            custService.updateInfoShipDirectPrint(params);
            custService.updateInfoReturnDirect(params);
            log.info("updateCustInfoExpire schedule finished");
        } catch (Exception e) {
            log.error("updateCustInfoExpire schedule error: {}", e.getMessage(), e);
        }
    }

    /** 매월 2일 05:10 — 주문 데이터 백업 (1년 이전 데이터) */
    @Scheduled(cron = "0 10 5 2 * ?")
    public void orderBackup() {
        log.info("orderBackup schedule start");
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -1);
            cal.add(Calendar.MONTH, -1);
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1);
            String startDate = formatter.format(cal.getTime());
            cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String endDate = formatter.format(cal.getTime());
            log.info("orderBackup range={} ~ {}", startDate, endDate);

            orderBackupService.insertBackupStatics(startDate, endDate);

            orderBackupService.deleteTempOrderNo();
            orderBackupService.insertTempOrderNo(startDate, endDate);

            orderBackupService.deleteBackupOrderShoplinkerDuplicate();
            orderBackupService.insertBackupOrderShoplinker();
            orderBackupService.deleteBackupOrderShoplinker();

            orderBackupService.deleteBackupOrderPayDuplicate();
            orderBackupService.insertBackupOrderPay();
            orderBackupService.deleteBackupOrderPay();

            orderBackupService.deleteBackupOrderDeliFeeDuplicate();
            orderBackupService.insertBackupOrderDeliFee();
            orderBackupService.deleteBackupOrderDeliFee();

            orderBackupService.deleteBackupOrderChangeNoDuplicate();
            orderBackupService.insertBackupOrderChangeNo();
            orderBackupService.deleteBackupOrderChangeNo();

            orderBackupService.deleteBackupOrderShipDirectDuplicate();
            orderBackupService.insertBackupOrderShipDirect();
            orderBackupService.deleteBackupOrderShipDirect();

            orderBackupService.deleteBackupOrderShipDirectPrintDuplicate();
            orderBackupService.insertBackupOrderShipDirectPrint();
            orderBackupService.deleteBackupOrderShipDirectPrint();

            orderBackupService.deleteBackupOrderReturnDirectDuplicate();
            orderBackupService.insertBackupOrderReturnDirect();
            orderBackupService.deleteBackupOrderReturnDirect();

            orderBackupService.deleteBackupOrderDuplicate();
            orderBackupService.insertBackupOrder();
            orderBackupService.deleteBackupOrder();

            orderBackupService.deleteBackupOrderProdDuplicate();
            orderBackupService.insertBackupOrderProd();
            orderBackupService.deleteBackupOrderProd();

            log.info("orderBackup schedule finished ({} ~ {})", startDate, endDate);
        } catch (Exception e) {
            log.error("orderBackup schedule error: {}", e.getMessage(), e);
        }
    }

    /** 매일 01:10 — 정산 상세 스케줄 적재 (비활성 → 필요 시 활성화) */
    // @Scheduled(cron = "0 10 1 * * ?")
    public void insertAdjustDetail() {
        log.info("insertAdjustDetail schedule start");
        try {
            adjustDetailService.insertSchedule(new HashMap<>());
            log.info("insertAdjustDetail schedule finished");
        } catch (Exception e) {
            log.error("insertAdjustDetail schedule error: {}", e.getMessage(), e);
        }
    }
}

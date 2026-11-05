package com.shopwizard.company.service;

import com.shopwizard.company.mapper.ShopMapper;
import com.shopwizard.company.model.BusinessInfoView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessInfoServiceTest {

    private final ShopMapper mapper = mock(ShopMapper.class);
    private final BusinessInfoService service = new BusinessInfoService(mapper, "CH1");

    @Test
    void 자사몰_채널로_조회하고_사업자번호_법인번호를_표시_형식으로_바꾼다() {
        BusinessInfoView v = new BusinessInfoView();
        v.setCompName("샵피온(주)");
        v.setBizNo("1208674775");
        v.setCorpNo("1101113114230");
        when(mapper.selectBusinessInfo("CH1")).thenReturn(v);

        BusinessInfoView out = service.get();

        assertEquals("120-86-74775", out.getBizNo());
        assertEquals("110111-3114230", out.getCorpNo());
        assertEquals("샵피온(주)", out.getCompName());
    }

    @Test
    void 형식이_다른_번호는_그대로_두고_null_도_안전하다() {
        assertEquals("ABC-1", BusinessInfoService.formatBizNo("ABC-1"));
        assertEquals("120-86-74775", BusinessInfoService.formatBizNo("120-86-74775"));
        assertNull(BusinessInfoService.formatBizNo(null));
        assertNull(BusinessInfoService.formatCorpNo(null));
        assertEquals("123", BusinessInfoService.formatCorpNo("123"));
    }

    @Test
    void 회사를_못_찾으면_빈_뷰를_돌려준다_예외가_아니다() {
        when(mapper.selectBusinessInfo("CH1")).thenReturn(null);
        BusinessInfoView out = service.get();
        assertNotNull(out);
        assertNull(out.getCompName());
    }
}

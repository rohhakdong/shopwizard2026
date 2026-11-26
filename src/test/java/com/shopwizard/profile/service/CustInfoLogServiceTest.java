package com.shopwizard.profile.service;

import com.shopwizard.profile.mapper.CustInfoLogMapper;
import com.shopwizard.profile.model.CustInfoLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 개인정보 조회 기록 — 레거시와 같은 열을 채우고 열 길이에 맞춰 자른다(길어서 INSERT 가 실패하지 않게). */
class CustInfoLogServiceTest {

    @Test
    void 기록은_레거시와_같은_값을_채우고_긴_값은_열_길이로_자른다() {
        CustInfoLogMapper mapper = mock(CustInfoLogMapper.class);
        CustInfoLogService service = new CustInfoLogService(mapper);

        service.record("/ship/print/list", "x".repeat(600), "svc(map)", "dao", "tShpShipDirectPrint", "u".repeat(80), "이름", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        ArgumentCaptor<CustInfoLog> cap = ArgumentCaptor.forClass(CustInfoLog.class);
        verify(mapper).insert(cap.capture());
        CustInfoLog log = cap.getValue();
        assertThat(log.getCustId()).isZero();
        assertThat(log.getState()).isZero();
        assertThat(log.getMenuName()).isEqualTo("/ship/print/list");
        assertThat(log.getExecCntnts()).hasSize(512);
        assertThat(log.getMngrLoginId()).hasSize(50);
        assertThat(log.getMngrUid()).hasSize(50);
        assertThat(log.getMngrIp()).hasSize(20);
        assertThat(log.getMngrName()).isEqualTo("이름");
        assertThat(log.getRegistId()).hasSize(50);
    }

    @Test
    void 행위자가_없어도_NOT_NULL_열은_빈_문자열로_채운다() {
        CustInfoLogMapper mapper = mock(CustInfoLogMapper.class);
        new CustInfoLogService(mapper).record("/x", null, "s", "d", "t", null, null, null);

        ArgumentCaptor<CustInfoLog> cap = ArgumentCaptor.forClass(CustInfoLog.class);
        verify(mapper).insert(cap.capture());
        assertThat(cap.getValue().getMngrUid()).isEmpty();
        assertThat(cap.getValue().getMngrLoginId()).isEmpty();
        assertThat(cap.getValue().getMngrName()).isEmpty();
    }
}

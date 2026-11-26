/**
 * 발주내역2 (사이드 메뉴 발주) — 레거시 발주내역2(wShpShipDirectPrint2List). 상점의 발주서 행을 사은품 행을 빼고
 *   채널상품번호·사은품·옵션금액·단량과 함께 본다(발주일 또는 주문일 기준). 엑셀은 한 시트.
 *   [발주내역2생성]은 상점·발주일 기간의 발주서 행에 샵링커 채널상품번호를 채우고, 사은품 행의 "상품명-수량" 문자열(사은품 열)과
 *   주문 옵션의 추가금액(옵션금액)을 다시 계산해 쓴다. 쓰기 전에 확인을 받고, 결과·이력을 남긴다.
 *   조회·엑셀은 개인정보 조회 기록이 남는다(서버가 기록).
 *   백엔드: /ship/print (list2, excel2, gift-option-run, gift-option-runs, gift-option-run-details)
 */
const PageShipPrintDetail = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 50;
  let currentPage = 1;
  let seq = 0;   // 늦게 도착한 이전 요청의 응답이 최신 결과를 덮어쓰지 않게 하는 순번

  const COLS = [
    ['주문번호', r => r.orderNo], ['순번', r => r.orderProdNo], ['변경', r => r.orderChangeNo], ['채널주문번호', r => r.chnlOrderNo],
    ['주문일자', r => r.orderReciptDate], ['발주월', r => r.shipMonth], ['일', r => r.shipDay], ['수령인', r => r.recverName],
    ['주문상태', r => r.orderState], ['상품코드', r => r.shopProdCode], ['채널상품번호', r => r.prodMemo01], ['상품명', r => r.orderProdName],
    ['속성명', r => r.prodName], ['단량', r => r.unitName], ['사은품', r => r.itemName],
    ['구성수량', r => num(r.compQty), 'n'], ['수량', r => num(r.orderProdQty), 'n'], ['낱개수량', r => num(r.prodQty), 'n'],
    ['금액', r => num(r.salePrice), 'n'], ['옵션금액', r => num(r.nvPrice), 'n'], ['판매금액', r => num(r.totalPrice), 'n'], ['채널공급가', r => num(r.supplyPrice), 'n'],
    ['공급단가', r => num(r.unitPrice), 'n'], ['공급소계', r => num(r.subTotal), 'n'], ['배송비', r => num(r.deliFeeAmt), 'n'],
    ['배송유형', r => r.deliFeeType], ['채널명', r => r.chnlName], ['합배송', r => (r.bundleYn ? '합배송' : '')],
  ];

  function num(n) { return Number(n) || 0; }
  function today() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function el(id) { return document.getElementById(id); }

  function render(container) {
    currentPage = 1;
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>발주내역2 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 사은품 행 제외, 채널상품번호·사은품·옵션금액 포함</span></span>
          <span>
            <button class="btn btn-ghost" id="pdBtnHistory" type="button">생성 이력</button>
            <button class="btn btn-ghost" id="pdBtnRun" type="button">발주내역2생성</button>
            <button class="btn btn-primary" id="pdBtnExcel" type="button">엑셀 받기</button>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            수령인 정보가 담긴 엑셀을 받을 수 있는 화면입니다. 조회와 엑셀 받기는 <b>개인정보 조회 기록</b>으로 남습니다.
            <b>발주내역2생성</b>은 선택한 상점·발주일 기간의 발주서 행에 채널상품번호·사은품·옵션금액을 다시 채웁니다(여러 번 실행해도 같은 결과).
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>날짜 기준</label>
              <select class="input" id="pdDateType" style="width:90px"><option value="S">발주일</option><option value="O">주문일</option></select></div>
            <div class="form-group"><label>시작</label><input class="input" id="pdStart" type="date" style="width:140px" value="${today()}"></div>
            <div class="form-group"><label>종료</label><input class="input" id="pdEnd" type="date" style="width:140px" value="${today()}"></div>
            <div class="form-group"><label>상점 *</label>
              <select class="input" id="pdShop" style="width:180px"><option value="">선택</option></select></div>
            <div class="form-group"><label>채널</label>
              <select class="input" id="pdChnl" style="width:150px"><option value="">전체</option></select></div>
            <div class="form-group"><label>담당MD</label>
              <select class="input" id="pdMd" style="width:100px"><option value="">전체</option></select></div>
            <div class="form-group"><label>속성명</label><input class="input" id="pdProdName" style="width:140px"></div>
            <button class="btn btn-primary" id="pdBtnSearch" type="button">검색</button>
          </div>
          <div id="pdRunResult"></div>
          <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="pdTotal">0</strong>건</div>
          <div class="table-wrap" id="pdWrap"><div style="text-align:center;padding:40px;color:var(--text-muted)">상점을 선택하고 검색하세요</div></div>
          <div id="pdPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    el('pdBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    el('pdProdName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    el('pdBtnExcel').addEventListener('click', () => {
      if (!el('pdShop').value) return UI.toast('상점을 선택하세요', 'error');
      PageShipPrintList.download('/ship/print/excel2', params());
    });
    el('pdBtnRun').addEventListener('click', openRunModal);
    el('pdBtnHistory').addEventListener('click', openHistoryModal);
    loadFilterOptions();
  }

  async function loadFilterOptions() {
    const { chnls, shops, mds } = await UI.loadFilterOptions(svcCode());
    UI.fillOptions('pdShop', shops.map(s => ({ v: s.shopCode, t: s.shopName })));
    UI.fillOptions('pdChnl', chnls.map(c => ({ v: c.chnlCode, t: c.chnlName })));
    UI.fillOptions('pdMd', mds.map(m => ({ v: m, t: m })));
  }

  function params() {
    return {
      pShopCode: el('pdShop').value, pDateType: el('pdDateType').value, pStartDate: el('pdStart').value, pEndDate: el('pdEnd').value,
      pChnlCode: el('pdChnl').value, pShopMDCode: el('pdMd').value, pProdName: el('pdProdName').value.trim(),
    };
  }

  async function loadList() {
    const wrap = el('pdWrap');
    if (!el('pdShop').value) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">상점을 선택하세요</div>'; return; }
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    el('pdPage').innerHTML = '';
    const mine = ++seq;
    try {
      const res = await Api.get('/ship/print/list2', { ...params(), pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      if (mine !== seq) return;
      el('pdTotal').textContent = Number(res.total).toLocaleString();
      if (!res.total) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>'; return; }
      wrap.innerHTML = `
        <table style="width:100%;font-size:12px;white-space:nowrap">
          <thead><tr>${COLS.map(c => `<th${c[2] === 'n' ? ' style="text-align:right"' : ''}>${c[0]}</th>`).join('')}</tr></thead>
          <tbody>${res.rows.map(r => `<tr>${COLS.map(c => {
            const v = c[1](r);
            return c[2] === 'n' ? `<td style="text-align:right">${Number(v).toLocaleString()}</td>` : `<td>${esc(v)}</td>`;
          }).join('')}</tr>`).join('')}</tbody>
        </table>`;
      UI.pagination(el('pdPage'), { total: res.total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      if (mine !== seq) return;
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  // ── 발주내역2생성 ───────────────────────────────────────────────────
  function openRunModal() {
    const shopOpts = [...el('pdShop').options].filter(o => o.value)
      .map(o => `<option value="${esc(o.value)}" ${o.value === el('pdShop').value ? 'selected' : ''}>${esc(o.textContent)}</option>`).join('');
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-group"><label>상점 *</label>
        <select class="input" id="pdRShop"><option value="">선택</option>${shopOpts}</select></div>
      <div style="display:flex;gap:8px;margin-top:8px">
        <div class="form-group" style="flex:1"><label>발주일 시작 *</label><input class="input" id="pdRStart" type="date" value="${today()}"></div>
        <div class="form-group" style="flex:1"><label>발주일 종료 *</label><input class="input" id="pdREnd" type="date" value="${today()}"></div>
      </div>
      <div style="font-size:12px;color:var(--text-muted);margin-top:10px;line-height:1.7">
        기간 안에 발주확인된 주문의 발주서 행에 <b>채널상품번호</b>(샵링커 값)를 채우고, <b>사은품</b> 열에 사은품 "상품명-수량" 을,
        <b>옵션금액</b>에 주문 옵션의 추가금액을 씁니다. 사은품 열(속성명)에 이미 다른 값이 있는 주문은 건드리지 않습니다. 한 번에 31일까지.
      </div>`;
    UI.modal({
      title: '발주내역2생성', body, confirmText: '생성',
      onConfirm: async close => {
        const shopCode = body.querySelector('#pdRShop').value;
        const startDate = body.querySelector('#pdRStart').value;
        const endDate = body.querySelector('#pdREnd').value;
        if (!shopCode) return UI.toast('상점을 선택하세요', 'error');
        if (!startDate || !endDate) return UI.toast('기간을 입력하세요', 'error');
        const btn = body.closest('.modal')?.querySelector('[data-action=confirm]');
        if (btn) btn.disabled = true;
        try {
          const r = await Api.post('/ship/print/gift-option-run', { shopCode, startDate, endDate });
          close();
          showRunResult(r);
          UI.toast(r.resultType === '성공' ? '발주내역2생성을 마쳤습니다' : '발주내역2생성이 실패했습니다', r.resultType === '성공' ? 'success' : 'error');
          if (el('pdShop').value === shopCode) loadList();
        } catch (e) {
          if (btn) btn.disabled = false;
          UI.toast(e.message, 'error');
        }
      },
    });
  }

  function showRunResult(r) {
    const ok = r.resultType === '성공';
    el('pdRunResult').innerHTML = `
      <div style="border:1px solid ${ok ? '#bbf7d0' : '#fecaca'};background:${ok ? '#f0fdf4' : '#fef2f2'};border-radius:var(--radius);padding:8px 12px;margin-top:10px;font-size:12px;line-height:1.7">
        <b>발주내역2생성 #${esc(r.updateId)} — ${esc(r.resultType)}</b> · 주문 ${Number(r.total).toLocaleString()}건 (성공 ${r.success} · 무시 ${r.ignore} · 오류 ${r.error})
        <div>${esc(r.resultMessag)}</div>
        ${(r.details || []).length ? `<details style="margin-top:4px"><summary>오류·무시 ${r.details.length}건${r.error + r.ignore > r.details.length ? '(앞부분만, 전체는 [생성 이력])' : ''}</summary>
          <table style="font-size:12px;margin-top:4px"><tbody>${r.details.map(d => `<tr><td>${esc(d.orderNo)}</td><td>${esc(d.resultType)}</td><td>${esc(d.message)}</td></tr>`).join('')}</tbody></table></details>` : ''}
      </div>`;
  }

  async function openHistoryModal() {
    const shopCode = el('pdShop').value;
    if (!shopCode) return UI.toast('상점을 선택하세요', 'error');
    const body = document.createElement('div');
    body.innerHTML = '<div style="padding:12px;color:var(--text-muted)">불러오는 중...</div>';
    UI.modal({ title: '발주내역2생성 이력 (최근 30건)', body, confirmText: null, cancelText: '닫기' });
    try {
      const runs = await Api.get('/ship/print/gift-option-runs', { pShopCode: shopCode });
      if (!runs.length) { body.innerHTML = '<div style="padding:12px;color:var(--text-muted)">이 상점에서 실행한 이력이 없습니다.</div>'; return; }
      body.innerHTML = `
        <table style="width:100%;font-size:12px">
          <thead><tr><th>번호</th><th>기간(발주일)</th><th>결과</th><th>실행자</th><th>시각</th><th>내용</th><th></th></tr></thead>
          <tbody>${runs.map(r => `<tr><td>${esc(r.updateId)}</td><td>${esc(r.startDate)} ~ ${esc(r.endDate)}</td><td>${esc(r.resultType)}</td>
            <td>${esc(r.registName)}</td><td>${esc((r.registDate || '').replace('T', ' ').slice(0, 16))}</td><td>${esc(r.resultMessag)}</td>
            <td><button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-run="${esc(r.updateId)}" type="button">상세</button></td></tr>`).join('')}</tbody>
        </table><div id="pdRunDetail" style="margin-top:10px"></div>`;
      body.querySelectorAll('[data-run]').forEach(b => b.addEventListener('click', async () => {
        const box = body.querySelector('#pdRunDetail');
        box.innerHTML = '<div style="color:var(--text-muted)">불러오는 중...</div>';
        try {
          const list = await Api.get('/ship/print/gift-option-run-details', { pShopCode: shopCode, pUpdateId: b.dataset.run });
          box.innerHTML = `<div style="font-size:12px;margin-bottom:4px">실행 #${esc(b.dataset.run)} — 주문 ${list.length}건</div>
            <div style="max-height:240px;overflow:auto"><table style="width:100%;font-size:12px"><thead><tr><th>순번</th><th>주문번호</th><th>결과</th><th>내용</th></tr></thead>
            <tbody>${list.map(d => `<tr><td>${esc(d.updateDetailNo)}</td><td>${esc(d.orderNo)}</td><td>${esc(d.resultType)}</td><td>${esc(d.resultMessag)}</td></tr>`).join('')}</tbody></table></div>`;
        } catch (e) { box.innerHTML = `<div style="color:var(--danger)">${esc(e.message)}</div>`; }
      }));
    } catch (e) { body.innerHTML = `<div style="color:var(--danger);padding:12px">${esc(e.message)}</div>`; }
  }

  return { render };
})();

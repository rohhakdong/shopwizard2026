/**
 * 자동발주 (배송 > 자동발주) — 레거시 자동발주 팝업(autoOrderWork.jsp)의 발주서 파이프라인.
 *   ① 상점 선택 + 현황  →  ② 점검(상품·발주단가 미매칭)  →  ②-2 수수료 규칙 적용  →  ③ 출고지시(기존 issue-batch, 발주서 행 생성)
 *   →  ④ 배송 데이터 생성(출고지·사은품 규칙·묶음배송·출력순서)  →  ⑤ 발주서 엑셀 + 발주확인  →  ⑥ 발주 목록(다시 받기)
 *
 *   각 단계는 순서를 강제하지 않고 그 시점의 데이터로 독립 실행된다(레거시도 단계 게이트가 없었다).
 *   발주서 엑셀은 조회성(상태를 바꾸지 않음)이고 발주확인은 별도 버튼(POST)이다 — 화면에 불러온 라인 그대로 확인하므로
 *   "내려받은 발주서 = 확인하는 건" 이다. 사은품은 사용 중인 사은품규칙이 ④에서 발주서 행으로 만들어진다.
 *
 *   백엔드: /ship/auto-order (summary, build, sheet, pending-lines, sheet-excel), /ship/ship-direct (issue-batch, check-batch),
 *           /ship/ship-check/list, 출고지시 대상 목록 /order/orderprod/list, 상점 /company/shop
 */
const PageShipAutoOrder = (() => {

  const ISSUE_PAGE = 20;
  const SHEET_PAGE = 50;
  const SELECT_ALL_MAX = 500;

  let shopList = [];
  let shopCode = '';
  let summary = null;
  let issuePage = 1;
  let issueTotal = 0;
  let sheetPage = 1;
  let pendingLines = [];            // 화면에 불러온 발주확인 대상(주문번호·라인번호)
  const selected = new Map();       // 출고지시 선택 — "orderNo:orderProdNo"

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function num(n) { return Number(n) || 0; }
  function key(o) { return `${o.orderNo}:${o.orderProdNo}`; }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function el(id) { return document.getElementById(id); }
  function goto(menuKey, label) { if (typeof navigate === 'function') navigate(menuKey, { key: menuKey, label }); }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    selected.clear();
    summary = null;
    pendingLines = [];
    issuePage = 1;
    sheetPage = 1;
    container.innerHTML = `
      <div class="card">
        <div class="card-header">자동발주 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 상점 단위로 출고지시 → 배송 데이터 생성 → 발주서 출력·발주확인</span></div>
        <div class="card-body" style="padding:12px 16px">
          <div class="search-bar" style="gap:8px;align-items:flex-end">
            <div class="form-group"><label>① 상점 (자동발주 대상)</label>
              <select class="input" id="aoShop" style="width:240px"><option value="">선택</option></select></div>
            <button class="btn btn-ghost" id="aoRefresh" type="button">새로고침</button>
          </div>
          <div id="aoSummary" style="margin-top:10px"></div>
        </div>
      </div>
      <div class="card" id="aoCheckCard" style="display:none">
        <div class="card-header">② 점검</div>
        <div class="card-body" style="padding:12px 16px" id="aoCheck"></div>
      </div>
      <div class="card" id="aoFeeCard" style="display:none">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>②-2 수수료 규칙 적용 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 지불완료 주문의 배송비·판촉비·공급가를 수수료규칙대로 고칩니다(출고지시 전에)</span></span>
          <button class="btn btn-primary" id="aoApplyFee" type="button">수수료 규칙 적용</button>
        </div>
        <div class="card-body" style="padding:12px 16px;font-size:12px;line-height:1.7">
          <div id="aoFeeInfo"></div>
          <div id="aoFeeResult" style="margin-top:6px"></div>
        </div>
      </div>
      <div class="card" id="aoIssueCard" style="display:none">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>③ 출고지시 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 처리하면 발주서 행이 만들어집니다(발주단가매칭 규칙 줄마다 1행)</span></span>
          <span>
            <button class="btn btn-ghost" id="aoSelAll" type="button">상점 전체 선택(최대 ${SELECT_ALL_MAX}건)</button>
            <button class="btn btn-primary" id="aoIssue" type="button" disabled>출고지시 처리 (<span id="aoSelCount">0</span>건)</button>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div id="aoIssueWrap"></div>
          <div id="aoIssuePager" style="margin-top:10px"></div>
        </div>
      </div>
      <div class="card" id="aoBuildCard" style="display:none">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>④ 배송 데이터 생성 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 출고지 채우기 · 사은품규칙 적용 · 합배송 표시 · 출력순서</span></span>
          <button class="btn btn-primary" id="aoBuild" type="button">배송 데이터 생성</button>
        </div>
        <div class="card-body" style="padding:12px 16px;font-size:12px;line-height:1.7">
          출고지시 후 발주서를 받기 전에 실행하세요. 몇 번을 다시 실행해도 같은 결과가 됩니다(사은품 행은 매번 새로 만듭니다).
          <div id="aoBuildResult" style="margin-top:6px"></div>
        </div>
      </div>
      <div class="card" id="aoSheetCard" style="display:none">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>⑤ 발주서 출력 · 발주확인</span>
          <span>
            <button class="btn btn-ghost" id="aoDownload" type="button">발주서 엑셀 받기</button>
            <button class="btn btn-primary" id="aoConfirm" type="button" disabled>발주확인 처리 (<span id="aoConfirmCount">0</span>건)</button>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div id="aoSheetNote" style="font-size:12px;margin-bottom:8px"></div>
          <div id="aoSheetWrap"></div>
          <div id="aoSheetPager" style="margin-top:10px"></div>
        </div>
      </div>
      <div class="card" id="aoHistoryCard" style="display:none">
        <div class="card-header">⑥ 발주 목록 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 발주확인한 묶음(최근 10건), 다시 받을 수 있습니다</span></div>
        <div class="card-body" style="padding:12px 16px"><div id="aoHistory"></div></div>
      </div>`;

    el('aoShop').addEventListener('change', onShopChange);
    el('aoRefresh').addEventListener('click', () => { if (shopCode) loadAll(); });
    el('aoSelAll').addEventListener('click', selectAllShop);
    el('aoIssue').addEventListener('click', issueSelected);
    el('aoApplyFee').addEventListener('click', applyFee);
    el('aoBuild').addEventListener('click', buildShipData);
    el('aoDownload').addEventListener('click', () => download({ pShopCode: shopCode }));
    el('aoConfirm').addEventListener('click', confirmOrders);
    loadShops();
  }

  async function loadShops() {
    try {
      const shops = await Api.get('/company/shop', { pAutoOrderYn: 1, pState: 1 });
      shopList = (shops || []).filter(s => !s.svcCode || s.svcCode === svcCode())
        .sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
      el('aoShop').insertAdjacentHTML('beforeend', shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
      if (shopList.length === 0) el('aoSummary').innerHTML = '<div style="color:var(--text-muted);font-size:12px">자동발주 대상 상점이 없습니다. 거래처 &gt; 상점 관리에서 "자동발주"를 예로 설정하세요.</div>';
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  function onShopChange() {
    shopCode = el('aoShop').value;
    selected.clear();
    issuePage = 1;
    sheetPage = 1;
    ['aoCheckCard', 'aoFeeCard', 'aoIssueCard', 'aoBuildCard', 'aoSheetCard', 'aoHistoryCard'].forEach(id => { el(id).style.display = shopCode ? '' : 'none'; });
    el('aoBuildResult').innerHTML = '';
    el('aoFeeResult').innerHTML = '';
    if (!shopCode) { el('aoSummary').innerHTML = ''; return; }
    loadAll();
  }

  async function loadAll() {
    await loadSummary();
    loadIssueList();
    loadSheet();
    loadHistory();
  }

  // ── ① 현황 · ② 점검 ────────────────────────────────────────────────
  async function loadSummary() {
    try {
      summary = await Api.get('/ship/auto-order/summary', { pShopCode: shopCode });
    } catch (e) { UI.toast(e.message, 'error'); return; }
    const tile = (label, n, warn) => `
      <div style="flex:1;min-width:130px;border:1px solid var(--border);border-radius:var(--radius);padding:8px 12px;${warn && n > 0 ? 'border-color:#f59e0b;background:#fffbeb' : ''}">
        <div style="font-size:11px;color:var(--text-muted)">${label}</div><div style="font-size:20px;font-weight:600">${won(n)}</div></div>`;
    el('aoSummary').innerHTML = `
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        ${tile('지불완료', num(summary.payCompleteCnt))}
        ${tile('상품코드 미매칭', num(summary.prodUnmatchedCnt), true)}
        ${tile('발주단가 미매칭', num(summary.upriceUnmatchedCnt), true)}
        ${tile('출고지시(발주확인 전)', num(summary.issuedCnt))}
        ${tile('발주서 대기(주문 라인)', num(summary.sheetLineCnt))}
      </div>`;

    const items = [];
    if (num(summary.prodUnmatchedCnt) > 0) {
      items.push(`상품코드가 없는 지불완료 주문이 <b>${won(summary.prodUnmatchedCnt)}건</b> 있습니다. <button class="btn btn-ghost" data-go="order-prod-match" data-label="상품 매칭" type="button" style="font-size:11px">상품 매칭 열기</button>`);
    }
    if (num(summary.upriceUnmatchedCnt) > 0) {
      items.push(`발주단가매칭 규칙이 없는 지불완료 주문이 <b>${won(summary.upriceUnmatchedCnt)}건</b> 있습니다 — 규칙이 없으면 출고지시가 거절됩니다. <button class="btn btn-ghost" data-go="order-uprice-match" data-label="발주단가매칭" type="button" style="font-size:11px">발주단가매칭 열기</button>`);
    }
    el('aoCheck').innerHTML = items.length
      ? items.map(h => `<div style="font-size:12px;line-height:2">⚠ ${h}</div>`).join('')
      : '<div style="font-size:12px;color:var(--text-muted)">점검할 미매칭 항목이 없습니다.</div>';
    el('aoFeeInfo').innerHTML = num(summary.feeRuleCnt) > 0
      ? `이 상점의 수수료규칙 <b>${won(summary.feeRuleCnt)}건</b>을 규칙 번호 순으로 적용합니다(나중 규칙이 앞 규칙을 덮어쓰고, 사용 여부와 상관없이 전부 적용). <button class="btn btn-ghost" data-go="order-fee-rule" data-label="수수료규칙" type="button" style="font-size:11px">수수료규칙 열기</button>`
      : '이 상점에는 수수료규칙이 없습니다 — 적용해도 바뀌는 것이 없습니다. <button class="btn btn-ghost" data-go="order-fee-rule" data-label="수수료규칙" type="button" style="font-size:11px">수수료규칙 열기</button>';
    el('aoFeeInfo').querySelectorAll('[data-go]').forEach(b => b.addEventListener('click', () => goto(b.dataset.go, b.dataset.label)));
    el('aoCheck').querySelectorAll('[data-go]').forEach(b => b.addEventListener('click', () => goto(b.dataset.go, b.dataset.label)));
  }

  // ── ③ 출고지시 ─────────────────────────────────────────────────────
  function issueParams() {
    // pProdJoin=LEFT: 상품코드가 아직 없는 지불완료 주문도 목록에 나와야 한다(점검 ②의 미매칭 건수와 같은 대상).
    return { pOrderState: '지불완료', pShopCode: shopCode, pSvcCode: svcCode(), pProdJoin: 'LEFT' };
  }

  async function loadIssueList() {
    const wrap = el('aoIssueWrap');
    wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted)">불러오는 중...</div>';
    el('aoIssuePager').innerHTML = '';
    try {
      issueTotal = await Api.get('/order/orderprod/list/count', issueParams());
      if (!issueTotal) {
        wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted)">이 상점의 지불완료 주문이 없습니다</div>';
        updateIssueButton();
        return;
      }
      const list = await Api.get('/order/orderprod/list', { ...issueParams(), topCnt: (issuePage - 1) * ISSUE_PAGE, countPerPage: ISSUE_PAGE });
      renderIssueTable(list);
      UI.pagination(el('aoIssuePager'), { total: issueTotal, page: issuePage, pageSize: ISSUE_PAGE, onChange: p => { issuePage = p; loadIssueList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:12px">${esc(e.message)}</div>`;
    }
  }

  function renderIssueTable(list) {
    el('aoIssueWrap').innerHTML = `
      <div class="table-wrap"><table style="width:100%;font-size:12px">
        <thead><tr><th style="width:30px"><input type="checkbox" id="aoIssueAll"></th><th>주문번호</th><th>순번</th><th>상품명</th><th>옵션</th>
          <th style="text-align:right">수량</th><th>수취인</th><th>채널</th></tr></thead>
        <tbody>${list.map((o, i) => `
          <tr><td style="text-align:center"><input type="checkbox" class="ao-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
            <td>${esc(o.orderNo)}</td><td>${esc(o.orderProdNo)}</td><td style="max-width:280px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.prodName)}">${esc(o.prodName)}</td>
            <td style="color:var(--text-muted)">${esc(o.itemName)}</td><td style="text-align:right">${esc(o.prodQty)}</td>
            <td>${esc(o.recverName)}</td><td>${esc(o.chnlName)}</td></tr>`).join('')}</tbody>
      </table></div>`;
    const wrap = el('aoIssueWrap');
    wrap.querySelectorAll('.ao-row').forEach(cb => cb.addEventListener('change', () => {
      const o = list[+cb.dataset.i];
      if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
      el('aoIssueAll').checked = list.every(x => selected.has(key(x)));
      updateIssueButton();
    }));
    el('aoIssueAll').checked = list.length > 0 && list.every(x => selected.has(key(x)));
    el('aoIssueAll').addEventListener('change', e => {
      wrap.querySelectorAll('.ao-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateIssueButton();
    });
    updateIssueButton();
  }

  function updateIssueButton() {
    el('aoSelCount').textContent = selected.size;
    el('aoIssue').disabled = selected.size === 0;
  }

  async function selectAllShop() {
    try {
      const list = await Api.get('/order/orderprod/list', { ...issueParams(), topCnt: 0, countPerPage: SELECT_ALL_MAX });
      list.forEach(o => selected.set(key(o), o));
      UI.toast(`${list.length}건을 선택했습니다${issueTotal > list.length ? ` (전체 ${won(issueTotal)}건 중 앞 ${SELECT_ALL_MAX}건)` : ''}`, 'success');
      loadIssueList();
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  function issueSelected() {
    const lines = [...selected.values()];
    if (!lines.length) return;
    UI.confirm(`선택한 ${lines.length}건을 출고지시 처리하시겠습니까? 처리하면 발주서 행이 만들어지고 발주확인 대기가 됩니다.`, async close => {
      el('aoIssue').disabled = true;
      try {
        const res = await Api.post('/ship/ship-direct/issue-batch', {
          lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo })),
          registId: info?.loginId || '', registName: info?.name || '',
        });
        if (res.failed > 0) UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
        else UI.toast(`${res.success}건 출고지시 처리되었습니다`, 'success');
        selected.clear();
        close();
        loadAll();
      } catch (e) { UI.toast(e.message, 'error'); updateIssueButton(); }
    });
  }

  // ── ②-2 수수료 규칙 적용 ───────────────────────────────────────────
  async function applyFee() {
    const btn = el('aoApplyFee');
    btn.disabled = true;
    try {
      const r = await Api.post('/ship/auto-order/apply-fee', { shopCode });
      el('aoFeeResult').innerHTML = `<span style="color:var(--success,#2e7d32)">✔ 완료</span> — 규칙 <b>${won(r.rules)}</b>건, 대상 지불완료 라인 <b>${won(r.lines)}</b>건 중 <b>${won(r.changed)}</b>건 변경
        (묶음 배송비 0 처리 ${won(r.bundleZeroed)}건${r.skipped ? `, 그 사이 상태가 바뀌어 건너뜀 ${won(r.skipped)}건` : ''})`;
      UI.toast('수수료 규칙을 적용했습니다', 'success');
      loadSummary();
    } catch (e) { UI.toast(e.message, 'error'); } finally { btn.disabled = false; }
  }

  // ── ④ 배송 데이터 생성 ─────────────────────────────────────────────
  async function buildShipData() {
    const btn = el('aoBuild');
    btn.disabled = true;
    try {
      const r = await Api.post('/ship/auto-order/build', { shopCode });
      el('aoBuildResult').innerHTML = `<span style="color:var(--success,#2e7d32)">✔ 완료</span> — 발주서 행 <b>${won(r.rows)}</b>개(사은품 <b>${won(r.giftRows)}</b>개 포함),
        합배송 표시 <b>${won(r.bundledRows)}</b>행, 출고지 <b>${esc(r.shipPlace)}</b>`;
      UI.toast('배송 데이터를 생성했습니다', 'success');
      await loadSummary();
      sheetPage = 1;
      loadSheet();
    } catch (e) { UI.toast(e.message, 'error'); } finally { btn.disabled = false; }
  }

  // ── ⑤ 발주서 · 발주확인 ────────────────────────────────────────────
  async function loadSheet() {
    const wrap = el('aoSheetWrap');
    wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const [sheet, lines] = await Promise.all([
        Api.get('/ship/auto-order/sheet', { pShopCode: shopCode, pPageOffset: (sheetPage - 1) * SHEET_PAGE, pPageSize: SHEET_PAGE }),
        Api.get('/ship/auto-order/pending-lines', { pShopCode: shopCode }),
      ]);
      pendingLines = lines || [];
      el('aoConfirmCount').textContent = pendingLines.length;
      el('aoConfirm').disabled = pendingLines.length === 0;
      const unbuilt = num(summary?.unbuiltLineCnt);
      el('aoSheetNote').innerHTML = (unbuilt > 0
        ? `<span style="color:#b45309">⚠ 배송 데이터 생성(④)을 하지 않은 주문이 ${won(unbuilt)}건 있습니다 — 출고지·합배송·사은품이 아직 반영되지 않았습니다.</span><br>` : '')
        + (pendingLines.length >= 3000 ? '<span style="color:#b45309">⚠ 한 번에 확인할 수 있는 3,000건까지만 불러왔습니다. 확인 후 다시 불러오세요.</span>' : '');
      if (!sheet.total) {
        wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted)">발주서 대기 행이 없습니다 (출고지시 후 나타납니다)</div>';
        el('aoSheetPager').innerHTML = '';
        return;
      }
      wrap.innerHTML = `
        <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px">발주서 행 ${won(sheet.total)}개 · 발주확인 대상 주문 라인 ${won(pendingLines.length)}건</div>
        <div class="table-wrap"><table style="width:100%;font-size:12px">
          <thead><tr><th>출고지</th><th>주문번호</th><th>구분</th><th>상품명</th><th style="text-align:right">수량</th><th>수령인</th><th>주소</th><th>합배송</th></tr></thead>
          <tbody>${sheet.rows.map(r => `
            <tr style="${r.orderType === '사은품' ? 'font-style:italic;color:var(--text-muted)' : ''}">
              <td>${esc(r.shipPlace)}</td><td>${esc(r.orderNo)}</td><td>${r.orderType === '사은품' ? '사은품' : ''}</td>
              <td style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(r.prodName)}">${esc(r.prodName)}</td>
              <td style="text-align:right">${esc(r.prodQty)}</td><td>${esc(r.recverName)}</td>
              <td style="max-width:240px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(r.recverAddr)}">${esc(r.recverAddr)}</td>
              <td>${Number(r.bundleYn) === 1 ? '합배송' : ''}</td></tr>`).join('')}</tbody>
        </table></div>`;
      UI.pagination(el('aoSheetPager'), { total: sheet.total, page: sheetPage, pageSize: SHEET_PAGE, onChange: p => { sheetPage = p; loadSheet(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:12px">${esc(e.message)}</div>`;
    }
  }

  /** 발주서 엑셀 받기(상태 변경 없음). params: { pShopCode } 또는 { pShipCheckNo }. 오류는 토스트로 보여주려고 fetch 로 받는다. */
  async function download(params) {
    try {
      const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null)).toString();
      const res = await fetch('/ship/auto-order/sheet-excel?' + qs, { credentials: 'same-origin' });
      if (!res.ok) {
        let msg = '';
        try { msg = JSON.parse(await res.text()).message; } catch (_) {}
        if (res.status === 403) msg = '이 기능을 사용할 권한이 없습니다.';
        throw new Error(msg || `HTTP ${res.status}`);
      }
      const blob = await res.blob();
      let name = '발주서.xls';
      const cd = res.headers.get('Content-Disposition') || '';
      const m = /filename\*=UTF-8''([^;]+)/i.exec(cd);
      if (m) { try { name = decodeURIComponent(m[1]); } catch (_) {} }
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
      UI.toast('발주서를 내려받았습니다 (발주확인은 따로 처리해야 합니다)', 'success');
      if (params.pShipCheckNo && shopCode) loadHistory();
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  async function confirmOrders() {
    if (!pendingLines.length) return;
    // 화면에 불러온 뒤 다른 출고지시가 끼어들었으면 확인하지 않는다 — 받은 발주서와 확인하는 건이 달라지지 않게.
    try { await loadSummary(); } catch (_) {}
    if (pendingLines.length < 3000 && num(summary?.sheetLineCnt) !== pendingLines.length) {
      UI.toast('발주서 대기 건이 바뀌었습니다. 다시 불러온 뒤 발주서를 받고 확인하세요', 'error');
      loadSheet();
      return;
    }
    const unbuilt = num(summary?.unbuiltLineCnt);
    UI.confirm(`${pendingLines.length}건을 발주확인 처리하시겠습니까?`
      + (unbuilt > 0 ? `\n\n⚠ 배송 데이터 생성(④)을 하지 않은 ${unbuilt}건이 포함돼 있습니다.` : '')
      + '\n\n발주서를 이미 받으셨는지 확인하세요. 처리하면 발주 목록에 기록되고 송장입력 대상이 됩니다.', async close => {
      el('aoConfirm').disabled = true;
      try {
        const res = await Api.post('/ship/ship-direct/check-batch', {
          lines: pendingLines.map(l => ({ orderNo: l.orderNo, orderProdNo: l.orderProdNo, orderChangeNo: 1 })),
          registId: info?.loginId || '', registName: info?.name || '',
        });
        if (res.failed > 0) UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
        else UI.toast(`${res.success}건 발주확인 처리되었습니다`, 'success');
        close();
        loadAll();
      } catch (e) { UI.toast(e.message, 'error'); el('aoConfirm').disabled = false; }
    });
  }

  // ── ⑥ 발주 목록 ────────────────────────────────────────────────────
  async function loadHistory() {
    const box = el('aoHistory');
    try {
      const list = await Api.get('/ship/ship-check/list', { pShopCode: shopCode, pPageOffset: 0, pPageSize: 10 });
      if (!list.length) { box.innerHTML = '<div style="font-size:12px;color:var(--text-muted)">아직 발주확인한 내역이 없습니다.</div>'; return; }
      box.innerHTML = `
        <div class="table-wrap"><table style="width:100%;font-size:12px">
          <thead><tr><th>번호</th><th>발주서</th><th>확인 시각</th><th style="text-align:right">주문 수</th><th style="text-align:right">출력 횟수</th><th></th></tr></thead>
          <tbody>${list.map(h => `
            <tr><td>${esc(h.shipCheckNo)}</td><td>${esc(h.shipCheckDesc)}</td><td>${esc(h.shipCheckDate)}</td>
              <td style="text-align:right">${won(h.orderCount)}</td><td style="text-align:right">${won(h.printCount)}</td>
              <td><button class="btn btn-ghost" data-check="${esc(h.shipCheckNo)}" type="button" style="padding:2px 7px;font-size:11px">다시 받기</button></td></tr>`).join('')}</tbody>
        </table></div>`;
      box.querySelectorAll('[data-check]').forEach(b => b.addEventListener('click', () => download({ pShipCheckNo: b.dataset.check })));
    } catch (e) { box.innerHTML = `<div style="color:var(--danger);font-size:12px">${esc(e.message)}</div>`; }
  }

  return { render, download };
})();

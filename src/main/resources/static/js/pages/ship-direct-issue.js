/**
 * 출고지시 (배송 > 출고지시)
 *   지불완료 상태인 주문(라인)을 조회 → 선택 → [출고지시 처리] 하면
 *   1) shopwizard.tShpShipDirect 로 주문데이터가 이전되고(ShipDirectMapper.insertShipDirect,
 *      OrderState='출고지시', ShipDirectDate=NOW() 로 SELECT-INSERT — 원본이 지불완료일 때만 성공)
 *   2) 원본 shopion.tOrdOrderProd.OrderState 도 '출고지시' 로 바뀌고 ShipDirectDate 가 채워진다
 *      (OrderProdMapper.updateOrderState).
 *   두 단계 다 성공해야 완료로 치며, 서버(ShipDirectIssueService)가 건별로 REQUIRES_NEW 트랜잭션으로
 *   처리해 한 건 실패가 나머지 건에 영향을 주지 않는다. 이후 관리는 "직배송 관리" 화면(ship-direct.js)에서.
 *
 * 목록은 주문관리와 같은 /order/orderprod/list 를 pOrderState=지불완료 고정으로 재사용한다.
 */
const PageShipDirectIssue = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let chnlList = [];
  let shopList = [];
  let mdList   = [];
  // 페이지를 넘나들어도 선택이 유지되도록 "orderNo:orderProdNo" 키로 보관
  const selected = new Map();

  const SEARCH_PARAM = {
    '주문번호':     'pOrderNo',
    '주문자명':     'pOrderName',
    '수취인명':     'pRecverName',
    '상품명':       'pProdName',
    '상품코드':     'pProdCode',
    '전화번호':     'pPhoneNo',
    '채널주문번호': 'pChnlOrderNo',
  };

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 16) : ''; }
  function num(n) { return Number(n) || 0; }
  function realSalePrice(o) { return num(o.salePrice) + num(o.nvPrice); }
  function toDateStr(d) { return d.toISOString().slice(0, 10); }
  function defaultStartDate() { const d = new Date(); d.setDate(d.getDate() - 7); return toDateStr(d); }
  function defaultEndDate() { return toDateStr(new Date()); }
  function key(o) { return `${o.orderNo}:${o.orderProdNo}`; }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>출고지시</span>
          <button class="btn btn-primary" id="siBtnIssue" disabled>출고지시 처리 (<span id="siSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>기간 유형</label>
              <select class="input" id="siDateType" style="width:100px">
                <option value="P" selected>지불일자</option>
                <option value="">주문일자</option>
              </select>
            </div>
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="siStartDate" type="date" value="${defaultStartDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="siEndDate" type="date" value="${defaultEndDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>채널</label>
              <select class="input" id="siChnlCode" style="width:150px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>상점</label>
              <select class="input" id="siShopCode" style="width:150px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>담당MD</label>
              <select class="input" id="siMngrMd" style="width:100px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>검색조건</label>
              <select class="input" id="siSearchCond" style="width:110px">
                <option value="주문번호">주문번호</option>
                <option value="주문자명">주문자명</option>
                <option value="수취인명">수취인명</option>
                <option value="상품명">상품명</option>
                <option value="상품코드">상품코드</option>
                <option value="전화번호">전화번호</option>
                <option value="채널주문번호">채널주문번호</option>
              </select>
            </div>
            <div class="form-group">
              <label>검색어</label>
              <input class="input" id="siSearchValue" placeholder="검색어" style="width:180px">
            </div>
            <button class="btn btn-primary" id="siBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            지불완료 <strong id="siTotal">0</strong>건
          </div>

          <div class="table-wrap" id="siTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="siPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('siBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('siSearchValue').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('siBtnIssue').addEventListener('click', issueSelected);

    loadFilterOptions();
    loadList();
  }

  async function loadFilterOptions() {
    try {
      const [chnls, shops] = await Promise.all([
        Api.get('/company/chnl', { pSvcCode: svcCode(), pPageOffset: 0, pPageSize: 2000 }),
        Api.get('/company/shop', {}),
      ]);
      chnlList = (chnls || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'));
      shopList = (shops || []).filter(s => s.svcCode === svcCode())
        .sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
      mdList = [...new Set(shopList.map(s => s.mngrMd).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko'));
    } catch (_) { /* 옵션 로드 실패해도 목록은 동작 */ }

    const fill = (id, opts) => {
      const el = document.getElementById(id);
      if (!el) return;
      el.insertAdjacentHTML('beforeend', opts.map(o => `<option value="${esc(o.v)}">${esc(o.t)}</option>`).join(''));
    };
    fill('siChnlCode', chnlList.map(c => ({ v: c.chnlCode, t: c.chnlName })));
    fill('siShopCode', shopList.map(s => ({ v: s.shopCode, t: s.shopName })));
    fill('siMngrMd', mdList.map(m => ({ v: m, t: m })));
  }

  function getParams() {
    const v = id => document.getElementById(id).value;
    const params = {
      pOrderState:    '지불완료',
      pOrderDateType: v('siDateType'),
      pStartDate:     v('siStartDate'),
      pEndDate:       v('siEndDate'),
      pChnlCode:      v('siChnlCode'),
      pShopCode:      v('siShopCode'),
      pMngrMd:        v('siMngrMd'),
      pSvcCode:       svcCode(),
      // 지불완료 주문은 매칭 안 된 상품도 목록에 나와야 처리할 수 있다 — OrderProdMapper 가
      // pOrderState=='지불완료' 일 때 이미 상품/상점을 LEFT JOIN 하도록 되어 있어 별도 파라미터 불필요.
    };
    const val = v('siSearchValue').trim();
    if (val) {
      const k = SEARCH_PARAM[v('siSearchCond')];
      if (k) params[k] = val;
    }
    return params;
  }

  async function loadList() {
    const wrap = document.getElementById('siTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('siPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/orderprod/list/count', params);
      document.getElementById('siTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">지불완료 주문이 없습니다</div>';
        return;
      }
      const list = await Api.get('/order/orderprod/list', {
        ...params, topCnt: (currentPage - 1) * PAGE_SIZE, countPerPage: PAGE_SIZE,
      });
      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const rows = list.map((o, i) => `
      <tr>
        <td style="text-align:center"><input type="checkbox" class="si-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
        <td style="text-align:right;font-size:11px">${(currentPage - 1) * PAGE_SIZE + i + 1}</td>
        <td style="font-size:11px">${o.orderNo}</td>
        <td style="text-align:right;font-size:11px">${o.orderProdNo}</td>
        <td style="font-size:12px">${esc(o.orderName)}</td>
        <td style="font-size:12px">${esc(o.recverName)}</td>
        <td style="font-size:11px">${esc(o.recverMobileNo || o.recverPhoneNo)}</td>
        <td style="font-size:11px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc((o.recverAddr1||'') + ' ' + (o.recverAddr2||''))}">${esc((o.recverAddr1||'') + ' ' + (o.recverAddr2||''))}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.prodName)}">${esc(o.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(o.itemName)}</td>
        <td style="text-align:right;font-size:11px">${o.prodQty}</td>
        <td style="text-align:right;font-size:11px">${won(realSalePrice(o))}</td>
        <td style="font-size:11px">${esc(o.chnlName)}</td>
        <td style="font-size:11px">${esc(o.shopName)}</td>
        <td style="font-size:11px">${fmtDt(o.payCmpletDate)}</td>
      </tr>`).join('');

    const wrap = document.getElementById('siTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1360px">
        <colgroup>
          <col style="width:36px"><col style="width:46px"><col style="width:78px"><col style="width:40px">
          <col style="width:70px"><col style="width:70px"><col style="width:110px"><col style="width:220px">
          <col style="width:200px"><col style="width:130px"><col style="width:44px"><col style="width:84px">
          <col style="width:120px"><col style="width:100px"><col style="width:120px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="siSelectAll"></th>
            <th>번호</th><th>주문번호</th><th>순번</th><th>주문자</th><th>수취인</th><th>연락처</th>
            <th>배송지</th><th>상품명</th><th>옵션</th><th style="text-align:right">수량</th>
            <th style="text-align:right">판매가</th><th>채널</th><th>상점</th><th>지불일자</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.si-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const o = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
        updateSelectAllState(list);
        updateIssueButton();
      });
    });
    document.getElementById('siSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.si-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateIssueButton();
    });
    updateSelectAllState(list);
    updateIssueButton();
  }

  function updateSelectAllState(list) {
    const all = list.length > 0 && list.every(o => selected.has(key(o)));
    const el = document.getElementById('siSelectAll');
    if (el) el.checked = all;
  }

  function updateIssueButton() {
    const btn = document.getElementById('siBtnIssue');
    document.getElementById('siSelCount').textContent = selected.size;
    btn.disabled = selected.size === 0;
  }

  function renderPagination() {
    const el = document.getElementById('siPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 출고지시 처리 ──────────────────────────────────────────────────
  function issueSelected() {
    const lines = [...selected.values()];
    if (lines.length === 0) return;

    UI.confirm(
      `선택한 ${lines.length}건을 출고지시 처리하시겠습니까? 처리 즉시 배송(직배송 관리) 대상으로 넘어갑니다.`,
      async close => {
        const btn = document.getElementById('siBtnIssue');
        btn.disabled = true;
        try {
          const res = await Api.post('/ship/ship-direct/issue-batch', {
            lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo })),
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (res.failed > 0) {
            UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
          } else {
            UI.toast(`${res.success}건 출고지시 처리되었습니다`, 'success');
          }
          selected.clear();
          close();
          loadList();
        } catch (e) {
          UI.toast(e.message, 'error');
          btn.disabled = false;
        }
      },
    );
  }

  return { render };
})();

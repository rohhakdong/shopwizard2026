/**
 * 반품요청 (배송 > 반품요청)
 *   배송완료 주문라인을 조회 → 선택 → 사유 입력 → [반품요청 처리] 하면
 *   1) shopwizard.tShpReturnDirect 에 반품 스테이징 행이 생성되고(ReturnDirectMapper.insertReturnDirect,
 *      WHERE OP.OrderState='배송완료' 가드 — 원본이 배송완료일 때만 성공)
 *   2) shopion.tOrdOrderProd.OrderState 도 '반품요청' 으로 전환된다(OrderProdMapper.updateOrderState).
 *   그 주문의 모든 라인이 반품요청에 도달하면 tOrdOrder 헤더도 승격된다. 서버(ReturnRequestService)가
 *   건별로 REQUIRES_NEW 트랜잭션으로 처리해 한 건 실패가 나머지 건에 영향을 주지 않는다.
 *   이후 관리는 "반품확정" 화면(ship-return.js)에서.
 *
 * 목록은 주문관리와 같은 /order/orderprod/list 를 pOrderState=배송완료 고정으로 재사용한다.
 */
const PageShipReturnRequest = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let reasonOptions = [];
  const selected = new Map();

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function num(n) { return Number(n) || 0; }
  function realSalePrice(o) { return num(o.salePrice) + num(o.nvPrice); }
  function key(o) { return `${o.orderNo}:${o.orderProdNo}`; }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>반품요청</span>
          <button class="btn btn-primary" id="rrBtnRequest" disabled>반품요청 처리 (<span id="rrSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="rrOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="rrOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <button class="btn btn-primary" id="rrBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            배송완료 <strong id="rrTotal">0</strong>건
          </div>

          <div class="table-wrap" id="rrTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="rrPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('rrBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('rrOrderNo').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('rrBtnRequest').addEventListener('click', openRequestModal);

    loadReasonOptions();
    loadList();
  }

  async function loadReasonOptions() {
    try {
      reasonOptions = await Api.get('/code/constr-val', { pConstrCode: 'cCustConsltReasonType', sidx: 'ConstrValSeq', sord: 'ASC' });
    } catch (_) { reasonOptions = []; }
  }

  function getParams() {
    return {
      pOrderState: '배송완료',
      pOrderNo:    document.getElementById('rrOrderNo').value.trim(),
      pOrderName:  document.getElementById('rrOrderName').value.trim(),
      pSvcCode:    svcCode(),
      pProdJoin:   'LEFT', // 존재하는 주문은 항상 목록에 나와야 함(order-list.js 와 동일 컨벤션)
    };
  }

  async function loadList() {
    const wrap = document.getElementById('rrTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('rrPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/orderprod/list/count', params);
      document.getElementById('rrTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">배송완료 주문이 없습니다</div>';
        updateRequestButton();
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
        <td style="text-align:center"><input type="checkbox" class="rr-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
        <td style="font-size:11px">${o.orderNo}<br><span style="color:var(--text-muted)">${o.orderProdNo}</span></td>
        <td style="font-size:12px">${esc(o.orderName)}</td>
        <td style="font-size:12px">${esc(o.recverName)}</td>
        <td style="font-size:11px">${esc(o.recverMobileNo || o.recverPhoneNo)}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.prodName)}">${esc(o.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(o.itemName)}</td>
        <td style="text-align:right;font-size:11px">${o.prodQty}</td>
        <td style="text-align:right;font-size:11px">${won(realSalePrice(o))}</td>
        <td style="font-size:11px">${esc(o.chnlName)}</td>
        <td style="font-size:11px">${esc(o.shopName)}</td>
      </tr>`).join('');

    const wrap = document.getElementById('rrTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1200px">
        <colgroup>
          <col style="width:36px"><col style="width:80px"><col style="width:70px"><col style="width:70px">
          <col style="width:110px"><col style="width:220px"><col style="width:130px"><col style="width:44px">
          <col style="width:84px"><col style="width:120px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="rrSelectAll"></th>
            <th>주문/상품번호</th><th>주문자</th><th>수취인</th><th>연락처</th>
            <th>상품명</th><th>옵션</th><th style="text-align:right">수량</th>
            <th style="text-align:right">판매가</th><th>채널</th><th>상점</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.rr-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const o = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
        updateSelectAllState(list);
        updateRequestButton();
      });
    });
    document.getElementById('rrSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.rr-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateRequestButton();
    });
    updateSelectAllState(list);
    updateRequestButton();
  }

  function updateSelectAllState(list) {
    const all = list.length > 0 && list.every(o => selected.has(key(o)));
    const el = document.getElementById('rrSelectAll');
    if (el) el.checked = all;
  }

  function updateRequestButton() {
    const btn = document.getElementById('rrBtnRequest');
    document.getElementById('rrSelCount').textContent = selected.size;
    btn.disabled = selected.size === 0;
  }

  function renderPagination() {
    const el = document.getElementById('rrPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 반품요청 처리 ──────────────────────────────────────────────────
  function openRequestModal() {
    const lines = [...selected.values()];
    if (lines.length === 0) return;

    const body = document.createElement('div');
    const reasonOpts = reasonOptions.map(r => `<option value="${esc(r.constrValDesc)}">${esc(r.constrValDesc)}</option>`).join('');
    body.innerHTML = `
      <div style="font-size:13px;color:var(--text-muted);margin-bottom:12px">선택한 ${lines.length}건을 반품요청 접수합니다.</div>
      <div class="form-grid">
        <div class="form-group full">
          <label>반품사유</label>
          <select class="input" id="rrReason">
            <option value="">선택</option>
            ${reasonOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>상세사유</label>
          <input class="input" id="rrReasonDetail" placeholder="상세 사유(선택)">
        </div>
      </div>`;

    UI.modal({
      title: '반품요청 접수',
      body,
      confirmText: '처리',
      onConfirm: async close => {
        const returnReason = document.getElementById('rrReason').value;
        const returnReasonDetail = document.getElementById('rrReasonDetail').value.trim();
        if (!returnReason) { UI.toast('반품사유를 선택하세요', 'error'); return; }
        try {
          const res = await Api.post('/ship/return-direct/request-batch', {
            lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo })),
            returnReason, returnReasonDetail,
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (res.failed > 0) {
            UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
          } else {
            UI.toast(`${res.success}건 반품요청 접수되었습니다`, 'success');
          }
          selected.clear();
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

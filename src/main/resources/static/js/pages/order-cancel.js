/**
 * 주문취소접수 (주문 > 주문취소접수)
 *   배송 시작 전(지불완료/출고지시/발주확인) 주문라인을 조회 → 선택 → 사유 입력 → [주문취소접수 처리]
 *   하면 shopion.tOrdOrderProd.OrderState 가 '주문취소' 로 바뀌고(OrderCancelService.cancelOne),
 *   그 주문의 모든 라인이 주문취소에 도달하면 tOrdOrder 헤더도 '주문취소' 로 승격된다.
 *   서버가 건별로 REQUIRES_NEW 트랜잭션으로 처리해 한 건 실패가 나머지 건에 영향을 주지 않는다.
 *
 * 목록은 주문관리와 같은 /order/orderprod/list 를 pOrderState 로 상태를 골라 재사용한다.
 */
const PageOrderCancel = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let reasonOptions = [];
  // 페이지를 넘나들어도 선택이 유지되도록 "orderNo:orderProdNo" 키로 보관
  const selected = new Map();

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 16) : ''; }
  function num(n) { return Number(n) || 0; }
  function realSalePrice(o) { return num(o.salePrice) + num(o.nvPrice); }
  function key(o) { return `${o.orderNo}:${o.orderProdNo}`; }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>주문취소접수</span>
          <button class="btn btn-primary" id="ocBtnCancel" disabled>주문취소접수 처리 (<span id="ocSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="ocOrderState" style="width:120px">
                <option value="지불완료">지불완료</option>
                <option value="출고지시">출고지시</option>
                <option value="발주확인">발주확인</option>
              </select>
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="ocOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="ocOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <button class="btn btn-primary" id="ocBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="ocTotal">0</strong>건
          </div>

          <div class="table-wrap" id="ocTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="ocPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('ocBtnSearch').addEventListener('click', () => { currentPage = 1; selected.clear(); loadList(); });
    document.getElementById('ocOrderState').addEventListener('change', () => { currentPage = 1; selected.clear(); loadList(); });
    document.getElementById('ocOrderNo').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('ocBtnCancel').addEventListener('click', openCancelModal);

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
      pOrderState: document.getElementById('ocOrderState').value,
      pOrderNo:    document.getElementById('ocOrderNo').value.trim(),
      pOrderName:  document.getElementById('ocOrderName').value.trim(),
      pSvcCode:    svcCode(),
      // 지불완료 상태는 OrderProdMapper가 이미 LEFT JOIN 하지만, 출고지시/발주확인 상태에서
      // 취소하는 주문도 상품이 매칭 안 됐을 수 있어(취소 전엔 매칭 여부와 무관) 존재하는 주문이
      // 목록에서 안 빠지도록 항상 LEFT JOIN 을 요청한다(order-list.js 와 동일 컨벤션).
      pProdJoin:   'LEFT',
    };
  }

  async function loadList() {
    const wrap = document.getElementById('ocTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('ocPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/orderprod/list/count', params);
      document.getElementById('ocTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">대상 주문이 없습니다</div>';
        updateCancelButton();
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
        <td style="text-align:center"><input type="checkbox" class="oc-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
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

    const wrap = document.getElementById('ocTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1200px">
        <colgroup>
          <col style="width:36px"><col style="width:80px"><col style="width:70px"><col style="width:70px">
          <col style="width:110px"><col style="width:220px"><col style="width:130px"><col style="width:44px">
          <col style="width:84px"><col style="width:120px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="ocSelectAll"></th>
            <th>주문/상품번호</th><th>주문자</th><th>수취인</th><th>연락처</th>
            <th>상품명</th><th>옵션</th><th style="text-align:right">수량</th>
            <th style="text-align:right">판매가</th><th>채널</th><th>상점</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.oc-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const o = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
        updateSelectAllState(list);
        updateCancelButton();
      });
    });
    document.getElementById('ocSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.oc-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateCancelButton();
    });
    updateSelectAllState(list);
    updateCancelButton();
  }

  function updateSelectAllState(list) {
    const all = list.length > 0 && list.every(o => selected.has(key(o)));
    const el = document.getElementById('ocSelectAll');
    if (el) el.checked = all;
  }

  function updateCancelButton() {
    const btn = document.getElementById('ocBtnCancel');
    document.getElementById('ocSelCount').textContent = selected.size;
    btn.disabled = selected.size === 0;
  }

  function renderPagination() {
    const el = document.getElementById('ocPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 주문취소접수 처리 ──────────────────────────────────────────────
  function openCancelModal() {
    const lines = [...selected.values()];
    if (lines.length === 0) return;

    const body = document.createElement('div');
    const reasonOpts = reasonOptions.map(r => `<option value="${esc(r.constrValDesc)}">${esc(r.constrValDesc)}</option>`).join('');
    body.innerHTML = `
      <div style="font-size:13px;color:var(--text-muted);margin-bottom:12px">선택한 ${lines.length}건을 주문취소접수 처리합니다.</div>
      <div class="form-grid">
        <div class="form-group full">
          <label>취소사유</label>
          <select class="input" id="ocReason">
            <option value="">선택</option>
            ${reasonOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>상세사유</label>
          <input class="input" id="ocReasonDetail" placeholder="상세 사유(선택)">
        </div>
      </div>`;

    UI.modal({
      title: '주문취소접수',
      body,
      confirmText: '처리',
      onConfirm: async close => {
        const reason = document.getElementById('ocReason').value;
        const reasonDetail = document.getElementById('ocReasonDetail').value.trim();
        if (!reason) { UI.toast('취소사유를 선택하세요', 'error'); return; }
        try {
          const res = await Api.post('/order/order/cancel-batch', {
            lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo })),
            reason, reasonDetail,
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (res.failed > 0) {
            UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
          } else {
            UI.toast(`${res.success}건 주문취소접수 처리되었습니다`, 'success');
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

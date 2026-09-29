/**
 * 배송완료 (배송 > 배송완료)
 *   배송시작(송장입력 완료) 상태인 건을 조회 → 선택 → [배송완료 처리] 하면
 *   1) shopwizard.tShpShipDirect.OrderState 가 '배송완료' 로 바뀌고 DeliCmpletDate 가 채워진다
 *      (ShipDirectMapper.update, WHERE OrderState='배송시작' 가드 — 원본이 배송시작일 때만 성공)
 *   2) shopion.tOrdOrderProd.OrderState 도 '배송완료' 로 동기화된다 (OrderProdMapper.updateOrderState).
 *   두 단계 다 성공해야 완료로 치며, 서버(ShipDirectCompleteService)가 건별로 REQUIRES_NEW 트랜잭션으로
 *   처리해 한 건 실패가 나머지 건에 영향을 주지 않는다.
 *
 * 목록은 tShpShipDirect 를 pOrderState=배송시작 고정으로 조회한다 (ShipDirectMapper.selectList).
 */
const PageShipDirectComplete = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  // 페이지를 넘나들어도 선택이 유지되도록 "orderNo:orderProdNo:orderChangeNo" 키로 보관
  const selected = new Map();

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 16) : ''; }
  function key(o) { return `${o.orderNo}:${o.orderProdNo}:${o.orderChangeNo || 0}`; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>배송완료</span>
          <button class="btn btn-primary" id="dcBtnComplete" disabled>배송완료 처리 (<span id="dcSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="dcOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="dcOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <button class="btn btn-primary" id="dcBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            배송시작 <strong id="dcTotal">0</strong>건
          </div>

          <div class="table-wrap" id="dcTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="dcPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('dcBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('dcOrderNo').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('dcBtnComplete').addEventListener('click', completeSelected);

    loadList();
  }

  function getParams() {
    return {
      pOrderState: '배송시작',
      pNotDependOnDate: 'Y',
      pOrderNo: document.getElementById('dcOrderNo').value.trim(),
      pOrderName: document.getElementById('dcOrderName').value.trim(),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('dcTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('dcPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/ship/ship-direct/count', params);
      document.getElementById('dcTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">배송시작 대기 건이 없습니다</div>';
        return;
      }
      const list = await Api.get('/ship/ship-direct/list', {
        ...params,
        pPageOffset: (currentPage - 1) * PAGE_SIZE,
        pPageSize: PAGE_SIZE,
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
        <td style="text-align:center"><input type="checkbox" class="dc-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
        <td style="font-size:11px">${o.orderNo}<br><span style="color:var(--text-muted)">${o.orderProdNo}</span></td>
        <td style="font-size:11px">${fmtDt(o.shipCmpletDate)}</td>
        <td style="font-size:12px">${esc(o.orderName)}</td>
        <td style="font-size:12px">${esc(o.recverName)}</td>
        <td style="font-size:12px">${esc(o.deliCompName)}</td>
        <td style="font-size:12px">${esc(o.invNo)}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.prodName)}">${esc(o.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(o.itemName)}</td>
        <td style="text-align:right;font-size:11px">${o.prodQty}</td>
        <td style="font-size:11px">${esc(o.chnlName)}</td>
        <td style="font-size:11px">${esc(o.shopName)}</td>
      </tr>`).join('');

    const wrap = document.getElementById('dcTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1240px">
        <colgroup>
          <col style="width:36px"><col style="width:80px"><col style="width:90px"><col style="width:70px">
          <col style="width:70px"><col style="width:90px"><col style="width:110px"><col style="width:200px">
          <col style="width:120px"><col style="width:44px"><col style="width:110px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="dcSelectAll"></th>
            <th>주문/상품번호</th><th>배송시작일</th><th>주문자</th><th>수취인</th>
            <th>택배사</th><th>송장번호</th><th>상품명</th><th>옵션</th>
            <th style="text-align:right">수량</th><th>채널</th><th>상점</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.dc-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const o = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
        updateSelectAllState(list);
        updateCompleteButton();
      });
    });
    document.getElementById('dcSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.dc-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateCompleteButton();
    });
    updateSelectAllState(list);
    updateCompleteButton();
  }

  function updateSelectAllState(list) {
    const all = list.length > 0 && list.every(o => selected.has(key(o)));
    const el = document.getElementById('dcSelectAll');
    if (el) el.checked = all;
  }

  function updateCompleteButton() {
    const btn = document.getElementById('dcBtnComplete');
    document.getElementById('dcSelCount').textContent = selected.size;
    btn.disabled = selected.size === 0;
  }

  function renderPagination() {
    const el = document.getElementById('dcPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 배송완료 처리 ──────────────────────────────────────────────────
  function completeSelected() {
    const lines = [...selected.values()];
    if (lines.length === 0) return;

    UI.confirm(
      `선택한 ${lines.length}건을 배송완료 처리하시겠습니까?`,
      async close => {
        const btn = document.getElementById('dcBtnComplete');
        btn.disabled = true;
        try {
          const res = await Api.post('/ship/ship-direct/complete-batch', {
            lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo, orderChangeNo: o.orderChangeNo || 0 })),
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (res.failed > 0) {
            UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
          } else {
            UI.toast(`${res.success}건 배송완료 처리되었습니다`, 'success');
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

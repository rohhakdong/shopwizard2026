/**
 * 환불확정 (주문 > 환불확정)
 *   주문취소 또는 반품완료 상태인 주문라인을 조회 → 선택 → [환불확정 처리] 하면
 *   shopion.tOrdOrderProd.OrderState 가 '환불완료' 로 바뀐다(RefundConfirmService.confirmOne).
 *   그 주문의 모든 라인이 환불완료에 도달하면 tOrdOrder 헤더도 승격된다. 별도 스테이징 테이블 없이
 *   주문 상태만 바로 전환하며, 서버가 건별로 REQUIRES_NEW 트랜잭션으로 처리해 한 건 실패가 나머지
 *   건에 영향을 주지 않는다.
 *
 * 목록은 주문관리와 같은 /order/orderprod/list 를 pOrderState 로 "구분"(주문취소/반품완료)을
 * 골라 재사용한다.
 */
const PageOrderRefundConfirm = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
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
          <span>환불확정</span>
          <button class="btn btn-primary" id="rcBtnConfirm" disabled>환불확정 처리 (<span id="rcSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>구분</label>
              <select class="input" id="rcOrderState" style="width:120px">
                <option value="주문취소">주문취소</option>
                <option value="반품완료">반품완료</option>
              </select>
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="rcOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="rcOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <button class="btn btn-primary" id="rcBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="rcTotal">0</strong>건
          </div>

          <div class="table-wrap" id="rcTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="rcPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('rcBtnSearch').addEventListener('click', () => { currentPage = 1; selected.clear(); loadList(); });
    document.getElementById('rcOrderState').addEventListener('change', () => { currentPage = 1; selected.clear(); loadList(); });
    document.getElementById('rcOrderNo').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('rcBtnConfirm').addEventListener('click', confirmSelected);

    loadList();
  }

  function getParams() {
    return {
      pOrderState: document.getElementById('rcOrderState').value,
      pOrderNo:    document.getElementById('rcOrderNo').value.trim(),
      pOrderName:  document.getElementById('rcOrderName').value.trim(),
      pSvcCode:    svcCode(),
      // 주문취소 건은 상품매칭 전에 취소됐을 수 있어(반품완료는 배송완료를 거쳐 이미 매칭된
      // 상태라 상관없지만) 존재하는 주문이 안 빠지도록 항상 LEFT JOIN 을 요청한다.
      pProdJoin:   'LEFT',
    };
  }

  async function loadList() {
    const wrap = document.getElementById('rcTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('rcPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/orderprod/list/count', params);
      document.getElementById('rcTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">대상 주문이 없습니다</div>';
        updateConfirmButton();
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
        <td style="text-align:center"><input type="checkbox" class="rc-row" data-i="${i}" ${selected.has(key(o)) ? 'checked' : ''}></td>
        <td style="font-size:11px">${o.orderNo}<br><span style="color:var(--text-muted)">${o.orderProdNo}</span></td>
        <td style="font-size:12px">${esc(o.orderName)}</td>
        <td style="font-size:12px">${esc(o.recverName)}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.prodName)}">${esc(o.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(o.itemName)}</td>
        <td style="text-align:right;font-size:11px">${o.prodQty}</td>
        <td style="text-align:right;font-size:11px">${won(realSalePrice(o))}</td>
        <td style="font-size:11px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(o.returnReason)}">${esc(o.returnReason) || '-'}</td>
        <td style="font-size:11px">${esc(o.chnlName)}</td>
        <td style="font-size:11px">${esc(o.shopName)}</td>
      </tr>`).join('');

    const wrap = document.getElementById('rcTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1260px">
        <colgroup>
          <col style="width:36px"><col style="width:80px"><col style="width:70px"><col style="width:70px">
          <col style="width:200px"><col style="width:130px"><col style="width:44px"><col style="width:84px">
          <col style="width:130px"><col style="width:120px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="rcSelectAll"></th>
            <th>주문/상품번호</th><th>주문자</th><th>수취인</th>
            <th>상품명</th><th>옵션</th><th style="text-align:right">수량</th>
            <th style="text-align:right">판매가</th><th>사유</th><th>채널</th><th>상점</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.rc-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const o = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(o), o); else selected.delete(key(o));
        updateSelectAllState(list);
        updateConfirmButton();
      });
    });
    document.getElementById('rcSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.rc-row').forEach(cb => {
        const o = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(o), o); else selected.delete(key(o));
      });
      updateConfirmButton();
    });
    updateSelectAllState(list);
    updateConfirmButton();
  }

  function updateSelectAllState(list) {
    const all = list.length > 0 && list.every(o => selected.has(key(o)));
    const el = document.getElementById('rcSelectAll');
    if (el) el.checked = all;
  }

  function updateConfirmButton() {
    const btn = document.getElementById('rcBtnConfirm');
    document.getElementById('rcSelCount').textContent = selected.size;
    btn.disabled = selected.size === 0;
  }

  function renderPagination() {
    const el = document.getElementById('rcPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 환불확정 처리 ──────────────────────────────────────────────────
  function confirmSelected() {
    const lines = [...selected.values()];
    if (lines.length === 0) return;

    UI.confirm(
      `선택한 ${lines.length}건을 환불확정 처리하시겠습니까?`,
      async close => {
        const btn = document.getElementById('rcBtnConfirm');
        btn.disabled = true;
        try {
          const res = await Api.post('/order/order/refund-confirm-batch', {
            lines: lines.map(o => ({ orderNo: o.orderNo, orderProdNo: o.orderProdNo, orderChangeNo: 0 })),
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (res.failed > 0) {
            UI.toast(`성공 ${res.success}건 · 실패 ${res.failed}건 — ${res.failDetails[0] || ''}`, 'error');
          } else {
            UI.toast(`${res.success}건 환불확정 처리되었습니다`, 'success');
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

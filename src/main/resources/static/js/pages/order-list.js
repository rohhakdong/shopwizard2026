/**
 * 주문 목록
 * - 기간/조건 검색, 페이지네이션
 * - 주문 클릭 시 주문상품 상세 모달
 * - 주문 취소
 */
const PageOrderList = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  // ── 날짜 유틸 ──────────────────────────────────────────────────────
  function toDateStr(date) {
    return date.toISOString().slice(0, 10);
  }
  function defaultStartDate() {
    const d = new Date(); d.setDate(d.getDate() - 30); return toDateStr(d);
  }
  function defaultEndDate() {
    return toDateStr(new Date());
  }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">주문 목록</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>기간 유형</label>
              <select class="input" id="olDateType" style="width:100px">
                <option value="">접수일</option>
                <option value="P">결제일</option>
                <option value="S">배송완료일</option>
                <option value="R">환불완료일</option>
              </select>
            </div>
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="olStartDate" type="date" value="${defaultStartDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="olEndDate" type="date" value="${defaultEndDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="olOrderNo" placeholder="주문번호" style="width:110px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="olOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <div class="form-group">
              <label>수취인명</label>
              <input class="input" id="olRecverName" placeholder="수취인명" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문상태</label>
              <select class="input" id="olOrderState" style="width:100px">
                <option value="">전체</option>
                <option value="주문접수">주문접수</option>
                <option value="지불완료">지불완료</option>
                <option value="발주시작">발주시작</option>
                <option value="배송완료">배송완료</option>
                <option value="주문취소">주문취소</option>
                <option value="환불완료">환불완료</option>
              </select>
            </div>
            <button class="btn btn-primary" id="olBtnSearch">검색</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="olTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="olTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="olPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('olBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('olOrderNo').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });

    loadList();
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pStartDate:     document.getElementById('olStartDate').value,
      pEndDate:       document.getElementById('olEndDate').value,
      pOrderDateType: document.getElementById('olDateType').value,
      pOrderNo:       document.getElementById('olOrderNo').value.trim(),
      pOrderName:     document.getElementById('olOrderName').value.trim(),
      pRecverName:    document.getElementById('olRecverName').value.trim(),
      pOrderState:    document.getElementById('olOrderState').value,
      pSvcCode:       (typeof info !== 'undefined' && info?.svcCode) || '',
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('olTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('olPagination').innerHTML = '';

    try {
      const params = getParams();

      totalCount = await Api.get('/order/order/orderlist/count', params);
      document.getElementById('olTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/order/order/orderlist', {
        ...params,
        topCnt:       (currentPage - 1) * PAGE_SIZE,
        countPerPage: PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 상태 배지 ─────────────────────────────────────────────────────
  function stateBadge(state) {
    const map = {
      '주문접수': 'badge-gray',
      '지불완료': 'badge-blue',
      '발주시작': 'badge-orange',
      '배송완료': 'badge-green',
      '주문취소': 'badge-red',
      '환불완료': 'badge-red',
    };
    return `<span class="badge ${map[state] || 'badge-gray'}">${state || '-'}</span>`;
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('olTableWrap');

    const rows = list.map(o => {
      const reciptDate = o.orderReciptDate ? o.orderReciptDate.substring(0, 10) : '-';
      const amt = o.orderAmt != null ? o.orderAmt.toLocaleString() + '원' : '-';
      const prodLabel = o.prodName ? (o.prodCount > 1 ? `${o.prodName} 외 ${o.prodCount - 1}건` : o.prodName) : '-';

      // 상태 카운트 요약
      const states = [];
      if (o.readyCount)    states.push(`<span style="color:var(--text-muted)">접수 ${o.readyCount}</span>`);
      if (o.approvCount)   states.push(`<span style="color:var(--primary)">승인 ${o.approvCount}</span>`);
      if (o.deliCount)     states.push(`<span style="color:#f59e0b">발주 ${o.deliCount}</span>`);
      if (o.completeCount) states.push(`<span style="color:var(--success)">완료 ${o.completeCount}</span>`);
      if (o.cancelCount)   states.push(`<span style="color:var(--danger)">취소 ${o.cancelCount}</span>`);

      return `
        <tr style="cursor:pointer" data-orderno="${o.orderNo}">
          <td style="font-size:12px;font-weight:600">${o.orderNo || ''}</td>
          <td style="font-size:12px">${reciptDate}</td>
          <td>
            <div style="font-size:13px;font-weight:500;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap"
                 title="${prodLabel}">${prodLabel}</div>
            <div style="font-size:11px;color:var(--text-muted)">${o.prodQty != null ? o.prodQty + '개' : ''}</div>
          </td>
          <td style="font-size:12px">${o.orderName || ''}</td>
          <td style="font-size:12px">${o.recverName || ''}</td>
          <td style="text-align:right;font-size:13px;font-weight:500">${amt}</td>
          <td style="font-size:11px;line-height:1.8">${states.join('<br>')}</td>
          <td style="text-align:center">${stateBadge(o.orderState)}</td>
          <td style="font-size:11px;color:var(--text-muted)">${o.chnlName || ''}</td>
          <td style="white-space:nowrap">
            <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
              data-action="detail" data-orderno="${o.orderNo}">상세</button>
            ${o.orderState !== '주문취소' && o.orderState !== '환불완료' ? `
            <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
              data-action="cancel" data-orderno="${o.orderNo}">취소</button>` : ''}
          </td>
        </tr>`;
    }).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:80px"><col style="width:90px"><col><col style="width:80px">
          <col style="width:80px"><col style="width:90px"><col style="width:90px">
          <col style="width:70px"><col style="width:90px"><col style="width:80px">
        </colgroup>
        <thead>
          <tr>
            <th>주문번호</th><th>접수일</th><th>상품</th><th>주문자</th>
            <th>수취인</th><th style="text-align:right">주문금액</th>
            <th>품목상태</th><th style="text-align:center">주문상태</th>
            <th>채널</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 상세
    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        openDetailModal(parseInt(btn.dataset.orderno));
      });
    });

    // 취소
    wrap.querySelectorAll('[data-action=cancel]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        UI.confirm(`주문번호 [${btn.dataset.orderno}]를 취소하시겠습니까?`, async close => {
          try {
            await Api.put('/order/order/cancel', { orderNo: parseInt(btn.dataset.orderno) });
            UI.toast('주문이 취소되었습니다', 'success');
            loadList();
          } catch (err) { UI.toast(err.message, 'error'); }
          close();
        });
      });
    });

    // 행 클릭 → 상세
    wrap.querySelectorAll('tbody tr').forEach(tr => {
      tr.addEventListener('click', () => openDetailModal(parseInt(tr.dataset.orderno)));
    });
  }

  // ── 페이지네이션 ───────────────────────────────────────────────────
  function renderPagination() {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    if (totalPages <= 1) return;

    const el = document.getElementById('olPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start - 1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end + 1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentPage = parseInt(btn.dataset.page);
        loadList();
      });
    });
  }

  // ── 주문 상세 모달 ─────────────────────────────────────────────────
  async function openDetailModal(orderNo) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';

    const { close } = UI.modal({
      title: `주문 상세 – #${orderNo}`,
      body,
      confirmText: null,
      cancelText: '닫기',
    });

    try {
      const svcCode = (typeof info !== 'undefined' && info?.svcCode) || '';
      const [order, prods] = await Promise.all([
        Api.get('/order/order', { orderNo }),
        Api.get('/order/orderprod/list', { pOrderNo: orderNo, pSvcCode: svcCode }),
      ]);

      const row = (label, value) => `
        <div class="form-group">
          <label>${label}</label>
          <div class="input" style="background:#f8fafc;min-height:34px;line-height:34px;padding:0 10px;border:1px solid var(--border);border-radius:var(--radius)">${value ?? '-'}</div>
        </div>`;

      const prodRows = (prods || []).map(p => `
        <tr>
          <td style="font-size:12px">${p.prodName || ''}</td>
          <td style="font-size:11px;color:var(--text-muted)">${p.itemName || ''}</td>
          <td style="text-align:right;font-size:12px">${p.prodQty || ''}</td>
          <td style="text-align:right;font-size:12px">${p.salePrice != null ? p.salePrice.toLocaleString() + '원' : '-'}</td>
          <td style="text-align:center;font-size:12px">${p.invNo || '-'}</td>
          <td style="text-align:center">
            <span class="badge ${({'주문접수':'badge-gray','지불완료':'badge-blue','발주시작':'badge-orange','배송완료':'badge-green','주문취소':'badge-red','환불완료':'badge-red'})[p.orderState] || 'badge-gray'}">${p.orderState || ''}</span>
          </td>
        </tr>`).join('');

      body.innerHTML = `
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;letter-spacing:.05em">주문 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('주문번호', order.orderNo)}
          ${row('접수일', order.orderReciptDate ? order.orderReciptDate.substring(0,10) : null)}
          ${row('주문자', order.orderName)}
          ${row('연락처', order.orderMobileNo || order.orderPhoneNo)}
          ${row('수취인', order.recverName)}
          ${row('수취인 연락처', order.recverMobileNo || order.recverPhoneNo)}
          ${row('배송지', (order.recverAddr1 || '') + ' ' + (order.recverAddr2 || ''))}
          ${row('배송 메모', order.deliMemo)}
          ${row('주문상태', order.orderState)}
          ${row('결제수단', order.payTypeName || order.payType)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;letter-spacing:.05em">주문 상품</div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>상품명</th><th>옵션</th>
                <th style="text-align:right">수량</th>
                <th style="text-align:right">금액</th>
                <th style="text-align:center">송장번호</th>
                <th style="text-align:center">상태</th>
              </tr>
            </thead>
            <tbody>${prodRows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:16px">주문 상품 없음</td></tr>'}</tbody>
          </table>
        </div>`;
    } catch (e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  return { render };
})();

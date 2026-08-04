/**
 * 직배송 관리
 * - 기간/조건 검색, 페이지네이션
 * - 상세 모달 (읽기 전용)
 * - 송장번호 수정
 */
const PageShipDirect = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList    = [];

  function toDateStr(date) { return date.toISOString().slice(0,10); }
  function defaultStart()  { const d = new Date(); d.setDate(d.getDate()-30); return toDateStr(d); }
  function defaultEnd()    { return toDateStr(new Date()); }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">직배송 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>출고확인 시작일</label>
              <input class="input" id="sdStartDate" type="date" value="${defaultStart()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>출고확인 종료일</label>
              <input class="input" id="sdEndDate" type="date" value="${defaultEnd()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="sdOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="sdOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <div class="form-group">
              <label>쇼핑몰</label>
              <select class="input" id="sdShopCode" style="width:140px">
                <option value="">전체</option>
              </select>
            </div>
            <button class="btn btn-primary" id="sdBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="sdTotalLabel">0</strong>건
          </div>
          <div class="table-wrap" id="sdTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>
          <div id="sdPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('sdBtnSearch').addEventListener('click', () => { currentPage=1; loadList(); });
    document.getElementById('sdOrderNo').addEventListener('keydown', e => { if(e.key==='Enter'){currentPage=1;loadList();} });

    loadShopList().then(() => loadList());
  }

  async function loadShopList() {
    try {
      shopList = await Api.get('/company/shop', {});
      const sel = document.getElementById('sdShopCode');
      if (!sel) return;
      shopList.forEach(s => {
        const o = document.createElement('option');
        o.value = s.shopCode; o.textContent = s.shopName || s.shopCode;
        sel.appendChild(o);
      });
    } catch(_) {}
  }

  function getParams() {
    return {
      pShipCheckStartDate: document.getElementById('sdStartDate').value,
      pShipCheckEndDate:   document.getElementById('sdEndDate').value,
      pOrderNo:            document.getElementById('sdOrderNo').value.trim(),
      pOrderName:          document.getElementById('sdOrderName').value.trim(),
      pShopCode:           document.getElementById('sdShopCode').value,
      pNotDependOnDate:    'Y',
    };
  }

  async function loadList() {
    const wrap = document.getElementById('sdTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('sdPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/ship/ship-direct/count', params);
      document.getElementById('sdTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/ship/ship-direct/list', {
        ...params,
        pPageOffset: (currentPage-1)*PAGE_SIZE,
        pPageSize:   PAGE_SIZE,
      });
      renderTable(list);
      renderPagination();
    } catch(e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function stateColor(state) {
    const map = {'발주시작':'badge-orange','배송완료':'badge-green','주문취소':'badge-red','환불완료':'badge-red'};
    return map[state] || 'badge-gray';
  }

  function renderTable(list) {
    const wrap = document.getElementById('sdTableWrap');

    const rows = list.map(d => `
      <tr>
        <td style="font-size:11px">${d.orderNo || ''}<br><span style="color:var(--text-muted)">${d.orderProdNo || ''}</span></td>
        <td style="font-size:11px">${d.shipCheckDate ? d.shipCheckDate.substring(0,10) : '-'}</td>
        <td>
          <div style="font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${d.prodName || ''}">${d.prodName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${d.itemName || ''}</div>
        </td>
        <td style="font-size:12px">${d.recverName || ''}</td>
        <td style="font-size:12px">${d.shopName || ''}</td>
        <td style="font-size:12px">${d.deliCompName || ''}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${d.invNo || ''}">${d.invNo || '-'}</td>
        <td style="text-align:center">
          <span class="badge ${stateColor(d.orderState)}">${d.orderState || ''}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-action="detail"
            data-ono="${d.orderNo}" data-opno="${d.orderProdNo}" data-ocno="${d.orderChangeNo||0}">상세</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--primary)" data-action="inv"
            data-ono="${d.orderNo}" data-opno="${d.orderProdNo}" data-ocno="${d.orderChangeNo||0}"
            data-inv="${(d.invNo||'').replace(/"/g,'&quot;')}" data-deli="${d.deliCompCode||''}">송장</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:90px"><col style="width:90px"><col><col style="width:75px">
          <col style="width:90px"><col style="width:80px"><col style="width:110px">
          <col style="width:70px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th>주문/상품번호</th><th>출고확인일</th><th>상품명</th><th>수취인</th>
            <th>쇼핑몰</th><th>택배사</th><th>송장번호</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 상세
    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', () => openDetailModal(btn));
    });

    // 송장번호 수정
    wrap.querySelectorAll('[data-action=inv]').forEach(btn => {
      btn.addEventListener('click', () => openInvoiceModal(btn));
    });
  }

  function renderPagination() {
    const totalPages = Math.ceil(totalCount/PAGE_SIZE);
    if (totalPages <= 1) return;
    const el = document.getElementById('sdPagination');
    const block = Math.floor((currentPage-1)/10);
    const start = block*10+1, end = Math.min(start+9, totalPages);
    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start-1}">‹</button>`;
    for (let p=start; p<=end; p++) html += `<button class="page-btn ${p===currentPage?'active':''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end+1}">›</button>`;
    html += `</div>`;
    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => { currentPage=parseInt(btn.dataset.page); loadList(); });
    });
  }

  async function openDetailModal(btn) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';
    UI.modal({ title: `직배송 상세 – 주문 #${btn.dataset.ono}`, body, confirmText: null, cancelText: '닫기' });

    try {
      const d = await Api.get('/ship/ship-direct', {
        orderNo: btn.dataset.ono, orderProdNo: btn.dataset.opno, orderChangeNo: btn.dataset.ocno
      });
      const row = (label, value) => `
        <div class="form-group">
          <label>${label}</label>
          <div class="input" style="background:#f8fafc;min-height:34px;line-height:34px;padding:0 10px;border:1px solid var(--border);border-radius:var(--radius)">${value ?? '-'}</div>
        </div>`;
      body.innerHTML = `
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">주문 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('주문번호', d.orderNo)} ${row('상품번호', d.orderProdNo)}
          ${row('주문자', d.orderName)} ${row('수취인', d.recverName)}
          ${row('수취인 연락처', d.recverMobileNo||d.recverPhoneNo)}
          ${row('배송지', d.recverAddr)} ${row('배송 메모', d.deliMemo)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">상품 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('상품명', d.prodName)} ${row('옵션', d.itemName)}
          ${row('수량', d.prodQty)} ${row('쇼핑몰', d.shopName)}
          ${row('협력사', d.supplyName)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">배송 정보</div>
        <div class="form-grid">
          ${row('택배사', d.deliCompName)}
          ${row('송장번호', d.invNo)}
          ${row('출고확인일', d.shipCheckDate ? d.shipCheckDate.substring(0,10) : null)}
          ${row('배송완료일', d.shipCmpletDate ? d.shipCmpletDate.substring(0,10) : null)}
          ${row('출고지', d.shipPlace)} ${row('주문상태', d.orderState)}
        </div>`;
    } catch(e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function openInvoiceModal(btn) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full">
          <label>송장번호</label>
          <input class="input" id="sdInvNo" value="${btn.dataset.inv}" placeholder="송장번호 입력">
        </div>
      </div>`;
    UI.modal({
      title: `송장번호 수정 – #${btn.dataset.ono}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const invNo = document.getElementById('sdInvNo').value.trim();
        try {
          await Api.put('/ship/ship-direct/invoice', {
            orderNo: parseInt(btn.dataset.ono),
            orderProdNo: parseInt(btn.dataset.opno),
            orderChangeNo: parseInt(btn.dataset.ocno),
            invNo,
            changeId:   (typeof info !== 'undefined' && info?.loginId) || '',
            changeName: (typeof info !== 'undefined' && info?.name) || '',
          });
          UI.toast('송장번호가 수정되었습니다', 'success');
          loadList();
          close();
        } catch(e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

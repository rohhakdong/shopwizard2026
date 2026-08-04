/**
 * 반품 관리
 * - 기간/조건 검색, 페이지네이션
 * - 상세 모달 (읽기 전용)
 * - 반품 완료 처리
 */
const PageShipReturn = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList    = [];

  function toDateStr(d) { return d.toISOString().slice(0,10); }
  function defaultStart() { const d=new Date(); d.setDate(d.getDate()-30); return toDateStr(d); }
  function defaultEnd()   { return toDateStr(new Date()); }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">반품 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>기간 유형</label>
              <select class="input" id="rtDateType" style="width:100px">
                <option value="">접수일</option>
                <option value="P">결제일</option>
                <option value="S">배송완료일</option>
                <option value="R">환불완료일</option>
              </select>
            </div>
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="rtStartDate" type="date" value="${defaultStart()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="rtEndDate" type="date" value="${defaultEnd()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="rtOrderNo" placeholder="주문번호" style="width:100px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="rtOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <div class="form-group">
              <label>쇼핑몰</label>
              <select class="input" id="rtShopCode" style="width:140px">
                <option value="">전체</option>
              </select>
            </div>
            <button class="btn btn-primary" id="rtBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="rtTotalLabel">0</strong>건
          </div>
          <div class="table-wrap" id="rtTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>
          <div id="rtPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('rtBtnSearch').addEventListener('click', () => { currentPage=1; loadList(); });
    document.getElementById('rtOrderNo').addEventListener('keydown', e => { if(e.key==='Enter'){currentPage=1;loadList();} });

    loadShopList().then(() => loadList());
  }

  async function loadShopList() {
    try {
      shopList = await Api.get('/company/shop', {});
      const sel = document.getElementById('rtShopCode');
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
      pStartDate:     document.getElementById('rtStartDate').value,
      pEndDate:       document.getElementById('rtEndDate').value,
      pOrderDateType: document.getElementById('rtDateType').value,
      pOrderNo:       document.getElementById('rtOrderNo').value.trim(),
      pOrderName:     document.getElementById('rtOrderName').value.trim(),
      pShopCode:      document.getElementById('rtShopCode').value,
      pNotDependOnDate: 'Y',
    };
  }

  async function loadList() {
    const wrap = document.getElementById('rtTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('rtPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/ship/return-direct/count', params);
      document.getElementById('rtTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/ship/return-direct/list', {
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

  function renderTable(list) {
    const wrap = document.getElementById('rtTableWrap');

    const rows = list.map(d => `
      <tr>
        <td style="font-size:11px">${d.orderNo || ''}<br><span style="color:var(--text-muted)">${d.orderProdNo || ''}</span></td>
        <td style="font-size:11px">${d.returnRqstDate ? d.returnRqstDate.substring(0,10) : '-'}</td>
        <td>
          <div style="font-size:13px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${d.prodName||''}">${d.prodName||''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${d.itemName||''}</div>
        </td>
        <td style="font-size:12px">${d.orderName||''}</td>
        <td style="font-size:12px">${d.shopName||''}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${d.returnReason||''}">${d.returnReason||'-'}</td>
        <td style="font-size:12px">${d.invNo||'-'}</td>
        <td style="text-align:center">
          <span class="badge ${d.returnCmpletDate ? 'badge-green' : 'badge-orange'}">${d.returnCmpletDate ? '완료' : '처리중'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-action="detail"
            data-ono="${d.orderNo}" data-opno="${d.orderProdNo}" data-ocno="${d.orderChangeNo||0}">상세</button>
          ${!d.returnCmpletDate ? `
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--success)" data-action="cmplet"
            data-ono="${d.orderNo}" data-opno="${d.orderProdNo}" data-ocno="${d.orderChangeNo||0}">완료처리</button>` : ''}
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:90px"><col style="width:90px"><col><col style="width:75px">
          <col style="width:90px"><col style="width:100px"><col style="width:100px">
          <col style="width:65px"><col style="width:110px">
        </colgroup>
        <thead>
          <tr>
            <th>주문/상품번호</th><th>반품요청일</th><th>상품명</th><th>주문자</th>
            <th>쇼핑몰</th><th>반품사유</th><th>송장번호</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', () => openDetailModal(btn));
    });

    wrap.querySelectorAll('[data-action=cmplet]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`주문번호 [${btn.dataset.ono}] 반품을 완료 처리하시겠습니까?`, async close => {
          try {
            await Api.put('/ship/return-direct', {
              orderNo: parseInt(btn.dataset.ono),
              orderProdNo: parseInt(btn.dataset.opno),
              orderChangeNo: parseInt(btn.dataset.ocno),
              returnCmpletDate: new Date().toISOString().slice(0,10).replace(/-/g,''),
              changeId:   (typeof info !== 'undefined' && info?.loginId) || '',
              changeName: (typeof info !== 'undefined' && info?.name) || '',
            });
            UI.toast('반품 완료 처리되었습니다', 'success');
            loadList();
          } catch(e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  function renderPagination() {
    const totalPages = Math.ceil(totalCount/PAGE_SIZE);
    if (totalPages <= 1) return;
    const el = document.getElementById('rtPagination');
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
    UI.modal({ title: `반품 상세 – 주문 #${btn.dataset.ono}`, body, confirmText: null, cancelText: '닫기' });

    try {
      const d = await Api.get('/ship/return-direct', {
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
          ${row('배송지', d.recverAddr)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">상품 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('상품명', d.prodName)} ${row('옵션', d.itemName)}
          ${row('수량', d.prodQty)} ${row('쇼핑몰', d.shopName)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">반품 정보</div>
        <div class="form-grid">
          ${row('반품사유', d.returnReason)}
          ${row('반품사유 상세', d.returnReasonDetail)}
          ${row('반품요청일', d.returnRqstDate ? d.returnRqstDate.substring(0,10) : null)}
          ${row('반품접수일', d.returnReciptDate ? d.returnReciptDate.substring(0,10) : null)}
          ${row('반품완료일', d.returnCmpletDate ? d.returnCmpletDate.substring(0,10) : null)}
          ${row('송장번호', d.invNo)}
        </div>`;
    } catch(e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  return { render };
})();

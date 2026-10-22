/**
 * 정산 대상 (정산 > 정산 대상)
 *   배송(송장입력) 건(+)과 반품완료 건(−) 중 아직 정산자료로 만들지 않은 것을 기간·상점·정산주기로 조회하고,
 *   선택해서 [정산자료 생성] 하면 서버(AdjustProcessController)가 건별로 원본 행에 정산 선택일을 찍고
 *   tAdjAdjustDetail 로 복사한다(한 건은 한 번만 들어간다). 이후 "정산 마감" 화면에서 금액 보정·상점 마감.
 *
 *   반품(−) 행은 수량·금액이 음수다. 한 키(주문번호·순번)에 배송과 반품이 같이 있을 수 있어 구분(kind)을 함께 보낸다.
 */
const PageAdjustTarget = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 50;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList = [];
  // 페이지를 넘나들어도 선택이 유지되도록 "kind:orderNo:orderProdNo:orderChangeNo" 로 보관
  const selected = new Map();

  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 10) : ''; }
  function toDateStr(d) { return d.toISOString().slice(0, 10); }
  function firstOfPrevMonth() { const d = new Date(); d.setMonth(d.getMonth() - 1, 1); return toDateStr(d); }
  function lastOfPrevMonth() { const d = new Date(); d.setDate(0); return toDateStr(d); }
  function key(t) { return `${t.kind}:${t.orderNo}:${t.orderProdNo}:${t.orderChangeNo}`; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    selected.clear();
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>정산 대상</span>
          <button class="btn btn-primary" id="atBtnCreate" disabled>선택 정산자료 생성 (<span id="atSelCount">0</span>건)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="atStartDate" type="date" value="${firstOfPrevMonth()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="atEndDate" type="date" value="${lastOfPrevMonth()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>정산주기</label>
              <select class="input" id="atPeriod" style="width:120px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>상점</label>
              <select class="input" id="atShopCode" style="width:190px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="atOrderName" placeholder="주문자명" style="width:130px">
            </div>
            <button class="btn btn-primary" id="atBtnSearch">조회</button>
          </div>

          <div id="atSummary" style="font-size:12px;color:var(--text-muted);margin-bottom:8px"></div>

          <div class="table-wrap" id="atTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">기간을 정하고 조회하세요</div>
          </div>

          <div id="atPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('atBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('atOrderName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('atBtnCreate').addEventListener('click', createSelected);

    loadShops();
  }

  async function loadShops() {
    try {
      const shops = await Api.get('/company/shop', {});
      shopList = (shops || []).slice().sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { shopList = []; }
    const shopSel = document.getElementById('atShopCode');
    const perSel = document.getElementById('atPeriod');
    if (!shopSel) return;
    shopSel.insertAdjacentHTML('beforeend', shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName || s.shopCode)}</option>`).join(''));
    const periods = [...new Set(shopList.map(s => s.adjustPeriodCode).filter(Boolean))].sort();
    perSel.insertAdjacentHTML('beforeend', periods.map(p => `<option value="${esc(p)}">${esc(p)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value;
    return {
      startDate: v('atStartDate'),
      endDate: v('atEndDate'),
      pAdjustPeriodType: v('atPeriod'),
      pShopCode: v('atShopCode'),
      pOrderName: v('atOrderName').trim(),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('atTableWrap');
    const params = getParams();
    if (!params.startDate || !params.endDate) { UI.toast('시작일과 종료일을 입력하세요', 'error'); return; }
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('atPagination').innerHTML = '';
    try {
      const [count, sums] = await Promise.all([
        Api.get('/adjust/process/target/count', params),
        Api.get('/adjust/process/target/sum', params),
      ]);
      totalCount = count;
      renderSummary(sums);
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">정산 대상이 없습니다</div>';
        updateButton();
        return;
      }
      const list = await Api.get('/adjust/process/target/list', {
        ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE,
      });
      renderTable(list);
      UI.pagination(document.getElementById('atPagination'), {
        total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
        onChange: p => { currentPage = p; loadList(); },
      });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderSummary(sums) {
    const by = k => (sums || []).find(s => s.kind === k) || { cnt: 0, saleAmt: 0, supplyAmt: 0, buyAmt: 0 };
    const ship = by('SHIP'), ret = by('RETURN');
    const net = f => Number(ship[f] || 0) + Number(ret[f] || 0);   // 반품 금액은 이미 음수
    document.getElementById('atSummary').innerHTML =
      `전체 <strong>${totalCount.toLocaleString()}</strong>건 — ` +
      `배송(+) <strong>${Number(ship.cnt).toLocaleString()}</strong>건 · 반품(−) <strong>${Number(ret.cnt).toLocaleString()}</strong>건 · ` +
      `판매액 순합 <strong>${won(net('saleAmt'))}</strong> · 공급가 순합 <strong>${won(net('supplyAmt'))}</strong> · 매입가 순합 <strong>${won(net('buyAmt'))}</strong>`;
  }

  function renderTable(list) {
    const rows = list.map((t, i) => `
      <tr>
        <td style="text-align:center"><input type="checkbox" class="at-row" data-i="${i}" ${selected.has(key(t)) ? 'checked' : ''}></td>
        <td style="text-align:center"><span class="badge ${esc(t.kind === 'SHIP' ? 'badge-blue' : 'badge-gray')}">${esc(t.kind === 'SHIP' ? '배송(+)' : '반품(−)')}</span></td>
        <td style="font-size:11px">${esc(t.orderNo)}</td>
        <td style="text-align:right;font-size:11px">${esc(t.orderProdNo)}</td>
        <td style="font-size:12px">${esc(t.shopName || t.shopCode)}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(t.prodName)}">${esc(t.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted);max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(t.itemName)}">${esc(t.itemName)}</td>
        <td style="text-align:right;font-size:11px">${esc(t.prodQty)}</td>
        <td style="text-align:right;font-size:11px">${won(t.salePrice)}</td>
        <td style="text-align:right;font-size:11px">${won(t.supplyPrice)}</td>
        <td style="text-align:right;font-size:11px">${won(t.buyPrice)}</td>
        <td style="text-align:right;font-size:11px">${esc(t.vatRate)}</td>
        <td style="font-size:11px">${esc(t.chnlName)}</td>
        <td style="font-size:11px">${esc(fmtDt(t.cmpletDate))}</td>
      </tr>`).join('');

    const wrap = document.getElementById('atTableWrap');
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1280px">
        <colgroup>
          <col style="width:36px"><col style="width:72px"><col style="width:80px"><col style="width:44px"><col style="width:140px">
          <col style="width:220px"><col style="width:130px"><col style="width:44px"><col style="width:90px"><col style="width:90px">
          <col style="width:90px"><col style="width:56px"><col style="width:110px"><col style="width:84px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="atSelectAll"></th>
            <th>구분</th><th>주문번호</th><th>순번</th><th>상점</th><th>상품명</th><th>옵션</th>
            <th style="text-align:right">수량</th><th style="text-align:right">판매가</th>
            <th style="text-align:right">공급가</th><th style="text-align:right">매입가</th>
            <th style="text-align:right">부가세율</th><th>채널</th><th>정산기준일</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.at-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const t = list[+cb.dataset.i];
        if (cb.checked) selected.set(key(t), t); else selected.delete(key(t));
        updateSelectAllState(list);
        updateButton();
      });
    });
    document.getElementById('atSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.at-row').forEach(cb => {
        const t = list[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.set(key(t), t); else selected.delete(key(t));
      });
      updateButton();
    });
    updateSelectAllState(list);
    updateButton();
  }

  function updateSelectAllState(list) {
    const el = document.getElementById('atSelectAll');
    if (el) el.checked = list.length > 0 && list.every(t => selected.has(key(t)));
  }

  function updateButton() {
    document.getElementById('atSelCount').textContent = selected.size;
    document.getElementById('atBtnCreate').disabled = selected.size === 0;
  }

  // ── 정산자료 생성 ──────────────────────────────────────────────────
  function createSelected() {
    const items = [...selected.values()];
    if (items.length === 0) return;
    const ship = items.filter(t => t.kind === 'SHIP').length;
    UI.confirm(
      `선택한 ${items.length}건(배송 ${ship}건 · 반품 ${items.length - ship}건)을 정산자료로 생성하시겠습니까? ` +
      `생성된 자료는 정산 마감 화면에서 금액을 보정하고 상점별로 마감합니다.`,
      async close => {
        const btn = document.getElementById('atBtnCreate');
        btn.disabled = true;
        try {
          const res = await Api.post('/adjust/process/create', {
            items: items.map(t => ({ kind: t.kind, orderNo: t.orderNo, orderProdNo: t.orderProdNo, orderChangeNo: t.orderChangeNo })),
          });
          if (res.failed > 0) {
            showFailures(res);
          } else {
            UI.toast(`${res.success}건 정산자료가 생성되었습니다`, 'success');
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

  function showFailures(res) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="margin-bottom:8px">성공 <strong>${esc(res.success)}</strong>건 · 실패 <strong style="color:var(--danger)">${esc(res.failed)}</strong>건</div>
      <div style="max-height:260px;overflow:auto;font-size:12px;border:1px solid var(--border);padding:8px;border-radius:4px">
        ${res.failDetails.map(d => `<div>${esc(d)}</div>`).join('')}
      </div>`;
    UI.modal({ title: '정산자료 생성 결과', body, confirmText: null, cancelText: '닫기' });
  }

  return { render };
})();

/**
 * 발주단가표 (사이드 메뉴 발주) — 레거시 wOrdOrderUpriceList
 *   키워드·용량마다 개당 발주단가를 정의하는 마스터(shopwizard.tOrdOrderUprice). 발주단가매칭(출고지시 원가 계산)이 이 코드를 쓴다.
 *   매칭단가코드(기본키)는 서버가 "키워드-용량" 으로 만든다. 수정에서는 상점·단가·단량·사용여부만 바꾼다.
 *   상점을 비우면 모든 서비스가 쓰는 공통 코드(발주단가매칭 화면에서 새로 만든 코드). 단가매칭 규칙이 쓰는 코드는 삭제할 수 없다.
 *   백엔드: /order/uprice (list, count, POST, PUT, DELETE ?matchUpriceCode)
 */
const PageOrderUprice = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  let shopList = [];

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
  const won = n => (n == null || n === '') ? '' : Number(n).toLocaleString();

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>발주단가표</span>
          <button class="btn btn-primary" id="upBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="upShop" style="width:160px"><option value="">전체</option></select></div>
            <div class="form-group"><label>발주상품명칭</label><input class="input" id="upName" style="width:180px"></div>
            <div class="form-group"><label>키워드</label><input class="input" id="upKeywrd" style="width:130px"></div>
            <div class="form-group"><label>용량</label><input class="input" id="upCapa" style="width:90px"></div>
            <div class="form-group"><label>사용여부</label>
              <select class="input" id="upUseYn" style="width:90px"><option value="">전체</option><option value="1">사용</option><option value="0">미사용</option></select></div>
            <button class="btn btn-primary" id="upBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="upTotal">0</strong>건</div>
          <div class="table-wrap" id="upWrap"></div>
          <div id="upPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('upBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('upBtnNew').addEventListener('click', () => openModal(null));
    ['upName', 'upKeywrd', 'upCapa'].forEach(id =>
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadShops().then(loadList);
  }

  async function loadShops() {
    try {
      const shops = await Api.get('/company/shop', {});
      shopList = (shops || []).filter(s => s.svcCode === svc()).sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { shopList = []; }
    document.getElementById('upShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('upShop')) p.pShopCode = v('upShop');
    if (v('upName')) p.pOrderProdName = v('upName');
    if (v('upKeywrd')) p.pKeywrd = v('upKeywrd');
    if (v('upCapa')) p.pCapa = v('upCapa');
    if (v('upUseYn')) p.pUseYn = v('upUseYn');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('upWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/uprice/count', params);
      document.getElementById('upTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('upPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/uprice/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('upPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('upWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>매칭단가코드</th><th>상점</th><th>발주상품명칭</th><th>키워드</th><th>용량</th>
          <th style="text-align:right">단가</th><th>단량</th><th style="text-align:center">사용</th><th>결과</th><th></th></tr></thead>
        <tbody>${list.map((u, i) => `
          <tr>
            <td style="color:var(--text-muted)">${esc(u.matchUpriceCode)}</td>
            <td>${u.shopCode ? esc(u.shopName || u.shopCode) : '<span style="color:var(--text-muted)">공통</span>'}</td>
            <td style="max-width:240px">${esc(u.orderProdName)}</td>
            <td>${esc(u.keywrd)}</td>
            <td>${esc(u.capa)}</td>
            <td style="text-align:right">${won(u.uprice)}</td>
            <td>${esc(u.unitName)}</td>
            <td style="text-align:center"><span class="badge ${u.useYn === 1 ? 'badge-green' : 'badge-gray'}">${u.useYn === 1 ? '사용' : '미사용'}</span></td>
            <td style="color:var(--text-muted)">${esc(u.resultType)}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const u = list[+b.dataset.del];
      UI.confirm(`[${u.matchUpriceCode}] 발주단가를 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/order/uprice', { matchUpriceCode: u.matchUpriceCode });
          UI.toast('삭제되었습니다', 'success');
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
        close();
      });
    }));
  }

  function openModal(row) {
    const isNew = !row;
    const v = row || {};
    const lock = isNew ? '' : 'readonly style="background:#f1f5f9"';
    const shopOpts = shopList.map(s => `<option value="${esc(s.shopCode)}" ${v.shopCode === s.shopCode ? 'selected' : ''}>${esc(s.shopName)}</option>`).join('');
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group"><label>상점</label>
          <select class="input" id="upFShop"><option value="">공통(모든 서비스)</option>${shopOpts}</select></div>
        <div class="form-group"><label>사용여부</label>
          <select class="input" id="upFUse"><option value="1" ${v.useYn !== 0 ? 'selected' : ''}>사용</option><option value="0" ${v.useYn === 0 ? 'selected' : ''}>미사용</option></select></div>
        <div class="form-group full"><label>발주상품명칭 *</label><input class="input" id="upFName" maxlength="255" value="${esc(v.orderProdName)}" ${lock}></div>
        <div class="form-group"><label>키워드 *</label><input class="input" id="upFKeywrd" maxlength="50" value="${esc(v.keywrd)}" ${lock}></div>
        <div class="form-group"><label>용량 *</label><input class="input" id="upFCapa" maxlength="50" value="${esc(v.capa)}" placeholder="예: 1kg" ${lock}></div>
        <div class="form-group"><label>단가(개당, 원) *</label><input class="input" id="upFUprice" type="number" min="0" value="${v.uprice ?? ''}"></div>
        <div class="form-group"><label>단량</label><input class="input" id="upFUnit" maxlength="50" value="${esc(v.unitName)}"></div>
        <div class="form-group full"><label>매칭단가코드(자동)</label>
          <div class="input" id="upFCode" style="background:#f8fafc;min-height:34px;line-height:34px">-</div></div>
      </div>
      ${isNew ? '' : '<div style="font-size:12px;color:var(--text-muted);margin-top:8px">발주상품명칭·키워드·용량은 매칭단가코드의 재료라 수정할 수 없습니다. 바꾸려면 새로 등록하세요.</div>'}`;
    const q = id => body.querySelector('#' + id);
    const refreshCode = () => {
      const kw = q('upFKeywrd').value.trim(), capa = q('upFCapa').value.trim();
      q('upFCode').textContent = (kw && capa) ? `${kw}-${capa}` : '-';
    };
    q('upFKeywrd').addEventListener('input', refreshCode);
    q('upFCapa').addEventListener('input', refreshCode);
    refreshCode();

    UI.modal({
      title: isNew ? '발주단가 등록' : `발주단가 수정 – ${v.matchUpriceCode}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        if (isNew && (!val('upFName') || !val('upFKeywrd') || !val('upFCapa'))) return UI.toast('발주상품명칭·키워드·용량을 입력하세요', 'error');
        if (val('upFUprice') === '' || Number(val('upFUprice')) < 0) return UI.toast('단가를 0 이상으로 입력하세요', 'error');
        const payload = {
          shopCode: val('upFShop'), uprice: Number(val('upFUprice')), unitName: val('upFUnit'), useYn: Number(val('upFUse')),
          ...(isNew
            ? { orderProdName: val('upFName'), keywrd: val('upFKeywrd'), capa: val('upFCapa') }
            : { matchUpriceCode: v.matchUpriceCode }),
        };
        try {
          if (isNew) await Api.post('/order/uprice', payload); else await Api.put('/order/uprice', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

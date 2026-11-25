/**
 * 발주상품코드 (사이드 메뉴 발주) — 레거시 wOrdOrderProdCodeList
 *   키워드와 용량 조합마다 상점 상품코드를 정의하는 마스터(shopwizard.tOrdOrderProdCode).
 *   매칭상품코드(기본키)는 서버가 "키워드::용량1x수량1[+용량2x수량2]" 로 만든다. 수정에서는 키워드·용량을 바꿀 수 없다(코드의 재료).
 *   백엔드: /order/prod-code (list, count, POST, PUT, DELETE ?matchProdCode)
 */
const PageOrderProdCode = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  let shopList = [];   // { shopCode, shopName } — 로그인 계정의 서비스 상점

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
  const shopName = code => (shopList.find(s => s.shopCode === code) || {}).shopName || '';

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>발주상품코드</span>
          <button class="btn btn-primary" id="opcBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="opcShop" style="width:160px"><option value="">전체</option></select></div>
            <div class="form-group"><label>상품코드</label><input class="input" id="opcProdCode" style="width:130px"></div>
            <div class="form-group"><label>상품명칭</label><input class="input" id="opcProdName" style="width:170px"></div>
            <div class="form-group"><label>키워드</label><input class="input" id="opcKeywrd" style="width:130px"></div>
            <div class="form-group"><label>용량1</label><input class="input" id="opcCapa1" style="width:90px"></div>
            <div class="form-group"><label>사용여부</label>
              <select class="input" id="opcUseYn" style="width:90px"><option value="">전체</option><option value="1">사용</option><option value="0">미사용</option></select></div>
            <button class="btn btn-primary" id="opcBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="opcTotal">0</strong>건</div>
          <div class="table-wrap" id="opcWrap"></div>
          <div id="opcPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('opcBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('opcBtnNew').addEventListener('click', () => openModal(null));
    ['opcProdCode', 'opcProdName', 'opcKeywrd', 'opcCapa1'].forEach(id =>
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadShops().then(loadList);
  }

  async function loadShops() {
    try {
      const shops = await Api.get('/company/shop', {});
      shopList = (shops || []).filter(s => s.svcCode === svc()).sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { shopList = []; }
    document.getElementById('opcShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('opcShop')) p.pShopCode = v('opcShop');
    if (v('opcProdCode')) p.pProdCode = v('opcProdCode');
    if (v('opcProdName')) p.pProdName = v('opcProdName');
    if (v('opcKeywrd')) p.pKeywrd = v('opcKeywrd');
    if (v('opcCapa1')) p.pCapa1 = v('opcCapa1');
    if (v('opcUseYn')) p.pUseYn = v('opcUseYn');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('opcWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/prod-code/count', params);
      document.getElementById('opcTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('opcPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/prod-code/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('opcPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  const capaText = (c, q) => (c ? `${esc(c)} × ${esc(q)}` : '-');

  function renderTable(list) {
    const wrap = document.getElementById('opcWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>매칭상품코드</th><th>상점</th><th>상품코드</th><th>상품명칭</th><th>키워드</th><th>용량1</th><th>용량2</th>
          <th style="text-align:center">사용</th><th>결과</th><th></th></tr></thead>
        <tbody>${list.map((c, i) => `
          <tr>
            <td style="color:var(--text-muted)">${esc(c.matchProdCode)}</td>
            <td>${esc(c.shopName || shopName(c.shopCode) || c.shopCode)}</td>
            <td>${esc(c.prodCode)}</td>
            <td style="max-width:240px">${esc(c.prodName)}</td>
            <td>${esc(c.keywrd)}</td>
            <td>${capaText(c.capa1, c.qty1)}</td>
            <td>${capaText(c.capa2, c.qty2)}</td>
            <td style="text-align:center"><span class="badge ${c.useYn === 1 ? 'badge-green' : 'badge-gray'}">${c.useYn === 1 ? '사용' : '미사용'}</span></td>
            <td style="color:var(--text-muted)">${esc(c.resultType)}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const c = list[+b.dataset.del];
      UI.confirm(`[${c.matchProdCode}] 발주상품코드를 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/order/prod-code', { matchProdCode: c.matchProdCode });
          UI.toast('삭제되었습니다', 'success');
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
        close();
      });
    }));
  }

  // ── 등록/수정 모달 ─────────────────────────────────────────────────
  function openModal(row) {
    const isNew = !row;
    const v = row || {};
    const lock = isNew ? '' : 'readonly style="background:#f1f5f9"';
    const shopOpts = shopList.map(s => `<option value="${esc(s.shopCode)}" ${v.shopCode === s.shopCode ? 'selected' : ''}>${esc(s.shopName)}</option>`).join('');
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group"><label>상점 *</label>
          <select class="input" id="opcFShop"><option value="">선택</option>${shopOpts}</select></div>
        <div class="form-group"><label>사용여부</label>
          <select class="input" id="opcFUse"><option value="1" ${v.useYn !== 0 ? 'selected' : ''}>사용</option><option value="0" ${v.useYn === 0 ? 'selected' : ''}>미사용</option></select></div>
        <div class="form-group"><label>상품코드 *</label><input class="input" id="opcFProdCode" maxlength="255" value="${esc(v.prodCode)}" placeholder="상점 상품코드"></div>
        <div class="form-group"><label>상품명칭 *</label><input class="input" id="opcFProdName" maxlength="255" value="${esc(v.prodName)}"></div>
        <div class="form-group"><label>키워드 *</label><input class="input" id="opcFKeywrd" maxlength="50" value="${esc(v.keywrd)}" ${lock}></div>
        <div class="form-group"></div>
        <div class="form-group"><label>용량1 *</label><input class="input" id="opcFCapa1" maxlength="50" value="${esc(v.capa1)}" placeholder="예: 1kg" ${lock}></div>
        <div class="form-group"><label>수량1 *</label><input class="input" id="opcFQty1" maxlength="5" value="${esc(v.qty1)}" placeholder="예: 3" ${lock}></div>
        <div class="form-group"><label>용량2</label><input class="input" id="opcFCapa2" maxlength="50" value="${esc(v.capa2)}" ${lock}></div>
        <div class="form-group"><label>수량2</label><input class="input" id="opcFQty2" maxlength="5" value="${esc(v.qty2)}" ${lock}></div>
        <div class="form-group full"><label>매칭상품코드(자동)</label>
          <div class="input" id="opcFCode" style="background:#f8fafc;min-height:34px;line-height:34px">-</div></div>
      </div>
      ${isNew ? '' : '<div style="font-size:12px;color:var(--text-muted);margin-top:8px">키워드·용량은 매칭상품코드의 재료라 수정할 수 없습니다. 바꾸려면 새로 등록하세요.</div>'}`;

    const q = id => body.querySelector('#' + id);
    const refreshCode = () => {
      const kw = q('opcFKeywrd').value.trim(), c1 = q('opcFCapa1').value.trim(), q1 = q('opcFQty1').value.trim();
      const c2 = q('opcFCapa2').value.trim(), q2 = q('opcFQty2').value.trim();
      q('opcFCode').textContent = (kw && c1 && q1) ? `${kw}::${c1}x${q1}${c2 ? `+${c2}x${q2}` : ''}` : '-';
    };
    ['opcFKeywrd', 'opcFCapa1', 'opcFQty1', 'opcFCapa2', 'opcFQty2'].forEach(id => q(id).addEventListener('input', refreshCode));
    refreshCode();

    UI.modal({
      title: isNew ? '발주상품코드 등록' : `발주상품코드 수정 – ${v.matchProdCode}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        if (!val('opcFShop')) return UI.toast('상점을 선택하세요', 'error');
        if (!val('opcFProdCode')) return UI.toast('상품코드를 입력하세요', 'error');
        if (!val('opcFProdName')) return UI.toast('상품명칭을 입력하세요', 'error');
        if (isNew && (!val('opcFKeywrd') || !val('opcFCapa1') || !val('opcFQty1'))) return UI.toast('키워드·용량1·수량1을 입력하세요', 'error');
        const payload = {
          shopCode: val('opcFShop'), prodCode: val('opcFProdCode'), prodName: val('opcFProdName'), useYn: Number(val('opcFUse')),
          ...(isNew
            ? { keywrd: val('opcFKeywrd'), capa1: val('opcFCapa1'), qty1: val('opcFQty1'), capa2: val('opcFCapa2'), qty2: val('opcFQty2') }
            : { matchProdCode: v.matchProdCode }),
        };
        try {
          if (isNew) await Api.post('/order/prod-code', payload); else await Api.put('/order/prod-code', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

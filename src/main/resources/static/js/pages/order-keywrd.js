/**
 * 키워드규칙 (사이드 메뉴 발주) — 레거시 wOrdOrderKeywrdList(발주상품명키워드)
 *   상점 미지정 주문 라인을 자동발주의 "키워드로 대상 가져오기"가 그 상점으로 배정하는 기준이다(shopwizard.tOrdOrderKeywrd).
 *   LIKE = 상품명에 포함, LIKE OPTION = 옵션명에 포함 → 하나라도 맞으면 후보. NOT LIKE / NOT LIKE OPTION = 포함되면 제외.
 *   사용 여부가 '사용'인 키워드만 쓰이며, 상점에 LIKE 계열 키워드가 없으면 가져올 라인이 없다.
 *   백엔드: /order/keywrd (list, count, POST, PUT, DELETE ?keywrdSeq)
 */
const PageOrderKeywrd = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  const SECTS = ['LIKE', 'LIKE OPTION', 'NOT LIKE', 'NOT LIKE OPTION'];
  const SECT_HELP = {
    'LIKE': '상품명에 포함되면 후보', 'LIKE OPTION': '옵션명에 포함되면 후보',
    'NOT LIKE': '상품명에 포함되면 제외', 'NOT LIKE OPTION': '옵션명에 포함되면 제외',
  };
  let currentPage = 1;
  let shopList = [];

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>키워드규칙 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 상점 미지정 주문을 상점으로 가져오는 상품명 키워드</span></span>
          <button class="btn btn-primary" id="kwBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            자동발주의 <b>"키워드로 대상 가져오기"</b>가 이 키워드로 <b>상점이 지정되지 않은 지불완료 주문</b>을 해당 상점에 배정합니다.<br>
            · <b>LIKE</b>(상품명에 포함) · <b>LIKE OPTION</b>(옵션명에 포함): 하나라도 맞으면 그 상점의 후보입니다. 상점에 이 두 구분의 키워드가 없으면 가져올 주문이 없습니다.<br>
            · <b>NOT LIKE</b> · <b>NOT LIKE OPTION</b>: 상품명/옵션명에 포함되면 후보에서 <b>제외</b>합니다. 대소문자는 구분하지 않고, 이미 다른 상점에 배정된 주문은 건드리지 않습니다.<br>
            · 사용 여부가 '사용'인 키워드만 적용됩니다.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="kwShop" style="width:180px"><option value="">전체</option></select></div>
            <div class="form-group"><label>구분</label>
              <select class="input" id="kwSect" style="width:150px"><option value="">전체</option>${SECTS.map(s => `<option>${s}</option>`).join('')}</select></div>
            <div class="form-group"><label>키워드</label><input class="input" id="kwName" style="width:160px"></div>
            <button class="btn btn-primary" id="kwBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="kwTotal">0</strong>건</div>
          <div class="table-wrap" id="kwWrap"></div>
          <div id="kwPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('kwBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('kwBtnNew').addEventListener('click', () => openModal(null));
    document.getElementById('kwName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    loadOptions().then(loadList);
  }

  async function loadOptions() {
    try {
      const shops = await Api.get('/company/shop', {});
      shopList = (shops || []).filter(s => s.svcCode === svc()).sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { shopList = []; }
    document.getElementById('kwShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('kwShop')) p.pShopCode = v('kwShop');
    if (v('kwSect')) p.pKeywrdSect = v('kwSect');
    if (v('kwName')) p.pKeywrdName = v('kwName');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('kwWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/keywrd/count', params);
      document.getElementById('kwTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('kwPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/keywrd/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('kwPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('kwWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>번호</th><th>상점</th><th>구분</th><th>키워드</th><th>의미</th><th>사용</th><th>비고</th><th></th></tr></thead>
        <tbody>${list.map((r, i) => `
          <tr>
            <td style="color:var(--text-muted)">${esc(r.keywrdSeq)}</td>
            <td>${esc(r.shopName || r.shopCode)}</td>
            <td><span style="${r.keywrdSect.startsWith('NOT') ? 'color:#b45309' : ''}">${esc(r.keywrdSect)}</span></td>
            <td><b>${esc(r.keywrdName)}</b></td>
            <td style="color:var(--text-muted)">${esc(SECT_HELP[r.keywrdSect] || '')}</td>
            <td>${Number(r.state) === 1 ? '사용' : '<span style="color:var(--text-muted)">안 함</span>'}</td>
            <td>${esc(r.remark)}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const r = list[+b.dataset.del];
      UI.confirm(`[${r.shopName || r.shopCode}] ${r.keywrdSect} "${r.keywrdName}" 키워드를 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/order/keywrd', { keywrdSeq: r.keywrdSeq });
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
    const shopOpts = shopList.map(s => `<option value="${esc(s.shopCode)}" ${v.shopCode === s.shopCode ? 'selected' : ''}>${esc(s.shopName)}</option>`).join('');
    const shopExtra = (v.shopCode && !shopList.some(s => s.shopCode === v.shopCode))
      ? `<option value="${esc(v.shopCode)}" selected>${esc(v.shopName || v.shopCode)} (현재값)</option>` : '';
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group"><label>상점 *</label>
          <select class="input" id="kwFShop"><option value="">선택</option>${shopOpts}${shopExtra}</select></div>
        <div class="form-group"><label>구분 *</label>
          <select class="input" id="kwFSect">${SECTS.map(s => `<option ${(v.keywrdSect || 'LIKE') === s ? 'selected' : ''}>${s}</option>`).join('')}</select></div>
        <div class="form-group full"><label>키워드 * <span style="font-weight:400;color:var(--text-muted)">(50자 이내, 상품명/옵션명에 포함되는지 봅니다)</span></label>
          <input class="input" id="kwFName" maxlength="50" value="${esc(v.keywrdName)}"></div>
        <div class="form-group"><label>사용 여부</label>
          <select class="input" id="kwFState"><option value="1" ${v.state == null || Number(v.state) === 1 ? 'selected' : ''}>사용</option><option value="0" ${v.state != null && Number(v.state) !== 1 ? 'selected' : ''}>사용 안 함</option></select></div>
        <div class="form-group"><label>비고</label><input class="input" id="kwFRemark" maxlength="50" value="${esc(v.remark)}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:6px">LIKE/LIKE OPTION: 포함되면 후보 · NOT LIKE/NOT LIKE OPTION: 포함되면 제외. 같은 상점·구분에 같은 키워드는 한 번만 등록할 수 있습니다.</div>`;
    const q = id => body.querySelector('#' + id);

    const modal = UI.modal({
      title: isNew ? '키워드 등록' : `키워드 수정 – ${v.keywrdSeq}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        if (!val('kwFShop')) return UI.toast('상점을 선택하세요', 'error');
        if (!val('kwFName')) return UI.toast('키워드를 입력하세요', 'error');
        const payload = { shopCode: val('kwFShop'), keywrdSect: val('kwFSect'), keywrdName: val('kwFName'), state: Number(val('kwFState')), remark: val('kwFRemark') };
        if (!isNew) payload.keywrdSeq = v.keywrdSeq;
        try {
          if (isNew) await Api.post('/order/keywrd', payload); else await Api.put('/order/keywrd', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
    modal.el.querySelector('.modal').style.width = '560px';
  }

  return { render };
})();

/**
 * 배송제한규칙 (사이드 메뉴 발주) — 레거시 피죤 전용 SQL(rCreateShipPigeon3)의 일반화
 *   큰 품목처럼 배송 제한이 있는 상점은 우편번호·주소에 따라 발주서 엑셀의 <b>시트</b>(출고지)를 나눈다 — 예: 택배비인상 · 배송불가.
 *   조건: 우편번호 구분(배송제한우편번호 화면의 목록) 또는 주소 패턴(LIKE, 줄마다 하나) 중 하나라도 맞고(OR),
 *   발주서 상품명 포함 · 제외 채널은 모두 만족(AND). 우편번호 구분과 주소 패턴이 모두 비면 상점의 모든 주문이 대상(기본 시트 규칙).
 *   규칙 번호 순으로 적용해 나중 규칙이 앞 규칙의 시트를 덮어쓰고, 수취인 표시(★ ■ …)는 누적된다.
 *   상점 관리의 "배송제한 규칙 사용"이 켜진 상점에만 자동발주 "배송 데이터 생성"에서 적용된다.
 *   백엔드: /ship/place-rule (list, count, zip-count, POST, PUT, DELETE ?ruleNo)
 */
const PageShipPlaceRule = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  let shopList = [];
  let chnlList = [];
  let sectList = [];

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
  const lines = s => (s || '').split(/\r?\n/).map(x => x.trim()).filter(Boolean);

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>배송제한규칙 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 우편번호·주소로 발주서 시트 나누기</span></span>
          <button class="btn btn-primary" id="prBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            배송 제한이 있는 상점의 발주서를 우편번호·주소에 따라 <b>시트</b>(예: 택배비인상, 배송불가)로 나눕니다. 조건은 <b>우편번호 구분 또는 주소 패턴</b> 중 하나만 맞아도 되고(OR),
            <b>발주서 상품명 포함·제외 채널</b>은 모두 만족해야 합니다(AND). 구분·주소 패턴이 모두 비면 상점의 모든 주문이 대상입니다(기본 시트).<br>
            · 규칙은 <b>번호 순</b>으로 적용되고 나중 규칙이 앞 규칙의 시트를 덮어씁니다. 수취인 표시(★ ■ 등)는 이름 앞에 한 번만 붙고 규칙을 지워도 남습니다.<br>
            · <b>상점 관리의 "배송제한 규칙 사용"이 '예'인 상점</b>만 자동발주의 "배송 데이터 생성"에서 적용됩니다.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="prShop" style="width:180px"><option value="">전체</option></select></div>
            <div class="form-group"><label>시트명</label><input class="input" id="prPlace" style="width:130px"></div>
            <button class="btn btn-primary" id="prBtnSearch" type="button">검색</button>
          </div>
          <div id="prShopFlag" style="font-size:12px;margin:8px 0"></div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="prTotal">0</strong>건</div>
          <div class="table-wrap" id="prWrap"></div>
          <div id="prPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('prBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); showFlag(); });
    document.getElementById('prBtnNew').addEventListener('click', () => openModal(null));
    document.getElementById('prShop').addEventListener('change', () => { currentPage = 1; loadList(); showFlag(); });
    document.getElementById('prPlace').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    loadOptions().then(loadList);
  }

  async function loadOptions() {
    try {
      const [shops, chnls, sects] = await Promise.all([
        Api.get('/company/shop', {}),
        Api.get('/company/chnl', { pSvcCode: svc(), pPageOffset: 0, pPageSize: 2000 }),
        Api.get('/ship/rstrct-zipcode/sects', {}).catch(() => []),
      ]);
      shopList = (shops || []).filter(s => s.svcCode === svc()).sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
      chnlList = (chnls || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'));
      sectList = sects || [];
    } catch (_) { shopList = []; chnlList = []; }
    document.getElementById('prShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}${s.shipRstrctYn === 1 ? ' ✔' : ''}</option>`).join(''));
  }

  /** 상점을 고르면 그 상점의 "배송제한 규칙 사용" 상태를 보여 준다. */
  function showFlag() {
    const code = document.getElementById('prShop').value;
    const box = document.getElementById('prShopFlag');
    const s = shopList.find(x => x.shopCode === code);
    if (!s) { box.innerHTML = ''; return; }
    box.innerHTML = s.shipRstrctYn === 1
      ? `<span style="color:var(--success,#2e7d32)">✔ ${esc(s.shopName)}: 배송제한 규칙 <b>사용</b> — 자동발주 "배송 데이터 생성"에서 아래 사용 중인 규칙이 적용됩니다.</span>`
      : `<span style="color:#b45309">⚠ ${esc(s.shopName)}: 배송제한 규칙 <b>사용 안 함</b> — 아래 규칙은 저장만 되고 적용되지 않습니다. 상점 관리에서 "배송제한 규칙 사용"을 '예'로 바꾸세요.</span>`;
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('prShop')) p.pShopCode = v('prShop');
    if (v('prPlace')) p.pShipPlace = v('prPlace');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('prWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/ship/place-rule/count', params);
      document.getElementById('prTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('prPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/ship/place-rule/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('prPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function condText(r) {
    const parts = [];
    if (r.zipSect) parts.push(`우편번호 <b>${esc(r.zipSect)}</b>`);
    const n = lines(r.addrPatterns).length;
    if (n) parts.push(`주소 패턴 ${n}개`);
    if (!parts.length) parts.push('<span style="color:#b45309">모든 주문</span>');
    let t = parts.join(' 또는 ');
    if (r.prodName) t += ` · 발주서 상품명에 "${esc(r.prodName)}"`;
    const ex = lines(r.exceptAddrPatterns).length;
    if (ex) t += ` · 제외 주소 ${ex}개`;
    const ec = (r.exceptChnlCodes || '').split(',').filter(Boolean).length;
    if (ec) t += ` · 제외 채널 ${ec}개`;
    return t;
  }

  function renderTable(list) {
    const wrap = document.getElementById('prWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>번호</th><th>상점</th><th>규칙명</th><th>조건</th><th>시트</th><th>수취인 표시</th><th>사용</th><th></th></tr></thead>
        <tbody>${list.map((r, i) => `
          <tr>
            <td style="color:var(--text-muted)">${esc(r.ruleNo)}</td>
            <td>${esc(r.shopName || r.shopCode)}</td>
            <td>${esc(r.ruleName)}</td>
            <td style="max-width:360px">${condText(r)}</td>
            <td><b>${esc(r.shipPlace)}</b></td>
            <td>${esc(r.namePrefix)}</td>
            <td>${Number(r.state) === 1 ? '사용' : '<span style="color:var(--text-muted)">안 함</span>'}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const r = list[+b.dataset.del];
      UI.confirm(`[규칙 ${r.ruleNo}] ${r.shopName || r.shopCode} ${r.ruleName || r.shipPlace} 규칙을 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/ship/place-rule', { ruleNo: r.ruleNo });
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
    const exceptSet = new Set((v.exceptChnlCodes || '').split(',').filter(Boolean));
    const chnlChecks = chnlList.map(c => `
      <label style="display:inline-flex;align-items:center;gap:4px;font-size:12px;margin:2px 10px 2px 0">
        <input type="checkbox" class="prFExChnl" value="${esc(c.chnlCode)}" ${exceptSet.has(c.chnlCode) ? 'checked' : ''}> ${esc(c.chnlName)}</label>`).join('');
    // 목록에 없는 기존 제외 채널 코드는 그대로 보존한다(수정하다 조용히 사라지지 않게)
    const unknownCh = [...exceptSet].filter(c => !chnlList.some(x => x.chnlCode === c));
    const sectOptions = `<option value="">(사용 안 함)</option>` + sectList.map(s => `<option value="${esc(s.sect)}" ${v.zipSect === s.sect ? 'selected' : ''}>${esc(s.sect)}${s.label ? ' — ' + esc(s.label) : ''} (${Number(s.cnt).toLocaleString()}건)</option>`).join('')
      + (v.zipSect && !sectList.some(s => s.sect === v.zipSect) ? `<option value="${esc(v.zipSect)}" selected>${esc(v.zipSect)} (등록된 우편번호 없음)</option>` : '');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group"><label>상점 *</label>
          <select class="input" id="prFShop"><option value="">선택</option>${shopOpts}${shopExtra}</select></div>
        <div class="form-group"><label>규칙명</label><input class="input" id="prFName" maxlength="50" value="${esc(v.ruleName)}" placeholder="예: 택배비인상 ★"></div>
        <div class="form-group"><label>시트명 * <span style="font-weight:400;color:var(--text-muted)">(31자 이하)</span></label>
          <input class="input" id="prFPlace" maxlength="31" value="${esc(v.shipPlace)}" placeholder="예: 택배비인상"></div>
        <div class="form-group"><label>수취인 표시</label><input class="input" id="prFPrefix" maxlength="10" value="${esc(v.namePrefix)}" placeholder="예: ★ (이름 앞에 붙음)"></div>
        <div class="form-group"><label>우편번호 구분</label><select class="input" id="prFSect">${sectOptions}</select></div>
        <div class="form-group"><label>발주서 상품명(포함)</label><input class="input" id="prFProd" maxlength="50" value="${esc(v.prodName)}" placeholder="예: 말통 — 비우면 조건 없음"><div style="font-size:11px;color:var(--text-muted)">채널 상품명이 아니라 <b>발주서에 찍히는 상품명(발주단가표의 발주상품명칭)</b>에 포함되는지 봅니다</div></div>
        <div class="form-group full"><label>주소 패턴 <span style="font-weight:400;color:var(--text-muted)">(한 줄에 하나, LIKE 패턴: % = 아무 글자, _ = 한 글자 — 예 %경기%광주%중대동%)</span></label>
          <textarea class="input" id="prFAddr" rows="5" style="width:100%;font-family:monospace">${esc(v.addrPatterns)}</textarea></div>
        <div class="form-group full"><label>제외 주소 패턴 <span style="font-weight:400;color:var(--text-muted)">(이 패턴에 맞으면 이 규칙에서 제외)</span></label>
          <textarea class="input" id="prFExAddr" rows="2" style="width:100%;font-family:monospace">${esc(v.exceptAddrPatterns)}</textarea></div>
        <div class="form-group full"><label>제외 채널 <span style="font-weight:400;color:var(--text-muted)">(체크한 채널은 이 규칙을 적용하지 않음)</span></label>
          <div style="max-height:110px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius);padding:6px 8px">${chnlChecks || '<span style="color:var(--text-muted);font-size:12px">채널 없음</span>'}</div></div>
        <div class="form-group"><label>사용 여부</label>
          <select class="input" id="prFState"><option value="1" ${v.state == null || Number(v.state) === 1 ? 'selected' : ''}>사용</option><option value="0" ${v.state != null && Number(v.state) !== 1 ? 'selected' : ''}>사용 안 함</option></select></div>
        <div class="form-group"><label>비고</label><input class="input" id="prFRemark" maxlength="50" value="${esc(v.remark)}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:6px">우편번호 구분과 주소 패턴이 <b>모두 비면 상점의 모든 주문</b>이 대상입니다. 시트명에는 [ ] : * ? / \\ 를 쓸 수 없습니다.</div>`;
    const q = id => body.querySelector('#' + id);

    const modal = UI.modal({
      title: isNew ? '배송제한규칙 등록' : `배송제한규칙 수정 – 규칙 ${v.ruleNo}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        if (!val('prFShop')) return UI.toast('상점을 선택하세요', 'error');
        if (!val('prFPlace')) return UI.toast('시트명을 입력하세요', 'error');
        if (/[\[\]:*?\/\\]/.test(val('prFPlace'))) return UI.toast('시트명에는 [ ] : * ? / \\ 를 쓸 수 없습니다', 'error');
        const exChnl = [...body.querySelectorAll('.prFExChnl:checked')].map(c => c.value).concat(unknownCh);
        const payload = {
          shopCode: val('prFShop'), ruleName: val('prFName'), shipPlace: val('prFPlace'), namePrefix: val('prFPrefix'),
          zipSect: val('prFSect'), prodName: val('prFProd'), addrPatterns: q('prFAddr').value, exceptAddrPatterns: q('prFExAddr').value,
          exceptChnlCodes: exChnl.join(','), state: Number(val('prFState')), remark: val('prFRemark'),
        };
        if (!isNew) payload.ruleNo = v.ruleNo;
        try {
          if (isNew) await Api.post('/ship/place-rule', payload); else await Api.put('/ship/place-rule', payload);
          if (payload.zipSect) {
            const z = await Api.get('/ship/place-rule/zip-count', { sect: payload.zipSect }).catch(() => null);
            if (z && !z.count) UI.toast(`저장했습니다. 단, 우편번호 구분 "${payload.zipSect}" 에는 아직 등록된 우편번호가 없습니다(배송제한우편번호에서 등록)`, 'error');
            else UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          } else UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
    modal.el.querySelector('.modal').style.width = '760px';
  }

  return { render };
})();

/**
 * 사은품규칙 (사이드 메뉴 발주) — 레거시 wOrdOrderGiftRuleList(발주 사은품 규칙)
 *   상점 단위 규칙(shopwizard.tOrdOrderGiftRule): 조건(채널·채널상품명·딜명·증정 기간·총 주문 수량/금액 구간)에 맞는 주문에
 *   사은품(1번 필수, 2~5 선택)을 증정수량만큼 붙인다. 구간의 0 은 "제한 없음".
 *   사은품 코드는 발주단가표의 코드(예: 증정마스크-소)이고, 명칭은 발주서에 찍히는 이름이다(관례상 '힣' 접두).
 *   중복여부: 0 = 수신자당 한 세트만, N = 총 주문수량 N개마다 증정수량을 곱함.
 *   사용 여부가 '사용'인 규칙만 자동발주의 "배송 데이터 생성"에서 발주서 사은품 행으로 반영된다.
 *   백엔드: /order/gift-rule (list, count, POST, PUT, DELETE ?ruleNo), 사은품 코드 선택 /order/uprice/list
 */
const PageOrderGiftRule = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  let shopList = [];
  let chnlList = [];

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
  const won = n => (n == null || n === '') ? '' : Number(n).toLocaleString();
  const day = s => (s || '').slice(0, 10);

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>사은품규칙</span>
          <button class="btn btn-primary" id="grBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            상점별로 "어떤 주문에 어떤 사은품을 몇 개 줄지"를 정해 두는 규칙입니다. 수량·금액 구간의 <b>0은 제한 없음</b>이며, 채널·채널상품명·딜명은 비워 두면 조건에서 빠집니다.<br>
            <b>사용 여부가 '사용'인 규칙</b>만 자동발주의 "배송 데이터 생성" 단계에서 발주서 사은품 행으로 반영됩니다(규칙 번호 순, 규칙끼리 서로 영향을 주지 않고 각각 적용).
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="grShop" style="width:160px"><option value="">전체</option></select></div>
            <div class="form-group"><label>채널</label>
              <select class="input" id="grChnl" style="width:150px"><option value="">전체</option></select></div>
            <div class="form-group"><label>채널상품명</label><input class="input" id="grChnlProd" style="width:150px"></div>
            <div class="form-group"><label>딜명</label><input class="input" id="grDeal" style="width:110px"></div>
            <div class="form-group"><label>사은품</label><input class="input" id="grGift" style="width:130px" placeholder="명칭 또는 코드"></div>
            <button class="btn btn-primary" id="grBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="grTotal">0</strong>건</div>
          <div class="table-wrap" id="grWrap"></div>
          <div id="grPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('grBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('grBtnNew').addEventListener('click', () => openModal(null));
    ['grChnlProd', 'grDeal', 'grGift'].forEach(id =>
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadOptions().then(loadList);
  }

  async function loadOptions() {
    try {
      const [shops, chnls] = await Promise.all([
        Api.get('/company/shop', {}),
        Api.get('/company/chnl', { pSvcCode: svc(), pPageOffset: 0, pPageSize: 2000 }),
      ]);
      shopList = (shops || []).filter(s => s.svcCode === svc()).sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
      chnlList = (chnls || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'));
    } catch (_) { shopList = []; chnlList = []; }
    document.getElementById('grShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
    document.getElementById('grChnl').insertAdjacentHTML('beforeend',
      chnlList.map(c => `<option value="${esc(c.chnlCode)}">${esc(c.chnlName)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('grShop')) p.pShopCode = v('grShop');
    if (v('grChnl')) p.pChnlCode = v('grChnl');
    if (v('grChnlProd')) p.pChnlProdName = v('grChnlProd');
    if (v('grDeal')) p.pDealName = v('grDeal');
    if (v('grGift')) p.pGiftProdName = v('grGift');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('grWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/gift-rule/count', params);
      document.getElementById('grTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('grPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/gift-rule/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('grPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  /** 시작~종료 구간 표기(0 = 제한 없음). */
  function rangeText(start, end, fmt) {
    const s = Number(start) || 0, e = Number(end) || 0;
    if (!s && !e) return '<span style="color:var(--text-muted)">제한 없음</span>';
    if (!e) return `${fmt(s)} 이상`;
    return `${fmt(s)} ~ ${fmt(e)}`;
  }

  function extraGifts(r) {
    return [2, 3, 4, 5].filter(n => r['giftProdName' + n] || r['giftProdCode' + n]).map(n => r['giftProdName' + n] || r['giftProdCode' + n]);
  }

  function renderTable(list) {
    const wrap = document.getElementById('grWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>번호</th><th>상점</th><th>채널</th><th>채널상품명</th><th>딜명</th>
          <th>총 주문수량</th><th>총 주문금액</th><th>사은품</th>
          <th style="text-align:right">증정수량</th><th style="text-align:right">판촉비</th><th>중복</th><th>기간</th><th>사용</th><th></th></tr></thead>
        <tbody>${list.map((r, i) => {
          const extra = extraGifts(r);
          return `
          <tr>
            <td style="color:var(--text-muted)">${esc(r.ruleNo)}</td>
            <td>${esc(r.shopName || r.shopCode)}</td>
            <td>${r.chnlCode ? esc(r.chnlName || r.chnlCode) : '<span style="color:var(--text-muted)">전체</span>'}</td>
            <td style="max-width:200px">${r.chnlProdName ? esc(r.chnlProdName) : '<span style="color:var(--text-muted)">-</span>'}${r.chnlProdCode ? `<div style="font-size:10px;color:var(--text-muted)">${esc(r.chnlProdCode)}</div>` : ''}</td>
            <td>${r.dealName ? esc(r.dealName) : '<span style="color:var(--text-muted)">-</span>'}</td>
            <td>${rangeText(r.totalOrderQtyStart, r.totalOrderQtyEnd, won)}</td>
            <td>${rangeText(r.totalOrderAmtStart, r.totalOrderAmtEnd, n => won(n) + '원')}</td>
            <td style="max-width:200px">${esc(r.giftProdName1)}<div style="font-size:10px;color:var(--text-muted)">${esc(r.giftProdCode1)}</div>
              ${extra.length ? `<div style="font-size:10px;color:var(--text-muted)" title="${esc(extra.join(', '))}">+ ${extra.length}개 더</div>` : ''}</td>
            <td style="text-align:right">${won(r.giftQty)}</td>
            <td style="text-align:right">${won(r.promotFee)}</td>
            <td>${r.dupYn ? `${esc(r.dupYn)}개마다` : '한 세트'}</td>
            <td style="white-space:nowrap">${r.giftStartDate ? `${esc(day(r.giftStartDate))}<br>~ ${esc(day(r.giftEndDate))}` : '<span style="color:var(--text-muted)">상시</span>'}</td>
            <td>${Number(r.state) === 1 ? '<span style="color:var(--success,#2e7d32)">사용</span>' : '<span style="color:var(--text-muted)">안 함</span>'}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`;
        }).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const r = list[+b.dataset.del];
      UI.confirm(`[규칙 ${r.ruleNo}] ${r.shopName || r.shopCode} / ${r.giftProdName1} 규칙을 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/order/gift-rule', { ruleNo: r.ruleNo });
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
    // 서비스 목록에 없는 기존 값(레거시)은 선택지에 끼워 넣어 수정할 때 조용히 비워지지 않게 한다.
    const shopExtra = (v.shopCode && !shopList.some(s => s.shopCode === v.shopCode))
      ? `<option value="${esc(v.shopCode)}" selected>${esc(v.shopName || v.shopCode)} (현재값)</option>` : '';
    const chnlOpts = chnlList.map(c => `<option value="${esc(c.chnlCode)}" ${v.chnlCode === c.chnlCode ? 'selected' : ''}>${esc(c.chnlName)}</option>`).join('');
    const chnlExtra = (v.chnlCode && !chnlList.some(c => c.chnlCode === v.chnlCode))
      ? `<option value="${esc(v.chnlCode)}" selected>${esc(v.chnlName || v.chnlCode)} (현재값)</option>` : '';
    const giftRows = [1, 2, 3, 4, 5].map(n => `
      <div style="display:grid;grid-template-columns:24px 1.3fr auto 1.5fr;gap:6px;align-items:center;margin-bottom:6px">
        <span style="font-size:12px;color:var(--text-muted)">${n}${n === 1 ? '<b style="color:var(--danger)">*</b>' : ''}</span>
        <input class="input" id="grFCode${n}" maxlength="255" placeholder="사은품 코드" value="${esc(v['giftProdCode' + n])}">
        <button class="btn btn-ghost" data-pick="${n}" type="button" style="font-size:11px;white-space:nowrap">선택</button>
        <input class="input" id="grFName${n}" maxlength="255" placeholder="사은품 명칭(발주서에 표시)" value="${esc(v['giftProdName' + n])}">
      </div>`).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;font-weight:600;margin-bottom:6px">조건</div>
      <div class="form-grid">
        <div class="form-group"><label>상점 *</label>
          <select class="input" id="grFShop"><option value="">선택</option>${shopOpts}${shopExtra}</select></div>
        <div class="form-group"><label>채널</label>
          <select class="input" id="grFChnl"><option value="">전체 채널</option>${chnlOpts}${chnlExtra}</select></div>
        <div class="form-group"><label>채널상품코드(참고용)</label><input class="input" id="grFChnlProdCode" maxlength="50" value="${esc(v.chnlProdCode)}"></div>
        <div class="form-group"><label>딜명칭(포함)</label><input class="input" id="grFDeal" maxlength="50" value="${esc(v.dealName)}"></div>
        <div class="form-group full"><label>채널상품명칭(포함)</label><input class="input" id="grFChnlProdName" maxlength="255" value="${esc(v.chnlProdName)}" placeholder="이 문구가 상품명에 들어 있는 주문만"></div>
        <div class="form-group"><label>총 주문수량 시작</label><input class="input" id="grFQtyS" type="number" min="0" value="${v.totalOrderQtyStart ?? 0}"></div>
        <div class="form-group"><label>총 주문수량 종료(0=제한 없음)</label><input class="input" id="grFQtyE" type="number" min="0" value="${v.totalOrderQtyEnd ?? 0}"></div>
        <div class="form-group"><label>총 주문금액 시작(원)</label><input class="input" id="grFAmtS" type="number" min="0" value="${v.totalOrderAmtStart ?? 0}"></div>
        <div class="form-group"><label>총 주문금액 종료(0=제한 없음)</label><input class="input" id="grFAmtE" type="number" min="0" value="${v.totalOrderAmtEnd ?? 0}"></div>
        <div class="form-group"><label>증정 시작일</label><input class="input" id="grFStart" type="date" value="${esc(day(v.giftStartDate))}"></div>
        <div class="form-group"><label>증정 종료일</label><input class="input" id="grFEnd" type="date" value="${esc(day(v.giftEndDate))}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin:4px 0 12px">총 주문수량·금액은 같은 수신자(주소·이름)에게 나가는 주문 전체의 합계입니다. 증정 기간은 시작·종료를 함께 입력하며, 비우면 상시입니다.</div>

      <div style="font-size:12px;font-weight:600;margin-bottom:6px">사은품 <span style="font-weight:400;color:var(--text-muted)">— 코드는 발주단가표의 코드이며, 1번만 필수입니다</span></div>
      ${giftRows}

      <div class="form-grid" style="margin-top:10px">
        <div class="form-group"><label>증정 수량 *</label><input class="input" id="grFGiftQty" type="number" min="1" value="${v.giftQty ?? 1}"></div>
        <div class="form-group"><label>판촉 수수료(원)</label><input class="input" id="grFFee" type="number" min="0" value="${v.promotFee ?? 0}"></div>
        <div class="form-group"><label>중복 여부</label><input class="input" id="grFDup" type="number" min="0" value="${v.dupYn ?? 0}"></div>
        <div class="form-group"><label>사용 여부</label>
          <select class="input" id="grFState"><option value="1" ${v.state == null || Number(v.state) === 1 ? 'selected' : ''}>사용</option><option value="0" ${v.state != null && Number(v.state) !== 1 ? 'selected' : ''}>사용 안 함</option></select></div>
        <div class="form-group"><label>비고</label><input class="input" id="grFRemark" maxlength="50" value="${esc(v.remark)}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:4px">중복 여부: <b>0</b> = 수신자당 한 세트만 증정, <b>N</b> = 총 주문수량 N개마다 증정 수량을 곱해 증정(예: 1 → 주문 수량만큼).</div>`;
    const q = id => body.querySelector('#' + id);
    body.querySelectorAll('[data-pick]').forEach(b => b.addEventListener('click', () => openCodePicker(picked => {
      const n = b.dataset.pick;
      q('grFCode' + n).value = picked.matchUpriceCode;
      q('grFName' + n).value = picked.orderProdName || '';
    })));

    const modal = UI.modal({
      title: isNew ? '사은품규칙 등록' : `사은품규칙 수정 – 규칙 ${v.ruleNo}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        const num = id => (val(id) === '' ? 0 : Number(val(id)));
        if (!val('grFShop')) return UI.toast('상점을 선택하세요', 'error');
        if (!val('grFCode1') || !val('grFName1')) return UI.toast('사은품 1 의 코드와 명칭을 입력하세요', 'error');
        if (num('grFGiftQty') < 1) return UI.toast('증정 수량은 1 이상이어야 합니다', 'error');
        if (!!val('grFStart') !== !!val('grFEnd')) return UI.toast('증정 기간은 시작일과 종료일을 함께 입력하세요', 'error');
        const payload = {
          shopCode: val('grFShop'), chnlCode: val('grFChnl'), chnlProdCode: val('grFChnlProdCode'),
          chnlProdName: val('grFChnlProdName'), dealName: val('grFDeal'),
          totalOrderQtyStart: num('grFQtyS'), totalOrderQtyEnd: num('grFQtyE'),
          totalOrderAmtStart: num('grFAmtS'), totalOrderAmtEnd: num('grFAmtE'),
          giftQty: num('grFGiftQty'), promotFee: num('grFFee'), dupYn: num('grFDup'),
          giftStartDate: val('grFStart'), giftEndDate: val('grFEnd'), remark: val('grFRemark'), state: Number(val('grFState')),
        };
        for (let n = 1; n <= 5; n++) { payload['giftProdCode' + n] = val('grFCode' + n); payload['giftProdName' + n] = val('grFName' + n); }
        if (!isNew) payload.ruleNo = v.ruleNo;
        try {
          if (isNew) await Api.post('/order/gift-rule', payload); else await Api.put('/order/gift-rule', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
    modal.el.querySelector('.modal').style.width = '720px';
  }

  /** 발주단가표에서 사은품 코드를 고른다(사용 중인 코드만). */
  function openCodePicker(onPick) {
    const pb = document.createElement('div');
    pb.innerHTML = `
      <div style="display:flex;gap:6px;margin-bottom:8px">
        <input class="input" id="grQKeywrd" placeholder="키워드로 검색 (예: 증정)" style="flex:1">
        <button class="btn btn-ghost" id="grQBtn" type="button">검색</button>
      </div>
      <div class="table-wrap" id="grQWrap" style="max-height:40vh;border:1px solid var(--border);border-radius:var(--radius)">
        <div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색하세요</div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:8px">없는 코드는 <b>발주단가표</b> 화면에서 먼저 등록하세요.</div>`;
    const sub = UI.modal({ title: '사은품 코드 선택 (발주단가표)', body: pb, confirmText: null, cancelText: '닫기' });
    sub.el.querySelector('.modal').style.width = '640px';
    const doSearch = async () => {
      const wrap = pb.querySelector('#grQWrap');
      wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색 중...</div>';
      try {
        const params = { pUseYn: 1, pPageOffset: 0, pPageSize: 30 };
        const kw = pb.querySelector('#grQKeywrd').value.trim();
        if (kw) params.pKeywrd = kw;
        const list = await Api.get('/order/uprice/list', params);
        if (!list || !list.length) {
          wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">결과 없음</div>';
          return;
        }
        wrap.innerHTML = `
          <table style="font-size:11px">
            <thead><tr><th>코드</th><th>발주상품명칭</th><th style="text-align:right">단가</th></tr></thead>
            <tbody>${list.map((u, i) => `
              <tr style="cursor:pointer" data-i="${i}"><td>${esc(u.matchUpriceCode)}</td><td>${esc(u.orderProdName)}</td><td style="text-align:right">${won(u.uprice)}</td></tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('tbody tr').forEach(tr => tr.addEventListener('click', () => { onPick(list[+tr.dataset.i]); sub.close(); }));
      } catch (e) {
        wrap.innerHTML = `<div style="color:var(--danger);padding:16px;font-size:12px">${esc(e.message)}</div>`;
      }
    };
    pb.querySelector('#grQBtn').addEventListener('click', doSearch);
    pb.querySelector('#grQKeywrd').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    doSearch();
  }

  return { render };
})();

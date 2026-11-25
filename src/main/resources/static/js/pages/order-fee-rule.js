/**
 * 수수료규칙 (사이드 메뉴 발주) — 레거시 wOrdOrderFeeRuleList(배송비·판촉비 규칙)
 *   상점 단위 규칙(shopwizard.tOrdOrderFeeRule): 조건(채널·상품명·딜명·주문 수량/금액 구간)에 맞는 지불완료 주문 라인에
 *   배송비·판촉비를 정하거나(채널 수수료율 0), 채널 수수료율로 공급가를 다시 계산한다(수수료율 ≠ 0). 구간의 0 은 "제한 없음".
 *   적용은 자동발주 화면의 "수수료 규칙 적용"이 한다 — 규칙 번호 순으로 하나씩, 나중 규칙이 앞 규칙을 덮어쓰고,
 *   사용 여부(State)는 보지 않는다(레거시와 같음).
 *   백엔드: /order/fee-rule (list, count, POST, PUT, DELETE ?ruleNo)
 */
const PageOrderFeeRule = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  const SECTS = ['전체무료', '무료', '선불', '착불'];
  let currentPage = 1;
  let shopList = [];
  let chnlList = [];

  const svc = () => (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
  const won = n => (n == null || n === '') ? '' : Number(n).toLocaleString();

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>수수료규칙 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 배송비·판촉비·채널 수수료율</span></span>
          <button class="btn btn-primary" id="frBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            상점별로 "어떤 지불완료 주문에 배송비·판촉비를 얼마로 할지"를 정합니다. 수량·금액 구간의 <b>0은 제한 없음</b>, 채널·상품명·딜명은 비워 두면 조건에서 빠집니다(상품명·딜명은 <b>포함</b> 검색).<br>
            · <b>전체무료</b>: 배송형태와 상관없이 적용하고 배송형태를 '무료'로 바꿉니다. 무료/선불/착불: 주문의 <b>현재 배송형태가 같은</b> 라인에만 적용합니다.<br>
            · <b>채널 수수료율 ≠ 0</b>: 배송비·판촉비는 건드리지 않고 공급가만 판매가 × (100 − 수수료율) ÷ 100 으로 다시 계산합니다.<br>
            · 자동발주의 "수수료 규칙 적용"이 규칙 번호 순으로 적용하며 <b>나중 규칙이 앞 규칙을 덮어씁니다</b>. <b>사용 여부와 상관없이 모든 규칙이 적용됩니다</b>(레거시와 같음).
            같은 수신자의 묶음 주문은 가장 작은 주문번호 외에는 배송비·판촉비가 0 이 되고, 스마일배송·11번가풀필먼트 채널은 선불/0/0 으로 고정됩니다.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점</label>
              <select class="input" id="frShop" style="width:160px"><option value="">전체</option></select></div>
            <div class="form-group"><label>채널</label>
              <select class="input" id="frChnl" style="width:150px"><option value="">전체</option></select></div>
            <div class="form-group"><label>상품명</label><input class="input" id="frProd" style="width:140px"></div>
            <div class="form-group"><label>딜명</label><input class="input" id="frDeal" style="width:110px"></div>
            <div class="form-group"><label>배송형태</label>
              <select class="input" id="frSect" style="width:100px"><option value="">전체</option>${SECTS.map(s => `<option>${s}</option>`).join('')}</select></div>
            <button class="btn btn-primary" id="frBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="frTotal">0</strong>건</div>
          <div class="table-wrap" id="frWrap"></div>
          <div id="frPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('frBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('frBtnNew').addEventListener('click', () => openModal(null));
    ['frProd', 'frDeal'].forEach(id =>
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
    document.getElementById('frShop').insertAdjacentHTML('beforeend',
      shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
    document.getElementById('frChnl').insertAdjacentHTML('beforeend',
      chnlList.map(c => `<option value="${esc(c.chnlCode)}">${esc(c.chnlName)}</option>`).join(''));
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('frShop')) p.pShopCode = v('frShop');
    if (v('frChnl')) p.pChnlCode = v('frChnl');
    if (v('frProd')) p.pProdName = v('frProd');
    if (v('frDeal')) p.pDealName = v('frDeal');
    if (v('frSect')) p.pDeliFeeSect = v('frSect');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('frWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/fee-rule/count', params);
      document.getElementById('frTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('frPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/fee-rule/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('frPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
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

  function effectText(r) {
    const rate = Number(r.chnlFeeRate) || 0;
    if (rate) return `공급가 = 판매가 × ${(100 - rate).toFixed(2).replace(/\.?0+$/, '')}%`;
    return `배송비 ${won(r.deliFee)}원 · 판촉비 ${won(r.promotFee)}원`;
  }

  function renderTable(list) {
    const wrap = document.getElementById('frWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>번호</th><th>상점</th><th>채널</th><th>상품명(포함)</th><th>딜명(포함)</th>
          <th>주문수량</th><th>주문금액</th><th>배송형태</th><th>적용 내용</th><th style="text-align:right">수수료율(%)</th><th>사용</th><th></th></tr></thead>
        <tbody>${list.map((r, i) => `
          <tr>
            <td style="color:var(--text-muted)">${esc(r.ruleNo)}</td>
            <td>${esc(r.shopName || r.shopCode)}</td>
            <td>${r.chnlCode ? esc(r.chnlName || r.chnlCode) : '<span style="color:var(--text-muted)">전체</span>'}</td>
            <td style="max-width:180px">${r.prodName ? esc(r.prodName) : '<span style="color:var(--text-muted)">-</span>'}</td>
            <td>${r.dealName ? esc(r.dealName) : '<span style="color:var(--text-muted)">-</span>'}</td>
            <td>${rangeText(r.orderQtyStart, r.orderQtyEnd, won)}</td>
            <td>${rangeText(r.orderAmtStart, r.orderAmtEnd, n => won(n) + '원')}</td>
            <td>${esc(r.deliFeeSect)}</td>
            <td>${esc(effectText(r))}</td>
            <td style="text-align:right">${Number(r.chnlFeeRate) ? esc(r.chnlFeeRate) : ''}</td>
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
      UI.confirm(`[규칙 ${r.ruleNo}] ${r.shopName || r.shopCode} 규칙을 삭제하시겠습니까?`, async close => {
        try {
          await Api.delete('/order/fee-rule', { ruleNo: r.ruleNo });
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
    const chnlOpts = chnlList.map(c => `<option value="${esc(c.chnlCode)}" ${v.chnlCode === c.chnlCode ? 'selected' : ''}>${esc(c.chnlName)}</option>`).join('');
    const chnlExtra = (v.chnlCode && !chnlList.some(c => c.chnlCode === v.chnlCode))
      ? `<option value="${esc(v.chnlCode)}" selected>${esc(v.chnlName || v.chnlCode)} (현재값)</option>` : '';
    const sectOpts = SECTS.map(s => `<option ${(v.deliFeeSect || '전체무료') === s ? 'selected' : ''}>${s}</option>`).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;font-weight:600;margin-bottom:6px">조건</div>
      <div class="form-grid">
        <div class="form-group"><label>상점 *</label>
          <select class="input" id="frFShop"><option value="">선택</option>${shopOpts}${shopExtra}</select></div>
        <div class="form-group"><label>채널</label>
          <select class="input" id="frFChnl"><option value="">전체 채널</option>${chnlOpts}${chnlExtra}</select></div>
        <div class="form-group"><label>상품명(포함)</label><input class="input" id="frFProd" maxlength="50" value="${esc(v.prodName)}" placeholder="상품명+옵션에 이 문구가 들어 있는 주문만"></div>
        <div class="form-group"><label>딜명(포함)</label><input class="input" id="frFDeal" maxlength="50" value="${esc(v.dealName)}"></div>
        <div class="form-group"><label>주문수량 시작</label><input class="input" id="frFQtyS" type="number" min="0" value="${v.orderQtyStart ?? 0}"></div>
        <div class="form-group"><label>주문수량 종료(0=제한 없음)</label><input class="input" id="frFQtyE" type="number" min="0" value="${v.orderQtyEnd ?? 0}"></div>
        <div class="form-group"><label>주문금액 시작(원)</label><input class="input" id="frFAmtS" type="number" min="0" value="${v.orderAmtStart ?? 0}"></div>
        <div class="form-group"><label>주문금액 종료(0=제한 없음)</label><input class="input" id="frFAmtE" type="number" min="0" value="${v.orderAmtEnd ?? 0}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin:4px 0 12px">수량은 주문 라인의 수량, 금액은 라인 합계 판매가 기준입니다.</div>

      <div style="font-size:12px;font-weight:600;margin-bottom:6px">적용 내용</div>
      <div class="form-grid">
        <div class="form-group"><label>배송형태 *</label><select class="input" id="frFSect">${sectOpts}</select></div>
        <div class="form-group"><label>채널 수수료율(%)</label><input class="input" id="frFRate" type="number" min="0" max="100" step="0.01" value="${Number(v.chnlFeeRate) || 0}"></div>
        <div class="form-group"><label>배송비(원)</label><input class="input" id="frFDeli" type="number" min="0" value="${v.deliFee ?? 0}"></div>
        <div class="form-group"><label>판촉비(원)</label><input class="input" id="frFPromot" type="number" min="0" value="${v.promotFee ?? 0}"></div>
        <div class="form-group"><label>사용 여부</label>
          <select class="input" id="frFState"><option value="1" ${v.state == null || Number(v.state) === 1 ? 'selected' : ''}>사용</option><option value="0" ${v.state != null && Number(v.state) !== 1 ? 'selected' : ''}>사용 안 함</option></select></div>
        <div class="form-group"><label>비고</label><input class="input" id="frFRemark" maxlength="50" value="${esc(v.remark)}"></div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:4px">수수료율을 입력하면 공급가만 다시 계산하고 배송비·판촉비·배송형태는 무시합니다. 사용 여부는 기록용이며 적용에는 영향이 없습니다.</div>`;
    const q = id => body.querySelector('#' + id);

    const modal = UI.modal({
      title: isNew ? '수수료규칙 등록' : `수수료규칙 수정 – 규칙 ${v.ruleNo}`,
      body, confirmText: '저장',
      onConfirm: async close => {
        const val = id => q(id).value.trim();
        const num = id => (val(id) === '' ? 0 : Number(val(id)));
        if (!val('frFShop')) return UI.toast('상점을 선택하세요', 'error');
        const rate = num('frFRate');
        if (rate < 0 || rate > 100) return UI.toast('채널 수수료율은 0~100 이어야 합니다', 'error');
        const qs = num('frFQtyS'), qe = num('frFQtyE'), as = num('frFAmtS'), ae = num('frFAmtE');
        if (qe && qs > qe) return UI.toast('주문수량 시작은 종료보다 클 수 없습니다', 'error');
        if (ae && as > ae) return UI.toast('주문금액 시작은 종료보다 클 수 없습니다', 'error');
        const payload = {
          shopCode: val('frFShop'), chnlCode: val('frFChnl'), prodName: val('frFProd'), dealName: val('frFDeal'),
          orderQtyStart: qs, orderQtyEnd: qe, orderAmtStart: as, orderAmtEnd: ae,
          deliFeeSect: val('frFSect'), chnlFeeRate: rate, deliFee: num('frFDeli'), promotFee: num('frFPromot'),
          state: Number(val('frFState')), remark: val('frFRemark'),
        };
        if (!isNew) payload.ruleNo = v.ruleNo;
        try {
          if (isNew) await Api.post('/order/fee-rule', payload); else await Api.put('/order/fee-rule', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
    modal.el.querySelector('.modal').style.width = '680px';
  }

  return { render };
})();

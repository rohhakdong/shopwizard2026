/**
 * 고객문의 (사이드 메뉴 운영) — 레거시 고객문의(wOprCustConsltList). 주문상세에서 입력한 고객상담(shopion.tOprCustConslt) 전체를
 *   조건(기간·상담구분/사유/매체·처리여부·채널·주문번호·고객명·연락처·검색어·상담자)으로 찾아본다. 읽기 전용 — 행을 누르면 상담·처리 내용 전문과
 *   [주문상세 열기]가 나오고, 새 상담 등록·처리내용 수정은 주문상세(주문관리 권한)에서 한다.
 *   고객 연락처가 나오므로 조회할 때마다 개인정보 조회 기록이 남고(서버), 상점 제한 계정은 볼 수 없다.
 *   백엔드: /operate/conslt (list), 코드표 /code/constr-val, 주문상세 PageOrderDetail(order-detail.js)
 */
const PageOperateConslt = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  let seq = 0;

  function el(id) { return document.getElementById(id); }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function fmt(s) { return s ? String(s).replace('T', ' ').slice(0, 16) : ''; }
  function dash(s) { return s ? esc(s) : '<span style="color:var(--text-muted)">-</span>'; }
  function badge(answered) {
    return answered
      ? '<span style="font-size:11px;padding:2px 8px;border-radius:10px;color:#15803d;background:#dcfce7">처리완료</span>'
      : '<span style="font-size:11px;padding:2px 8px;border-radius:10px;color:#b45309;background:#fef3c7">미처리</span>';
  }

  function render(container) {
    currentPage = 1;
    container.innerHTML = `
      <div class="card">
        <div class="card-header">고객문의 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 주문에서 입력한 고객상담 전체</span></div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            고객 이름·연락처가 표시되는 화면입니다. 조회는 <b>개인정보 조회 기록</b>으로 남습니다. 상담 등록·처리내용 수정은 행을 눌러 <b>주문상세</b>에서 합니다.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상담일 시작</label><input class="input" id="ocStart" type="date" style="width:135px"></div>
            <div class="form-group"><label>종료</label><input class="input" id="ocEnd" type="date" style="width:135px"></div>
            <div class="form-group"><label>상담구분</label><select class="input" id="ocType" style="width:130px"><option value="">전체</option></select></div>
            <div class="form-group"><label>상담사유</label><select class="input" id="ocReason" style="width:130px"><option value="">전체</option></select></div>
            <div class="form-group"><label>매체</label><select class="input" id="ocMedia" style="width:90px"><option value="">전체</option></select></div>
            <div class="form-group"><label>처리</label>
              <select class="input" id="ocAnswr" style="width:95px"><option value="">전체</option><option value="Y">처리완료</option><option value="N">미처리</option></select></div>
            <div class="form-group"><label>채널</label><select class="input" id="ocChnl" style="width:150px"><option value="">전체</option></select></div>
            <div class="form-group"><label>주문번호</label><input class="input" id="ocOrderNo" style="width:100px" inputmode="numeric"></div>
            <div class="form-group"><label>고객명</label><input class="input" id="ocCust" style="width:100px"></div>
            <div class="form-group"><label>연락처(숫자 3자리~)</label><input class="input" id="ocPhone" style="width:120px"></div>
            <div class="form-group"><label>제목·내용</label><input class="input" id="ocKeyword" style="width:140px"></div>
            <div class="form-group"><label>상담자</label><input class="input" id="ocMngr" style="width:90px"></div>
            <button class="btn btn-primary" id="ocBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="ocTotal">0</strong>건</div>
          <div class="table-wrap" id="ocWrap"></div>
          <div id="ocPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    el('ocBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['ocOrderNo', 'ocCust', 'ocPhone', 'ocKeyword', 'ocMngr'].forEach(i => el(i).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadOptions();
    loadList();
  }

  async function loadOptions() {
    const get = c => Api.get('/code/constr-val', { pConstrCode: c, sidx: 'ConstrValSeq', sord: 'ASC' })
      .then(l => (l || []).filter(x => String(x.state ?? '1') === '1')).catch(() => []);
    const [type, reason, media, filt] = await Promise.all([get('cCustConsltType'), get('cCustConsltReasonType'), get('cConsltMediaType'), UI.loadFilterOptions(svcCode())]);
    UI.fillOptions('ocType', type.map(x => ({ v: x.constrVal, t: x.constrValDesc })));
    UI.fillOptions('ocReason', reason.map(x => ({ v: x.constrVal, t: x.constrValDesc })));
    UI.fillOptions('ocMedia', media.map(x => ({ v: x.constrVal, t: x.constrValDesc })));
    UI.fillOptions('ocChnl', filt.chnls.map(c => ({ v: c.chnlCode, t: c.chnlName })));
  }

  function params() {
    return {
      pStartDate: el('ocStart').value, pEndDate: el('ocEnd').value, pCustConsltType: el('ocType').value, pCustConsltReasonType: el('ocReason').value,
      pConsltMediaType: el('ocMedia').value, pAnswrYn: el('ocAnswr').value, pChnlCode: el('ocChnl').value, pOrderNo: el('ocOrderNo').value.trim(),
      pCustName: el('ocCust').value.trim(), pPhone: el('ocPhone').value.trim(), pKeyword: el('ocKeyword').value.trim(), pMngrName: el('ocMngr').value.trim(),
    };
  }

  async function loadList() {
    const wrap = el('ocWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    el('ocPage').innerHTML = '';
    const mine = ++seq;
    try {
      const res = await Api.get('/operate/conslt/list', { ...params(), pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      if (mine !== seq) return;
      el('ocTotal').textContent = Number(res.total).toLocaleString();
      if (!res.total) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">고객문의가 없습니다</div>'; return; }
      const rows = res.rows;
      wrap.innerHTML = `
        <table style="width:100%;font-size:12px">
          <thead><tr><th>번호</th><th>상담일시</th><th>구분 / 사유</th><th>채널</th><th>주문번호</th><th>상품</th><th>고객</th><th>연락처</th><th>제목</th><th>상담자</th><th>처리</th></tr></thead>
          <tbody>${rows.map((r, i) => `
            <tr style="cursor:pointer" data-i="${i}">
              <td style="text-align:right">${esc(r.custConsltNo)}</td>
              <td style="white-space:nowrap">${esc(fmt(r.consltDate))}</td>
              <td>${dash(r.custConsltTypeName || r.custConsltType)}<div style="font-size:11px;color:var(--text-muted)">${esc(r.custConsltReasonTypeName || r.custConsltReasonType || '')}${r.consltMediaTypeName ? ' · ' + esc(r.consltMediaTypeName) : ''}</div></td>
              <td>${dash(r.chnlName || r.chnlCode)}</td>
              <td>${dash(r.orderNo)}</td>
              <td style="max-width:150px">${r.orderProdNo ? esc(r.prodName || r.prodCode) : '<span style="color:var(--text-muted)">주문 전체</span>'}</td>
              <td>${dash(r.custName)}</td><td style="white-space:nowrap">${dash(r.custMobileNo || r.custPhoneNo)}</td>
              <td style="max-width:240px"><b>${esc(r.consltTitle)}</b></td>
              <td>${dash(r.mngrName)}</td><td>${badge(r.answered)}</td>
            </tr>`).join('')}</tbody>
        </table>`;
      wrap.querySelectorAll('tr[data-i]').forEach(tr => tr.addEventListener('click', () => openDetail(rows[+tr.dataset.i])));
      UI.pagination(el('ocPage'), { total: res.total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      if (mine !== seq) return;
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function openDetail(r) {
    const cell = (k, v) => `<div style="display:flex;gap:8px;padding:2px 0"><span style="width:84px;color:var(--text-muted)">${k}</span><span style="flex:1">${v}</span></div>`;
    const body = document.createElement('div');
    body.style.cssText = 'font-size:13px;line-height:1.6';
    body.innerHTML = `
      ${cell('번호', esc(r.custConsltNo))}${cell('상담일시', esc(fmt(r.consltDate)))}
      ${cell('구분 / 사유', `${dash(r.custConsltTypeName || r.custConsltType)} / ${dash(r.custConsltReasonTypeName || r.custConsltReasonType)}${r.consltMediaTypeName ? ' · ' + esc(r.consltMediaTypeName) : ''}`)}
      ${cell('채널', dash(r.chnlName || r.chnlCode))}
      ${cell('주문', r.orderNo ? `${esc(r.orderNo)}${r.orderProdNo ? ' / ' + esc(r.prodName || r.prodCode || '') + (r.itemName ? ' (' + esc(r.itemName) + ')' : '') : ' (주문 전체)'}` : '-')}
      ${cell('고객', `${dash(r.custName)} ${r.custMobileNo ? esc(r.custMobileNo) : ''} ${r.custPhoneNo ? esc(r.custPhoneNo) : ''} ${r.custEmail ? esc(r.custEmail) : ''}`)}
      ${cell('상담자', dash(r.mngrName))}
      <div style="margin-top:10px;font-weight:600">상담 — ${esc(r.consltTitle || '')}</div>
      <div style="white-space:pre-wrap;border:1px solid var(--border);border-radius:var(--radius);padding:8px 10px;margin-top:4px;max-height:200px;overflow:auto">${esc(r.consltCntnts || '')}</div>
      <div style="margin-top:10px;font-weight:600">처리 ${r.answered ? '— ' + esc(r.answrTitle || '') + ' <span style="font-weight:400;color:var(--text-muted)">' + esc(fmt(r.answrDate)) + '</span>' : badge(false)}</div>
      ${r.answered ? `<div style="white-space:pre-wrap;border:1px solid var(--border);border-radius:var(--radius);padding:8px 10px;margin-top:4px;max-height:200px;overflow:auto">${esc(r.answrCntnts || '')}</div>` : ''}`;
    const hasOrder = /^[0-9]+$/.test(String(r.orderNo || ''));
    const m = UI.modal({
      title: `고객문의 #${r.custConsltNo}`, body, confirmText: hasOrder ? '주문상세 열기' : null, cancelText: '닫기',
      onConfirm: async close => {
        close();
        PageOrderDetail.open(parseInt(r.orderNo, 10), loadList);
      },
    });
    return m;
  }

  return { render };
})();

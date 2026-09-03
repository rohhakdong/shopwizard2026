/**
 * 정산 관리
 * - 정산기간 검색, 페이지네이션
 * - 상세 모달 (읽기 전용)
 * - 세금계산서 정보 수정
 */
const PageAdjustList = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  function toYm(date) { return date.toISOString().slice(0,7); }
  function defaultStart() {
    const d = new Date(); d.setMonth(d.getMonth()-3);
    return d.toISOString().slice(0,7).replace('-','') + '01';
  }
  function defaultEnd() {
    const d = new Date();
    return d.toISOString().slice(0,7).replace('-','') + '01';
  }
  function toDateInput(yyyymmdd) {
    if (!yyyymmdd || yyyymmdd.length < 8) return '';
    return `${yyyymmdd.slice(0,4)}-${yyyymmdd.slice(4,6)}-${yyyymmdd.slice(6,8)}`;
  }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    const now = new Date();
    const thisMonth = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
    const threeAgo  = new Date(); threeAgo.setMonth(now.getMonth()-3);
    const prevMonth = `${threeAgo.getFullYear()}-${String(threeAgo.getMonth()+1).padStart(2,'0')}`;

    container.innerHTML = `
      <div class="card">
        <div class="card-header">정산 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>정산 시작월</label>
              <input class="input" id="ajStartDate" type="month" value="${prevMonth}" style="width:140px">
            </div>
            <div class="form-group">
              <label>정산 종료월</label>
              <input class="input" id="ajEndDate" type="month" value="${thisMonth}" style="width:140px">
            </div>
            <div class="form-group">
              <label>정산번호</label>
              <input class="input" id="ajAdjustNo" placeholder="정산번호" style="width:110px">
            </div>
            <button class="btn btn-primary" id="ajBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="ajTotalLabel">0</strong>건
          </div>
          <div class="table-wrap" id="ajTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>
          <div id="ajPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('ajBtnSearch').addEventListener('click', () => { currentPage=1; loadList(); });
    document.getElementById('ajAdjustNo').addEventListener('keydown', e => { if(e.key==='Enter'){currentPage=1;loadList();} });

    loadList();
  }

  function getParams() {
    const start = document.getElementById('ajStartDate').value; // yyyy-MM
    const end   = document.getElementById('ajEndDate').value;
    return {
      pAdjustStartDate: start ? start.replace('-','') + '01' : '',
      pAdjustEndDate:   end   ? end.replace('-','')   + '01' : '',
      pAdjustNo:        document.getElementById('ajAdjustNo').value.trim(),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('ajTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('ajPagination').innerHTML = '';

    const params = getParams();
    if (!params.pAdjustStartDate || !params.pAdjustEndDate) {
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">정산 기간을 선택하세요</div>';
      return;
    }

    try {
      totalCount = await Api.get('/adjust/adjust/count', params);
      document.getElementById('ajTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/adjust/adjust/list', {
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

  function fmtDate(s) {
    if (!s || s.length < 8) return s || '-';
    return `${s.slice(0,4)}-${s.slice(4,6)}-${s.slice(6,8)}`;
  }

  function stateBadge(state) {
    const map = {1:'badge-blue', 2:'badge-green', 9:'badge-red'};
    const label = {1:'정산중', 2:'정산완료', 9:'취소'};
    return `<span class="badge ${map[state]||'badge-gray'}">${label[state]||state||'-'}</span>`;
  }

  function renderTable(list) {
    const wrap = document.getElementById('ajTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(a => `
      <tr>
        <td style="font-size:12px;font-weight:600;${ell}">${a.adjustNo || ''}</td>
        <td style="font-size:12px;${ell}">${fmtDate(a.adjustStartDate)} ~ ${fmtDate(a.adjustEndDate)}</td>
        <td style="${ell}">
          <div style="font-size:13px;font-weight:500;${ell}" title="${a.shopName || ''}">${a.shopName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${a.supplyName || ''}">${a.supplyName || ''}</div>
        </td>
        <td style="text-align:right;font-size:13px;font-weight:500;${ell}">
          ${a.saleAmt != null ? a.saleAmt.toLocaleString()+'원' : '-'}
        </td>
        <td style="text-align:right;font-size:13px;${ell}">
          ${a.buyAmt != null ? a.buyAmt.toLocaleString()+'원' : '-'}
        </td>
        <td style="text-align:right;font-size:13px;${ell}">
          ${a.prodMargin != null ? a.prodMargin.toLocaleString()+'원' : '-'}
        </td>
        <td style="font-size:11px;color:var(--text-muted);${ell}">${fmtDate(a.payDate)}</td>
        <td style="text-align:center">${stateBadge(a.state)}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-action="detail"
            data-no="${a.adjustNo}">상세</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--primary)" data-action="taxinv"
            data-no="${a.adjustNo}"
            data-sect="${(a.taxinvSect||'').replace(/"/g,'&quot;')}"
            data-memo="${(a.taxinvMemo||'').replace(/"/g,'&quot;')}"
            data-state="${a.state||1}">세금계산서</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("쇼핑몰/협력사")은 지정된 열들의
    // 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블
    // 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:990px">
        <colgroup>
          <col style="width:80px"><col style="width:155px"><col style="width:180px">
          <col style="width:100px"><col style="width:100px"><col style="width:100px">
          <col style="width:90px"><col style="width:65px"><col style="width:120px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">정산번호</th><th style="${thEll}">정산기간</th><th style="${thEll}">쇼핑몰 / 협력사</th>
            <th style="${thEll};text-align:right">판매금액</th>
            <th style="${thEll};text-align:right">매입금액</th>
            <th style="${thEll};text-align:right">마진</th>
            <th style="${thEll}">지급예정일</th>
            <th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', () => openDetailModal(btn.dataset.no));
    });

    wrap.querySelectorAll('[data-action=taxinv]').forEach(btn => {
      btn.addEventListener('click', () => openTaxinvModal(btn));
    });
  }

  function renderPagination() {
    const totalPages = Math.ceil(totalCount/PAGE_SIZE);
    if (totalPages <= 1) return;
    const el = document.getElementById('ajPagination');
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

  async function openDetailModal(adjustNo) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';
    UI.modal({ title: `정산 상세 – #${adjustNo}`, body, confirmText: null, cancelText: '닫기' });

    try {
      const a = await Api.get('/adjust/adjust', { adjustNo });
      const row = (label, value) => `
        <div class="form-group">
          <label>${label}</label>
          <div class="input" style="background:#f8fafc;min-height:34px;line-height:34px;padding:0 10px;border:1px solid var(--border);border-radius:var(--radius)">${value ?? '-'}</div>
        </div>`;
      const amt = v => v != null ? v.toLocaleString()+'원' : null;
      body.innerHTML = `
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">기본 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('정산번호', a.adjustNo)}
          ${row('정산기간', `${fmtDate(a.adjustStartDate)} ~ ${fmtDate(a.adjustEndDate)}`)}
          ${row('쇼핑몰', a.shopName)} ${row('협력사', a.supplyName)}
          ${row('정산월', a.adjustMonth)} ${row('지급예정일', fmtDate(a.payDate))}
          ${row('입금은행', a.reciptBank)} ${row('입금계좌', a.reciptAccnt)}
          ${row('예금주', a.reciptDepost)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">금액</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('판매금액', amt(a.saleAmt))} ${row('매입금액', amt(a.buyAmt))}
          ${row('마진', amt(a.prodMargin))} ${row('배송비', amt(a.deliFeeAmt))}
          ${row('VAT율', a.vatRate != null ? a.vatRate+'%' : null)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px">세금계산서</div>
        <div class="form-grid">
          ${row('세금계산서 구분', a.taxinvSect)} ${row('확인일', a.taxinvCheckDate ? a.taxinvCheckDate.substring(0,10) : null)}
          ${row('메모', a.taxinvMemo)}
        </div>`;
    } catch(e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function openTaxinvModal(btn) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>세금계산서 구분</label>
          <select class="input" id="ajTaxinvSect">
            <option value="" ${!btn.dataset.sect ? 'selected' : ''}>선택</option>
            <option value="발행" ${btn.dataset.sect==='발행' ? 'selected' : ''}>발행</option>
            <option value="미발행" ${btn.dataset.sect==='미발행' ? 'selected' : ''}>미발행</option>
            <option value="면세" ${btn.dataset.sect==='면세' ? 'selected' : ''}>면세</option>
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="ajTaxinvState">
            <option value="1" ${btn.dataset.state==='1' ? 'selected' : ''}>정산중</option>
            <option value="2" ${btn.dataset.state==='2' ? 'selected' : ''}>정산완료</option>
            <option value="9" ${btn.dataset.state==='9' ? 'selected' : ''}>취소</option>
          </select>
        </div>
        <div class="form-group full">
          <label>메모</label>
          <input class="input" id="ajTaxinvMemo" value="${btn.dataset.memo}">
        </div>
      </div>`;

    UI.modal({
      title: `세금계산서 정보 – #${btn.dataset.no}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        try {
          await Api.put('/adjust/adjust', {
            adjustNo:    parseInt(btn.dataset.no),
            taxinvSect:  document.getElementById('ajTaxinvSect').value,
            taxinvMemo:  document.getElementById('ajTaxinvMemo').value.trim(),
            taxinvImg:   null,
            state:       parseInt(document.getElementById('ajTaxinvState').value),
            changeId:    (typeof info !== 'undefined' && info?.loginId) || '',
            changeName:  (typeof info !== 'undefined' && info?.name) || '',
          });
          UI.toast('저장되었습니다', 'success');
          loadList();
          close();
        } catch(e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

/**
 * 공급사 관리 (거래처 - 공급사)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (소속 회사 선택)
 * - 삭제
 */
const PageCompanySupply = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let compOptions = []; // { compCode, compName } — 소속 회사 선택용, 진입 시 1회 로드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">공급사 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>공급사코드</label>
              <input class="input" id="spSupplyCode" placeholder="공급사코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>공급사명</label>
              <input class="input" id="spSupplyName" placeholder="공급사명" style="width:160px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="spState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="spBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="spBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="spTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="spTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="spPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('spBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['spSupplyCode','spSupplyName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('spBtnNew').addEventListener('click', () => openEditModal(null));

    loadCompOptions();
    loadList();
  }

  // 소속 회사 선택용 목록 (많아야 수백 건이라 전체를 한 번에 불러온다)
  async function loadCompOptions() {
    try {
      compOptions = await Api.get('/company/comp', { pPageOffset: 0, pPageSize: 2000 });
    } catch (_) { compOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pSupplyCode: document.getElementById('spSupplyCode').value.trim(),
      pSupplyName: document.getElementById('spSupplyName').value.trim(),
      pState:      document.getElementById('spState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('spTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('spPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/supply-comp/count', params);
      document.getElementById('spTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/supply-comp', {
        ...params,
        pPageOffset: (currentPage - 1) * PAGE_SIZE,
        pPageSize:   PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('spTableWrap');

    const rows = list.map(s => `
      <tr>
        <td style="font-size:11px;font-family:monospace">${s.supplyCode || ''}</td>
        <td>
          <div style="font-weight:500;font-size:13px">${s.supplyName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${s.comp?.compName || ''}</div>
        </td>
        <td style="font-size:12px">${s.mngrName || ''}</td>
        <td style="font-size:12px">${s.mobileNo || s.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted)">${s.email || ''}</td>
        <td style="text-align:center">
          <span class="badge ${s.state === 1 ? 'badge-green' : 'badge-gray'}">${s.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${s.supplyCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${s.supplyCode}" data-name="${(s.supplyName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:110px"><col><col style="width:80px">
          <col style="width:110px"><col style="width:160px">
          <col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th>공급사코드</th><th>공급사명 / 소속회사</th><th>담당자</th>
            <th>연락처</th><th>이메일</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const supply = await Api.get(`/company/supply-comp/${btn.dataset.code}`);
          openEditModal(supply);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 공급사를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/supply-comp/${btn.dataset.code}`);
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 페이지네이션 ───────────────────────────────────────────────────
  function renderPagination() {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    if (totalPages <= 1) return;

    const el = document.getElementById('spPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start-1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end+1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => { currentPage = parseInt(btn.dataset.page); loadList(); });
    });
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  function openEditModal(supply) {
    const isNew = !supply;
    const v = supply || {};

    const compOpts = compOptions.map(c =>
      `<option value="${c.compCode}" ${v.compCode === c.compCode ? 'selected' : ''}>${c.compName} (${c.compCode})</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>공급사코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="spFSupplyCode" value="${v.supplyCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="공급사코드">
        </div>
        <div class="form-group">
          <label>소속 회사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="spFCompCode">
            <option value="">-- 선택 --</option>
            ${compOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>공급사명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="spFSupplyName" value="${v.supplyName || ''}">
        </div>
        <div class="form-group">
          <label>담당자명</label>
          <input class="input" id="spFMngrName" value="${v.mngrName || ''}">
        </div>
        <div class="form-group">
          <label>휴대폰</label>
          <input class="input" id="spFMobileNo" value="${v.mobileNo || ''}">
        </div>
        <div class="form-group">
          <label>전화번호</label>
          <input class="input" id="spFPhoneNo" value="${v.phoneNo || ''}">
        </div>
        <div class="form-group">
          <label>팩스번호</label>
          <input class="input" id="spFFaxNo" value="${v.faxNo || ''}">
        </div>
        <div class="form-group full">
          <label>이메일</label>
          <input class="input" id="spFEmail" value="${v.email || ''}">
        </div>
        <div class="form-group full">
          <label>주소</label>
          <input class="input" id="spFAddr1" value="${v.addr1 || ''}" placeholder="기본 주소">
        </div>
        <div class="form-group full">
          <label>상세주소</label>
          <input class="input" id="spFAddr2" value="${v.addr2 || ''}" placeholder="상세 주소">
        </div>
        <div class="form-group">
          <label>계약시작일</label>
          <input class="input" id="spFStartDate" type="date" value="${v.applyStartDate ? v.applyStartDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>계약종료일</label>
          <input class="input" id="spFEndDate" type="date" value="${v.applyEndDate ? v.applyEndDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="spFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="spFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '공급사 신규 등록' : `공급사 수정 – ${v.supplyCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const supplyCode = document.getElementById('spFSupplyCode').value.trim();
        const compCode   = document.getElementById('spFCompCode').value;
        const supplyName = document.getElementById('spFSupplyName').value.trim();

        if (!supplyCode) { UI.toast('공급사코드를 입력하세요', 'error'); return; }
        if (!compCode)   { UI.toast('소속 회사를 선택하세요', 'error'); return; }
        if (!supplyName) { UI.toast('공급사명을 입력하세요', 'error'); return; }

        const payload = {
          supplyCode,
          compCode,
          supplyName,
          mngrName:       document.getElementById('spFMngrName').value.trim(),
          mobileNo:       document.getElementById('spFMobileNo').value.trim(),
          phoneNo:        document.getElementById('spFPhoneNo').value.trim(),
          faxNo:          document.getElementById('spFFaxNo').value.trim(),
          email:          document.getElementById('spFEmail').value.trim(),
          addr1:          document.getElementById('spFAddr1').value.trim(),
          addr2:          document.getElementById('spFAddr2').value.trim(),
          applyStartDate: document.getElementById('spFStartDate').value || null,
          applyEndDate:   document.getElementById('spFEndDate').value || null,
          state:          parseInt(document.getElementById('spFState').value),
          remark:         document.getElementById('spFRemark').value.trim(),
          registId:       (typeof info !== 'undefined' && info?.loginId) || '',
          registName:     (typeof info !== 'undefined' && info?.name) || '',
          changeId:       (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:     (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/supply-comp', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/supply-comp', payload);
            UI.toast('저장되었습니다', 'success');
          }
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

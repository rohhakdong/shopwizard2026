/**
 * 채널 관리 (거래처 - 채널)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (판매회사 / 채널정책 선택)
 * - 삭제
 */
const PageCompanyChnl = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let saleCompOptions   = []; // { saleCompCode, saleCompName }
  let chnlPolicyOptions = []; // { chnlPolicyCode, chnlPolicyName }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">채널 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>채널코드</label>
              <input class="input" id="chChnlCode" placeholder="채널코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>채널명</label>
              <input class="input" id="chChnlName" placeholder="채널명" style="width:160px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="chState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="chBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="chBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="chTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="chTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="chPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('chBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['chChnlCode','chChnlName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('chBtnNew').addEventListener('click', () => openEditModal(null));

    loadOptions();
    loadList();
  }

  // 판매회사 / 채널정책 선택용 목록 (전체를 한 번에 불러온다)
  async function loadOptions() {
    try {
      saleCompOptions = await Api.get('/company/sale-comp', { pPageOffset: 0, pPageSize: 1000 });
    } catch (_) { saleCompOptions = []; }
    try {
      chnlPolicyOptions = await Api.get('/company/chnl-policy', { pPageOffset: 0, pPageSize: 1000 });
    } catch (_) { chnlPolicyOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pChnlCode: document.getElementById('chChnlCode').value.trim(),
      pChnlName: document.getElementById('chChnlName').value.trim(),
      pState:    document.getElementById('chState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('chTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('chPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/chnl/count', params);
      document.getElementById('chTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/chnl', {
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
    const wrap = document.getElementById('chTableWrap');

    const rows = list.map(c => `
      <tr>
        <td style="font-size:11px;font-family:monospace">${c.chnlCode || ''}</td>
        <td>
          <div style="font-weight:500;font-size:13px">${c.chnlName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${c.saleCompName || ''}</div>
        </td>
        <td style="font-size:12px">${c.chnlPolicyName || ''}</td>
        <td style="font-size:12px">${c.mngrName || ''}</td>
        <td style="font-size:12px;color:var(--text-muted)">${c.mngrEmail || ''}</td>
        <td style="text-align:center">
          <span class="badge ${c.state === 1 ? 'badge-green' : 'badge-gray'}">${c.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${c.chnlCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${c.chnlCode}" data-name="${(c.chnlName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:110px"><col><col style="width:110px">
          <col style="width:90px"><col style="width:160px">
          <col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th>채널코드</th><th>채널명 / 판매회사</th><th>채널정책</th>
            <th>담당자</th><th>담당자 이메일</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const chnl = await Api.get(`/company/chnl/${btn.dataset.code}`);
          openEditModal(chnl);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 채널을 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/chnl/${btn.dataset.code}`);
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

    const el = document.getElementById('chPagination');
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
  function openEditModal(chnl) {
    const isNew = !chnl;
    const v = chnl || {};

    const saleCompOpts = saleCompOptions.map(s =>
      `<option value="${s.saleCompCode}" ${v.saleCompCode === s.saleCompCode ? 'selected' : ''}>${s.saleCompName} (${s.saleCompCode})</option>`
    ).join('');
    const chnlPolicyOpts = chnlPolicyOptions.map(p =>
      `<option value="${p.chnlPolicyCode}" ${v.chnlPolicyCode === p.chnlPolicyCode ? 'selected' : ''}>${p.chnlPolicyName} (${p.chnlPolicyCode})</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>채널코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="chFChnlCode" value="${v.chnlCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="채널코드">
        </div>
        <div class="form-group">
          <label>판매회사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="chFSaleCompCode">
            <option value="">-- 선택 --</option>
            ${saleCompOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>채널명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="chFChnlName" value="${v.chnlName || ''}">
        </div>
        <div class="form-group">
          <label>채널정책</label>
          <select class="input" id="chFChnlPolicyCode">
            <option value="">-- 선택 안 함 --</option>
            ${chnlPolicyOpts}
          </select>
        </div>
        <div class="form-group">
          <label>소속 회사코드</label>
          <input class="input" id="chFCompCode" value="${v.compCode || ''}" placeholder="회사코드">
        </div>
        <div class="form-group full">
          <label>채널 URL</label>
          <input class="input" id="chFChnlUrl" value="${v.chnlUrl || ''}">
        </div>
        <div class="form-group">
          <label>담당자명</label>
          <input class="input" id="chFMngrName" value="${v.mngrName || ''}">
        </div>
        <div class="form-group">
          <label>담당 MD</label>
          <input class="input" id="chFMngrMd" value="${v.mngrMd || ''}">
        </div>
        <div class="form-group">
          <label>담당자 휴대폰</label>
          <input class="input" id="chFMngrMobileNo" value="${v.mngrMobileNo || ''}">
        </div>
        <div class="form-group">
          <label>담당자 전화번호</label>
          <input class="input" id="chFMngrPhoneNo" value="${v.mngrPhoneNo || ''}">
        </div>
        <div class="form-group full">
          <label>담당자 이메일</label>
          <input class="input" id="chFMngrEmail" value="${v.mngrEmail || ''}">
        </div>
        <div class="form-group">
          <label>운영시작일</label>
          <input class="input" id="chFMallStartDate" type="date" value="${v.mallStartDate ? v.mallStartDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>운영종료일</label>
          <input class="input" id="chFMallEndDate" type="date" value="${v.mallEndDate ? v.mallEndDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="chFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="chFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '채널 신규 등록' : `채널 수정 – ${v.chnlCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const chnlCode     = document.getElementById('chFChnlCode').value.trim();
        const saleCompCode = document.getElementById('chFSaleCompCode').value;
        const chnlName     = document.getElementById('chFChnlName').value.trim();

        if (!chnlCode)     { UI.toast('채널코드를 입력하세요', 'error'); return; }
        if (!saleCompCode) { UI.toast('판매회사를 선택하세요', 'error'); return; }
        if (!chnlName)     { UI.toast('채널명을 입력하세요', 'error'); return; }

        const payload = {
          chnlCode,
          saleCompCode,
          chnlName,
          chnlPolicyCode: document.getElementById('chFChnlPolicyCode').value || null,
          compCode:       document.getElementById('chFCompCode').value.trim(),
          chnlUrl:        document.getElementById('chFChnlUrl').value.trim(),
          mngrName:       document.getElementById('chFMngrName').value.trim(),
          mngrMd:         document.getElementById('chFMngrMd').value.trim(),
          mngrMobileNo:   document.getElementById('chFMngrMobileNo').value.trim(),
          mngrPhoneNo:    document.getElementById('chFMngrPhoneNo').value.trim(),
          mngrEmail:      document.getElementById('chFMngrEmail').value.trim(),
          mallStartDate:  document.getElementById('chFMallStartDate').value || null,
          mallEndDate:    document.getElementById('chFMallEndDate').value || null,
          state:          parseInt(document.getElementById('chFState').value),
          remark:         document.getElementById('chFRemark').value.trim(),
          registId:       (typeof info !== 'undefined' && info?.loginId) || '',
          registName:     (typeof info !== 'undefined' && info?.name) || '',
          changeId:       (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:     (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/chnl', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/chnl', payload);
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

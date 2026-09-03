/**
 * 회사 관리 (거래처 - 회사)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정
 * - 삭제
 */
const PageCompanyComp = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">회사 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>회사코드</label>
              <input class="input" id="cpCompCode" placeholder="회사코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>회사명</label>
              <input class="input" id="cpCompName" placeholder="회사명" style="width:160px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="cpState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="cpBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="cpBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="cpTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="cpTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="cpPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('cpBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['cpCompCode','cpCompName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('cpBtnNew').addEventListener('click', () => openEditModal(null));

    loadList();
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pCompCode: document.getElementById('cpCompCode').value.trim(),
      pCompName: document.getElementById('cpCompName').value.trim(),
      pState:    document.getElementById('cpState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('cpTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('cpPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/comp/count', params);
      document.getElementById('cpTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/comp', {
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
    const wrap = document.getElementById('cpTableWrap');

    const rows = list.map(c => `
      <tr>
        <td style="font-size:11px;font-family:monospace">${c.compCode || ''}</td>
        <td>
          <div style="font-weight:500;font-size:13px">${c.compName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${c.prsdntName || ''}</div>
        </td>
        <td style="font-size:12px">${c.corpCode || ''}</td>
        <td style="font-size:12px">${c.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted)">${c.email || ''}</td>
        <td style="text-align:center">
          <span class="badge ${c.state === 1 ? 'badge-green' : 'badge-gray'}">${c.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${c.compCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${c.compCode}" data-name="${(c.compName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:110px"><col><col style="width:120px">
          <col style="width:110px"><col style="width:160px">
          <col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th>회사코드</th><th>회사명 / 대표자</th><th>사업자번호</th>
            <th>전화번호</th><th>이메일</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const comp = await Api.get(`/company/comp/${btn.dataset.code}`);
          openEditModal(comp);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 회사를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/comp/${btn.dataset.code}`);
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

    const el = document.getElementById('cpPagination');
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
  function openEditModal(comp) {
    const isNew = !comp;
    const v = comp || {};

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>회사코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="cpFCompCode" value="${v.compCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="회사코드">
        </div>
        <div class="form-group full">
          <label>회사명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="cpFCompName" value="${v.compName || ''}">
        </div>
        <div class="form-group">
          <label>서비스코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="cpFSvcCode" value="${v.svcCode || (typeof info !== 'undefined' && info?.svcCode) || ''}" placeholder="예: SHP001">
        </div>
        <div class="form-group">
          <label>대표자명</label>
          <input class="input" id="cpFPrsdntName" value="${v.prsdntName || ''}">
        </div>
        <div class="form-group">
          <label>사업자등록번호</label>
          <input class="input" id="cpFCorpCode" value="${v.corpCode || ''}" placeholder="000-00-00000">
        </div>
        <div class="form-group">
          <label>전화번호</label>
          <input class="input" id="cpFPhoneNo" value="${v.phoneNo || ''}">
        </div>
        <div class="form-group">
          <label>팩스번호</label>
          <input class="input" id="cpFFaxNo" value="${v.faxNo || ''}">
        </div>
        <div class="form-group">
          <label>이메일</label>
          <input class="input" id="cpFEmail" value="${v.email || ''}">
        </div>
        <div class="form-group">
          <label>홈페이지</label>
          <input class="input" id="cpFHomepg" value="${v.homepg || ''}">
        </div>
        <div class="form-group full">
          <label>주소</label>
          <input class="input" id="cpFAddr1" value="${v.addr1 || ''}" placeholder="기본 주소">
        </div>
        <div class="form-group full">
          <label>상세주소</label>
          <input class="input" id="cpFAddr2" value="${v.addr2 || ''}" placeholder="상세 주소">
        </div>
        <div class="form-group">
          <label>정산은행</label>
          <input class="input" id="cpFReciptBank" value="${v.reciptBank || ''}">
        </div>
        <div class="form-group">
          <label>정산계좌</label>
          <input class="input" id="cpFReciptAccnt" value="${v.reciptAccnt || ''}">
        </div>
        <div class="form-group">
          <label>예금주</label>
          <input class="input" id="cpFReciptDepost" value="${v.reciptDepost || ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="cpFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="cpFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '회사 신규 등록' : `회사 수정 – ${v.compCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const compCode = document.getElementById('cpFCompCode').value.trim();
        const compName = document.getElementById('cpFCompName').value.trim();
        const svcCode  = document.getElementById('cpFSvcCode').value.trim();

        if (!compCode) { UI.toast('회사코드를 입력하세요', 'error'); return; }
        if (!compName) { UI.toast('회사명을 입력하세요', 'error'); return; }
        if (!svcCode)  { UI.toast('서비스코드를 입력하세요', 'error'); return; }

        const payload = {
          compCode,
          compName,
          svcCode,
          prsdntName:   document.getElementById('cpFPrsdntName').value.trim(),
          corpCode:     document.getElementById('cpFCorpCode').value.trim(),
          phoneNo:      document.getElementById('cpFPhoneNo').value.trim(),
          faxNo:        document.getElementById('cpFFaxNo').value.trim(),
          email:        document.getElementById('cpFEmail').value.trim(),
          homepg:       document.getElementById('cpFHomepg').value.trim(),
          addr1:        document.getElementById('cpFAddr1').value.trim(),
          addr2:        document.getElementById('cpFAddr2').value.trim(),
          reciptBank:   document.getElementById('cpFReciptBank').value.trim(),
          reciptAccnt:  document.getElementById('cpFReciptAccnt').value.trim(),
          reciptDepost: document.getElementById('cpFReciptDepost').value.trim(),
          state:        parseInt(document.getElementById('cpFState').value),
          remark:       document.getElementById('cpFRemark').value.trim(),
          registId:     (typeof info !== 'undefined' && info?.loginId) || '',
          registName:   (typeof info !== 'undefined' && info?.name) || '',
          changeId:     (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:   (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/comp', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/comp', payload);
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

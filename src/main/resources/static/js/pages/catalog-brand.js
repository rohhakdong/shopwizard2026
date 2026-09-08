/**
 * 브랜드 관리 (상품 분류 - 브랜드)
 * - tCatBrand: BrandId(AUTO_INCREMENT) 기반, SvcCode별로 구분되는 브랜드 마스터.
 * - 목록 조회 (페이지네이션) / 등록 / 수정 / 삭제
 */
const PageCatalogBrand = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let svcOptions  = [];

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">브랜드 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>서비스</label>
              <select class="input" id="brSvcCode" style="width:180px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>브랜드명</label>
              <input class="input" id="brBrandName" placeholder="브랜드명(한/영)" style="width:180px">
            </div>
            <button class="btn btn-primary" id="brBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="brBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="brTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="brTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="brPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('brBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('brBrandName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('brBtnNew').addEventListener('click', () => openEditModal(null));

    loadSvcOptions().then(() => {
      const sel = document.getElementById('brSvcCode');
      sel.innerHTML = '<option value="">전체</option>' + svcOptions.map(s =>
        `<option value="${s.svcCode}">${s.svcName || s.svcCode} (${s.svcCode})</option>`
      ).join('');
    });
    loadList();
  }

  async function loadSvcOptions() {
    try { svcOptions = await Api.get('/authority/svc/list', {}); } catch (_) { svcOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pSvcCode:   document.getElementById('brSvcCode').value,
      pBrandName: document.getElementById('brBrandName').value.trim(),
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('brTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('brPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/catalog/brand/count', params);
      document.getElementById('brTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/catalog/brand/list', {
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
    const wrap = document.getElementById('brTableWrap');
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(b => `
      <tr>
        <td style="font-size:11px;color:var(--text-muted);${ell}">${b.brandId}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${b.brandKorName || ''}">${b.brandKorName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${b.brandEngName || ''}">${b.brandEngName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${b.compKorName || ''}">${b.compKorName || ''}</td>
        <td style="font-size:12px;font-family:monospace;${ell}" title="${b.svcCode || ''}">${b.svcCode || ''}</td>
        <td style="text-align:center">
          <span class="badge ${b.state === 1 ? 'badge-green' : 'badge-gray'}">${b.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-id="${b.brandId}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-id="${b.brandId}" data-name="${(b.brandKorName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭을 못 준 열(브랜드명)은 지정된 열들의 폭 합계가
    // 카드 폭을 넘는 순간 강제로 찌부러진다. 이 열에도 고정폭을 주고 테이블 자체는 width:100%
    // 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신 table-wrap의 가로 스크롤
    // (overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:730px">
        <colgroup>
          <col style="width:50px"><col style="width:220px"><col style="width:160px">
          <col style="width:90px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">ID</th><th style="${thEll}">브랜드명(한/영)</th><th style="${thEll}">회사명</th>
            <th style="${thEll}">서비스</th><th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const brand = await Api.get('/catalog/brand', { brandId: btn.dataset.id });
          openEditModal(brand);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 브랜드를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/catalog/brand/${btn.dataset.id}`);
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

    const el = document.getElementById('brPagination');
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
  function openEditModal(brand) {
    const isNew = !brand;
    const v = brand || {};

    const svcOpts = svcOptions.map(s =>
      `<option value="${s.svcCode}" ${v.svcCode === s.svcCode ? 'selected' : ''}>${s.svcName || s.svcCode} (${s.svcCode})</option>`
    ).join('');
    const defaultSvc = (typeof info !== 'undefined' && info?.svcCode) || '';

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>서비스 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="brFSvcCode">
            <option value="">-- 선택 --</option>
            ${svcOpts}
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="brFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group">
          <label>브랜드명(한글) <span style="color:var(--danger)">*</span></label>
          <input class="input" id="brFBrandKorName" value="${v.brandKorName || ''}">
        </div>
        <div class="form-group">
          <label>브랜드명(영문)</label>
          <input class="input" id="brFBrandEngName" value="${v.brandEngName || ''}">
        </div>
        <div class="form-group">
          <label>회사명(한글)</label>
          <input class="input" id="brFCompKorName" value="${v.compKorName || ''}">
        </div>
        <div class="form-group">
          <label>회사명(영문)</label>
          <input class="input" id="brFCompEngName" value="${v.compEngName || ''}">
        </div>
        <div class="form-group full">
          <label>브랜드 약어</label>
          <input class="input" id="brFBrandAbbrName" value="${v.brandAbbrName || ''}">
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="brFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 신규 등록 시 로그인한 관리자 자신의 서비스코드를 기본값으로 미리 선택해둔다
    // (다른 거래처 화면들의 관례와 동일).
    if (isNew && defaultSvc) {
      setTimeout(() => { const sel = body.querySelector('#brFSvcCode'); if (sel) sel.value = defaultSvc; }, 0);
    }

    UI.modal({
      title: isNew ? '브랜드 신규 등록' : `브랜드 수정 – ${v.brandKorName}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const svcCode      = document.getElementById('brFSvcCode').value;
        const brandKorName = document.getElementById('brFBrandKorName').value.trim();

        if (!svcCode)      { UI.toast('서비스를 선택하세요', 'error'); return; }
        if (!brandKorName) { UI.toast('브랜드명(한글)을 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          ...(isNew ? {} : { brandId: v.brandId }),
          svcCode,
          brandKorName,
          brandEngName:  document.getElementById('brFBrandEngName').value.trim(),
          compKorName:   document.getElementById('brFCompKorName').value.trim(),
          compEngName:   document.getElementById('brFCompEngName').value.trim(),
          brandAbbrName: document.getElementById('brFBrandAbbrName').value.trim(),
          state:         parseInt(document.getElementById('brFState').value),
          remark:        document.getElementById('brFRemark').value.trim(),
          registId, registName,
          changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/brand', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/brand', payload);
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

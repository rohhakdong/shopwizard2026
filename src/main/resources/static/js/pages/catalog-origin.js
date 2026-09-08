/**
 * 원산지 관리 (상품 분류 - 원산지)
 * - tCatOrigin: OriginId(AUTO_INCREMENT) 기반, SvcCode 구분 없이 전역 공용 마스터.
 * - NationCode는 공통코드(wizardn.tCodConstrVal, ConstrCode='cNationCode')를 참조한다.
 * - 목록 조회 (페이지네이션) / 등록 / 수정 / 삭제
 */
const PageCatalogOrigin = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let nationOptions = []; // { constrVal, constrValDesc } — 국가코드 공통코드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">원산지 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>원산지명</label>
              <input class="input" id="orOriginName" placeholder="원산지명(한/영)" style="width:200px">
            </div>
            <button class="btn btn-primary" id="orBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="orBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="orTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="orTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="orPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('orBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('orOriginName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('orBtnNew').addEventListener('click', () => openEditModal(null));

    loadNationOptions();
    loadList();
  }

  async function loadNationOptions() {
    try { nationOptions = await Api.get('/code/constr-val', { pConstrCode: 'cNationCode' }); }
    catch (_) { nationOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return { pOriginName: document.getElementById('orOriginName').value.trim() };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('orTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('orPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/catalog/origin/count', params);
      document.getElementById('orTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/catalog/origin/list', {
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
    const wrap = document.getElementById('orTableWrap');
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(o => `
      <tr>
        <td style="font-size:11px;color:var(--text-muted);${ell}">${o.originId}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${o.originKorName || ''}">${o.originKorName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${o.originEngName || ''}">${o.originEngName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${o.nationName || ''}">${o.nationName || o.nationCode || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${o.originDesc || ''}">${o.originDesc || ''}</td>
        <td style="text-align:center">
          <span class="badge ${o.state === 1 ? 'badge-green' : 'badge-gray'}">${o.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-id="${o.originId}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-id="${o.originId}" data-name="${(o.originKorName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:750px">
        <colgroup>
          <col style="width:50px"><col style="width:200px"><col style="width:100px">
          <col style="width:170px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">ID</th><th style="${thEll}">원산지명(한/영)</th><th style="${thEll}">국가</th>
            <th style="${thEll}">설명</th><th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const origin = await Api.get('/catalog/origin', { originId: btn.dataset.id });
          openEditModal(origin);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 원산지를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/catalog/origin/${btn.dataset.id}`);
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

    const el = document.getElementById('orPagination');
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
  function openEditModal(origin) {
    const isNew = !origin;
    const v = origin || {};

    const nationOpts = nationOptions.map(n =>
      `<option value="${n.constrVal}" ${v.nationCode === n.constrVal ? 'selected' : ''}>${n.constrValDesc || n.constrVal}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>원산지명(한글) <span style="color:var(--danger)">*</span></label>
          <input class="input" id="orFOriginKorName" value="${v.originKorName || ''}">
        </div>
        <div class="form-group">
          <label>원산지명(영문)</label>
          <input class="input" id="orFOriginEngName" value="${v.originEngName || ''}">
        </div>
        <div class="form-group">
          <label>국가</label>
          <select class="input" id="orFNationCode">
            <option value="">-- 선택 안 함 --</option>
            ${nationOpts}
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="orFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="orFOriginDesc" value="${v.originDesc || ''}">
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="orFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '원산지 신규 등록' : `원산지 수정 – ${v.originKorName}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const originKorName = document.getElementById('orFOriginKorName').value.trim();
        if (!originKorName) { UI.toast('원산지명(한글)을 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          ...(isNew ? {} : { originId: v.originId }),
          originKorName,
          originEngName: document.getElementById('orFOriginEngName').value.trim(),
          originDesc:    document.getElementById('orFOriginDesc').value.trim(),
          nationCode:    document.getElementById('orFNationCode').value || null,
          state:         parseInt(document.getElementById('orFState').value),
          remark:        document.getElementById('orFRemark').value.trim(),
          registId, registName,
          changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/origin', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/origin', payload);
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

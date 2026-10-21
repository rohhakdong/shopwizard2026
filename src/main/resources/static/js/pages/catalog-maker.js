/**
 * 제조사 관리 (상품 분류 - 제조사)
 * - tCatMaker: MakerId(AUTO_INCREMENT) 기반, SvcCode 구분 없이 전역 공용 마스터.
 * - NationCode는 공통코드(wizardn.tCodConstrVal, ConstrCode='cNationCode')를 참조한다.
 * - 목록 조회 (페이지네이션) / 등록 / 수정 / 삭제
 */
const PageCatalogMaker = (() => {

  // 외부/사용자 입력이 섞이는 값은 innerHTML 에 넣기 전에 반드시 esc() 를 거친다.
  const esc = UI.esc;

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let nationOptions = []; // { constrVal, constrValDesc } — 국가코드 공통코드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">제조사 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>제조사명</label>
              <input class="input" id="mkMakerName" placeholder="제조사명(한/영)" style="width:200px">
            </div>
            <button class="btn btn-primary" id="mkBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="mkBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="mkTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="mkTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="mkPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('mkBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('mkMakerName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('mkBtnNew').addEventListener('click', () => openEditModal(null));

    loadNationOptions();
    loadList();
  }

  async function loadNationOptions() {
    try { nationOptions = await Api.get('/code/constr-val', { pConstrCode: 'cNationCode' }); }
    catch (_) { nationOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return { pMakerName: document.getElementById('mkMakerName').value.trim() };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('mkTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('mkPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/catalog/maker/count', params);
      document.getElementById('mkTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/catalog/maker/list', {
        ...params,
        pPageOffset: (currentPage - 1) * PAGE_SIZE,
        pPageSize:   PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('mkTableWrap');
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(m => `
      <tr>
        <td style="font-size:11px;color:var(--text-muted);${esc(ell)}">${esc(m.makerId)}</td>
        <td style="${esc(ell)}">
          <div style="font-weight:500;font-size:13px;${esc(ell)}" title="${esc(m.makerKorName || '')}">${esc(m.makerKorName || '')}</div>
          <div style="font-size:11px;color:var(--text-muted);${esc(ell)}" title="${esc(m.makerEngName || '')}">${esc(m.makerEngName || '')}</div>
        </td>
        <td style="font-size:12px;${esc(ell)}" title="${esc(m.prsdntName || '')}">${esc(m.prsdntName || '')}</td>
        <td style="font-size:12px;${esc(ell)}" title="${esc(m.nationName || '')}">${esc(m.nationName || m.nationCode || '')}</td>
        <td style="text-align:center">
          <span class="badge ${esc(m.state === 1 ? 'badge-green' : 'badge-gray')}">${esc(m.state === 1 ? '정상' : '중지')}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-id="${esc(m.makerId)}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-id="${esc(m.makerId)}" data-name="${esc(m.makerKorName||'')}">삭제</button>
        </td>
      </tr>`).join('');

    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:730px">
        <colgroup>
          <col style="width:50px"><col style="width:220px"><col style="width:100px">
          <col style="width:150px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${esc(thEll)}">ID</th><th style="${esc(thEll)}">제조사명(한/영)</th><th style="${esc(thEll)}">대표자</th>
            <th style="${esc(thEll)}">국가</th><th style="${esc(thEll)};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const maker = await Api.get('/catalog/maker', { makerId: btn.dataset.id });
          openEditModal(maker);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 제조사를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/catalog/maker/${btn.dataset.id}`);
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

    const el = document.getElementById('mkPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${esc(start-1)}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${esc(p === currentPage ? 'active' : '')}" data-page="${esc(p)}">${esc(p)}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${esc(end+1)}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => { currentPage = parseInt(btn.dataset.page); loadList(); });
    });
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  function openEditModal(maker) {
    const isNew = !maker;
    const v = maker || {};

    const nationOpts = nationOptions.map(n =>
      `<option value="${esc(n.constrVal)}" ${esc(v.nationCode === n.constrVal ? 'selected' : '')}>${esc(n.constrValDesc || n.constrVal)}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>제조사명(한글) <span style="color:var(--danger)">*</span></label>
          <input class="input" id="mkFMakerKorName" value="${esc(v.makerKorName || '')}">
        </div>
        <div class="form-group">
          <label>제조사명(영문)</label>
          <input class="input" id="mkFMakerEngName" value="${esc(v.makerEngName || '')}">
        </div>
        <div class="form-group">
          <label>대표자명</label>
          <input class="input" id="mkFPrsdntName" value="${esc(v.prsdntName || '')}">
        </div>
        <div class="form-group">
          <label>국가</label>
          <select class="input" id="mkFNationCode">
            <option value="">-- 선택 안 함 --</option>
            ${nationOpts}
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mkFState">
            <option value="1" ${esc((v.state ?? 1) === 1 ? 'selected' : '')}>정상</option>
            <option value="0" ${esc(v.state === 0 ? 'selected' : '')}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mkFRemark" value="${esc(v.remark || '')}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '제조사 신규 등록' : `제조사 수정 – ${v.makerKorName}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const makerKorName = document.getElementById('mkFMakerKorName').value.trim();
        if (!makerKorName) { UI.toast('제조사명(한글)을 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          ...(isNew ? {} : { makerId: v.makerId }),
          makerKorName,
          makerEngName: document.getElementById('mkFMakerEngName').value.trim(),
          prsdntName:   document.getElementById('mkFPrsdntName').value.trim(),
          nationCode:   document.getElementById('mkFNationCode').value || null,
          state:        parseInt(document.getElementById('mkFState').value),
          remark:       document.getElementById('mkFRemark').value.trim(),
          registId, registName,
          changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/maker', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/maker', payload);
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

/**
 * 택배회사 관리 (거래처 - 택배회사)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (택배회사코드는 자동 채번 — DB AUTO_INCREMENT)
 * - 삭제
 */
const PageCompanyDeli = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">택배회사 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>택배회사코드</label>
              <input class="input" id="dlDeliCompCode" placeholder="택배회사코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="dlState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="dlBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="dlBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="dlTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="dlTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="dlPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('dlBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('dlDeliCompCode').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('dlBtnNew').addEventListener('click', () => openEditModal(null));

    loadList();
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pDeliCompCode: document.getElementById('dlDeliCompCode').value.trim(),
      pState:        document.getElementById('dlState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('dlTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('dlPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/deli-comp/count', params);
      document.getElementById('dlTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/deli-comp', {
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
    const wrap = document.getElementById('dlTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(d => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${d.deliCompCode ?? ''}">${d.deliCompCode ?? ''}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${d.deliCompName || ''}">${d.deliCompName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${d.deliCompDesc || ''}">${d.deliCompDesc || ''}</div>
        </td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${d.linkUrl || ''}">${d.linkUrl || ''}</td>
        <td style="font-size:12px;${ell}" title="${d.invNoVar || ''}">${d.invNoVar || ''}</td>
        <td style="text-align:center">
          <span class="badge ${d.state === 1 ? 'badge-green' : 'badge-gray'}">${d.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${d.deliCompCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${d.deliCompCode}" data-name="${(d.deliCompName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("택배회사명/설명")은 지정된 열들의
    // 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블
    // 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:760px">
        <colgroup>
          <col style="width:110px"><col style="width:180px"><col style="width:200px">
          <col style="width:120px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">코드</th><th style="${thEll}">택배회사명 / 설명</th><th style="${thEll}">배송조회 URL</th>
            <th style="${thEll}">운송장번호 형식</th><th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const deli = await Api.get(`/company/deli-comp/${btn.dataset.code}`);
          openEditModal(deli);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 택배회사를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/deli-comp/${btn.dataset.code}`);
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

    const el = document.getElementById('dlPagination');
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
  function openEditModal(deli) {
    const isNew = !deli;
    const v = deli || {};

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        ${!isNew ? `
        <div class="form-group">
          <label>택배회사코드</label>
          <input class="input" value="${v.deliCompCode ?? ''}" readonly style="background:#f8fafc">
        </div>` : ''}
        <div class="form-group full">
          <label>택배회사명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="dlFDeliCompName" value="${v.deliCompName || ''}">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="dlFDeliCompDesc" value="${v.deliCompDesc || ''}">
        </div>
        <div class="form-group full">
          <label>배송조회 URL</label>
          <input class="input" id="dlFLinkUrl" value="${v.linkUrl || ''}" placeholder="운송장번호를 붙여 조회하는 URL">
        </div>
        <div class="form-group full">
          <label>운송장번호 형식</label>
          <input class="input" id="dlFInvNoVar" value="${v.invNoVar || ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="dlFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="dlFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '택배회사 신규 등록' : `택배회사 수정 – ${v.deliCompName}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const deliCompName = document.getElementById('dlFDeliCompName').value.trim();
        if (!deliCompName) { UI.toast('택배회사명을 입력하세요', 'error'); return; }

        const payload = {
          ...(isNew ? {} : { deliCompCode: v.deliCompCode }),
          deliCompName,
          deliCompDesc: document.getElementById('dlFDeliCompDesc').value.trim(),
          linkUrl:      document.getElementById('dlFLinkUrl').value.trim(),
          invNoVar:     document.getElementById('dlFInvNoVar').value.trim(),
          state:        parseInt(document.getElementById('dlFState').value),
          remark:       document.getElementById('dlFRemark').value.trim(),
          registId:     (typeof info !== 'undefined' && info?.loginId) || '',
          registName:   (typeof info !== 'undefined' && info?.name) || '',
          changeId:     (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:   (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/deli-comp', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/deli-comp', payload);
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

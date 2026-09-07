/**
 * 판매사 관리 (거래처 - 판매사)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (소속 회사 선택)
 * - 삭제
 * 채널 관리(company-chnl.js)가 등록/수정 시 선택하는 판매회사가 바로 이 판매사이다.
 * 공급사가 상점에 대해 갖는 관계(소속 회사 → 공급사 → 상점)와 동일하게,
 * 소속 회사 → 판매사 → 채널 관계를 여기서 관리한다.
 */
const PageCompanySaleComp = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let compOptions = []; // { compCode, compName } — 소속 회사 선택용, 진입 시 1회 로드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">판매사 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>판매사코드</label>
              <input class="input" id="scSaleCompCode" placeholder="판매사코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>판매사명</label>
              <input class="input" id="scSaleCompName" placeholder="판매사명" style="width:160px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="scState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="scBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="scBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="scTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="scTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="scPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('scBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['scSaleCompCode','scSaleCompName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('scBtnNew').addEventListener('click', () => openEditModal(null));

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
      pSaleCompCode: document.getElementById('scSaleCompCode').value.trim(),
      pSaleCompName: document.getElementById('scSaleCompName').value.trim(),
      pState:        document.getElementById('scState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('scTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('scPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/sale-comp/count', params);
      document.getElementById('scTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/sale-comp', {
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
    const wrap = document.getElementById('scTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다 (다른 거래처 화면과 통일된 패턴).
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(s => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${s.saleCompCode || ''}">${s.saleCompCode || ''}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${s.saleCompName || ''}">${s.saleCompName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${s.comp?.compName || ''}">소속: ${s.comp?.compName || ''}</div>
        </td>
        <td style="font-size:12px;font-family:monospace;${ell}" title="${s.svcCode || ''}">${s.svcCode || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${s.saleCompDesc || ''}">${s.saleCompDesc || ''}</td>
        <td style="text-align:center">
          <span class="badge ${s.state === 1 ? 'badge-green' : 'badge-gray'}">${s.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${s.saleCompCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${s.saleCompCode}" data-name="${(s.saleCompName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭을 못 준 열("판매사명/소속회사")은 지정된
    // 열들의 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 이 열에도 고정폭을 주고
    // 테이블 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는
    // 대신 table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:750px">
        <colgroup>
          <col style="width:110px"><col style="width:220px"><col style="width:90px">
          <col style="width:170px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">판매사코드</th>
            <th style="${thEll}">판매사명 / 소속회사</th>
            <th style="${thEll}">서비스코드</th>
            <th style="${thEll}">설명</th>
            <th style="text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const saleComp = await Api.get(`/company/sale-comp/${btn.dataset.code}`);
          openEditModal(saleComp);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 판매사를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/sale-comp/${btn.dataset.code}`);
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

    const el = document.getElementById('scPagination');
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
  function openEditModal(saleComp) {
    const isNew = !saleComp;
    const v = saleComp || {};

    const compOpts = compOptions.map(c =>
      `<option value="${c.compCode}" ${v.compCode === c.compCode ? 'selected' : ''}>${c.compName || '(회사명 미입력)'} (${c.compCode})</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>판매사코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="scFSaleCompCode" value="${v.saleCompCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="소속 회사를 고르면 자동으로 채워집니다">
        </div>
        <div class="form-group">
          <label>소속 회사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="scFCompCode">
            <option value="">-- 선택 --</option>
            ${compOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>판매사명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="scFSaleCompName" value="${v.saleCompName || ''}">
        </div>
        <div class="form-group">
          <label>서비스코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="scFSvcCode" value="${v.svcCode || (isNew ? ((typeof info !== 'undefined' && info?.svcCode) || 'SHP001') : '')}"
            ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="예: SHP001">
        </div>
        <div class="form-group">
          <label>승인일</label>
          <input class="input" id="scFApprovDate" type="date" value="${v.approvDate ? v.approvDate.substring(0,10) : ''}">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="scFSaleCompDesc" value="${v.saleCompDesc || ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="scFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="scFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 판매사코드 = 회사코드 + "Z" 관례 (기존 26건 전부 이 규칙을 따름) — 신규 등록 시
    // 소속 회사를 고르면 자동으로 채워준다. 완전히 잠그지는 않고 필요하면 직접 고칠 수 있게 둔다.
    if (isNew) {
      body.querySelector('#scFCompCode').addEventListener('change', e => {
        const compCode = e.target.value;
        document.getElementById('scFSaleCompCode').value = compCode ? `${compCode}Z` : '';
      });
    }

    UI.modal({
      title: isNew ? '판매사 신규 등록' : `판매사 수정 – ${v.saleCompCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const saleCompCode = document.getElementById('scFSaleCompCode').value.trim();
        const compCode     = document.getElementById('scFCompCode').value;
        const saleCompName = document.getElementById('scFSaleCompName').value.trim();
        // 서비스코드는 수정 시 readonly라 값을 유지(v.svcCode 그대로) — 채널 관리 목록/등록의
        // pSvcCode 의존 로직(OrderProdMapper와 동일한 부류의 위험)을 애초에 만들지 않기 위해
        // 등록 이후엔 바꾸지 않는다.
        const svcCode      = isNew ? document.getElementById('scFSvcCode').value.trim() : (v.svcCode || '');

        if (!saleCompCode) { UI.toast('판매사코드를 입력하세요', 'error'); return; }
        if (!compCode)     { UI.toast('소속 회사를 선택하세요', 'error'); return; }
        if (!saleCompName) { UI.toast('판매사명을 입력하세요', 'error'); return; }
        if (isNew && !svcCode) { UI.toast('서비스코드를 입력하세요', 'error'); return; }

        const payload = {
          saleCompCode,
          compCode,
          saleCompName,
          svcCode,
          saleCompDesc: document.getElementById('scFSaleCompDesc').value.trim(),
          approvDate:   document.getElementById('scFApprovDate').value || null,
          state:        parseInt(document.getElementById('scFState').value),
          remark:       document.getElementById('scFRemark').value.trim(),
          registId:     (typeof info !== 'undefined' && info?.loginId) || '',
          registName:   (typeof info !== 'undefined' && info?.name) || '',
          changeId:     (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:   (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/sale-comp', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/sale-comp', payload);
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

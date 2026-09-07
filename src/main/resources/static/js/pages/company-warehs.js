/**
 * 창고 관리 (거래처 - 창고)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (소속 회사 선택 — 창고코드 자동 채번에만 쓰이고 저장되진 않는다,
 *   tCmpWarehs에 소속회사 FK 컬럼이 없는 레거시 설계라 코드 문자열 자체가 소속을 나타낸다)
 * - 삭제
 * - 비밀번호는 SHA 해싱 저장 (신규 등록 시 해싱 / 일반 수정은 비밀번호 미변경 / 변경은 별도 API)
 */
const PageCompanyWarehs = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let compOptions = []; // { compCode, compName } — 창고코드 자동 채번용, 진입 시 1회 로드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">창고 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>창고코드</label>
              <input class="input" id="wrWarehsCode" placeholder="창고코드" style="width:150px">
            </div>
            <div class="form-group">
              <label>창고명</label>
              <input class="input" id="wrWarehsName" placeholder="창고명" style="width:160px">
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="wrState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="wrBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="wrBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="wrTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="wrTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="wrPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('wrBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['wrWarehsCode','wrWarehsName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('wrBtnNew').addEventListener('click', () => openEditModal(null));

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
      pWarehsCode: document.getElementById('wrWarehsCode').value.trim(),
      pWarehsName: document.getElementById('wrWarehsName').value.trim(),
      pState:      document.getElementById('wrState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('wrTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('wrPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/warehs/count', params);
      document.getElementById('wrTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/warehs', {
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
    const wrap = document.getElementById('wrTableWrap');
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(w => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${w.warehsCode || ''}">${w.warehsCode || ''}</td>
        <td style="${ell}" title="${w.warehsName || ''}">${w.warehsName || ''}</td>
        <td style="font-size:12px;${ell}" title="${w.mngrName || ''}">${w.mngrName || ''}</td>
        <td style="font-size:12px;${ell}" title="${w.mobileNo || w.phoneNo || ''}">${w.mobileNo || w.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${w.email || ''}">${w.email || ''}</td>
        <td style="text-align:center">
          <span class="badge ${w.state === 1 ? 'badge-green' : 'badge-gray'}">${w.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${w.warehsCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${w.warehsCode}" data-name="${(w.warehsName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭을 못 준 열(창고명)은 지정된 열들의 폭 합계가
    // 카드 폭을 넘는 순간 강제로 찌부러진다. 이 열에도 고정폭을 주고 테이블 자체는 width:100%
    // 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신 table-wrap의 가로 스크롤
    // (overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:770px">
        <colgroup>
          <col style="width:130px"><col style="width:180px"><col style="width:90px">
          <col style="width:120px"><col style="width:160px">
          <col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">창고코드</th><th style="${thEll}">창고명</th><th style="${thEll}">담당자</th>
            <th style="${thEll}">연락처</th><th style="${thEll}">이메일</th>
            <th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const warehs = await Api.get(`/company/warehs/${btn.dataset.code}`);
          openEditModal(warehs);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 창고를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/warehs/${btn.dataset.code}`);
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

    const el = document.getElementById('wrPagination');
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
  function openEditModal(warehs) {
    const isNew = !warehs;
    const v = warehs || {};

    const compOpts = compOptions.map(c =>
      `<option value="${c.compCode}">${c.compName || '(회사명 미입력)'} (${c.compCode})</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>창고코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="wrFWarehsCode" value="${v.warehsCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="소속 회사를 고르면 자동으로 채워집니다">
        </div>
        ${isNew ? `
        <div class="form-group">
          <label>소속 회사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="wrFCompCode">
            <option value="">-- 선택 --</option>
            ${compOpts}
          </select>
        </div>` : `
        <div class="form-group"></div>`}
        <div class="form-group full">
          <label>창고명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="wrFWarehsName" value="${v.warehsName || ''}">
        </div>
        <div class="form-group">
          <label>로그인ID <span style="color:var(--danger)">*</span></label>
          <input class="input" id="wrFLoginId" value="${v.loginId || ''}" placeholder="창고 로그인 ID">
        </div>
        <div class="form-group">
          <label>비밀번호 ${!isNew ? '<span style="font-weight:400;color:var(--text-muted)">(변경 시에만 입력)</span>' : '<span style="color:var(--danger)">*</span>'}</label>
          <input class="input" id="wrFPasswd" type="text" placeholder="${!isNew ? '기존 비밀번호 유지' : '비밀번호'}">
        </div>
        <div class="form-group">
          <label>서비스코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="wrFSvcCode" value="${v.svcCode || (isNew ? ((typeof info !== 'undefined' && info?.svcCode) || 'SHP001') : '')}"
            ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="예: SHP001">
        </div>
        <div class="form-group">
          <label>정산 주기 코드</label>
          <input class="input" id="wrFAdjustPeriodCode" value="${v.adjustPeriodCode || ''}">
        </div>
        <div class="form-group">
          <label>담당자명</label>
          <input class="input" id="wrFMngrName" value="${v.mngrName || ''}">
        </div>
        <div class="form-group">
          <label>휴대폰</label>
          <input class="input" id="wrFMobileNo" value="${v.mobileNo || ''}">
        </div>
        <div class="form-group">
          <label>전화번호</label>
          <input class="input" id="wrFPhoneNo" value="${v.phoneNo || ''}">
        </div>
        <div class="form-group">
          <label>팩스번호</label>
          <input class="input" id="wrFFaxNo" value="${v.faxNo || ''}">
        </div>
        <div class="form-group full">
          <label>이메일 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="wrFEmail" value="${v.email || ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="wrFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="wrFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 창고코드 = 소속회사코드 + "W" + 순번(1부터) 관례 — 신규 등록 시 소속 회사를 고르면
    // 그 회사의 마지막 창고코드 다음 번호로 자동 채워준다. 완전히 잠그지는 않고 필요하면
    // 직접 고칠 수 있게 둔다. 소속 회사 자체는 코드 생성에만 쓰이고 저장되지 않는다
    // (tCmpWarehs에 소속회사 FK 컬럼이 없는 레거시 설계).
    if (isNew) {
      body.querySelector('#wrFCompCode').addEventListener('change', async e => {
        const compCode = e.target.value;
        const warehsCodeInput = document.getElementById('wrFWarehsCode');
        if (!compCode) { warehsCodeInput.value = ''; return; }
        try {
          const maxCode = await Api.get('/company/warehs/max', { compCode });
          const prefix = `${compCode}W`;
          const suffix = maxCode && maxCode.startsWith(prefix) ? maxCode.slice(prefix.length) : '';
          const n = parseInt(suffix, 10);
          warehsCodeInput.value = prefix + (Number.isFinite(n) ? n + 1 : 1);
        } catch (_) {
          warehsCodeInput.value = `${compCode}W1`;
        }
      });
    }

    UI.modal({
      title: isNew ? '창고 신규 등록' : `창고 수정 – ${v.warehsCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const warehsCode = document.getElementById('wrFWarehsCode').value.trim();
        const warehsName = document.getElementById('wrFWarehsName').value.trim();
        const loginId    = document.getElementById('wrFLoginId').value.trim();
        const email      = document.getElementById('wrFEmail').value.trim();
        // 비밀번호는 서버가 해싱해서 저장하고 조회 응답에도 절대 실어주지 않으므로(항상 null),
        // "입력 안 하면 기존 값 재전송" 방식은 쓸 수 없다. 신규 등록 시엔 필수 입력이고,
        // 수정 시엔 입력했을 때만 별도 API(PUT /company/warehs/passwd)로 바꾼다.
        const passwdInput = document.getElementById('wrFPasswd').value;
        // 서비스코드는 수정 시 readonly라 값을 유지한다.
        const svcCode = isNew ? document.getElementById('wrFSvcCode').value.trim() : (v.svcCode || '');

        if (!warehsCode)  { UI.toast('창고코드를 입력하세요', 'error'); return; }
        if (!warehsName)  { UI.toast('창고명을 입력하세요', 'error'); return; }
        if (!loginId)     { UI.toast('로그인ID를 입력하세요', 'error'); return; }
        if (!email)       { UI.toast('이메일을 입력하세요', 'error'); return; }
        if (isNew && !svcCode) { UI.toast('서비스코드를 입력하세요', 'error'); return; }
        if (isNew && !passwdInput) { UI.toast('비밀번호를 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          warehsCode,
          warehsName,
          loginId,
          email,
          svcCode,
          ...(isNew ? { passwd: passwdInput } : {}),
          adjustPeriodCode: document.getElementById('wrFAdjustPeriodCode').value.trim(),
          mngrName:         document.getElementById('wrFMngrName').value.trim(),
          mobileNo:         document.getElementById('wrFMobileNo').value.trim(),
          phoneNo:          document.getElementById('wrFPhoneNo').value.trim(),
          faxNo:            document.getElementById('wrFFaxNo').value.trim(),
          state:            parseInt(document.getElementById('wrFState').value),
          remark:           document.getElementById('wrFRemark').value.trim(),
          registId,
          registName,
          changeId:         registId,
          changeName:       registName,
        };

        try {
          if (isNew) {
            await Api.post('/company/warehs', payload);
          } else {
            await Api.put('/company/warehs', payload);
            // 비밀번호를 새로 입력했을 때만 별도 호출 — 일반 정보 저장과 완전히 분리해,
            // "변경 안 함"을 이중 해싱하거나 실수로 지우는 사고를 막는다.
            if (passwdInput) {
              await Api.put('/company/warehs/passwd', { warehsCode, passwd: passwdInput, changeId: registId, changeName: registName });
            }
          }
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

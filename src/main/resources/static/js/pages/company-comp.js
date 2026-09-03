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
              <label>회사코드(사업자번호)</label>
              <input class="input" id="cpCompCode" placeholder="숫자만 또는 000-00-00000" style="width:150px">
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
      // 회사코드=사업자등록번호이므로, 검색창에 하이픈을 넣어 검색해도 되게 숫자만 남긴다.
      pCompCode: document.getElementById('cpCompCode').value.replace(/[^0-9]/g, ''),
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

    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다 (order-list.js 등 다른 화면의 기존 패턴과 통일).
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';
    const rows = list.map(c => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${c.compCode || ''}">${c.compCode || ''}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${c.compName || ''}">${c.compName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${c.prsdntName || ''}">${c.prsdntName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${c.phoneNo || ''}">${c.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${c.email || ''}">${c.email || ''}</td>
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

    // table-layout:fixed + width:100%에서 폭 미지정 열("회사명/대표자")은 지정된 열들의
    // 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블
    // 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함
    // (다른 목록 화면들과 동일한 규칙).
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:770px">
        <colgroup>
          <col style="width:110px"><col style="width:220px"><col style="width:120px">
          <col style="width:170px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}" title="회사코드(사업자등록번호)">회사코드</th>
            <th style="${thEll}">회사명 / 대표자</th>
            <th style="${thEll}">전화번호</th>
            <th style="${thEll}">이메일</th>
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

  // ── 사업자등록번호 (회사코드) 유틸 ─────────────────────────────────
  // 회사코드는 사업자등록번호(하이픈 제외 10자리 숫자)를 그대로 쓴다. 입력창엔
  // 000-00-00000 형태로 자동 하이픈을 넣어 보여주고, 실제 저장 값은 숫자만 남긴다.
  function formatBizNo(raw) {
    const digits = (raw || '').replace(/[^0-9]/g, '').slice(0, 10);
    if (digits.length <= 3) return digits;
    if (digits.length <= 5) return `${digits.slice(0,3)}-${digits.slice(3)}`;
    return `${digits.slice(0,3)}-${digits.slice(3,5)}-${digits.slice(5)}`;
  }

  // 사업자등록번호 체크섬 검증 (국세청 공개 알고리즘: 가중치 1,3,7,1,3,7,1,3,5 +
  // 9번째 자리 보정항). 10자리가 아니거나 체크섬이 안 맞으면 false.
  function isValidBizNo(raw) {
    const digits = (raw || '').replace(/[^0-9]/g, '');
    if (digits.length !== 10) return false;
    const weight = [1, 3, 7, 1, 3, 7, 1, 3, 5];
    let sum = 0;
    for (let i = 0; i < 9; i++) sum += parseInt(digits[i], 10) * weight[i];
    sum += Math.floor((parseInt(digits[8], 10) * 5) / 10);
    const check = (10 - (sum % 10)) % 10;
    return check === parseInt(digits[9], 10);
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  function openEditModal(comp) {
    const isNew = !comp;
    const v = comp || {};

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>회사코드 (사업자등록번호) <span style="color:var(--danger)">*</span></label>
          <input class="input" id="cpFCompCode" value="${isNew ? '' : formatBizNo(v.compCode || '')}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="000-00-00000" inputmode="numeric">
          ${isNew ? '<div style="font-size:11px;color:var(--text-muted);margin-top:4px">사업자등록번호를 입력하면 회사코드로 그대로 사용됩니다.</div>' : ''}
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
          <label>법인등록번호</label>
          <input class="input" id="cpFCorpCode" value="${v.corpCode || ''}" placeholder="법인인 경우에만 입력 (개인사업자는 공란)">
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

    // 신규 등록일 때만: 타이핑하는 동안 000-00-00000 형태로 자동 하이픈 삽입.
    if (isNew) {
      const compCodeInput = body.querySelector('#cpFCompCode');
      compCodeInput.addEventListener('input', () => {
        compCodeInput.value = formatBizNo(compCodeInput.value);
      });
    }

    UI.modal({
      title: isNew ? '회사 신규 등록' : `회사 수정 – ${v.compCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        // 저장 값은 하이픈 뺀 순수 숫자만 사용 (기존 데이터도 하이픈 없는 10자리 그대로 저장돼 있음).
        const compCode = document.getElementById('cpFCompCode').value.replace(/[^0-9]/g, '');
        const compName = document.getElementById('cpFCompName').value.trim();
        const svcCode  = document.getElementById('cpFSvcCode').value.trim();

        if (!compCode) { UI.toast('회사코드(사업자등록번호)를 입력하세요', 'error'); return; }
        if (isNew && !isValidBizNo(compCode)) {
          UI.toast('사업자등록번호가 올바르지 않습니다. 숫자와 자릿수를 확인해주세요', 'error');
          return;
        }
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

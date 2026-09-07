/**
 * 상점 관리 (거래처 - 상점)
 * - 목록 조회 (페이지네이션, 공급사 선택 필터 포함)
 * - 등록 / 수정 (공급사 선택)
 * - 삭제
 */
const PageCompanyShop = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let supplyOptions = []; // { supplyCode, supplyName } — 공급사 선택용, 진입 시 1회 로드

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">상점 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>상점코드</label>
              <input class="input" id="shShopCode" placeholder="상점코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>상점명</label>
              <input class="input" id="shShopName" placeholder="상점명" style="width:160px">
            </div>
            <div class="form-group">
              <label>공급사</label>
              <select class="input" id="shSupplyCode" style="width:180px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="shState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="shBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="shBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="shTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="shTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="shPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('shBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['shShopCode','shShopName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('shBtnNew').addEventListener('click', () => openEditModal(null));

    loadSupplyOptions().then(() => {
      const sel = document.getElementById('shSupplyCode');
      sel.innerHTML = '<option value="">전체</option>' + supplyOptions.map(sc =>
        `<option value="${sc.supplyCode}">${sc.supplyName || '(공급사명 미입력)'} (${sc.supplyCode})</option>`
      ).join('');
    });
    loadList();
  }

  // 공급사 선택용 목록 (검색 필터 + 등록/수정 모달 공용, 전체를 한 번에 불러온다)
  async function loadSupplyOptions() {
    try {
      supplyOptions = await Api.get('/company/supply-comp', { pPageOffset: 0, pPageSize: 2000 });
    } catch (_) { supplyOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pShopCode:   document.getElementById('shShopCode').value.trim(),
      pShopName:   document.getElementById('shShopName').value.trim(),
      pSupplyCode: document.getElementById('shSupplyCode').value,
      pState:      document.getElementById('shState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('shTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('shPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/shop/count', params);
      document.getElementById('shTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/shop', {
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
    const wrap = document.getElementById('shTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(s => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${s.shopCode || ''}">${s.shopCode || ''}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${s.shopName || ''}">${s.shopName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${s.supplyName || ''}">${s.supplyName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${s.mngrName || ''}">${s.mngrName || ''}</td>
        <td style="font-size:12px;${ell}" title="${s.mobileNo || s.phoneNo || ''}">${s.mobileNo || s.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${s.email || ''}">${s.email || ''}</td>
        <td style="font-size:11px;${ell}">
          ${s.applyStartDate ? `${s.applyStartDate.substring(0,10)} ~` : ''}
          ${s.applyEndDate ? s.applyEndDate.substring(0,10) : ''}
        </td>
        <td style="text-align:center">
          <span class="badge ${s.state === 1 ? 'badge-green' : 'badge-gray'}">${s.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${s.shopCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${s.shopCode}" data-name="${(s.shopName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("상점명/공급사")은 지정된 열들의
    // 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블
    // 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:930px">
        <colgroup>
          <col style="width:110px"><col style="width:200px"><col style="width:80px">
          <col style="width:110px"><col style="width:140px">
          <col style="width:140px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">상점코드</th><th style="${thEll}">상점명 / 공급사</th><th style="${thEll}">담당자</th>
            <th style="${thEll}">연락처</th><th style="${thEll}">이메일</th><th style="${thEll}">계약기간</th>
            <th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 수정
    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const shop = await Api.get(`/company/shop/${btn.dataset.code}`);
          openEditModal(shop);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    // 삭제
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상점을 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/shop/${btn.dataset.code}`);
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

    const el = document.getElementById('shPagination');
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
  function openEditModal(shop) {
    const isNew = !shop;
    const v = shop || {};

    const supplyOpts = supplyOptions.map(sc =>
      `<option value="${sc.supplyCode}" ${v.supplyCode === sc.supplyCode ? 'selected' : ''}>${sc.supplyName || '(공급사명 미입력)'} (${sc.supplyCode})</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>상점코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFShopCode" value="${v.shopCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="공급사를 고르면 자동으로 채워집니다">
        </div>
        <div class="form-group">
          <label>공급사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="shFSupplyCode" ${!isNew ? 'disabled style="background:#f8fafc"' : ''}>
            <option value="">-- 선택 --</option>
            ${supplyOpts}
          </select>
        </div>
        <div class="form-group">
          <label>서비스코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFSvcCode" value="${v.svcCode || (isNew ? ((typeof info !== 'undefined' && info?.svcCode) || 'SHP001') : '')}"
            ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="예: SHP001">
        </div>
        <div class="form-group full">
          <label>상점명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFShopName" value="${v.shopName || ''}">
        </div>
        <div class="form-group">
          <label>로그인ID <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFLoginId" value="${v.loginId || ''}" placeholder="상점 로그인 ID">
        </div>
        <div class="form-group">
          <label>비밀번호 ${!isNew ? '<span style="font-weight:400;color:var(--text-muted)">(변경 시에만 입력)</span>' : '<span style="color:var(--danger)">*</span>'}</label>
          <input class="input" id="shFPasswd" type="text" placeholder="${!isNew ? '기존 비밀번호 유지' : '비밀번호'}">
        </div>
        <div class="form-group">
          <label>담당자명</label>
          <input class="input" id="shFMngrName" value="${v.mngrName || ''}">
        </div>
        <div class="form-group">
          <label>담당 MD</label>
          <input class="input" id="shFMngrMd" value="${v.mngrMd || ''}">
        </div>
        <div class="form-group">
          <label>휴대폰</label>
          <input class="input" id="shFMobileNo" value="${v.mobileNo || ''}">
        </div>
        <div class="form-group">
          <label>전화번호</label>
          <input class="input" id="shFPhoneNo" value="${v.phoneNo || ''}">
        </div>
        <div class="form-group full">
          <label>이메일</label>
          <input class="input" id="shFEmail" value="${v.email || ''}">
        </div>
        <div class="form-group">
          <label>계약시작일</label>
          <input class="input" id="shFStartDate" type="date" value="${v.applyStartDate ? v.applyStartDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>계약종료일</label>
          <input class="input" id="shFEndDate" type="date" value="${v.applyEndDate ? v.applyEndDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>자동주문여부</label>
          <select class="input" id="shFAutoOrderYn">
            <option value="0" ${v.autoOrderYn === 0 ? 'selected' : ''}>아니오</option>
            <option value="1" ${v.autoOrderYn === 1 ? 'selected' : ''}>예</option>
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="shFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="shFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 상점코드 = 공급사코드 + 순번(1부터) 관례 (기존 112건 전부 이 규칙을 따름) — 신규
    // 등록 시 공급사를 고르면 그 공급사의 마지막 상점코드 다음 번호로 자동 채워준다.
    // 완전히 잠그지는 않고 필요하면 직접 고칠 수 있게 둔다.
    if (isNew) {
      body.querySelector('#shFSupplyCode').addEventListener('change', async e => {
        const supplyCode = e.target.value;
        const shopCodeInput = document.getElementById('shFShopCode');
        if (!supplyCode) { shopCodeInput.value = ''; return; }
        try {
          const maxCode = await Api.get('/company/shop/max', { supplyCode });
          const suffix = maxCode && maxCode.startsWith(supplyCode) ? maxCode.slice(supplyCode.length) : '';
          const n = parseInt(suffix, 10);
          shopCodeInput.value = supplyCode + (Number.isFinite(n) ? n + 1 : 1);
        } catch (_) {
          shopCodeInput.value = supplyCode + '1';
        }
      });
    }

    UI.modal({
      title: isNew ? '상점 신규 등록' : `상점 수정 – ${v.shopCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const shopCode   = document.getElementById('shFShopCode').value.trim();
        // 수정 모드에선 select가 disabled라 .value가 빈 문자열로 읽히므로, 기존 값(v.supplyCode)을 그대로 쓴다.
        const supplyCode = isNew ? document.getElementById('shFSupplyCode').value : (v.supplyCode || '');
        // 서비스코드도 마찬가지 이유(readonly 시 값 유실 방지)로 수정 모드에선 기존 값을 그대로 쓴다.
        // 이 값이 비어있으면(신규 등록 화면에 필드가 없던 예전 버전으로 등록된 상점) 주문조회 API가
        // OrderProdMapper의 pSvcCode 의존 JOIN 때문에 오류나므로, 신규 등록은 항상 값을 채우도록 강제한다.
        const svcCode    = isNew ? document.getElementById('shFSvcCode').value.trim() : (v.svcCode || '');
        const shopName   = document.getElementById('shFShopName').value.trim();
        const loginId    = document.getElementById('shFLoginId').value.trim();
        // 비밀번호는 서버가 해싱해서 저장하고 조회 응답에도 절대 실어주지 않으므로(항상 null),
        // "입력 안 하면 기존 값 재전송" 방식은 쓸 수 없다. 신규 등록 시엔 필수 입력이고,
        // 수정 시엔 입력했을 때만 별도 API(PUT /company/shop/passwd)로 바꾼다.
        const passwdInput = document.getElementById('shFPasswd').value;

        if (!shopCode)   { UI.toast('상점코드를 입력하세요', 'error'); return; }
        if (!supplyCode) { UI.toast('공급사를 선택하세요', 'error'); return; }
        if (isNew && !svcCode) { UI.toast('서비스코드를 입력하세요', 'error'); return; }
        if (!shopName)   { UI.toast('상점명을 입력하세요', 'error'); return; }
        if (!loginId)    { UI.toast('로그인ID를 입력하세요', 'error'); return; }
        if (isNew && !passwdInput) { UI.toast('비밀번호를 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          shopCode,
          supplyCode,
          svcCode,
          shopName,
          loginId,
          ...(isNew ? { passwd: passwdInput } : {}),
          mngrName:       document.getElementById('shFMngrName').value.trim(),
          mngrMd:         document.getElementById('shFMngrMd').value.trim(),
          mobileNo:       document.getElementById('shFMobileNo').value.trim(),
          phoneNo:        document.getElementById('shFPhoneNo').value.trim(),
          email:          document.getElementById('shFEmail').value.trim(),
          applyStartDate: document.getElementById('shFStartDate').value || null,
          applyEndDate:   document.getElementById('shFEndDate').value || null,
          autoOrderYn:    parseInt(document.getElementById('shFAutoOrderYn').value),
          state:          parseInt(document.getElementById('shFState').value),
          remark:         document.getElementById('shFRemark').value.trim(),
          registId,
          registName,
          changeId:       registId,
          changeName:     registName,
        };

        try {
          if (isNew) {
            await Api.post('/company/shop', payload);
          } else {
            await Api.put('/company/shop', payload);
            // 비밀번호를 새로 입력했을 때만 별도 호출 — 일반 정보 저장과 완전히 분리해,
            // "변경 안 함"을 이중 해싱하거나 실수로 지우는 사고를 막는다.
            if (passwdInput) {
              await Api.put('/company/shop/passwd', { shopCode, passwd: passwdInput, changeId: registId, changeName: registName });
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

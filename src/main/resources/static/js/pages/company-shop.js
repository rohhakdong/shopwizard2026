/**
 * 쇼핑몰 관리
 * - 목록 조회 (페이지네이션)
 * - 상세보기 / 수정
 * - 삭제
 */
const PageCompanyShop = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">쇼핑몰 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>쇼핑몰코드</label>
              <input class="input" id="shShopCode" placeholder="쇼핑몰코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>쇼핑몰명</label>
              <input class="input" id="shShopName" placeholder="쇼핑몰명" style="width:160px">
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

    loadList();
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pShopCode: document.getElementById('shShopCode').value.trim(),
      pShopName: document.getElementById('shShopName').value.trim(),
      pState:    document.getElementById('shState').value,
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

    const rows = list.map(s => `
      <tr>
        <td style="font-size:11px;font-family:monospace">${s.shopCode || ''}</td>
        <td>
          <div style="font-weight:500;font-size:13px">${s.shopName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${s.supplyName || ''}</div>
        </td>
        <td style="font-size:12px">${s.mngrName || ''}</td>
        <td style="font-size:12px">${s.mobileNo || s.phoneNo || ''}</td>
        <td style="font-size:12px;color:var(--text-muted)">${s.email || ''}</td>
        <td style="font-size:11px">
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

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:110px"><col><col style="width:80px">
          <col style="width:110px"><col style="width:140px">
          <col style="width:140px"><col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th>쇼핑몰코드</th><th>쇼핑몰명 / 협력사</th><th>담당자</th>
            <th>연락처</th><th>이메일</th><th>계약기간</th>
            <th style="text-align:center">상태</th><th></th>
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
        UI.confirm(`[${btn.dataset.name}] 쇼핑몰을 삭제하시겠습니까?`, async close => {
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

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>쇼핑몰코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFShopCode" value="${v.shopCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="예: SHP001A001">
        </div>
        <div class="form-group">
          <label>협력사코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFSupplyCode" value="${v.supplyCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="협력사코드">
        </div>
        <div class="form-group full">
          <label>쇼핑몰명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFShopName" value="${v.shopName || ''}">
        </div>
        <div class="form-group">
          <label>로그인ID <span style="color:var(--danger)">*</span></label>
          <input class="input" id="shFLoginId" value="${v.loginId || ''}" placeholder="쇼핑몰 로그인 ID">
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

    UI.modal({
      title: isNew ? '쇼핑몰 신규 등록' : `쇼핑몰 수정 – ${v.shopCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const shopCode   = document.getElementById('shFShopCode').value.trim();
        const supplyCode = document.getElementById('shFSupplyCode').value.trim();
        const shopName   = document.getElementById('shFShopName').value.trim();
        const loginId    = document.getElementById('shFLoginId').value.trim();
        const passwdInput = document.getElementById('shFPasswd').value;
        // LoginId/Passwd는 tCmpShop의 NOT NULL 컬럼이라, 신규 등록 시엔 반드시 입력받고
        // 수정 시엔 비워두면 기존 값을 그대로 유지한다 (수정할 때마다 재입력을 강제하지 않기 위함).
        const passwd = passwdInput ? passwdInput : (v.passwd || '');

        if (!shopCode)   { UI.toast('쇼핑몰코드를 입력하세요', 'error'); return; }
        if (!supplyCode) { UI.toast('협력사코드를 입력하세요', 'error'); return; }
        if (!shopName)   { UI.toast('쇼핑몰명을 입력하세요', 'error'); return; }
        if (!loginId)    { UI.toast('로그인ID를 입력하세요', 'error'); return; }
        if (!passwd)     { UI.toast('비밀번호를 입력하세요', 'error'); return; }

        const payload = {
          shopCode,
          supplyCode,
          shopName,
          loginId,
          passwd,
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
          registId:       (typeof info !== 'undefined' && info?.loginId) || '',
          registName:     (typeof info !== 'undefined' && info?.name) || '',
          changeId:       (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:     (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/shop', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/shop', payload);
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

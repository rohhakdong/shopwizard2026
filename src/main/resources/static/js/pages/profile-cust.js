/**
 * 회원(고객) 관리
 */
const PageProfileCust = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let custGroupList = []; // 회원등급 목록 캐시

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>회원 관리</span>
          <button class="btn btn-primary" id="btnAddCust">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>아이디</label>
              <input class="input" id="cLoginId" placeholder="아이디" style="width:140px">
            </div>
            <div class="form-group">
              <label>이름</label>
              <input class="input" id="cCustName" placeholder="이름" style="width:140px">
            </div>
            <div class="form-group">
              <label>등급</label>
              <select class="input" id="cCustGroupCode" style="width:150px">
                <option value="">전체</option>
              </select>
            </div>
            <button class="btn btn-primary" id="cBtnSearch">검색</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="cTotalLabel">0</strong>명
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="cTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="cPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('cBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('cLoginId').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('cCustName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('btnAddCust').addEventListener('click', () => openModal(null));

    loadCustGroups().then(loadList);
  }

  // ── 회원등급 목록 로드 (select 옵션 채우기) ─────────────────────────
  async function loadCustGroups() {
    try {
      custGroupList = await Api.get('/profile/custGroup/list', {}) || [];
      const sel = document.getElementById('cCustGroupCode');
      custGroupList.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.custGroupCode;
        opt.textContent = g.custGroupName;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pLoginId:       document.getElementById('cLoginId').value.trim(),
      pCustName:      document.getElementById('cCustName').value.trim(),
      pCustGroupCode: document.getElementById('cCustGroupCode').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('cTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('cPagination').innerHTML = '';

    try {
      const params = getParams();

      totalCount = await Api.get('/profile/cust/count', params);
      document.getElementById('cTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/profile/cust/list', {
        ...params,
        topCnt:       (currentPage - 1) * PAGE_SIZE,
        countPerPage: PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('cTableWrap');

    const rows = list.map(c => `
      <tr>
        <td style="font-size:12px">${c.loginId || ''}</td>
        <td>${c.custName || ''}</td>
        <td style="font-size:12px">${c.custGroupName || '-'}</td>
        <td style="font-size:12px">${c.mobilePhoneNo || c.homePhoneNo || ''}</td>
        <td style="font-size:12px">${c.email || ''}</td>
        <td style="font-size:11px;color:var(--text-muted)">${c.chnlName || ''}</td>
        <td style="text-align:center">
          <span class="badge ${c.state === 1 ? 'badge-green' : 'badge-gray'}">${c.state === 1 ? '사용' : '미사용'}</span>
        </td>
        <td style="font-size:11px;color:var(--text-muted)">${c.registDate ? c.registDate.substring(0, 10) : ''}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-custid="${c.custId}">수정</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="passwd" data-custid="${c.custId}" data-name="${c.custName}">비번재설정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-custid="${c.custId}" data-name="${c.custName}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:100px"><col style="width:90px"><col style="width:100px">
          <col style="width:110px"><col><col style="width:80px">
          <col style="width:60px"><col style="width:80px"><col style="width:170px">
        </colgroup>
        <thead>
          <tr>
            <th>아이디</th><th>이름</th><th>등급</th><th>연락처</th>
            <th>이메일</th><th>가입채널</th><th style="text-align:center">상태</th>
            <th>가입일</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const c = await Api.get('/profile/cust/' + btn.dataset.custid);
          openModal(c);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=passwd]').forEach(btn => {
      btn.addEventListener('click', () => openPasswdModal(btn.dataset.custid, btn.dataset.name));
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`회원 [${btn.dataset.name}]을(를) 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/profile/cust/' + btn.dataset.custid);
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

    const el = document.getElementById('cPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start - 1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end + 1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentPage = parseInt(btn.dataset.page);
        loadList();
      });
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  function openModal(c) {
    const isEdit = !!c;
    const groupOpts = custGroupList.map(g =>
      `<option value="${g.custGroupCode}" ${c?.custGroupCode === g.custGroupCode ? 'selected' : ''}>${g.custGroupName}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>아이디 *</label>
          <input class="input" id="mLoginId" value="${c?.loginId || ''}"
            ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="로그인 아이디">
        </div>
        <div class="form-group">
          <label>이름 *</label>
          <input class="input" id="mCustName" value="${c?.custName || ''}" placeholder="회원 이름">
        </div>
        ${!isEdit ? `
        <div class="form-group">
          <label>비밀번호 *</label>
          <input class="input" type="password" id="mPasswd" placeholder="비밀번호">
        </div>` : ''}
        <div class="form-group">
          <label>등급</label>
          <select class="input" id="mCustGroupCode">
            <option value="">-- 선택 --</option>
            ${groupOpts}
          </select>
        </div>
        <div class="form-group">
          <label>이메일</label>
          <input class="input" id="mEmail" value="${c?.email || ''}" placeholder="이메일">
        </div>
        <div class="form-group">
          <label>휴대전화</label>
          <input class="input" id="mMobilePhoneNo" value="${c?.mobilePhoneNo || ''}" placeholder="010-0000-0000">
        </div>
        <div class="form-group">
          <label>자택전화</label>
          <input class="input" id="mHomePhoneNo" value="${c?.homePhoneNo || ''}" placeholder="02-0000-0000">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${c?.state === 1 || !c ? 'selected' : ''}>사용</option>
            <option value="0" ${c?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>주소</label>
          <input class="input" id="mAddr1" value="${c?.addr1 || ''}" placeholder="주소">
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${c?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '회원 수정' : '회원 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const loginId  = document.getElementById('mLoginId').value.trim();
        const custName = document.getElementById('mCustName').value.trim();
        const passwd   = !isEdit ? document.getElementById('mPasswd').value : null;

        if (!loginId || !custName)  { UI.toast('아이디와 이름은 필수입니다', 'error'); return; }
        if (!isEdit && !passwd)     { UI.toast('비밀번호를 입력하세요', 'error'); return; }

        const payload = {
          custId: c?.custId,
          loginId,
          custName,
          custGroupCode:  document.getElementById('mCustGroupCode').value,
          email:          document.getElementById('mEmail').value.trim(),
          mobilePhoneNo:  document.getElementById('mMobilePhoneNo').value.trim(),
          homePhoneNo:    document.getElementById('mHomePhoneNo').value.trim(),
          addr1:          document.getElementById('mAddr1').value.trim(),
          state:          parseInt(document.getElementById('mState').value),
          remark:         document.getElementById('mRemark').value.trim(),
          ...(isEdit
            ? { changeId: info?.loginId, changeName: info?.name }
            : { passwd, registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/profile/cust', payload);
          else        await Api.post('/profile/cust', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  // ── 비밀번호 재설정 모달 (일반 수정 API와 분리 — 평문 노출/오처리 방지) ─
  function openPasswdModal(custId, custName) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full">
          <label>${custName} 님의 새 비밀번호 *</label>
          <input class="input" type="password" id="mNewPasswd" placeholder="새 비밀번호">
        </div>
      </div>`;

    UI.modal({
      title: '비밀번호 재설정',
      confirmText: '변경',
      body,
      onConfirm: async close => {
        const passwd = document.getElementById('mNewPasswd').value;
        if (!passwd) { UI.toast('새 비밀번호를 입력하세요', 'error'); return; }

        try {
          await Api.put('/profile/cust/passwd', {
            custId: parseInt(custId),
            passwd,
            changeName: info?.name,
          });
          UI.toast('비밀번호가 변경되었습니다', 'success');
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  return { render };
})();

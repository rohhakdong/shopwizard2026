/**
 * 관리자 계정 관리
 */
const PageAuthMngr = (() => {

  let roleList = [];   // 역할 목록 캐시

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>관리자 계정</span>
          <button class="btn btn-primary" id="btnAddMngr">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar">
            <div class="form-group">
              <label>아이디</label>
              <input class="input" id="sLoginId" placeholder="아이디" style="width:160px">
            </div>
            <div class="form-group">
              <label>이름</label>
              <input class="input" id="sMngrName" placeholder="이름" style="width:160px">
            </div>
            <div class="form-group">
              <label>역할</label>
              <select class="input" id="sRoleUid" style="width:180px">
                <option value="">전체</option>
              </select>
            </div>
            <button class="btn btn-ghost" id="btnSearch" style="margin-top:18px">검색</button>
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="mngrTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>
          </div>

        </div>
      </div>`;

    document.getElementById('btnSearch').addEventListener('click', loadList);
    document.getElementById('sLoginId').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('sMngrName').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('btnAddMngr').addEventListener('click', () => openModal(null));

    loadRoles().then(loadList);
  }

  // ── 역할 목록 로드 (select 옵션 채우기) ────────────────────────────
  async function loadRoles() {
    try {
      roleList = await Api.get('/authority/role/list', {});
      const sel = document.getElementById('sRoleUid');
      roleList.forEach(r => {
        const opt = document.createElement('option');
        opt.value = r.roleUid;
        opt.textContent = r.roleName;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('mngrTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/mngr/list', {
        pLoginId:   document.getElementById('sLoginId').value.trim(),
        pMngrName:  document.getElementById('sMngrName').value.trim(),
        pRoleUid:   document.getElementById('sRoleUid').value,
      });
      renderTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('mngrTableWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
      return;
    }

    const rows = list.map(m => `
      <tr>
        <td>${m.loginId || ''}</td>
        <td>${m.mngrName || ''}</td>
        <td>${m.roleName || ''}</td>
        <td>${m.svcCode || ''}</td>
        <td>${m.email || ''}</td>
        <td>${m.mobileNo || ''}</td>
        <td style="text-align:center">
          <span class="badge ${m.state === 1 ? 'badge-green' : 'badge-gray'}">${m.state === 1 ? '사용' : '미사용'}</span>
        </td>
        <td>${m.registDate ? m.registDate.substring(0, 10) : ''}</td>
        <td>
          <button class="btn btn-ghost" style="padding:3px 8px;font-size:12px"
            data-action="edit" data-uid="${m.mngrUid}">수정</button>
          <button class="btn btn-danger" style="padding:3px 8px;font-size:12px"
            data-action="del" data-uid="${m.mngrUid}" data-name="${m.mngrName}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>아이디</th><th>이름</th><th>역할</th><th>서비스</th>
            <th>이메일</th><th>휴대폰</th><th style="width:60px">상태</th>
            <th style="width:90px">등록일</th><th style="width:110px"></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const m = await Api.get('/authority/mngr/info/' + btn.dataset.uid);
          openModal(m);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`관리자 [${btn.dataset.name}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/authority/mngr/' + btn.dataset.uid);
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  function openModal(m) {
    const isEdit = !!m;
    const roleOpts = roleList.map(r =>
      `<option value="${r.roleUid}" ${m?.roleUid === r.roleUid ? 'selected' : ''}>${r.roleName}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>아이디 *</label>
          <input class="input" id="mLoginId" value="${m?.loginId || ''}"
            ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="로그인 아이디">
        </div>
        <div class="form-group">
          <label>이름 *</label>
          <input class="input" id="mMngrName" value="${m?.mngrName || ''}" placeholder="관리자 이름">
        </div>
        <div class="form-group">
          <label>비밀번호 ${isEdit ? '(변경 시 입력)' : '*'}</label>
          <input class="input" type="password" id="mPasswd" placeholder="${isEdit ? '변경 시 입력' : '비밀번호'}">
        </div>
        <div class="form-group">
          <label>역할 *</label>
          <select class="input" id="mRoleUid">
            <option value="">-- 선택 --</option>
            ${roleOpts}
          </select>
        </div>
        <div class="form-group">
          <label>이메일</label>
          <input class="input" id="mEmail" value="${m?.email || ''}" placeholder="이메일">
        </div>
        <div class="form-group">
          <label>휴대폰</label>
          <input class="input" id="mMobileNo" value="${m?.mobileNo || ''}" placeholder="010-0000-0000">
        </div>
        <div class="form-group">
          <label>직통번호</label>
          <input class="input" id="mPhoneNo" value="${m?.phoneNo || ''}" placeholder="02-0000-0000">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${m?.state === 1 || !m ? 'selected' : ''}>사용</option>
            <option value="0" ${m?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${m?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '관리자 수정' : '관리자 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const loginId  = document.getElementById('mLoginId').value.trim();
        const mngrName = document.getElementById('mMngrName').value.trim();
        const passwd   = document.getElementById('mPasswd').value;
        const roleUid  = document.getElementById('mRoleUid').value;

        if (!loginId || !mngrName) { UI.toast('아이디와 이름은 필수입니다', 'error'); return; }
        if (!isEdit && !passwd)    { UI.toast('비밀번호를 입력하세요', 'error'); return; }
        if (!roleUid)              { UI.toast('역할을 선택하세요', 'error'); return; }

        const payload = {
          mngrUid:  m?.mngrUid,
          loginId,
          mngrName,
          roleUid,
          email:    document.getElementById('mEmail').value.trim(),
          mobileNo: document.getElementById('mMobileNo').value.trim(),
          phoneNo:  document.getElementById('mPhoneNo').value.trim(),
          state:    parseInt(document.getElementById('mState').value),
          remark:   document.getElementById('mRemark').value.trim(),
          ...(passwd ? { passwd } : {}),
          ...(isEdit
            ? { changeId: info?.loginId, changeName: info?.name }
            : { registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/authority/mngr', payload);
          else        await Api.post('/authority/mngr', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  return { render };
})();

/**
 * 역할 관리
 */
const PageAuthRole = (() => {

  let svcList = [];   // 서비스 목록 캐시

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>역할 관리</span>
          <button class="btn btn-primary" id="btnAddRole">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar">
            <div class="form-group">
              <label>역할명</label>
              <input class="input" id="sRoleName" placeholder="역할명" style="width:160px">
            </div>
            <div class="form-group">
              <label>설명</label>
              <input class="input" id="sRoleDesc" placeholder="설명" style="width:160px">
            </div>
            <div class="form-group">
              <label>서비스</label>
              <select class="input" id="sSvcCode" style="width:160px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>협력사</label>
              <select class="input" id="sSupplyYn" style="width:100px">
                <option value="">전체</option>
                <option value="1">협력사</option>
                <option value="0">일반</option>
              </select>
            </div>
            <button class="btn btn-ghost" id="btnSearch" style="margin-top:18px">검색</button>
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="roleTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>
          </div>

        </div>
      </div>`;

    document.getElementById('btnSearch').addEventListener('click', loadList);
    document.getElementById('sRoleName').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('sRoleDesc').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('btnAddRole').addEventListener('click', () => openModal(null));

    loadSvcList().then(loadList);
  }

  // ── 서비스 목록 로드 ───────────────────────────────────────────────
  async function loadSvcList() {
    try {
      svcList = await Api.get('/authority/svc/list', {});
      const sel = document.getElementById('sSvcCode');
      svcList.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.svcCode;
        opt.textContent = s.svcName || s.svcCode;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('roleTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/role/list', {
        pRoleName:  document.getElementById('sRoleName').value.trim(),
        pRoleDesc:  document.getElementById('sRoleDesc').value.trim(),
        pSvcCode:   document.getElementById('sSvcCode').value,
        pSupplyYn:  document.getElementById('sSupplyYn').value,
      });
      renderTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('roleTableWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
      return;
    }

    const rows = list.map(r => `
      <tr>
        <td>${r.roleName || ''}</td>
        <td>${r.roleDesc || ''}</td>
        <td>${r.svcName || r.svcCode || ''}</td>
        <td style="text-align:center">
          <span class="badge ${r.supplyYn === 1 ? 'badge-blue' : 'badge-gray'}">${r.supplyYn === 1 ? '협력사' : '일반'}</span>
        </td>
        <td style="text-align:center">
          <span class="badge ${r.state === 1 ? 'badge-green' : 'badge-gray'}">${r.state === 1 ? '사용' : '미사용'}</span>
        </td>
        <td>${r.registDate ? r.registDate.substring(0, 10) : ''}</td>
        <td>
          <button class="btn btn-ghost" style="padding:3px 8px;font-size:12px"
            data-action="edit" data-uid="${r.roleUid}">수정</button>
          <button class="btn btn-danger" style="padding:3px 8px;font-size:12px"
            data-action="del" data-uid="${r.roleUid}" data-name="${r.roleName}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>역할명</th><th>설명</th><th>서비스</th>
            <th style="width:60px">협력사</th><th style="width:60px">상태</th>
            <th style="width:90px">등록일</th><th style="width:110px"></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 수정 버튼: 해당 행 데이터를 list에서 찾아 모달 오픈
    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', () => {
        const item = list.find(r => r.roleUid === btn.dataset.uid);
        openModal(item);
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`역할 [${btn.dataset.name}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/authority/role/' + btn.dataset.uid);
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  function openModal(r) {
    const isEdit = !!r;
    const svcOpts = svcList.map(s =>
      `<option value="${s.svcCode}" ${r?.svcCode === s.svcCode ? 'selected' : ''}>${s.svcName || s.svcCode}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>역할명 *</label>
          <input class="input" id="mRoleName" value="${r?.roleName || ''}" placeholder="역할명">
        </div>
        <div class="form-group">
          <label>서비스</label>
          <select class="input" id="mSvcCode">
            <option value="">-- 선택 --</option>
            ${svcOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="mRoleDesc" value="${r?.roleDesc || ''}" placeholder="역할 설명">
        </div>
        <div class="form-group">
          <label>협력사 여부</label>
          <select class="input" id="mSupplyYn">
            <option value="0" ${!r || r.supplyYn === 0 ? 'selected' : ''}>일반</option>
            <option value="1" ${r?.supplyYn === 1 ? 'selected' : ''}>협력사</option>
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${!r || r.state === 1 ? 'selected' : ''}>사용</option>
            <option value="0" ${r?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${r?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '역할 수정' : '역할 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const roleName = document.getElementById('mRoleName').value.trim();
        if (!roleName) { UI.toast('역할명은 필수입니다', 'error'); return; }

        const payload = {
          roleName,
          roleDesc:  document.getElementById('mRoleDesc').value.trim(),
          svcCode:   document.getElementById('mSvcCode').value || null,
          supplyYn:  parseInt(document.getElementById('mSupplyYn').value),
          state:     parseInt(document.getElementById('mState').value),
          remark:    document.getElementById('mRemark').value.trim(),
          ...(isEdit
            ? { roleUid: r.roleUid, changeId: info?.loginId, changeName: info?.name }
            : { roleUid: crypto.randomUUID(), registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/authority/role', payload);
          else        await Api.post('/authority/role', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  return { render };
})();

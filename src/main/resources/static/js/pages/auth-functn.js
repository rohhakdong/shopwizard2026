/**
 * 권한 관리 (Functn) – 역할별 메뉴+기능URI 접근 권한
 * 좌측: 역할 선택 + 메뉴 트리 (지연 로드)
 * 우측: 선택된 메뉴의 기능URI 목록 (CRUD)
 */
const PageAuthFunctn = (() => {

  let roleList      = [];
  let currentRole   = null;   // { roleUid, roleName }
  let selectedMenu  = null;   // { menuCode, menuName }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;align-items:start;min-width:0">

        <!-- 왼쪽: 역할+메뉴 트리 -->
        <div class="card" style="min-width:0;overflow:hidden">
          <div class="card-header">메뉴 선택</div>
          <div class="card-body" style="padding:12px 16px">
            <div class="search-bar" style="margin-bottom:12px">
              <div class="form-group" style="flex:1">
                <select class="input" id="functnRoleUid">
                  <option value="">-- 역할 선택 --</option>
                </select>
              </div>
              <button class="btn btn-ghost" id="btnLoadFunctnTree">불러오기</button>
            </div>
            <div id="functnMenuTree">
              <div style="text-align:center;padding:30px;color:var(--text-muted)">역할을 선택하세요</div>
            </div>
          </div>
        </div>

        <!-- 오른쪽: 기능 목록 -->
        <div class="card" style="min-width:0;overflow:hidden">
          <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
            <span>기능 권한 <span id="functnMenuTitle" style="font-weight:400;color:var(--text-muted);font-size:13px"></span></span>
            <button class="btn btn-primary" id="btnAddFunctn" disabled>+ 등록</button>
          </div>
          <div class="card-body" style="padding:12px 16px">
            <div id="functnListWrap">
              <div style="text-align:center;padding:30px;color:var(--text-muted)">메뉴를 선택하세요</div>
            </div>
          </div>
        </div>

      </div>`;

    document.getElementById('btnLoadFunctnTree').addEventListener('click', loadTree);
    document.getElementById('functnRoleUid').addEventListener('change', () => {
      document.getElementById('functnMenuTree').innerHTML =
        '<div style="text-align:center;padding:30px;color:var(--text-muted)">불러오기를 클릭하세요</div>';
      resetRight();
      currentRole = null;
    });
    document.getElementById('btnAddFunctn').addEventListener('click', () => openModal(null));

    loadRoles();
  }

  function resetRight() {
    selectedMenu = null;
    document.getElementById('functnMenuTitle').textContent = '';
    document.getElementById('btnAddFunctn').disabled = true;
    document.getElementById('functnListWrap').innerHTML =
      '<div style="text-align:center;padding:30px;color:var(--text-muted)">메뉴를 선택하세요</div>';
  }

  // ── 역할 목록 ──────────────────────────────────────────────────────
  async function loadRoles() {
    try {
      roleList = await Api.get('/authority/role/list', {});
      const sel = document.getElementById('functnRoleUid');
      roleList.forEach(r => {
        const opt = document.createElement('option');
        opt.value = r.roleUid;
        opt.textContent = r.roleName;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 메뉴 트리 로드 ─────────────────────────────────────────────────
  async function loadTree() {
    const roleUid = document.getElementById('functnRoleUid').value;
    if (!roleUid) { UI.toast('역할을 선택하세요', 'error'); return; }

    currentRole = roleList.find(r => r.roleUid === roleUid);
    resetRight();

    const treeEl = document.getElementById('functnMenuTree');
    treeEl.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/menu/list', { pRoleUid: roleUid });
      renderMenuTree(treeEl, list, null, 0);
    } catch (e) {
      treeEl.innerHTML = `<div style="color:var(--danger);padding:12px">${e.message}</div>`;
    }
  }

  // ── 메뉴 트리 렌더 ─────────────────────────────────────────────────
  function renderMenuTree(container, list, parentCode, level) {
    if (!list || list.length === 0) {
      container.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">메뉴가 없습니다</div>';
      return;
    }

    if (level === 0) {
      const wrap = document.createElement('div');
      wrap.className = 'table-wrap';
      wrap.innerHTML = `
        <table id="functnMenuTable">
          <thead><tr><th>메뉴명</th><th style="width:40px">순서</th></tr></thead>
          <tbody id="functnMenuTbody"></tbody>
        </table>`;
      container.innerHTML = '';
      container.appendChild(wrap);
    }

    appendMenuRows(document.getElementById('functnMenuTbody'), list, level);
  }

  function appendMenuRows(tbody, list, level) {
    list.forEach(item => {
      const tr = document.createElement('tr');
      tr.dataset.menuCode = item.menuCode;
      tr.style.cursor = 'pointer';

      const indent = level * 16;
      const hasChild = item.childCount > 0;

      tr.innerHTML = `
        <td>
          <span style="display:inline-block;width:${indent}px"></span>
          <button class="tree-toggle" data-expanded="false" data-code="${item.menuCode}"
            style="background:none;border:none;cursor:pointer;width:16px;font-size:10px;color:var(--text-muted);padding:0;margin-right:2px">
            ${hasChild ? '▶' : '&nbsp;'}
          </button>
          <span class="menu-label" style="font-size:${level === 0 ? '13px' : '12px'};font-weight:${level === 0 ? '600' : '400'}">
            ${item.menuName || ''}
          </span>
        </td>
        <td style="text-align:center;font-size:12px;color:var(--text-muted)">${item.menuSeq ?? ''}</td>`;

      tbody.appendChild(tr);

      // 메뉴 행 클릭 → 우측 기능 목록 로드
      tr.querySelector('.menu-label').addEventListener('click', () => {
        tbody.querySelectorAll('tr').forEach(r => r.style.background = '');
        tr.style.background = '#eff6ff';
        selectedMenu = { menuCode: item.menuCode, menuName: item.menuName };
        document.getElementById('functnMenuTitle').textContent = `– ${item.menuName}`;
        document.getElementById('btnAddFunctn').disabled = false;
        loadFunctnList();
      });

      // 토글 클릭 → 자식 로드
      if (hasChild) {
        tr.querySelector('.tree-toggle').addEventListener('click', async function() {
          const expanded = this.dataset.expanded === 'true';
          if (expanded) {
            collapseChildren(tbody, item.menuCode);
            this.textContent = '▶';
            this.dataset.expanded = 'false';
          } else {
            this.textContent = '▼';
            this.dataset.expanded = 'true';
            try {
              const children = await Api.get('/authority/menu/list', {
                pRoleUid: currentRole.roleUid,
                pParentMenuCode: item.menuCode,
              });
              const tmp = document.createElement('tbody');
              appendMenuRows(tmp, children, level + 1);
              const next = tr.nextSibling;
              Array.from(tmp.children).forEach(row => tbody.insertBefore(row, next));
            } catch (e) { UI.toast(e.message, 'error'); }
          }
        });
      }
    });
  }

  function collapseChildren(tbody, parentCode) {
    Array.from(tbody.querySelectorAll('tr')).forEach(row => {
      const code = row.dataset.menuCode;
      if (code && code !== parentCode && code.startsWith(parentCode)) {
        const toggle = row.querySelector('.tree-toggle');
        if (toggle) { toggle.textContent = '▶'; toggle.dataset.expanded = 'false'; }
        row.remove();
      }
    });
  }

  // ── 기능 목록 조회 ─────────────────────────────────────────────────
  async function loadFunctnList() {
    const wrap = document.getElementById('functnListWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/functn/list', {
        pRoleUid:  currentRole.roleUid,
        pMenuCode: selectedMenu.menuCode,
      });
      renderFunctnTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:12px">${e.message}</div>`;
    }
  }

  function renderFunctnTable(list) {
    const wrap = document.getElementById('functnListWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">등록된 기능 권한이 없습니다</div>';
      return;
    }

    const rows = list.map(f => `
      <tr>
        <td style="max-width:0;width:60%">
          <code style="font-size:11px;display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"
            title="${f.functnUri || ''}">${f.functnUri || ''}</code>
        </td>
        <td>${f.functnDesc || ''}</td>
        <td style="text-align:center">
          <span class="badge ${f.state === 1 ? 'badge-green' : 'badge-gray'}">${f.state === 1 ? '사용' : '미사용'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-uri="${encodeURIComponent(f.functnUri)}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-uri="${encodeURIComponent(f.functnUri)}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <thead>
          <tr>
            <th>기능 URI</th><th style="width:30%">설명</th><th style="width:55px">상태</th><th style="width:90px"></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', () => {
        const uri = decodeURIComponent(btn.dataset.uri);
        const item = list.find(f => f.functnUri === uri);
        openModal(item);
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        const uri = decodeURIComponent(btn.dataset.uri);
        UI.confirm(`기능 URI [${uri}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/authority/functn', {
              roleUid:  currentRole.roleUid,
              menuCode: selectedMenu.menuCode,
              functnUri: uri,
            });
            UI.toast('삭제되었습니다', 'success');
            loadFunctnList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  function openModal(f) {
    const isEdit = !!f;
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full">
          <label>기능 URI *</label>
          <input class="input" id="mFunctnUri" value="${f?.functnUri || ''}"
            ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="/authority/mngr/list">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="mFunctnDesc" value="${f?.functnDesc || ''}" placeholder="기능 설명">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${!f || f.state === 1 ? 'selected' : ''}>사용</option>
            <option value="0" ${f?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${f?.remark || ''}">
        </div>
      </div>
      <div style="font-size:12px;color:var(--text-muted);margin-top:8px">
        역할: <strong>${currentRole?.roleName || ''}</strong>
        &nbsp;|&nbsp; 메뉴: <strong>${selectedMenu?.menuName || ''}</strong>
        <code style="font-size:11px;margin-left:4px">${selectedMenu?.menuCode || ''}</code>
      </div>`;

    UI.modal({
      title: isEdit ? '기능 권한 수정' : '기능 권한 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const functnUri = document.getElementById('mFunctnUri').value.trim();
        if (!functnUri) { UI.toast('기능 URI는 필수입니다', 'error'); return; }

        const payload = {
          roleUid:   currentRole.roleUid,
          menuCode:  selectedMenu.menuCode,
          functnUri,
          functnDesc: document.getElementById('mFunctnDesc').value.trim() || null,
          state:      parseInt(document.getElementById('mState').value),
          remark:     document.getElementById('mRemark').value.trim() || null,
          ...(isEdit
            ? { changeId: info?.loginId, changeName: info?.name }
            : { registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/authority/functn', payload);
          else        await Api.post('/authority/functn', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadFunctnList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  return { render };
})();

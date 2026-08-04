/**
 * 메뉴 관리 – 역할별 트리 구조 (최대 4레벨)
 *
 * MenuCode 규칙: Level0=4자, Level1=7자(부모4+3), Level2=10자, Level3=13자
 * selectList: pParentMenuCode 없으면 Level0, 있으면 해당 부모의 자식 반환
 */
const PageAuthMenu = (() => {

  let roleList    = [];
  let currentRole = null;   // { roleUid, roleName }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">메뉴 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 역할 선택 -->
          <div class="search-bar" style="margin-bottom:16px">
            <div class="form-group">
              <label>역할 선택</label>
              <select class="input" id="menuRoleUid" style="width:240px">
                <option value="">-- 역할을 선택하세요 --</option>
              </select>
            </div>
            <button class="btn btn-primary" id="btnLoadMenu" style="margin-top:18px">불러오기</button>
          </div>

          <!-- 트리 영역 -->
          <div id="menuTreeArea">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">역할을 선택하면 메뉴 트리가 표시됩니다.</div>
          </div>

        </div>
      </div>`;

    document.getElementById('btnLoadMenu').addEventListener('click', loadTree);
    document.getElementById('menuRoleUid').addEventListener('change', () => {
      document.getElementById('menuTreeArea').innerHTML =
        '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오기를 클릭하세요.</div>';
      currentRole = null;
    });

    loadRoles();
  }

  // ── 역할 목록 ──────────────────────────────────────────────────────
  async function loadRoles() {
    try {
      roleList = await Api.get('/authority/role/list', {});
      const sel = document.getElementById('menuRoleUid');
      roleList.forEach(r => {
        const opt = document.createElement('option');
        opt.value = r.roleUid;
        opt.textContent = r.roleName;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 트리 최상위 로드 ───────────────────────────────────────────────
  async function loadTree() {
    const roleUid = document.getElementById('menuRoleUid').value;
    if (!roleUid) { UI.toast('역할을 선택하세요', 'error'); return; }

    const role = roleList.find(r => r.roleUid === roleUid);
    currentRole = role;

    const area = document.getElementById('menuTreeArea');
    area.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/menu/list', { pRoleUid: roleUid });
      renderTree(area, list, null, 0);
    } catch (e) {
      area.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 트리 렌더링 ────────────────────────────────────────────────────
  function renderTree(container, list, parentMenuCode, level) {
    if (!list || list.length === 0) {
      if (level === 0) {
        container.innerHTML = `
          <div style="text-align:center;padding:40px;color:var(--text-muted)">등록된 메뉴가 없습니다.</div>
          <div style="text-align:center">
            <button class="btn btn-primary" id="btnAddRoot">+ 최상위 메뉴 등록</button>
          </div>`;
        document.getElementById('btnAddRoot').addEventListener('click', () =>
          openModal(null, null, 0)
        );
      }
      return;
    }

    // 테이블 생성 (최초 level0일 때만 헤더 포함)
    if (level === 0) {
      const wrap = document.createElement('div');
      wrap.className = 'table-wrap';
      const addRootBtn = `<div style="text-align:right;margin-bottom:8px">
        <button class="btn btn-primary btn-add-root">+ 최상위 메뉴 등록</button>
      </div>`;
      wrap.innerHTML = addRootBtn + `
        <table id="menuTable">
          <thead>
            <tr>
              <th>메뉴명</th>
              <th style="width:220px">URI</th>
              <th style="width:40px">순서</th>
              <th style="width:50px">상태</th>
              <th style="width:130px"></th>
            </tr>
          </thead>
          <tbody id="menuTbody"></tbody>
        </table>`;
      container.innerHTML = '';
      container.appendChild(wrap);

      wrap.querySelector('.btn-add-root').addEventListener('click', () =>
        openModal(null, null, 0)
      );

      appendRows(document.getElementById('menuTbody'), list, parentMenuCode, level);
    } else {
      appendRows(container, list, parentMenuCode, level);
    }
  }

  function appendRows(tbody, list, parentMenuCode, level) {
    list.forEach(item => {
      const tr = document.createElement('tr');
      tr.dataset.menuCode = item.menuCode;
      tr.dataset.level = level;

      const indent = level * 20;
      const hasChild = item.childCount > 0;
      const toggleId = `toggle_${item.menuCode.replace(/[^a-zA-Z0-9]/g, '_')}`;

      tr.innerHTML = `
        <td>
          <span style="display:inline-block;width:${indent}px"></span>
          <button class="tree-toggle" id="${toggleId}" data-expanded="false" data-code="${item.menuCode}"
            style="background:none;border:none;cursor:pointer;width:18px;font-size:11px;color:var(--text-muted);padding:0;margin-right:2px">
            ${hasChild ? '▶' : '&nbsp;'}
          </button>
          <span style="font-size:${level === 0 ? '14px' : '13px'};font-weight:${level === 0 ? '600' : '400'}">
            ${item.menuName || ''}
          </span>
          <code style="font-size:10px;color:var(--text-muted);margin-left:6px">${item.menuCode}</code>
        </td>
        <td style="font-size:12px;color:var(--text-muted)">${item.menuUri || ''}</td>
        <td style="text-align:center;font-size:12px">${item.menuSeq ?? ''}</td>
        <td style="text-align:center">
          <span class="badge ${item.state === 1 ? 'badge-green' : 'badge-gray'}" style="font-size:11px">
            ${item.state === 1 ? '사용' : '미사용'}
          </span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="add-child" data-code="${item.menuCode}" data-level="${level}">+ 하위</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${item.menuCode}" data-level="${level}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${item.menuCode}" data-name="${item.menuName}">삭제</button>
        </td>`;

      tbody.appendChild(tr);

      // 토글 이벤트
      if (hasChild) {
        tr.querySelector('.tree-toggle').addEventListener('click', async function() {
          const expanded = this.dataset.expanded === 'true';
          if (expanded) {
            // 접기: 해당 menuCode의 자식 행 모두 제거
            collapseChildren(tbody, item.menuCode);
            this.textContent = '▶';
            this.dataset.expanded = 'false';
          } else {
            // 펼치기: 자식 로드
            this.textContent = '▼';
            this.dataset.expanded = 'true';
            try {
              const children = await Api.get('/authority/menu/list', {
                pRoleUid: currentRole.roleUid,
                pParentMenuCode: item.menuCode,
              });
              // tr 다음에 자식 행들 삽입
              const childTbody = document.createElement('tbody');
              childTbody.dataset.parentCode = item.menuCode;
              appendRows(childTbody, children, item.menuCode, level + 1);
              // childTbody의 tr들을 tr 뒤에 삽입
              const nextSibling = tr.nextSibling;
              Array.from(childTbody.children).forEach(row => {
                tbody.insertBefore(row, nextSibling);
              });
            } catch (e) { UI.toast(e.message, 'error'); }
          }
        });
      }

      // 하위 등록
      tr.querySelector('[data-action=add-child]').addEventListener('click', () => {
        if (level >= 3) { UI.toast('최대 4레벨까지만 등록 가능합니다', 'error'); return; }
        openModal(null, item.menuCode, level + 1);
      });

      // 수정
      tr.querySelector('[data-action=edit]').addEventListener('click', () => openModal(item, item.parentMenuCode, level));

      // 삭제
      tr.querySelector('[data-action=del]').addEventListener('click', () => {
        UI.confirm(`메뉴 [${item.menuName}]를 삭제하시겠습니까?${item.childCount > 0 ? '\n하위 메뉴가 있으면 삭제되지 않을 수 있습니다.' : ''}`, async close => {
          try {
            await Api.delete('/authority/menu', { roleUid: currentRole.roleUid, menuCode: item.menuCode });
            UI.toast('삭제되었습니다', 'success');
            loadTree();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // 특정 부모의 자식 행 모두 제거 (재귀)
  function collapseChildren(tbody, parentCode) {
    const rows = Array.from(tbody.querySelectorAll('tr'));
    rows.forEach(row => {
      // menuCode가 parentCode로 시작하는 자식 행 제거 (단, parentCode 자신은 제외)
      const code = row.dataset.menuCode;
      if (code && code !== parentCode && code.startsWith(parentCode)) {
        // 자식의 토글도 초기화
        const toggle = row.querySelector('.tree-toggle');
        if (toggle) { toggle.textContent = '▶'; toggle.dataset.expanded = 'false'; }
        row.remove();
      }
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  async function openModal(item, parentMenuCode, level) {
    const isEdit = !!item;

    // 다음 menuCode 자동 계산
    let nextCode = '';
    if (!isEdit) {
      try {
        const params = { roleUid: currentRole.roleUid };
        if (parentMenuCode) params.parentMenuCode = parentMenuCode;
        const maxCode = await Api.get('/authority/menu/child-max', params);
        nextCode = calcNextCode(maxCode, parentMenuCode, level);
      } catch (_) {
        nextCode = parentMenuCode ? parentMenuCode + '001' : 'A001';
      }
    }

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>메뉴 코드 ${isEdit ? '' : '*'}</label>
          <input class="input" id="mMenuCode" value="${isEdit ? item.menuCode : nextCode}"
            ${isEdit ? 'readonly style="background:#f8fafc"' : ''}>
        </div>
        <div class="form-group">
          <label>순서</label>
          <input class="input" type="number" id="mMenuSeq" value="${isEdit ? (item.menuSeq ?? '') : ''}" placeholder="0">
        </div>
        <div class="form-group full">
          <label>메뉴명 *</label>
          <input class="input" id="mMenuName" value="${isEdit ? (item.menuName || '') : ''}" placeholder="메뉴명">
        </div>
        <div class="form-group full">
          <label>URI</label>
          <input class="input" id="mMenuUri" value="${isEdit ? (item.menuUri || '') : ''}" placeholder="/example/path">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${!item || item.state === 1 ? 'selected' : ''}>사용</option>
            <option value="0" ${item?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${isEdit ? (item.remark || '') : ''}">
        </div>
      </div>
      <div style="font-size:12px;color:var(--text-muted);margin-top:8px">
        역할: <strong>${currentRole?.roleName || ''}</strong>
        ${parentMenuCode ? ` &nbsp;|&nbsp; 상위 메뉴 코드: <code>${parentMenuCode}</code>` : ' &nbsp;|&nbsp; 최상위 메뉴'}
        &nbsp;|&nbsp; 레벨: ${level}
      </div>`;

    UI.modal({
      title: isEdit ? '메뉴 수정' : '메뉴 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const menuCode = document.getElementById('mMenuCode').value.trim();
        const menuName = document.getElementById('mMenuName').value.trim();
        if (!menuCode || !menuName) { UI.toast('메뉴 코드와 메뉴명은 필수입니다', 'error'); return; }

        const payload = {
          menuCode,
          menuName,
          menuUri:        document.getElementById('mMenuUri').value.trim() || null,
          menuSeq:        parseInt(document.getElementById('mMenuSeq').value) || 0,
          state:          parseInt(document.getElementById('mState').value),
          remark:         document.getElementById('mRemark').value.trim() || null,
          roleUid:        currentRole.roleUid,
          parentMenuCode: parentMenuCode || null,
          menuLevel:      level,
          ...(isEdit
            ? { changeId: info?.loginId, changeName: info?.name }
            : { registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/authority/menu', payload);
          else        await Api.post('/authority/menu', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadTree();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  // ── 다음 MenuCode 계산 ─────────────────────────────────────────────
  function calcNextCode(maxCode, parentCode, level) {
    const prefix = parentCode || '';
    if (!maxCode) {
      // 첫 번째 항목
      return level === 0 ? 'A001' : prefix + '001';
    }
    // 마지막 3자리(Level0는 마지막 3자리 + 앞 1자리 알파) 증가
    const suffixLen = level === 0 ? maxCode.length : maxCode.length - prefix.length;
    const suffix = maxCode.slice(maxCode.length - suffixLen);
    // 숫자 파트만 증가
    const numPart = suffix.replace(/\D/g, '');
    const alphaPart = suffix.replace(/\d/g, '');
    if (numPart) {
      const next = (parseInt(numPart) + 1).toString().padStart(numPart.length, '0');
      return prefix + alphaPart + next;
    }
    return prefix + suffix + '1';
  }

  return { render };
})();

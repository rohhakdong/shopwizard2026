/**
 * 코드 관리 – 코드그룹(Constr) + 코드값(ConstrVal) 마스터-디테일
 */
const PageCodeConstr = (() => {

  let selectedConstrCode = null;

  // ── 렌더 진입점 ────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;align-items:start;min-width:0">

        <!-- 왼쪽: 코드그룹 -->
        <div class="card" style="min-width:0">
          <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
            <span>코드그룹</span>
            <button class="btn btn-primary" id="btnAddConstr">+ 등록</button>
          </div>
          <div class="card-body" style="padding:12px 16px">
            <div class="search-bar" style="margin-bottom:12px">
              <div class="form-group" style="flex:1">
                <input class="input" type="text" id="searchConstrName" placeholder="코드그룹명 검색">
              </div>
              <button class="btn btn-ghost" id="btnSearchConstr">검색</button>
            </div>
            <div class="table-wrap" id="constrTableWrap">
              <div style="text-align:center;padding:30px;color:var(--text-muted)">불러오는 중...</div>
            </div>
          </div>
        </div>

        <!-- 오른쪽: 코드값 -->
        <div class="card" style="min-width:0">
          <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
            <span>코드값 <span id="constrValTitle" style="font-weight:400;color:var(--text-muted);font-size:13px"></span></span>
            <button class="btn btn-primary" id="btnAddConstrVal" disabled>+ 등록</button>
          </div>
          <div class="card-body" style="padding:12px 16px">
            <div class="table-wrap" id="constrValTableWrap">
              <div style="text-align:center;padding:30px;color:var(--text-muted)">코드그룹을 선택하세요</div>
            </div>
          </div>
        </div>

      </div>`;

    document.getElementById('btnSearchConstr').addEventListener('click', loadConstrList);
    document.getElementById('searchConstrName').addEventListener('keydown', e => { if (e.key === 'Enter') loadConstrList(); });
    document.getElementById('btnAddConstr').addEventListener('click', () => openConstrModal(null));
    document.getElementById('btnAddConstrVal').addEventListener('click', () => openConstrValModal(null));

    loadConstrList();
  }

  // ── 코드그룹 목록 ──────────────────────────────────────────────────
  async function loadConstrList() {
    const keyword = document.getElementById('searchConstrName').value.trim();
    const wrap = document.getElementById('constrTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/code/constr', { pConstrName: keyword });
      renderConstrTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function renderConstrTable(list) {
    const wrap = document.getElementById('constrTableWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">데이터가 없습니다</div>';
      return;
    }

    const rows = list.map(r => `
      <tr data-code="${r.constrCode}" class="constr-row" style="cursor:pointer">
        <td><code style="font-size:12px">${r.constrCode}</code></td>
        <td>${r.constrName || ''}</td>
        <td>${r.constrDesc || ''}</td>
        <td>
          <span class="badge ${r.state === 'Y' ? 'badge-green' : 'badge-gray'}">${r.state === 'Y' ? '사용' : '미사용'}</span>
        </td>
        <td>
          <button class="btn btn-ghost" style="padding:3px 8px;font-size:12px" data-action="edit" data-code="${r.constrCode}">수정</button>
          <button class="btn btn-danger" style="padding:3px 8px;font-size:12px" data-action="del" data-code="${r.constrCode}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead><tr><th>코드</th><th>코드그룹명</th><th>설명</th><th>상태</th><th style="width:100px"></th></tr></thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('.constr-row').forEach(tr => {
      tr.addEventListener('click', e => {
        if (e.target.closest('button')) return;
        wrap.querySelectorAll('.constr-row').forEach(r => r.style.background = '');
        tr.style.background = '#eff6ff';
        selectedConstrCode = tr.dataset.code;
        document.getElementById('btnAddConstrVal').disabled = false;
        document.getElementById('constrValTitle').textContent = `– ${tr.dataset.code}`;
        loadConstrValList(selectedConstrCode);
      });
    });

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const code = btn.dataset.code;
        const item = list.find(r => r.constrCode === code);
        openConstrModal(item);
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const code = btn.dataset.code;
        UI.confirm(`코드그룹 [${code}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/code/constr/${code}`);
            UI.toast('삭제되었습니다', 'success');
            if (selectedConstrCode === code) {
              selectedConstrCode = null;
              document.getElementById('btnAddConstrVal').disabled = true;
              document.getElementById('constrValTitle').textContent = '';
              document.getElementById('constrValTableWrap').innerHTML =
                '<div style="text-align:center;padding:30px;color:var(--text-muted)">코드그룹을 선택하세요</div>';
            }
            loadConstrList();
          } catch (err) { UI.toast(err.message, 'error'); }
          close();
        });
      });
    });

    // 이전 선택 유지
    if (selectedConstrCode) {
      const row = wrap.querySelector(`[data-code="${selectedConstrCode}"]`);
      if (row) row.style.background = '#eff6ff';
    }
  }

  // ── 코드그룹 등록/수정 모달 ────────────────────────────────────────
  function openConstrModal(item) {
    const isEdit = !!item;
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>코드그룹 코드 *</label>
          <input class="input" id="mConstrCode" value="${item?.constrCode || ''}" ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="예) PROD001">
        </div>
        <div class="form-group">
          <label>코드그룹명 *</label>
          <input class="input" id="mConstrName" value="${item?.constrName || ''}" placeholder="코드그룹명">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="mConstrDesc" value="${item?.constrDesc || ''}" placeholder="설명">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="Y" ${item?.state === 'Y' || !item ? 'selected' : ''}>사용</option>
            <option value="N" ${item?.state === 'N' ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group">
          <label>비고</label>
          <input class="input" id="mRemark" value="${item?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '코드그룹 수정' : '코드그룹 등록',
      body,
      confirmText: isEdit ? '수정' : '등록',
      onConfirm: async close => {
        const code = document.getElementById('mConstrCode').value.trim();
        const name = document.getElementById('mConstrName').value.trim();
        if (!code || !name) { UI.toast('코드와 코드그룹명은 필수입니다', 'error'); return; }

        const payload = {
          constrCode: code,
          constrName: name,
          constrDesc: document.getElementById('mConstrDesc').value.trim(),
          state:      document.getElementById('mState').value,
          remark:     document.getElementById('mRemark').value.trim(),
        };

        try {
          if (isEdit) await Api.put(`/code/constr/${code}`, payload);
          else        await Api.post('/code/constr', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadConstrList();
          close();
        } catch (err) { UI.toast(err.message, 'error'); }
      }
    });
  }

  // ── 코드값 목록 ────────────────────────────────────────────────────
  async function loadConstrValList(constrCode) {
    const wrap = document.getElementById('constrValTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/code/constr-val', { pConstrCode: constrCode, sidx: 'ConstrValSeq', sord: 'ASC' });
      renderConstrValTable(list, constrCode);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function renderConstrValTable(list, constrCode) {
    const wrap = document.getElementById('constrValTableWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:30px;color:var(--text-muted)">코드값이 없습니다</div>';
      return;
    }

    const rows = list.map(r => `
      <tr>
        <td style="text-align:center">${r.constrValSeq ?? ''}</td>
        <td><code style="font-size:12px">${r.constrVal}</code></td>
        <td>${r.constrValDesc || ''}</td>
        <td style="text-align:center">
          <span class="badge ${r.defaltYn === 'Y' ? 'badge-blue' : 'badge-gray'}">${r.defaltYn === 'Y' ? '기본' : '-'}</span>
        </td>
        <td>
          <span class="badge ${r.state === 'Y' ? 'badge-green' : 'badge-gray'}">${r.state === 'Y' ? '사용' : '미사용'}</span>
        </td>
        <td>
          <button class="btn btn-ghost" style="padding:3px 8px;font-size:12px" data-action="edit"
            data-code="${r.constrCode}" data-val="${r.constrVal}">수정</button>
          <button class="btn btn-danger" style="padding:3px 8px;font-size:12px" data-action="del"
            data-code="${r.constrCode}" data-val="${r.constrVal}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead><tr><th style="width:40px">순서</th><th>코드값</th><th>설명</th><th style="width:50px">기본</th><th style="width:60px">상태</th><th style="width:100px"></th></tr></thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', () => {
        const item = list.find(r => r.constrCode === btn.dataset.code && r.constrVal === btn.dataset.val);
        openConstrValModal(item);
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`코드값 [${btn.dataset.val}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/code/constr-val', { constrCode: btn.dataset.code, constrVal: btn.dataset.val });
            UI.toast('삭제되었습니다', 'success');
            loadConstrValList(constrCode);
          } catch (err) { UI.toast(err.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 코드값 등록/수정 모달 ──────────────────────────────────────────
  function openConstrValModal(item) {
    const isEdit = !!item;
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>코드값 *</label>
          <input class="input" id="mConstrVal" value="${item?.constrVal || ''}" ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="예) 01">
        </div>
        <div class="form-group">
          <label>순서</label>
          <input class="input" type="number" id="mConstrValSeq" value="${item?.constrValSeq ?? ''}">
        </div>
        <div class="form-group full">
          <label>설명 *</label>
          <input class="input" id="mConstrValDesc" value="${item?.constrValDesc || ''}" placeholder="코드값 설명">
        </div>
        <div class="form-group">
          <label>기본값</label>
          <select class="input" id="mDefaltYn">
            <option value="N" ${item?.defaltYn !== 'Y' ? 'selected' : ''}>아니오</option>
            <option value="Y" ${item?.defaltYn === 'Y' ? 'selected' : ''}>예</option>
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mValState">
            <option value="Y" ${item?.state === 'Y' || !item ? 'selected' : ''}>사용</option>
            <option value="N" ${item?.state === 'N' ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mValRemark" value="${item?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '코드값 수정' : '코드값 등록',
      body,
      confirmText: isEdit ? '수정' : '등록',
      onConfirm: async close => {
        const val  = document.getElementById('mConstrVal').value.trim();
        const desc = document.getElementById('mConstrValDesc').value.trim();
        if (!val || !desc) { UI.toast('코드값과 설명은 필수입니다', 'error'); return; }

        const payload = {
          constrCode:    selectedConstrCode,
          constrVal:     val,
          constrValDesc: desc,
          constrValSeq:  parseInt(document.getElementById('mConstrValSeq').value) || null,
          defaltYn:      document.getElementById('mDefaltYn').value,
          state:         document.getElementById('mValState').value,
          remark:        document.getElementById('mValRemark').value.trim(),
        };

        try {
          if (isEdit) await Api.put('/code/constr-val', payload);
          else        await Api.post('/code/constr-val', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadConstrValList(selectedConstrCode);
          close();
        } catch (err) { UI.toast(err.message, 'error'); }
      }
    });
  }

  return { render };
})();

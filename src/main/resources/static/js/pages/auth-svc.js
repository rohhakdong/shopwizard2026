/**
 * 서비스 관리
 */
const PageAuthSvc = (() => {

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>서비스 관리</span>
          <button class="btn btn-primary" id="btnAddSvc">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar">
            <div class="form-group">
              <label>서비스 코드</label>
              <input class="input" id="sSvcCode" placeholder="서비스 코드" style="width:160px">
            </div>
            <div class="form-group">
              <label>서비스명</label>
              <input class="input" id="sSvcName" placeholder="서비스명" style="width:160px">
            </div>
            <button class="btn btn-ghost" id="btnSearch" style="margin-top:18px">검색</button>
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="svcTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>
          </div>

        </div>
      </div>`;

    document.getElementById('btnSearch').addEventListener('click', loadList);
    document.getElementById('sSvcCode').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('sSvcName').addEventListener('keydown', e => { if (e.key === 'Enter') loadList(); });
    document.getElementById('btnAddSvc').addEventListener('click', () => openModal(null));

    loadList();
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('svcTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/authority/svc/list', {
        pSvcCode: document.getElementById('sSvcCode').value.trim(),
        pSvcName: document.getElementById('sSvcName').value.trim(),
      });
      renderTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('svcTableWrap');
    if (!list || list.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
      return;
    }

    const rows = list.map(s => `
      <tr>
        <td><code style="font-size:12px">${s.svcCode || ''}</code></td>
        <td>${s.svcName || ''}</td>
        <td>${s.remark || ''}</td>
        <td style="text-align:center">
          <span class="badge ${s.state === 1 ? 'badge-green' : 'badge-gray'}">${s.state === 1 ? '사용' : '미사용'}</span>
        </td>
        <td>${s.registDate ? s.registDate.substring(0, 10) : ''}</td>
        <td>
          <button class="btn btn-ghost" style="padding:3px 8px;font-size:12px"
            data-action="edit" data-code="${s.svcCode}">수정</button>
          <button class="btn btn-danger" style="padding:3px 8px;font-size:12px"
            data-action="del" data-code="${s.svcCode}" data-name="${s.svcName}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th style="width:120px">서비스 코드</th><th>서비스명</th><th>비고</th>
            <th style="width:60px">상태</th><th style="width:90px">등록일</th>
            <th style="width:110px"></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', () => {
        const item = list.find(s => s.svcCode === btn.dataset.code);
        openModal(item);
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`서비스 [${btn.dataset.name}]를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/authority/svc', { svcCode: btn.dataset.code });
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 등록/수정 모달 ────────────────────────────────────────────────
  function openModal(s) {
    const isEdit = !!s;
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>서비스 코드 *</label>
          <input class="input" id="mSvcCode" value="${s?.svcCode || ''}"
            ${isEdit ? 'readonly style="background:#f8fafc"' : ''} placeholder="예) ANYB">
        </div>
        <div class="form-group">
          <label>서비스명 *</label>
          <input class="input" id="mSvcName" value="${s?.svcName || ''}" placeholder="서비스명">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="mState">
            <option value="1" ${!s || s.state === 1 ? 'selected' : ''}>사용</option>
            <option value="0" ${s?.state === 0 ? 'selected' : ''}>미사용</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="mRemark" value="${s?.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isEdit ? '서비스 수정' : '서비스 등록',
      confirmText: isEdit ? '수정' : '등록',
      body,
      onConfirm: async close => {
        const svcCode = document.getElementById('mSvcCode').value.trim();
        const svcName = document.getElementById('mSvcName').value.trim();
        if (!svcCode || !svcName) { UI.toast('코드와 서비스명은 필수입니다', 'error'); return; }

        const payload = {
          svcCode,
          svcName,
          state:  parseInt(document.getElementById('mState').value),
          remark: document.getElementById('mRemark').value.trim(),
          ...(isEdit
            ? { changeId: info?.loginId, changeName: info?.name }
            : { registId: info?.loginId, registName: info?.name }),
        };

        try {
          if (isEdit) await Api.put('/authority/svc', payload);
          else        await Api.post('/authority/svc', payload);
          UI.toast(isEdit ? '수정되었습니다' : '등록되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      }
    });
  }

  return { render };
})();

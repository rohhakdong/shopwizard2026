/**
 * 카테고리 관리 (상품 분류 - 대/중/소/세분류 4단계 트리)
 * - tCatCate: SvcCode별로 독립된 분류 트리. CateCode는 'G' + 3자리씩 4단(대/중/소/세) = 13자
 *   고정폭 코드 (예: G006008002002). 레벨0(대분류)만 ParentCateCode가 없고, 그 아래로는
 *   상위 코드를 그대로 접두어로 물려받는다.
 * - 4단 컬럼(macOS Finder의 column view와 동일한 구조)으로 상위를 클릭하면 오른쪽에
 *   그 하위 목록이 로드되는 방식. 각 컬럼에서 신규/수정/삭제가 가능하다.
 */
const PageCatalogCate = (() => {

  const LEVEL_LABEL = ['대분류', '중분류', '소분류', '세분류'];
  let svcOptions = [];
  let currentSvcCode = '';
  // 각 레벨의 현재 목록과 선택된 코드. selected[0]이 선택되어야 levels[1]을 로드하는 식.
  let levels = [[], [], [], []];
  let selected = [null, null, null, null];

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">카테고리 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="gap:8px">
            <div class="form-group">
              <label>서비스</label>
              <select class="input" id="ctSvcCode" style="width:220px">
                <option value="">불러오는 중...</option>
              </select>
            </div>
          </div>

          <div style="overflow-x:auto;margin-top:8px">
            <div id="ctColumns" style="display:flex;gap:10px;height:520px;width:1070px"></div>
          </div>

        </div>
      </div>`;

    document.getElementById('ctSvcCode').addEventListener('change', e => {
      currentSvcCode = e.target.value;
      selected = [null, null, null, null];
      levels = [[], [], [], []];
      loadLevel(0);
    });

    loadSvcOptions();
  }

  async function loadSvcOptions() {
    try {
      svcOptions = await Api.get('/authority/svc/list', {});
    } catch (_) { svcOptions = []; }

    const sel = document.getElementById('ctSvcCode');
    if (!svcOptions.length) {
      sel.innerHTML = '<option value="">서비스가 없습니다</option>';
      return;
    }
    // 로그인한 관리자 자신의 서비스코드를 기본 선택 — 다른 거래처 화면들의 관례와 동일.
    const defaultSvc = (typeof info !== 'undefined' && info?.svcCode) || svcOptions[0].svcCode;
    sel.innerHTML = svcOptions.map(s =>
      `<option value="${s.svcCode}" ${s.svcCode === defaultSvc ? 'selected' : ''}>${s.svcName || s.svcCode} (${s.svcCode})</option>`
    ).join('');
    currentSvcCode = defaultSvc;
    loadLevel(0);
  }

  // ── 레벨별 목록 로드 ───────────────────────────────────────────────
  // level 0(대분류)은 부모 없이 CateLevel=0으로 조회, 그 아래는 parentCateCode로 조회.
  async function loadLevel(level) {
    if (!currentSvcCode) return;
    const parentCode = level === 0 ? null : selected[level - 1]?.cateCode;
    if (level > 0 && !parentCode) {
      levels[level] = [];
      renderColumns();
      return;
    }
    try {
      const params = { pSvcCode: currentSvcCode };
      if (level === 0) { /* selectList: pParentCateCode 없으면 CateLevel=0 */ }
      else params.pParentCateCode = parentCode;
      levels[level] = await Api.get('/catalog/cate/list', params);
    } catch (e) {
      levels[level] = [];
      UI.toast(e.message, 'error');
    }
    // 하위 레벨은 선택이 리셋되므로 목록도 비워준다.
    for (let l = level + 1; l <= 3; l++) { levels[l] = []; selected[l] = null; }
    renderColumns();
  }

  function selectItem(level, cate) {
    selected[level] = cate;
    for (let l = level + 1; l <= 3; l++) selected[l] = null;
    renderColumns();
    if (level < 3) loadLevel(level + 1);
  }

  // ── 컬럼 렌더 ─────────────────────────────────────────────────────
  function renderColumns() {
    const wrap = document.getElementById('ctColumns');
    wrap.innerHTML = LEVEL_LABEL.map((label, level) => {
      const disabled = level > 0 && !selected[level - 1];
      const items = levels[level] || [];

      const itemsHtml = disabled
        ? `<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">상위 분류를 먼저 선택하세요</div>`
        : (items.length === 0
            ? `<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">하위 분류가 없습니다</div>`
            : items.map(c => `
                <div class="cate-item" data-level="${level}" data-code="${c.cateCode}"
                     style="display:flex;align-items:center;justify-content:space-between;padding:8px 10px;
                            border-radius:6px;cursor:pointer;font-size:13px;
                            background:${selected[level]?.cateCode === c.cateCode ? '#eff6ff' : 'transparent'};
                            color:${c.state === 0 ? 'var(--text-muted)' : 'var(--text)'}">
                  <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;flex:1;min-width:0" title="${c.cateName || ''}">
                    ${c.cateName || '(이름 없음)'} ${c.state === 0 ? '<span class="badge badge-gray" style="margin-left:4px">중지</span>' : ''}
                    ${c.childCount > 0 ? `<span style="color:var(--text-muted);font-size:11px">(${c.childCount})</span>` : ''}
                  </span>
                  <span style="white-space:nowrap;flex-shrink:0;margin-left:6px">
                    <button class="btn btn-ghost" style="padding:1px 5px;font-size:12px" data-action="edit" data-level="${level}" data-code="${c.cateCode}" title="수정">✏️</button>
                    <button class="btn btn-ghost" style="padding:1px 5px;font-size:12px" data-action="del" data-level="${level}" data-code="${c.cateCode}" data-name="${(c.cateName||'').replace(/"/g,'&quot;')}" title="삭제">🗑️</button>
                  </span>
                </div>`).join(''));

      return `
        <div style="flex:0 0 260px;display:flex;flex-direction:column;border:1px solid var(--border);border-radius:8px;overflow:hidden">
          <div style="padding:8px 10px;background:#f8fafc;border-bottom:1px solid var(--border);display:flex;justify-content:space-between;align-items:center">
            <strong style="font-size:13px">${label}</strong>
            <button class="btn btn-ghost" style="padding:2px 8px;font-size:11px" data-action="new" data-level="${level}" ${disabled ? 'disabled' : ''}>+ 추가</button>
          </div>
          <div style="flex:1;overflow-y:auto;padding:4px">${itemsHtml}</div>
        </div>`;
    }).join('');

    // 항목 클릭 → 하위 레벨 로드 (버튼 클릭은 별도 리스너에서 stopPropagation)
    wrap.querySelectorAll('.cate-item').forEach(el => {
      el.addEventListener('click', () => {
        const level = parseInt(el.dataset.level);
        const cate = levels[level].find(c => c.cateCode === el.dataset.code);
        if (cate) selectItem(level, cate);
      });
    });

    wrap.querySelectorAll('[data-action=new]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const level = parseInt(btn.dataset.level);
        openEditModal(level, null);
      });
    });
    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const level = parseInt(btn.dataset.level);
        const cate = levels[level].find(c => c.cateCode === btn.dataset.code);
        openEditModal(level, cate);
      });
    });
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        const level = parseInt(btn.dataset.level);
        deleteCate(level, btn.dataset.code, btn.dataset.name);
      });
    });
  }

  // ── 삭제 ──────────────────────────────────────────────────────────
  async function deleteCate(level, cateCode, cateName) {
    try {
      const childCount = await Api.get('/catalog/cate/count-child', { svcCode: currentSvcCode, parentCateCode: cateCode });
      if (childCount > 0) {
        UI.toast(`[${cateName}] 아래에 하위 분류가 ${childCount}건 있어 삭제할 수 없습니다. 하위 분류를 먼저 삭제하세요.`, 'error');
        return;
      }
    } catch (e) { UI.toast(e.message, 'error'); return; }

    UI.confirm(`[${cateName}] 분류를 삭제하시겠습니까?`, async close => {
      try {
        await Api.delete('/catalog/cate', { svcCode: currentSvcCode, cateCode });
        UI.toast('삭제되었습니다', 'success');
        // 삭제한 레벨을 다시 불러온다 (부모가 있으면 그 부모 기준, 대분류면 전체 재조회).
        if (selected[level]?.cateCode === cateCode) selected[level] = null;
        loadLevel(level);
      } catch (e) { UI.toast(e.message, 'error'); }
      close();
    });
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  async function openEditModal(level, cate) {
    const isNew = !cate;
    const v = cate || {};
    const parentCateCode = level === 0 ? null : selected[level - 1].cateCode;

    // 코드 자동 채번: 'G' + 3자리씩 4단(13자). 레벨의 유효 자릿수만큼만 값이 있고 나머지는 0.
    // 예) 레벨0 다음값: G011000000000, 레벨2 다음값: G006010005000
    let newCateCode = '';
    if (isNew) {
      try {
        const maxCode = await Api.get('/catalog/cate/code-max', { svcCode: currentSvcCode, parentCateCode: parentCateCode || '' });
        const validLen = 4 + level * 3; // 레벨0=4, 레벨1=7, 레벨2=10, 레벨3=13
        const prefix = level === 0 ? 'G' : parentCateCode.substring(0, validLen - 3);
        const lastSeg = maxCode ? maxCode.substring(validLen - 3, validLen) : '000';
        const nextSeg = String(parseInt(lastSeg, 10) + 1).padStart(3, '0');
        newCateCode = (prefix + nextSeg).padEnd(13, '0');
      } catch (_) {
        UI.toast('분류코드를 계산하지 못했습니다', 'error');
        return;
      }
    }

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full">
          <label>분류코드</label>
          <input class="input" value="${isNew ? newCateCode : v.cateCode}" readonly style="background:#f8fafc;font-family:monospace">
        </div>
        <div class="form-group full">
          <label>분류명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="ctFCateName" value="${v.cateName || ''}">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="ctFCateDesc" value="${v.cateDesc || ''}">
        </div>
        <div class="form-group">
          <label>정렬순서</label>
          <input class="input" id="ctFCateSeq" type="number" value="${v.cateSeq ?? 999}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="ctFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="ctFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    UI.modal({
      title: isNew ? `${LEVEL_LABEL[level]} 신규 등록` : `${LEVEL_LABEL[level]} 수정 – ${v.cateName}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const cateName = document.getElementById('ctFCateName').value.trim();
        if (!cateName) { UI.toast('분류명을 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          svcCode:        currentSvcCode,
          cateCode:       isNew ? newCateCode : v.cateCode,
          cateName,
          cateDesc:       document.getElementById('ctFCateDesc').value.trim(),
          parentCateCode: isNew ? (parentCateCode || '') : v.parentCateCode,
          cateLevel:      level,
          cateSeq:        parseInt(document.getElementById('ctFCateSeq').value) || 0,
          state:          parseInt(document.getElementById('ctFState').value),
          remark:         document.getElementById('ctFRemark').value.trim(),
          registId, registName,
          changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/cate', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/cate', payload);
            UI.toast('저장되었습니다', 'success');
          }
          loadLevel(level);
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

/**
 * 유지보수 (사이드 메뉴 운영) — 레거시 유지보수(wOprMantncList)를 단순화: 요청 → 작업 예정일 입력 → 작업완료 세 단계만.
 *   · 요청: 상점 계정을 뺀 누구나(범위 제한 계정은 자기 요청만 보고 고치고 지운다).
 *   · 작업 예정일 입력·작업완료 처리, 전체 요청 조회: 상점·채널·서비스 제한이 없는 계정만(서버가 검사, 화면은 버튼만 숨긴다).
 *   · 작업완료된 요청은 이력이라 수정·삭제·예정일 변경이 안 된다. 예정일이 지났는데 미완료면 "지연" 으로 표시.
 *   백엔드: /operate/mantnc (list, summary, GET {no}, POST, PUT, PUT schedule, PUT complete, DELETE {no})
 */
const PageOperateMantnc = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  const STATE = { recipt: ['요청', '#b45309', '#fef3c7'], exec: ['작업중', '#1d4ed8', '#dbeafe'], cmplet: ['작업완료', '#15803d', '#dcfce7'] };
  let currentPage = 1;
  let seq = 0;
  let me = { canHandle: false, loginId: '' };

  function el(id) { return document.getElementById(id); }
  function fmt(s) { return s ? String(s).replace('T', ' ').slice(0, 16) : ''; }
  function today() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
  function badge(state, overdue) {
    const b = STATE[state] || ['-', '#6b7280', '#f3f4f6'];
    const chip = (t, c, bg) => `<span style="font-size:11px;padding:2px 8px;border-radius:10px;color:${c};background:${bg}">${t}</span>`;
    return chip(b[0], b[1], b[2]) + (overdue ? ' ' + chip('지연', '#b91c1c', '#fee2e2') : '');
  }

  function render(container) {
    currentPage = 1;
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>유지보수 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 요청 → 작업 예정일 → 작업완료</span></span>
          <button class="btn btn-primary" id="mtBtnNew" type="button">+ 유지보수 요청</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div id="mtChips" style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px"></div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상태</label>
              <select class="input" id="mtStatus" style="width:110px"><option value="">전체</option><option value="recipt">요청</option><option value="exec">작업중</option><option value="cmplet">작업완료</option><option value="overdue">지연</option></select></div>
            <div class="form-group"><label>제목·내용·대상 화면</label><input class="input" id="mtKeyword" style="width:180px"></div>
            <div class="form-group"><label>요청자</label><input class="input" id="mtRequester" style="width:100px"></div>
            <div class="form-group"><label>요청일</label><span><input class="input" id="mtStart" type="date" style="width:135px"> ~ <input class="input" id="mtEnd" type="date" style="width:135px"></span></div>
            <button class="btn btn-primary" id="mtBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="mtTotal">0</strong>건</div>
          <div class="table-wrap" id="mtWrap"></div>
          <div id="mtPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    el('mtBtnNew').addEventListener('click', () => openRequestForm(null));
    el('mtBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    el('mtStatus').addEventListener('change', () => { currentPage = 1; loadList(); });
    ['mtKeyword', 'mtRequester'].forEach(i => el(i).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadSummary();
    loadList();
  }

  async function loadSummary() {
    try {
      const s = await Api.get('/operate/mantnc/summary', {});
      me = { canHandle: !!s.canHandle, loginId: s.loginId || '' };
      const chip = (label, n, status, color) => `<button type="button" data-st="${status}" style="cursor:pointer;border:1px solid var(--border);background:var(--bg, #fff);border-radius:var(--radius);padding:6px 14px;font-size:13px">${label} <b style="color:${color}">${Number(n).toLocaleString()}</b></button>`;
      el('mtChips').innerHTML = chip('요청', s.recipt, 'recipt', '#b45309') + chip('작업중', s.exec, 'exec', '#1d4ed8') + chip('지연', s.overdue, 'overdue', '#b91c1c') + chip('작업완료', s.cmplet, 'cmplet', '#15803d')
        + (me.canHandle ? '' : '<span style="font-size:12px;color:var(--text-muted);align-self:center">내가 올린 요청만 표시됩니다</span>');
      el('mtChips').querySelectorAll('[data-st]').forEach(b => b.addEventListener('click', () => { el('mtStatus').value = b.dataset.st; currentPage = 1; loadList(); }));
    } catch (e) { el('mtChips').innerHTML = `<span style="color:var(--danger);font-size:12px">${esc(e.message)}</span>`; }
  }

  function params() {
    return { pStatus: el('mtStatus').value, pKeyword: el('mtKeyword').value.trim(), pRequester: el('mtRequester').value.trim(), pStartDate: el('mtStart').value, pEndDate: el('mtEnd').value };
  }

  async function loadList() {
    const wrap = el('mtWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    el('mtPage').innerHTML = '';
    const mine = ++seq;
    try {
      const res = await Api.get('/operate/mantnc/list', { ...params(), pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      if (mine !== seq) return;
      el('mtTotal').textContent = Number(res.total).toLocaleString();
      if (!res.total) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">유지보수 요청이 없습니다</div>'; return; }
      wrap.innerHTML = `
        <table style="width:100%;font-size:12px">
          <thead><tr><th>번호</th><th>요청일</th><th>제목</th><th>대상 화면</th><th>요청자</th><th>상태</th><th>작업 예정일</th><th>완료일</th></tr></thead>
          <tbody>${res.rows.map((r, i) => `
            <tr style="cursor:pointer" data-i="${i}">
              <td>${esc(r.mantncNo)}</td><td style="white-space:nowrap">${esc(fmt(r.reciptDate).slice(0, 10))}</td>
              <td><b>${esc(r.reciptTitle)}</b></td><td>${esc(r.menuName || '')}</td><td>${esc(r.reciptName)}</td>
              <td>${badge(r.execState, r.overdue)}</td>
              <td style="white-space:nowrap${r.overdue ? ';color:#b91c1c;font-weight:600' : ''}">${esc(r.execExpectDate || '')}</td>
              <td style="white-space:nowrap">${esc(fmt(r.cmpletDate).slice(0, 10))}</td>
            </tr>`).join('')}</tbody>
        </table>`;
      wrap.querySelectorAll('tr[data-i]').forEach(tr => tr.addEventListener('click', () => openDetail(res.rows[+tr.dataset.i].mantncNo)));
      UI.pagination(el('mtPage'), { total: res.total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      if (mine !== seq) return;
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function refresh() { loadSummary(); loadList(); }

  // ── 요청 등록·수정 ──────────────────────────────────────────────
  function openRequestForm(row, onDone) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-group"><label>제목 * <span style="font-weight:400;color:var(--text-muted)">(50자 이내)</span></label>
        <input class="input" id="mtFTitle" maxlength="50" value="${esc(row?.reciptTitle || '')}"></div>
      <div class="form-group" style="margin-top:8px"><label>대상 화면 <span style="font-weight:400;color:var(--text-muted)">(선택 — 어느 화면의 요청인지)</span></label>
        <input class="input" id="mtFMenu" maxlength="255" value="${esc(row?.menuName || '')}"></div>
      <div class="form-group" style="margin-top:8px"><label>요청 내용 *</label>
        <textarea class="input" id="mtFContent" rows="9" style="width:100%" maxlength="10000">${esc(row?.reciptCntnts || '')}</textarea></div>`;
    const q = s => body.querySelector('#' + s);
    UI.modal({
      title: row ? `요청 수정 #${row.mantncNo}` : '유지보수 요청', body, confirmText: row ? '저장' : '요청',
      onConfirm: async close => {
        const payload = { mantncNo: row?.mantncNo, title: q('mtFTitle').value.trim(), menuName: q('mtFMenu').value.trim(), content: q('mtFContent').value };
        if (!payload.title) return UI.toast('제목을 입력하세요', 'error');
        if (!payload.content.trim()) return UI.toast('요청 내용을 입력하세요', 'error');
        try {
          if (row) await Api.put('/operate/mantnc', payload); else await Api.post('/operate/mantnc', payload);
          UI.toast(row ? '수정되었습니다' : '요청이 등록되었습니다', 'success');
          close();
          if (!row) currentPage = 1;
          refresh();
          if (onDone) onDone();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  // ── 상세·처리 ───────────────────────────────────────────────────
  async function openDetail(no) {
    let r;
    try { r = await Api.get('/operate/mantnc/' + no, {}); } catch (e) { return UI.toast(e.message, 'error'); }
    const done = r.execState === 'cmplet';
    const mineRow = me.canHandle || r.reciptId === me.loginId;
    const cell = (k, v) => `<div style="display:flex;gap:8px;padding:2px 0"><span style="width:84px;color:var(--text-muted)">${k}</span><span style="flex:1">${v}</span></div>`;
    const box = t => `<div style="white-space:pre-wrap;border:1px solid var(--border);border-radius:var(--radius);padding:8px 10px;margin-top:4px;max-height:220px;overflow:auto">${esc(t)}</div>`;
    const body = document.createElement('div');
    body.style.cssText = 'font-size:13px;line-height:1.6';
    body.innerHTML = `
      ${cell('상태', badge(r.execState, r.overdue))}
      ${cell('요청', `${esc(r.reciptName)} · ${esc(fmt(r.reciptDate))}`)}
      ${cell('대상 화면', r.menuName ? esc(r.menuName) : '<span style="color:var(--text-muted)">-</span>')}
      ${cell('작업 예정일', r.execExpectDate ? `<b${r.overdue ? ' style="color:#b91c1c"' : ''}>${esc(r.execExpectDate)}</b>${r.execName ? ` <span style="color:var(--text-muted)">(${esc(r.execName)} 입력)</span>` : ''}` : '<span style="color:var(--text-muted)">아직 정해지지 않았습니다</span>')}
      <div style="margin-top:10px;font-weight:600">${esc(r.reciptTitle)}</div>${box(r.reciptCntnts || '')}
      ${r.execCntnts ? `<div style="margin-top:10px;font-weight:600">작업 메모</div>${box(r.execCntnts)}` : ''}
      ${done ? `<div style="margin-top:10px;font-weight:600">작업완료 <span style="font-weight:400;color:var(--text-muted)">${esc(r.cmpletName || '')} · ${esc(fmt(r.cmpletDate))}</span></div>${r.cmpletCntnts ? box(r.cmpletCntnts) : ''}` : ''}
      <div id="mtActions" style="display:flex;gap:8px;flex-wrap:wrap;margin-top:14px"></div>`;
    const m = UI.modal({ title: `유지보수 #${r.mantncNo}`, body, confirmText: null, cancelText: '닫기' });
    const actions = body.querySelector('#mtActions');
    const add = (label, cls, fn) => {
      const b = document.createElement('button');
      b.type = 'button'; b.className = 'btn ' + cls; b.textContent = label;
      b.addEventListener('click', fn);
      actions.appendChild(b);
    };
    if (!done && me.canHandle) {
      add(r.execExpectDate ? '예정일 변경' : '작업 예정일 입력', 'btn-primary', () => openSchedule(r, m.close));
      add('작업완료', 'btn-primary', () => openComplete(r, m.close));
    }
    if (!done && mineRow) {
      add('요청 수정', 'btn-ghost', () => { m.close(); openRequestForm(r); });
      add('삭제', 'btn-danger', () => UI.confirm(`요청 "${r.reciptTitle}" 를 삭제하시겠습니까? 되돌릴 수 없습니다.`, async close => {
        try { await Api.delete('/operate/mantnc/' + r.mantncNo); UI.toast('삭제되었습니다', 'success'); close(); m.close(); refresh(); } catch (e) { UI.toast(e.message, 'error'); close(); }
      }));
    }
  }

  function openSchedule(r, closeDetail) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-group"><label>작업 예정일 *</label><input class="input" id="mtSDate" type="date" value="${esc(r.execExpectDate || today())}"></div>
      <div class="form-group" style="margin-top:8px"><label>작업 메모 <span style="font-weight:400;color:var(--text-muted)">(선택)</span></label>
        <textarea class="input" id="mtSMemo" rows="4" style="width:100%" maxlength="2000">${esc(r.execCntnts || '')}</textarea></div>`;
    UI.modal({
      title: `작업 예정일 — #${r.mantncNo}`, body, confirmText: '저장',
      onConfirm: async close => {
        const execExpectDate = body.querySelector('#mtSDate').value;
        if (!execExpectDate) return UI.toast('작업 예정일을 입력하세요', 'error');
        try {
          await Api.put('/operate/mantnc/schedule', { mantncNo: r.mantncNo, execExpectDate, execCntnts: body.querySelector('#mtSMemo').value });
          UI.toast('작업 예정일이 저장되었습니다', 'success');
          close(); closeDetail(); refresh();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  function openComplete(r, closeDetail) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">작업완료로 처리하면 이후 수정·삭제·예정일 변경이 되지 않습니다.</div>
      <div class="form-group"><label>완료 내용 <span style="font-weight:400;color:var(--text-muted)">(선택, 255자 이내)</span></label>
        <textarea class="input" id="mtCText" rows="4" style="width:100%" maxlength="255"></textarea></div>`;
    UI.modal({
      title: `작업완료 — #${r.mantncNo}`, body, confirmText: '작업완료 처리',
      onConfirm: async close => {
        try {
          await Api.put('/operate/mantnc/complete', { mantncNo: r.mantncNo, cmpletCntnts: body.querySelector('#mtCText').value });
          UI.toast('작업완료로 처리되었습니다', 'success');
          close(); closeDetail(); refresh();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

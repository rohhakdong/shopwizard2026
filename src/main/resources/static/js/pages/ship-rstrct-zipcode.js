/**
 * 배송제한우편번호 (사이드 메뉴 발주) — 레거시 배송제한 우편번호(shopwizard.tShpDeliRstrctZipcodeAddr)
 *   구분(예: DeliFeeUpStar · DeliFeeUpSquare · JerrycanLimit)별로 우편번호를 모아 두면 [배송제한규칙]이 "이 구분의 우편번호"로
 *   발주서 시트를 나누는 조건을 건다. 우편번호는 하이픈을 뗀 숫자 5~6자리로 저장되고, 붙여넣기로 한꺼번에 등록할 수 있다.
 *   전 상점이 함께 쓰는 마스터라 등록·삭제는 제한 없는 계정만 할 수 있다.
 *   백엔드: /ship/rstrct-zipcode (sects, list, count, POST preview|bulk, DELETE ?sect[&zipcode])
 */
const PageShipRstrctZipcode = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 50;
  let currentPage = 1;
  let sects = [];

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>배송제한우편번호 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 구분별 우편번호 목록(배송제한규칙의 조건)</span></span>
          <button class="btn btn-primary" id="rzBtnNew" type="button">+ 붙여넣어 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            우편번호를 <b>구분</b>(예: DeliFeeUpStar = 택배비 인상 ★)별로 모아 둡니다. [배송제한규칙]에서 "우편번호 구분"을 고르면 이 목록에 있는 우편번호의 주문이 규칙 대상이 됩니다.
            우편번호는 줄·쉼표·공백으로 나눠 한꺼번에 붙여넣을 수 있고, 하이픈은 자동으로 뗍니다(5~6자리 숫자만 인정).
          </div>
          <div id="rzSects" style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:10px"></div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>구분</label>
              <select class="input" id="rzSect" style="width:200px"><option value="">전체</option></select></div>
            <div class="form-group"><label>우편번호(앞자리)</label><input class="input" id="rzZip" style="width:120px"></div>
            <button class="btn btn-primary" id="rzBtnSearch" type="button">검색</button>
            <button class="btn btn-danger" id="rzBtnDelSect" type="button" disabled>선택한 구분 전체 삭제</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="rzTotal">0</strong>건</div>
          <div class="table-wrap" id="rzWrap"></div>
          <div id="rzPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('rzBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('rzBtnNew').addEventListener('click', () => openBulkModal());
    document.getElementById('rzBtnDelSect').addEventListener('click', deleteSect);
    document.getElementById('rzSect').addEventListener('change', () => {
      document.getElementById('rzBtnDelSect').disabled = !document.getElementById('rzSect').value;
      currentPage = 1;
      loadList();
    });
    document.getElementById('rzZip').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    loadSects().then(loadList);
  }

  async function loadSects() {
    try { sects = await Api.get('/ship/rstrct-zipcode/sects', {}); } catch (_) { sects = []; }
    const sel = document.getElementById('rzSect');
    const cur = sel.value;
    sel.innerHTML = '<option value="">전체</option>' + sects.map(s => `<option value="${esc(s.sect)}">${esc(s.sect)} (${Number(s.cnt).toLocaleString()})</option>`).join('');
    sel.value = sects.some(s => s.sect === cur) ? cur : '';
    document.getElementById('rzBtnDelSect').disabled = !sel.value;
    document.getElementById('rzSects').innerHTML = sects.length
      ? sects.map(s => `<span style="border:1px solid var(--border);border-radius:var(--radius);padding:3px 10px;font-size:12px"><b>${esc(s.sect)}</b> ${Number(s.cnt).toLocaleString()}건</span>`).join('')
      : '<span style="font-size:12px;color:var(--text-muted)">등록된 구분이 없습니다. [붙여넣어 등록]으로 시작하세요.</span>';
  }

  function getParams() {
    const p = {};
    const sect = document.getElementById('rzSect').value;
    const zip = document.getElementById('rzZip').value.replace(/-/g, '').trim();
    if (sect) p.pDeliRstrctSect = sect;
    if (zip) p.pZipcode = zip;
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('rzWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/ship/rstrct-zipcode/count', params);
      document.getElementById('rzTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('rzPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/ship/rstrct-zipcode/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      wrap.innerHTML = `
        <table style="width:100%;font-size:12px">
          <thead><tr><th>구분</th><th>우편번호</th><th>등록자</th><th>등록일</th><th></th></tr></thead>
          <tbody>${list.map((r, i) => `<tr><td>${esc(r.deliRstrctSect)}</td><td><b>${esc(r.zipcode)}</b></td><td>${esc(r.registName)}</td>
            <td>${esc((r.registDate || '').slice(0, 10))}</td>
            <td><button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button></td></tr>`).join('')}</tbody>
        </table>`;
      wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
        const r = list[+b.dataset.del];
        UI.confirm(`[${r.deliRstrctSect}] ${r.zipcode} 를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/ship/rstrct-zipcode', { sect: r.deliRstrctSect, zipcode: r.zipcode });
            UI.toast('삭제되었습니다', 'success');
            await loadSects();
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      }));
      UI.pagination(document.getElementById('rzPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function deleteSect() {
    const sect = document.getElementById('rzSect').value;
    if (!sect) return;
    const n = (sects.find(s => s.sect === sect) || {}).cnt || 0;
    UI.confirm(`구분 "${sect}" 의 우편번호 ${Number(n).toLocaleString()}건을 모두 삭제하시겠습니까? 되돌릴 수 없습니다.`, async close => {
      try {
        const r = await Api.delete('/ship/rstrct-zipcode', { sect });
        UI.toast(`${r}건 삭제되었습니다`, 'success');
        await loadSects();
        loadList();
      } catch (e) { UI.toast(e.message, 'error'); }
      close();
    });
  }

  function openBulkModal() {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-group"><label>구분 * <span style="font-weight:400;color:var(--text-muted)">(기존 구분을 고르거나 새로 입력)</span></label>
        <input class="input" id="rzFSect" list="rzSectList" maxlength="50" placeholder="예: DeliFeeUpStar" value="${esc(document.getElementById('rzSect').value)}">
        <datalist id="rzSectList">${sects.map(s => `<option value="${esc(s.sect)}">`).join('')}</datalist></div>
      <div class="form-group" style="margin-top:8px"><label>우편번호 * <span style="font-weight:400;color:var(--text-muted)">(줄·쉼표·공백으로 구분, 하이픈 자동 제거)</span></label>
        <textarea class="input" id="rzFZips" rows="9" style="width:100%;font-family:monospace" placeholder="06236&#10;13529, 13530&#10;12-345"></textarea></div>
      <div id="rzFPreview" style="font-size:12px;margin-top:8px;color:var(--text-muted)">우편번호를 붙여넣으면 인식 결과가 여기에 표시됩니다.</div>`;
    const q = id => body.querySelector('#' + id);
    let timer = null;
    q('rzFZips').addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(async () => {
        const text = q('rzFZips').value;
        if (!text.trim()) { q('rzFPreview').textContent = '우편번호를 붙여넣으면 인식 결과가 여기에 표시됩니다.'; return; }
        try {
          const p = await Api.post('/ship/rstrct-zipcode/preview', { zipcodes: text });
          q('rzFPreview').innerHTML = `인식 <b>${p.zipcodes.length.toLocaleString()}</b>건${p.duplicatedInInput ? ` · 입력 중복 ${p.duplicatedInInput}건` : ''}
            ${p.invalid.length ? `<br><span style="color:var(--danger)">읽지 못한 값 ${p.invalid.length}개: ${esc(p.invalid.slice(0, 8).join(', '))}${p.invalid.length > 8 ? ' …' : ''}</span>` : ''}`;
        } catch (e) { q('rzFPreview').innerHTML = `<span style="color:var(--danger)">${esc(e.message)}</span>`; }
      }, 300);
    });
    UI.modal({
      title: '배송제한 우편번호 붙여넣어 등록', body, confirmText: '등록',
      onConfirm: async close => {
        if (!q('rzFSect').value.trim()) return UI.toast('구분을 입력하세요', 'error');
        if (!q('rzFZips').value.trim()) return UI.toast('우편번호를 입력하세요', 'error');
        try {
          const r = await Api.post('/ship/rstrct-zipcode/bulk', { sect: q('rzFSect').value.trim(), zipcodes: q('rzFZips').value });
          UI.toast(`${r.added.toLocaleString()}건 등록${r.alreadyExisted ? ` · 이미 있음 ${r.alreadyExisted}` : ''}${r.invalid.length ? ` · 읽지 못함 ${r.invalid.length}` : ''}`, 'success');
          close();
          document.getElementById('rzSect').value = '';
          await loadSects();
          currentPage = 1;
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

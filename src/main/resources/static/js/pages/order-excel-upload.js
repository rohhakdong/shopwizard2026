/**
 * 주문엑셀등록 (구 "주문 엑셀 업로드" 메뉴)
 *   채널(홈앤쇼핑·농협e쇼핑 등) 주문을 예전 양식(orderExcel.xls, 28열)으로 한 번에 등록한다.
 *   [양식 받기] → 작성 → [검사]로 행별 결과(정상/중복/오류)를 확인 → [등록]하면 정상 행만 저장된다(오류·중복 행은 건너뜀).
 *   서버는 [등록] 때 파일을 다시 검사하므로 검사 결과를 믿지 않는다. 같은 파일을 다시 올려도 이미 처리된 주문번호는 중복으로 빠진다.
 *   아래 카드는 업로드 이력(shopion.tOrdExcelUpload) — [상세]에서 행별 결과를 본다.
 *   백엔드: /order/excel/{template,preview,import}, /order/excel/upload/{list,count,detail}
 */
const PageOrderExcelUpload = (() => {

  const PAGE_SIZE = 20;
  let histPage = 1;
  let checked = false;   // 현재 선택한 파일을 검사했고 정상 행이 있는가

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function localDateStr(offsetDays) {
    const d = new Date();
    d.setDate(d.getDate() + (offsetDays || 0));
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  const el = id => document.getElementById(id);

  function render(container) {
    checked = false;
    container.innerHTML = `
      <div class="card">
        <div class="card-header">주문엑셀등록</div>
        <div class="card-body" style="padding:12px 16px">
          <div style="font-size:12px;color:var(--text-muted);line-height:1.7;margin-bottom:12px">
            채널 주문을 엑셀 양식(.xls, .xlsx)으로 한 번에 등록합니다. 한 행이 주문 1건(상품 1줄)이고, 한 번에 최대 300행까지 올릴 수 있습니다.<br>
            [검사]에서 행마다 <b>정상 / 중복 / 오류</b>를 확인하고 [등록]하면 <b>정상 행만</b> 저장됩니다(오류·중복 행은 건너뜀).
            등록된 주문은 <b>지불완료</b> 상태, 결제는 <b>엑셀</b>로 기록되며, 공급·판매·매입 가격은 <b>수량을 곱하기 전 1개 가격</b>입니다.
            상품이 연결되지 않은 주문은 경고와 함께 저장되니 <b>상품 매칭</b> 화면에서 연결하세요.
            <a href="#" id="oxTemplate" style="margin-left:6px">엑셀 양식 받기</a>
          </div>
          <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
            <input type="file" id="oxFile" accept=".xls,.xlsx" style="flex:1">
            <button class="btn btn-ghost" id="oxCheck">검사</button>
            <button class="btn btn-primary" id="oxImport" disabled>등록</button>
          </div>
          <div id="oxResult"></div>
        </div>
      </div>

      <div class="card" style="margin-top:16px">
        <div class="card-header">업로드 이력</div>
        <div class="card-body" style="padding:12px 16px">
          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group"><label>시작일</label>
              <input class="input" id="oxStart" type="date" value="${localDateStr(-30)}" style="width:140px"></div>
            <div class="form-group"><label>종료일</label>
              <input class="input" id="oxEnd" type="date" value="${localDateStr(0)}" style="width:140px"></div>
            <button class="btn btn-primary" id="oxSearch">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="oxTotal">0</strong>건</div>
          <div class="table-wrap" id="oxHist"></div>
          <div id="oxPage" style="margin-top:12px"></div>
        </div>
      </div>`;

    el('oxTemplate').addEventListener('click', e => { e.preventDefault(); downloadTemplate(); });
    el('oxFile').addEventListener('change', () => { resetCheck(); el('oxResult').innerHTML = ''; });   // 파일을 바꾸면 이전 검사 결과는 이 파일의 것이 아니다
    el('oxCheck').addEventListener('click', runCheck);
    el('oxImport').addEventListener('click', runImport);
    el('oxSearch').addEventListener('click', () => { histPage = 1; loadHistory(); });
    loadHistory();
  }

  function resetCheck() {
    checked = false;
    el('oxImport').disabled = true;
  }

  const formOf = file => { const f = new FormData(); f.append('file', file); return f; };

  async function downloadTemplate() {
    try {
      const res = await fetch('/order/excel/template', { credentials: 'same-origin' });
      if (!res.ok) throw new Error(res.status === 403 ? '이 기능을 사용할 권한이 없습니다.' : `HTTP ${res.status}`);
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'orderExcel.xls';
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  async function runCheck() {
    const file = el('oxFile').files[0];
    if (!file) { UI.toast('엑셀 파일을 선택하세요', 'error'); return; }
    resetCheck();
    el('oxResult').innerHTML = '<div style="padding:12px;color:var(--text-muted)">검사 중...</div>';
    try {
      renderResult(await Api.upload('/order/excel/preview', formOf(file)), false);
    } catch (e) {
      el('oxResult').innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
    }
  }

  async function runImport() {
    const file = el('oxFile').files[0];
    if (!file || !checked) { UI.toast('먼저 파일을 선택하고 검사하세요', 'error'); return; }
    el('oxImport').disabled = true;
    el('oxCheck').disabled = true;
    el('oxResult').innerHTML = '<div style="padding:12px;color:var(--text-muted)">등록 중... (행마다 저장하므로 시간이 걸릴 수 있습니다)</div>';
    try {
      const r = await Api.upload('/order/excel/import', formOf(file));
      renderResult(r, true);
      resetCheck();   // 같은 파일을 또 [등록]하면 전부 중복이 되므로 다시 검사하게 한다
      if (r.imported) {
        UI.toast(`주문 ${r.createdCount}건이 등록되었습니다`, r.createdCount ? 'success' : 'info');
        histPage = 1;
        loadHistory();
      }
    } catch (e) {
      el('oxResult').innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
    } finally {
      el('oxCheck').disabled = false;
    }
  }

  const STATUS = {
    OK:    { cls: 'badge-green', text: '정상' },
    DUP:   { cls: 'badge-gray',  text: '중복' },
    ERROR: { cls: 'badge-red',   text: '오류' },
  };

  function renderResult(r, afterImport) {
    if (r.fileError) {
      el('oxResult').innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(r.fileError)}</div>`;
      return;
    }
    if (!afterImport) {
      checked = !!r.importable;
      el('oxImport').disabled = !checked;
    }
    const n = v => esc(String(v));
    const summary = `
      <div style="font-size:13px;margin-bottom:8px;line-height:1.7">
        총 <b>${n((r.rows || []).length)}</b>행 ·
        ${afterImport ? `<span style="color:#15803d">등록 <b>${n(r.createdCount)}</b></span>` : `정상 <b>${n(r.okCount)}</b>`} ·
        중복 <b>${n(r.dupCount)}</b> ·
        <span style="color:${r.errorCount ? 'var(--danger)' : 'inherit'}">오류 <b>${n(r.errorCount)}</b></span> ·
        경고 <b>${n(r.warningCount)}</b>
        ${!afterImport && !r.okCount ? '<span style="color:var(--danger);margin-left:8px">등록할 정상 행이 없습니다.</span>' : ''}
        ${!afterImport && r.okCount && (r.errorCount || r.dupCount) ? '<span style="color:var(--text-muted);margin-left:8px">[등록]하면 정상 행만 저장되고 오류·중복 행은 건너뜁니다.</span>' : ''}
        ${afterImport && r.message ? `<div style="color:var(--text-muted)">${esc(r.message)}</div>` : ''}
      </div>`;
    const rows = (r.rows || []).map(row => {
      const st = STATUS[row.status] || STATUS.ERROR;
      const done = afterImport && row.orderNo != null;
      return `
        <tr>
          <td style="text-align:right">${n(row.rowNo)}</td>
          <td>${esc(row.excelOrderNo)}</td>
          <td>${esc(row.chnlName)}</td>
          <td style="max-width:240px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(row.prodName)}">${esc(row.prodName)}</td>
          <td><span class="badge ${done ? 'badge-blue' : st.cls}">${done ? '등록' : st.text}</span></td>
          <td style="font-size:12px;line-height:1.5">
            ${done ? `<div>주문번호 ${n(row.orderNo)}</div>` : ''}
            ${row.status === 'DUP' ? '<div style="color:var(--text-muted)">이미 처리된 주문번호라 건너뜁니다.</div>' : ''}
            ${(row.errors || []).map(m => `<div style="color:var(--danger)">${esc(m)}</div>`).join('')}
            ${(row.warnings || []).map(m => `<div style="color:#b45309">${esc(m)}</div>`).join('')}
          </td>
        </tr>`;
    }).join('');
    el('oxResult').innerHTML = `${summary}
      <div style="max-height:420px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
        <table class="table" style="width:100%;font-size:12px">
          <thead><tr><th>행</th><th>주문번호</th><th>채널</th><th>상품명</th><th>상태</th><th>메시지</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  }

  // ── 업로드 이력 ────────────────────────────────────────────────────
  async function loadHistory() {
    const wrap = el('oxHist');
    wrap.innerHTML = '<div style="text-align:center;padding:24px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const base = { pStartDate: el('oxStart').value, pEndDate: el('oxEnd').value };
      const [total, list] = await Promise.all([
        Api.get('/order/excel/upload/count', base),
        Api.get('/order/excel/upload/list', { ...base, pPageOffset: (histPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE }),
      ]);
      el('oxTotal').textContent = Number(total || 0).toLocaleString();
      if (!list || !list.length) {
        wrap.innerHTML = '<div style="text-align:center;padding:24px;color:var(--text-muted)">업로드 이력이 없습니다</div>';
      } else {
        wrap.innerHTML = `
          <table class="table" style="width:100%;font-size:12px">
            <thead><tr><th>업로드ID</th><th>파일</th><th>시작</th><th>종료</th><th>결과</th><th>메시지</th><th>등록자</th><th></th></tr></thead>
            <tbody>${list.map(u => `
              <tr>
                <td style="text-align:right">${esc(u.uploadId)}</td>
                <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(u.fileLoc)}">${esc(u.fileLoc)}</td>
                <td style="white-space:nowrap">${esc(u.uploadStartDate)}</td>
                <td style="white-space:nowrap">${esc(u.uploadEndDate)}</td>
                <td><span class="badge ${u.resultType === '성공' ? 'badge-green' : 'badge-gray'}">${esc(u.resultType)}</span></td>
                <td>${esc(u.resultMessag)}</td>
                <td style="white-space:nowrap">${esc(u.registName || u.registId)}</td>
                <td><button class="btn btn-ghost" data-id="${esc(u.uploadId)}">상세</button></td>
              </tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('button[data-id]').forEach(b => b.addEventListener('click', () => openDetail(b.dataset.id)));
      }
      UI.pagination(el('oxPage'), {
        total: Number(total || 0), page: histPage, pageSize: PAGE_SIZE,
        onChange: p => { histPage = p; loadHistory(); },
      });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  async function openDetail(uploadId) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="padding:16px;color:var(--text-muted)">불러오는 중...</div>';
    const { el: modalEl } = UI.modal({ title: `업로드 #${uploadId} 상세`, body, confirmText: null, cancelText: '닫기' });
    modalEl.querySelector('.modal').style.width = '820px';
    try {
      const list = await Api.get('/order/excel/upload/detail', { uploadId });
      const cls = { '성공': 'badge-green', '중복': 'badge-gray', '오류': 'badge-red' };
      body.innerHTML = !list || !list.length
        ? '<div style="padding:16px;color:var(--text-muted)">상세 이력이 없습니다</div>'
        : `<div style="max-height:60vh;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
            <table class="table" style="width:100%;font-size:12px">
              <thead><tr><th>행</th><th>주문번호</th><th>결과</th><th>메시지</th></tr></thead>
              <tbody>${list.map(d => `
                <tr>
                  <td style="text-align:right">${esc(d.uploadDetailNo)}</td>
                  <td>${esc(d.excelOrderNo)}</td>
                  <td><span class="badge ${cls[d.resultType] || 'badge-gray'}">${esc(d.resultType)}</span></td>
                  <td style="line-height:1.5">${esc(d.resultMessag)}</td>
                </tr>`).join('')}</tbody>
            </table>
          </div>`;
    } catch (e) {
      body.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
    }
  }

  return { render };
})();

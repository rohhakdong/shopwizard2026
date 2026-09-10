/**
 * 상품 매칭 (주문 > 상품 매칭)
 *   샵링커/엑셀 수집 주문 중 상품이 매칭되지 않은(ProdCode NULL) 주문라인을 실제 상품에 연결한다.
 *   매칭 = 그 라인에 상품코드를 지정하고 공급가/원가/마진/세율을 상품 기준으로 채우는 것
 *   (레거시 runUpdatePriceFromExcel 이식). 상품명+옵션이 같은 라인은 한 번에 backfill.
 *
 * - 목록: /order/match/unmatched/list(+/count) — (상품명, 옵션) 단위로 묶은 미매칭 그룹
 * - [매칭]: /product/prod/list 로 상품 검색·선택 → POST /order/match/apply
 *          "규칙 저장" 체크 시 tOrdProdCodeMatch 에 (상품명,옵션→상품코드) 저장 → 이후 수집분 자동 매칭
 * - CSV: GET /order/match/export 로 내려받아 상품코드 채워 POST /order/match/upload
 */
const PageOrderProdMatch = (() => {

  const PAGE_SIZE = 30;
  let currentPage = 1;
  let totalCount = 0;

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function today() { return new Date().toISOString().slice(0, 10); }
  function daysAgo(d) { const t = new Date(); t.setDate(t.getDate() - d); return t.toISOString().slice(0, 10); }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>상품 매칭</span>
          <span>
            <button class="btn btn-ghost" id="pmCsvDown">CSV 내려받기</button>
            <button class="btn btn-ghost" id="pmCsvUp">CSV 업로드</button>
            <input type="file" id="pmCsvFile" accept=".csv,text/csv" hidden>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>채널</label>
              <select class="input" id="pmChnl" style="width:150px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>주문일 시작</label>
              <input class="input" id="pmStart" type="date" value="${daysAgo(90)}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문일 종료</label>
              <input class="input" id="pmEnd" type="date" value="${today()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>상품명</label>
              <input class="input" id="pmProdName" placeholder="상품명 일부" style="width:200px">
            </div>
            <button class="btn btn-primary" id="pmSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            미매칭 <strong id="pmTotal">0</strong>종 — 상품명+옵션이 같은 주문라인은 한 번 매칭하면 모두 반영됩니다.
          </div>

          <div class="table-wrap" id="pmTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="pmPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('pmSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('pmProdName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    document.getElementById('pmCsvDown').addEventListener('click', downloadCsv);
    document.getElementById('pmCsvUp').addEventListener('click', () => document.getElementById('pmCsvFile').click());
    document.getElementById('pmCsvFile').addEventListener('change', uploadCsv);

    loadChannels();
    loadList();
  }

  async function loadChannels() {
    try {
      const chnls = await Api.get('/company/chnl', { pSvcCode: svcCode(), pPageOffset: 0, pPageSize: 2000 });
      const el = document.getElementById('pmChnl');
      (chnls || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'))
        .forEach(c => el.insertAdjacentHTML('beforeend', `<option value="${esc(c.chnlCode)}">${esc(c.chnlName)}</option>`));
    } catch (_) { /* 옵션 로드 실패해도 목록은 동작 */ }
  }

  function getParams() {
    const v = id => document.getElementById(id).value;
    return {
      pSvcCode:   svcCode(),
      pChnlCode:  v('pmChnl'),
      pStartDate: v('pmStart'),
      pEndDate:   v('pmEnd'),
      pProdName:  v('pmProdName').trim(),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('pmTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('pmPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/match/unmatched/list/count', params);
      document.getElementById('pmTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">미매칭 주문이 없습니다</div>';
        return;
      }
      const list = await Api.get('/order/match/unmatched/list', {
        ...params, topCnt: (currentPage - 1) * PAGE_SIZE, countPerPage: PAGE_SIZE,
      });
      renderTable(list);
      UI.pagination(document.getElementById('pmPagination'), {
        total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
        onChange: p => { currentPage = p; loadList(); },
      });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const rows = list.map((g, i) => `
      <tr>
        <td style="font-size:12px">${esc(g.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(g.prodOption) || '-'}</td>
        <td style="font-size:11px">${esc(g.chnlNames)}</td>
        <td style="text-align:right">${g.cnt}</td>
        <td style="text-align:right;font-size:11px">${esc(g.sampleOrderNo)}</td>
        <td style="text-align:center">
          <button class="btn btn-primary" style="padding:2px 10px;font-size:11px"
            data-i="${i}">매칭</button>
        </td>
      </tr>`).join('');

    const wrap = document.getElementById('pmTableWrap');
    wrap.innerHTML = `
      <table>
        <thead><tr>
          <th>상품명</th><th>옵션</th><th>채널</th>
          <th style="text-align:right">건수</th><th style="text-align:right">대표주문</th>
          <th style="text-align:center">매칭</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>`;
    wrap.querySelectorAll('button[data-i]').forEach(btn => {
      btn.addEventListener('click', () => openMatchModal(list[+btn.dataset.i]));
    });
  }

  // ── 매칭 모달 ──────────────────────────────────────────────────────
  function openMatchModal(group) {
    let picked = null;   // { prodCode, prodName, ... }
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;margin-bottom:10px">
        <div><strong>${esc(group.prodName)}</strong></div>
        <div style="color:var(--text-muted)">옵션: ${esc(group.prodOption) || '-'} · ${group.cnt}건 · ${esc(group.chnlNames)}</div>
      </div>
      <div style="display:flex;gap:6px;margin-bottom:8px">
        <input class="input" id="pmqName" placeholder="상품명" style="flex:1">
        <input class="input" id="pmqCode" placeholder="상품코드/상점상품코드" style="width:180px">
        <button class="btn btn-ghost" id="pmqBtn" style="white-space:nowrap">검색</button>
      </div>
      <div class="table-wrap" id="pmqWrap" style="max-height:38vh;border:1px solid var(--border);border-radius:var(--radius)">
        <div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">상품을 검색하세요</div>
      </div>
      <label style="display:flex;align-items:center;gap:6px;font-size:12px;margin-top:10px">
        <input type="checkbox" id="pmSaveRule" checked>
        이 매칭 규칙 저장 — 앞으로 같은 상품명+옵션으로 수집되는 주문은 자동 매칭
      </label>
      <div id="pmApplyMsg" style="margin-top:8px;font-size:12px"></div>`;

    const modal = UI.modal({
      title: '상품 매칭',
      body,
      confirmText: '매칭 적용',
      cancelText: '닫기',
      onConfirm: async (close) => {
        if (!picked) { UI.toast('연결할 상품을 선택하세요', 'error'); return; }
        const msgEl = body.querySelector('#pmApplyMsg');
        msgEl.textContent = '적용 중...';
        const btn = modal.el.querySelector('[data-action=confirm]');
        if (btn) btn.disabled = true;
        try {
          const f = getParams();
          const res = await Api.post('/order/match/apply', {
            prodName: group.prodName,
            prodOption: group.prodOption,
            prodCode: picked.prodCode,
            saveRule: body.querySelector('#pmSaveRule').checked,
            svcCode: svcCode(),
            pStartDate: f.pStartDate,
            pEndDate: f.pEndDate,
            pChnlCode: f.pChnlCode,
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          if (!res.ok) { msgEl.innerHTML = `<span style="color:var(--danger)">${esc(res.message)}</span>`; if (btn) btn.disabled = false; return; }
          UI.toast(res.message, 'success');
          close();
          loadList();
        } catch (e) {
          msgEl.innerHTML = `<span style="color:var(--danger)">${esc(e.message)}</span>`;
          if (btn) btn.disabled = false;
        }
      },
    });

    body.querySelector('#pmqName').value = group.prodName || '';
    const doSearch = async () => {
      const wrap = body.querySelector('#pmqWrap');
      wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">검색 중...</div>';
      try {
        const params = { pPageOffset: 0, pPageSize: 30 };
        const nm = body.querySelector('#pmqName').value.trim();
        const cd = body.querySelector('#pmqCode').value.trim();
        if (nm) params.pProdName = nm;
        if (cd) params.pProdCode = cd;
        const list = await Api.get('/product/prod/list', params);
        if (!list || list.length === 0) {
          wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">결과 없음</div>';
          return;
        }
        wrap.innerHTML = `
          <table>
            <thead><tr><th></th><th>상품코드</th><th>상품명</th><th>상점</th><th style="text-align:right">판매가</th><th style="text-align:right">공급가</th></tr></thead>
            <tbody>${list.map((p, i) => `
              <tr style="cursor:pointer" data-i="${i}">
                <td><input type="radio" name="pmpick" value="${i}"></td>
                <td style="font-size:11px">${esc(p.prodCode)}</td>
                <td style="font-size:11px">${esc(p.prodName)}</td>
                <td style="font-size:11px">${esc(p.shopName)}</td>
                <td style="text-align:right;font-size:11px">${won(p.salePrice)}</td>
                <td style="text-align:right;font-size:11px">${won(p.supplyPrice)}</td>
              </tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('tbody tr').forEach(tr => {
          tr.addEventListener('click', () => {
            const p = list[+tr.dataset.i];
            picked = p;
            wrap.querySelector(`input[value="${tr.dataset.i}"]`).checked = true;
            body.querySelector('#pmApplyMsg').innerHTML =
              `선택: <strong>${esc(p.prodCode)}</strong> ${esc(p.prodName)}`;
          });
        });
      } catch (e) {
        wrap.innerHTML = `<div style="color:var(--danger);padding:16px;font-size:12px">${esc(e.message)}</div>`;
      }
    };
    body.querySelector('#pmqBtn').addEventListener('click', doSearch);
    body.querySelector('#pmqName').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    body.querySelector('#pmqCode').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    doSearch();
  }

  // ── CSV ───────────────────────────────────────────────────────────
  function downloadCsv() {
    const p = getParams();
    const qs = new URLSearchParams(Object.entries(p).filter(([, v]) => v)).toString();
    window.location.href = '/order/match/export?' + qs;
  }

  async function uploadCsv(e) {
    const file = e.target.files[0];
    e.target.value = '';
    if (!file) return;
    const f = getParams();
    const fd = new FormData();
    fd.append('file', file);
    fd.append('svcCode', svcCode());
    fd.append('pStartDate', f.pStartDate || '');
    fd.append('pEndDate', f.pEndDate || '');
    fd.append('pChnlCode', f.pChnlCode || '');
    fd.append('registId', info?.loginId || '');
    fd.append('registName', info?.name || '');

    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">업로드 처리 중...</div>';
    UI.modal({ title: 'CSV 매칭 결과', body, confirmText: null, cancelText: '닫기' });
    try {
      const results = await Api.upload('/order/match/upload', fd);
      const ok = results.filter(r => r.ok).length;
      const rows = results.map(r => `
        <tr>
          <td style="font-size:11px">${esc(r.prodName)}</td>
          <td style="font-size:11px;color:var(--text-muted)">${esc(r.prodOption) || '-'}</td>
          <td style="text-align:center">${r.ok ? '<span style="color:#15803d">성공</span>' : '<span style="color:#b91c1c">실패</span>'}</td>
          <td style="text-align:right">${r.applied || 0}</td>
          <td style="text-align:right">${r.skipped || 0}</td>
          <td style="font-size:11px">${esc(r.message)}</td>
        </tr>`).join('');
      body.innerHTML = `
        <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 ${results.length}행 — 성공 ${ok} / 실패 ${results.length - ok}</div>
        <div class="table-wrap" style="max-height:50vh">
          <table>
            <thead><tr><th>상품명</th><th>옵션</th><th style="text-align:center">결과</th><th style="text-align:right">적용</th><th style="text-align:right">건너뜀</th><th>메시지</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>`;
      loadList();
    } catch (err) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(err.message)}</div>`;
    }
  }

  return { render };
})();

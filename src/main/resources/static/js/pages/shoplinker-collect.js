/**
 * 샵링커 수집 (주문 > 샵링커 수집)
 *   샵링커 협력사 API에서 주문을 긁어와 샵피온 주문(tOrdOrder/Prod/Pay)으로 생성한다.
 *   레거시 orderShoplinkerUpload.jsp + QuartzInsertOrderFromShoplinker 이식.
 *
 * - 목록: shopion.tOrdSlnkTrans (수집 실행 이력) — /order/slnktrans/list(+/list/count)
 * - 행클릭: shopion.tOrdSlnkTransDetail (건별 결과) — /order/slnktrans/detail/list?pTransId=
 * - [수집 실행]: 기간 + 몰 체크 → POST /order/shoplinker/collect (동기), 몰별 요약 표시
 *
 * 몰 목록은 샵링커 API 제약상 고정(레거시 동일):
 *   오픈마켓제외 쇼핑몰(order.php) + 옥션/지마켓/11번가 오픈마켓별 아이디(order2.php)
 */
const PageShoplinkerCollect = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount = 0;

  // 수집 대상 몰 (mallCode, loginId, 표시명). mallCode "" = 오픈마켓제외 쇼핑몰(order.php)
  const MALLS = [
    { code: '',   id: '',          label: '오픈마켓제외 쇼핑몰' },
    { code: 'A',  id: 'shopion1',  label: '옥션 (shopion1)' },
    { code: 'A',  id: 'shopion2',  label: '옥션 (shopion2)' },
    { code: 'G',  id: 'shopion1',  label: '지마켓 (shopion1)' },
    { code: 'G',  id: 'shopion2',  label: '지마켓 (shopion2)' },
    { code: 'G',  id: 'shopion3',  label: '지마켓 (shopion3)' },
    { code: 'R',  id: 'ray77255',  label: '지마켓 (ray77255)' },
    { code: 'R',  id: '2ray77255', label: '지마켓 (2ray77255)' },
    { code: 'R',  id: '3ray77255', label: '지마켓 (3ray77255)' },
    { code: '11', id: 'plus825',   label: '11번가 (plus825)' },
    { code: '11', id: 'splus825',  label: '11번가 (splus825)' },
    { code: '11', id: 'shopion1',  label: '11번가 (shopion1)' },
    { code: '11', id: 'shopion3',  label: '11번가 (shopion3)' },
  ];

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 19) : ''; }
  function today() { return new Date().toISOString().slice(0, 10); }
  function mallLabel(t) {
    const m = { '': '오픈마켓제외', 'A': '옥션', 'G': '지마켓(법인)', 'R': '지마켓(개인)', '11': '11번가' }[t.mallCode] ?? t.mallCode;
    return t.loginId ? `${m} ${t.loginId}` : m;
  }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>샵링커 수집</span>
          <button class="btn btn-primary" id="scBtnRun">수집 실행</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>몰</label>
              <select class="input" id="scMall" style="width:150px">
                <option value="">전체</option>
                <option value="M">오픈마켓제외 쇼핑몰</option>
                <option value="A">옥션</option>
                <option value="G">지마켓(법인)</option>
                <option value="R">지마켓(개인)</option>
                <option value="11">11번가</option>
              </select>
            </div>
            <div class="form-group">
              <label>결과</label>
              <select class="input" id="scResult" style="width:100px">
                <option value="">전체</option>
                <option value="진행">진행</option>
                <option value="성공">성공</option>
                <option value="실패">실패</option>
              </select>
            </div>
            <div class="form-group">
              <label>실행일 시작</label>
              <input class="input" id="scStart" type="date" style="width:140px">
            </div>
            <div class="form-group">
              <label>실행일 종료</label>
              <input class="input" id="scEnd" type="date" style="width:140px">
            </div>
            <button class="btn btn-primary" id="scBtnSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="scTotal">0</strong>건
          </div>

          <div class="table-wrap" id="scTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="scPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('scBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('scBtnRun').addEventListener('click', openRunModal);
    loadList();
  }

  // ── 목록 ───────────────────────────────────────────────────────────
  function getParams() {
    const v = id => document.getElementById(id).value;
    return {
      pMallCode:   v('scMall'),
      pResultType: v('scResult'),
      pStartDate:  v('scStart'),
      pEndDate:    v('scEnd'),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('scTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('scPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/slnktrans/list/count', params);
      document.getElementById('scTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }
      const list = await Api.get('/order/slnktrans/list', {
        ...params,
        topCnt: (currentPage - 1) * PAGE_SIZE,
        countPerPage: PAGE_SIZE,
      });
      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function resultBadge(rt) {
    const cls = { '성공': 'badge-green', '실패': 'badge-red', '진행': 'badge-blue', '준비': 'badge-gray' }[rt] || 'badge-gray';
    return `<span class="badge ${cls}">${esc(rt) || '-'}</span>`;
  }

  function renderTable(list) {
    const wrap = document.getElementById('scTableWrap');
    const rows = list.map(t => `
      <tr style="cursor:pointer" data-transid="${t.transId}">
        <td style="text-align:right">${t.transId}</td>
        <td>${esc(mallLabel(t))}</td>
        <td>${esc(t.startDate)} ~ ${esc(t.endDate)}</td>
        <td style="text-align:center">${resultBadge(t.resultType)}</td>
        <td style="font-size:11px">${esc(t.resultMessag)}</td>
        <td style="font-size:11px">${esc(fmtDt(t.transStartDate))}</td>
        <td style="font-size:11px">${esc(fmtDt(t.transEndDate))}</td>
        <td style="font-size:11px">${esc(t.registName || t.registId)}</td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th style="text-align:right">전송ID</th><th>몰</th><th>수집기간</th>
            <th style="text-align:center">결과</th><th>결과메시지</th>
            <th>실행시작</th><th>실행종료</th><th>실행자</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('tbody tr').forEach(tr => {
      tr.addEventListener('click', () => openDetailModal(parseInt(tr.dataset.transid)));
    });
  }

  function renderPagination() {
    const el = document.getElementById('scPagination');
    UI.pagination(el, {
      total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
      onChange: p => { currentPage = p; loadList(); },
    });
  }

  // ── 건별 결과 모달 ─────────────────────────────────────────────────
  async function openDetailModal(transId) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';
    UI.modal({ title: `수집 상세 – 전송ID ${transId}`, body, confirmText: null, cancelText: '닫기' });
    try {
      const list = await Api.get('/order/slnktrans/detail/list', { pTransId: transId });
      const cnt = list.reduce((a, d) => { a[d.resultType] = (a[d.resultType] || 0) + 1; return a; }, {});
      const summary = ['성공', '중복', '무시', '오류'].map(k => `${k} ${cnt[k] || 0}`).join(' · ');
      const rows = list.map(d => `
        <tr>
          <td style="text-align:right;font-size:11px">${d.transDetailNo}</td>
          <td style="font-size:11px">${esc(d.slnkOrderCode)}</td>
          <td style="text-align:center;font-size:11px">${esc(d.resultType)}</td>
          <td style="font-size:11px">${esc(d.resultMessag)}</td>
        </tr>`).join('');
      body.innerHTML = `
        <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 ${list.length}건 — ${esc(summary)}</div>
        <div class="table-wrap" style="max-height:50vh">
          <table>
            <thead><tr><th style="text-align:right">순번</th><th>주문코드</th><th style="text-align:center">결과</th><th>메시지</th></tr></thead>
            <tbody>${rows || '<tr><td colspan="4" style="text-align:center;padding:16px;color:var(--text-muted)">내역 없음</td></tr>'}</tbody>
          </table>
        </div>`;
    } catch (e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  // ── 수집 실행 모달 ─────────────────────────────────────────────────
  function openRunModal() {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid" style="margin-bottom:12px">
        <div class="form-group">
          <label>시작일</label>
          <input class="input" id="scRunStart" type="date" value="${today()}">
        </div>
        <div class="form-group">
          <label>종료일</label>
          <input class="input" id="scRunEnd" type="date" value="${today()}">
        </div>
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin:4px 0 6px">수집 대상 몰</div>
      <div style="margin-bottom:6px">
        <a href="javascript:void(0)" id="scMallAll" style="font-size:12px">전체선택</a>
        &nbsp;/&nbsp;
        <a href="javascript:void(0)" id="scMallNone" style="font-size:12px">전체해제</a>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:4px 12px;max-height:240px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius);padding:8px">
        ${MALLS.map((m, i) => `
          <label style="font-size:12px;display:flex;align-items:center;gap:6px">
            <input type="checkbox" class="sc-mall" data-idx="${i}"> ${esc(m.label)}
          </label>`).join('')}
      </div>
      <div id="scRunResult" style="margin-top:12px"></div>`;

    const modal = UI.modal({
      title: '샵링커 수집 실행',
      body,
      confirmText: '수집 실행',
      cancelText: '닫기',
      onConfirm: async (close) => {
        const start = body.querySelector('#scRunStart').value;
        const end = body.querySelector('#scRunEnd').value;
        const checked = [...body.querySelectorAll('.sc-mall:checked')].map(c => MALLS[+c.dataset.idx]);
        if (!start || !end) { UI.toast('시작일과 종료일을 입력하세요', 'error'); return; }
        if (checked.length === 0) { UI.toast('수집할 몰을 선택하세요', 'error'); return; }

        const resultEl = body.querySelector('#scRunResult');
        resultEl.innerHTML = '<div style="color:var(--text-muted);font-size:12px">수집 중... (몰/건수에 따라 시간이 걸릴 수 있습니다)</div>';
        const btn = modal.el.querySelector('[data-action=confirm]');
        if (btn) btn.disabled = true;

        try {
          const info = await Auth.getInfo();
          const res = await Api.post('/order/shoplinker/collect', {
            startDate: start,
            endDate: end,
            malls: checked.map(m => ({ mallCode: m.code, loginId: m.id })),
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          renderRunResult(resultEl, res);
          loadList();
        } catch (e) {
          resultEl.innerHTML = `<div style="color:var(--danger);font-size:12px">${esc(e.message)}</div>`;
        } finally {
          if (btn) btn.disabled = false;
        }
      },
    });

    body.querySelector('#scMallAll').addEventListener('click', () => body.querySelectorAll('.sc-mall').forEach(c => c.checked = true));
    body.querySelector('#scMallNone').addEventListener('click', () => body.querySelectorAll('.sc-mall').forEach(c => c.checked = false));
  }

  function renderRunResult(el, res) {
    const rows = (res.malls || []).map(m => `
      <tr>
        <td style="font-size:11px">${esc(m.mallName)}</td>
        <td style="text-align:center">${resultBadge(m.resultType)}</td>
        <td style="text-align:right">${m.total}</td>
        <td style="text-align:right;color:#15803d">${m.success}</td>
        <td style="text-align:right;color:#b45309">${m.duplicate}</td>
        <td style="text-align:right;color:#64748b">${m.skip}</td>
        <td style="text-align:right;color:#b91c1c">${m.error}</td>
      </tr>
      ${m.resultMessag && m.resultType === '실패'
        ? `<tr><td colspan="7" style="font-size:11px;color:var(--danger)">${esc(m.resultMessag)}</td></tr>` : ''}`).join('');

    el.innerHTML = `
      ${res.message ? `<div style="font-size:12px;color:${res.ok ? 'var(--text-muted)' : 'var(--danger)'};margin-bottom:6px">${esc(res.message)}</div>` : ''}
      <div class="table-wrap" style="max-height:40vh">
        <table>
          <thead><tr>
            <th>몰</th><th style="text-align:center">결과</th>
            <th style="text-align:right">총</th><th style="text-align:right">성공</th>
            <th style="text-align:right">중복</th><th style="text-align:right">무시</th><th style="text-align:right">오류</th>
          </tr></thead>
          <tbody>${rows || '<tr><td colspan="7" style="text-align:center;padding:12px;color:var(--text-muted)">결과 없음</td></tr>'}</tbody>
        </table>
      </div>`;
  }

  return { render };
})();

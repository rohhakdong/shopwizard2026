/**
 * 정산 마감 (정산 > 정산 마감)
 *   "정산 대상" 화면에서 생성한 정산자료 중 아직 마감하지 않은 것을 상점별로 모아 보여주고,
 *   선택한 상점을 기간 단위로 마감한다(정산번호 발급 → "정산 관리" 목록에 나타남).
 *   마감 전에는 상점 행의 [자료]에서 라인별 금액(공급가·할인공급가·매입가·부가세율)을 보정하거나 생성을 취소(다시 정산 대상으로)할 수 있다.
 *   마감한 정산은 이 앱에서 되돌릴 수 없다 — 마감 전에 "미선택 대상" 건수(기간 안에 아직 정산자료로 만들지 않은 건)를 꼭 확인할 것.
 */
const PageAdjustClose = (() => {

  const esc = UI.esc;
  let shopList = [];
  let rows = [];                 // 상점별 집계
  const selected = new Set();    // 선택한 상점코드

  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function toDateStr(d) { return d.toISOString().slice(0, 10); }
  function firstOfPrevMonth() { const d = new Date(); d.setMonth(d.getMonth() - 1, 1); return toDateStr(d); }
  function lastOfPrevMonth() { const d = new Date(); d.setDate(0); return toDateStr(d); }
  function svc() { return (typeof info !== 'undefined' && info?.svcCode) || ''; }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    selected.clear();
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;justify-content:space-between;align-items:center">
          <span>정산 마감</span>
          <button class="btn btn-primary" id="acBtnClose" disabled>선택 상점 마감 (<span id="acSelCount">0</span>곳)</button>
        </div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="acStartDate" type="date" value="${firstOfPrevMonth()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="acEndDate" type="date" value="${lastOfPrevMonth()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>정산주기</label>
              <select class="input" id="acPeriod" style="width:120px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>상점</label>
              <select class="input" id="acShopCode" style="width:190px"><option value="">전체</option></select>
            </div>
            <button class="btn btn-primary" id="acBtnSearch">조회</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            마감 전 정산자료를 상점별로 보여줍니다. <strong>미선택 대상</strong>이 0이 아니면 기간 안에 아직 정산자료로 만들지 않은
            배송/반품 건이 있다는 뜻이니 "정산 대상" 화면에서 먼저 생성하세요.
          </div>

          <div class="table-wrap" id="acTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">기간을 정하고 조회하세요</div>
          </div>
        </div>
      </div>`;

    document.getElementById('acBtnSearch').addEventListener('click', load);
    document.getElementById('acBtnClose').addEventListener('click', closeSelected);
    loadShops();
  }

  async function loadShops() {
    try {
      const shops = await Api.get('/company/shop', {});
      shopList = (shops || []).slice().sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { shopList = []; }
    const shopSel = document.getElementById('acShopCode');
    const perSel = document.getElementById('acPeriod');
    if (!shopSel) return;
    shopSel.insertAdjacentHTML('beforeend', shopList.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName || s.shopCode)}</option>`).join(''));
    const periods = [...new Set(shopList.map(s => s.adjustPeriodCode).filter(Boolean))].sort();
    perSel.insertAdjacentHTML('beforeend', periods.map(p => `<option value="${esc(p)}">${esc(p)}</option>`).join(''));
  }

  function filters() {
    const v = id => document.getElementById(id).value;
    return { start: v('acStartDate'), end: v('acEndDate'), period: v('acPeriod'), shop: v('acShopCode') };
  }

  async function load() {
    const f = filters();
    const wrap = document.getElementById('acTableWrap');
    if (!f.start || !f.end) { UI.toast('시작일과 종료일을 입력하세요', 'error'); return; }
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    selected.clear();
    updateButton();
    try {
      const [sums, open] = await Promise.all([
        Api.get('/adjust/detail/sumshop/list', {
          pStartDate: f.start, pEndDate: f.end, pBuyAdjustCloseDate: 'Y',
          pAdjustPeriodType: f.period, pShopCode: f.shop, pSvcCode: svc(),
        }),
        Api.get('/adjust/process/target/open-count', {
          startDate: f.start, endDate: f.end, pAdjustPeriodType: f.period, pShopCode: f.shop,
        }),
      ]);
      const openBy = new Map((open || []).map(o => [o.shopCode, Number(o.cnt)]));
      const by = new Map();
      (sums || []).forEach(s => {
        const r = by.get(s.shopCode) || { shopCode: s.shopCode, shopName: s.shopName, saleAmt: 0, buyAmt: 0, deliFeeAmt: 0, vats: [] };
        r.saleAmt += Number(s.saleAmt || 0);
        r.buyAmt += Number(s.buyAmt || 0);
        r.deliFeeAmt += Number(s.deliFeeAmt || 0);
        if (s.vatRate != null && !r.vats.includes(s.vatRate)) r.vats.push(s.vatRate);
        by.set(s.shopCode, r);
      });
      rows = [...by.values()].map(r => ({ ...r, open: openBy.get(r.shopCode) || 0 }))
        .sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
      renderTable();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable() {
    const wrap = document.getElementById('acTableWrap');
    if (rows.length === 0) {
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">마감할 정산자료가 없습니다</div>';
      return;
    }
    const body = rows.map((r, i) => `
      <tr>
        <td style="text-align:center"><input type="checkbox" class="ac-row" data-i="${i}" ${selected.has(r.shopCode) ? 'checked' : ''}></td>
        <td style="font-size:12px">${esc(r.shopName || r.shopCode)}<div style="font-size:10px;color:var(--text-muted)">${esc(r.shopCode)}</div></td>
        <td style="font-size:11px;text-align:center">${esc(r.vats.join(', '))}</td>
        <td style="text-align:right;font-size:12px">${won(r.saleAmt)}</td>
        <td style="text-align:right;font-size:12px">${won(r.buyAmt)}</td>
        <td style="text-align:right;font-size:12px">${won(r.saleAmt - r.buyAmt)}</td>
        <td style="text-align:right;font-size:12px">${won(r.deliFeeAmt)}</td>
        <td style="text-align:right;font-size:12px;${esc(r.open > 0 ? 'color:var(--danger);font-weight:600' : '')}">${esc(r.open)}</td>
        <td><button class="btn btn-ghost" style="padding:2px 8px;font-size:11px" data-action="detail" data-i="${i}">자료</button></td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th style="width:36px;text-align:center"><input type="checkbox" id="acSelectAll"></th>
            <th>상점</th><th style="width:80px;text-align:center">부가세율</th>
            <th style="text-align:right">판매액</th><th style="text-align:right">매입액</th>
            <th style="text-align:right">마진</th><th style="text-align:right">배송비</th>
            <th style="text-align:right;width:90px">미선택 대상</th><th style="width:70px"></th>
          </tr>
        </thead>
        <tbody>${body}</tbody>
      </table>`;

    wrap.querySelectorAll('.ac-row').forEach(cb => {
      cb.addEventListener('change', () => {
        const r = rows[+cb.dataset.i];
        if (cb.checked) selected.add(r.shopCode); else selected.delete(r.shopCode);
        document.getElementById('acSelectAll').checked = rows.every(x => selected.has(x.shopCode));
        updateButton();
      });
    });
    document.getElementById('acSelectAll').addEventListener('change', e => {
      wrap.querySelectorAll('.ac-row').forEach(cb => {
        const r = rows[+cb.dataset.i];
        cb.checked = e.target.checked;
        if (e.target.checked) selected.add(r.shopCode); else selected.delete(r.shopCode);
      });
      updateButton();
    });
    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', () => openDetailModal(rows[+btn.dataset.i]));
    });
  }

  function updateButton() {
    document.getElementById('acSelCount').textContent = selected.size;
    document.getElementById('acBtnClose').disabled = selected.size === 0;
  }

  // ── 상점 마감 ──────────────────────────────────────────────────────
  function closeSelected() {
    const f = filters();
    const shops = rows.filter(r => selected.has(r.shopCode));
    if (shops.length === 0) return;
    const openTotal = shops.reduce((n, r) => n + r.open, 0);
    UI.confirm(
      `선택한 ${shops.length}개 상점을 ${f.start} ~ ${f.end} 기간으로 마감하시겠습니까?\n` +
      (openTotal > 0 ? `⚠ 아직 정산자료로 만들지 않은 대상이 ${openTotal}건 있습니다(마감에서 제외됩니다).\n` : '') +
      `마감한 정산은 이 화면에서 되돌릴 수 없습니다.`,
      async close => {
        const btn = document.getElementById('acBtnClose');
        btn.disabled = true;
        try {
          const res = await Api.post('/adjust/process/close', {
            shopCodes: shops.map(r => r.shopCode), startDate: f.start, endDate: f.end,
          });
          close();
          showCloseResult(res);
          load();
        } catch (e) {
          UI.toast(e.message, 'error');
          btn.disabled = false;
        }
      },
    );
  }

  function showCloseResult(res) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="margin-bottom:8px">마감 <strong>${esc(res.success)}</strong>곳 · 실패 <strong style="color:var(--danger)">${esc(res.failed)}</strong>곳</div>
      <div style="max-height:260px;overflow:auto;font-size:12px;border:1px solid var(--border);padding:8px;border-radius:4px">
        ${res.successDetails.map(d => `<div>✅ ${esc(d)}</div>`).join('')}
        ${res.failDetails.map(d => `<div style="color:var(--danger)">❌ ${esc(d)}</div>`).join('')}
      </div>
      <div style="margin-top:8px;font-size:12px;color:var(--text-muted)">마감 결과는 "정산 관리" 화면에서 확인하세요.</div>`;
    UI.modal({ title: '정산 마감 결과', body, confirmText: null, cancelText: '닫기' });
  }

  // ── 정산자료 보기 / 금액 수정 / 생성 취소 ───────────────────────────
  const MAX_DETAIL = 300;

  async function openDetailModal(shop) {
    const f = filters();
    const body = document.createElement('div');
    body.innerHTML = '<div style="padding:30px;text-align:center;color:var(--text-muted)">불러오는 중...</div>';
    UI.modal({ title: `정산자료 — ${shop.shopName || shop.shopCode} (${f.start} ~ ${f.end})`, body, confirmText: null, cancelText: '닫기' });

    const reload = async () => {
      try {
        const params = { pShopCode: shop.shopCode, pStartDate: f.start, pEndDate: f.end, pBuyAdjustCloseDate: 'Y' };
        const [count, list] = await Promise.all([
          Api.get('/adjust/detail/count', params),
          Api.get('/adjust/detail/list', { ...params, pPageOffset: 0, pPageSize: MAX_DETAIL }),
        ]);
        renderDetail(body, list, count, reload);
      } catch (e) {
        body.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
      }
    };
    reload();
  }

  function renderDetail(body, list, count, reload) {
    if (!list.length) {
      body.innerHTML = '<div style="padding:30px;text-align:center;color:var(--text-muted)">마감 전 정산자료가 없습니다</div>';
      return;
    }
    const trs = list.map((d, i) => `
      <tr>
        <td style="font-size:11px">${esc(d.adjustDetailNo)}</td>
        <td style="text-align:center"><span class="badge ${esc(d.prodQty < 0 ? 'badge-gray' : 'badge-blue')}">${esc(d.prodQty < 0 ? '반품(−)' : '배송(+)')}</span></td>
        <td style="font-size:11px">${esc(d.orderNo)}-${esc(d.orderProdNo)}</td>
        <td style="font-size:12px;max-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(d.prodName)}">${esc(d.prodName)}</td>
        <td style="text-align:right;font-size:11px">${esc(d.prodQty)}</td>
        <td style="text-align:right;font-size:11px">${won(d.salePrice)}</td>
        <td style="text-align:right;font-size:11px">${won(d.supplyPrice)}</td>
        <td style="text-align:right;font-size:11px">${won(d.discntSupplyPrice)}</td>
        <td style="text-align:right;font-size:11px">${won(d.buyPrice)}</td>
        <td style="text-align:right;font-size:11px">${esc(d.vatRate)}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 6px;font-size:11px" data-act="edit" data-i="${i}">수정</button>
          <button class="btn btn-danger" style="padding:2px 6px;font-size:11px" data-act="cancel" data-i="${i}">생성취소</button>
        </td>
      </tr>`).join('');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px">
        총 ${Number(count).toLocaleString()}건${count > MAX_DETAIL ? ` (앞의 ${MAX_DETAIL}건만 표시)` : ''} — 반품(−) 행의 금액은 음수입니다.
      </div>
      <div style="max-height:420px;overflow:auto">
        <table style="width:1000px;table-layout:fixed">
          <colgroup><col style="width:56px"><col style="width:70px"><col style="width:100px"><col style="width:220px"><col style="width:44px">
            <col style="width:84px"><col style="width:84px"><col style="width:84px"><col style="width:84px"><col style="width:60px"><col style="width:130px"></colgroup>
          <thead><tr><th>번호</th><th>구분</th><th>주문-순번</th><th>상품명</th><th style="text-align:right">수량</th>
            <th style="text-align:right">판매가</th><th style="text-align:right">공급가</th><th style="text-align:right">할인공급가</th>
            <th style="text-align:right">매입가</th><th style="text-align:right">부가세율</th><th></th></tr></thead>
          <tbody>${trs}</tbody>
        </table>
      </div>`;

    body.querySelectorAll('[data-act=edit]').forEach(btn => btn.addEventListener('click', () => openEdit(list[+btn.dataset.i], reload)));
    body.querySelectorAll('[data-act=cancel]').forEach(btn => btn.addEventListener('click', () => {
      const d = list[+btn.dataset.i];
      UI.confirm(`정산자료 ${d.adjustDetailNo}(주문 ${d.orderNo}-${d.orderProdNo})의 생성을 취소하시겠습니까? 다시 "정산 대상"에 나타납니다.`, async close => {
        try {
          const res = await Api.delete('/adjust/process/detail/' + d.adjustDetailNo);
          if (res.failed > 0) UI.toast(res.failDetails[0] || '취소하지 못했습니다', 'error');
          else UI.toast('생성이 취소되었습니다', 'success');
        } catch (e) { UI.toast(e.message, 'error'); }
        close();
        reload();
        load();
      });
    }));
  }

  function openEdit(d, reload) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
        ${esc(d.prodName)} · 주문 ${esc(d.orderNo)}-${esc(d.orderProdNo)} · ${esc(d.prodQty < 0 ? '반품(−) — 금액은 음수로 입력' : '배송(+)')}
      </div>
      <div class="form-grid">
        <div class="form-group"><label>공급가</label><input class="input" id="acpSupply" type="number" value="${esc(d.supplyPrice ?? 0)}"></div>
        <div class="form-group"><label>할인공급가</label><input class="input" id="acpDiscnt" type="number" value="${esc(d.discntSupplyPrice ?? 0)}"></div>
        <div class="form-group"><label>매입가</label><input class="input" id="acpBuy" type="number" value="${esc(d.buyPrice ?? 0)}"></div>
        <div class="form-group"><label>부가세율(%)</label><input class="input" id="acpVat" type="number" min="0" max="100" value="${esc(d.vatRate ?? 0)}"></div>
      </div>`;
    UI.modal({
      title: '정산자료 금액 수정', confirmText: '저장', body,
      onConfirm: async close => {
        const n = id => Number(document.getElementById(id).value);
        const payload = {
          adjustDetailNo: d.adjustDetailNo,
          supplyPrice: n('acpSupply'), discntSupplyPrice: n('acpDiscnt'), buyPrice: n('acpBuy'), vatRate: n('acpVat'),
        };
        if ([payload.supplyPrice, payload.discntSupplyPrice, payload.buyPrice, payload.vatRate].some(x => !Number.isFinite(x))) {
          UI.toast('금액을 숫자로 입력하세요', 'error'); return;
        }
        try {
          await Api.put('/adjust/process/prices', payload);
          UI.toast('수정되었습니다', 'success');
          close();
          reload();
          load();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

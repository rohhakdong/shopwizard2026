/**
 * 업체(상점·창고) 포털의 "발주 목록" 카드 — 자기 발주 목록을 보고 확정된 발주서(.xls)를 받는다.
 *   상점 포털: VendorSheets.mount(el, { base: '/vendor/shop' })
 *   창고 포털: VendorSheets.mount(el, { base: '/vendor/warehs', withShops: true })  — 연결된 상점 드롭다운
 * 서버가 세션으로 허용 상점을 정하므로 여기서 보내는 상점 값은 좁히는 용도일 뿐이다.
 * 일반 fetch 만 쓴다(포털마다 api.js 유무가 달라서).
 */
const VendorSheets = (() => {

  const PAGE_SIZE = 10;

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  async function getJson(url) {
    const res = await fetch(url, { credentials: 'same-origin' });
    if (!res.ok) {
      let msg = '';
      try { msg = JSON.parse(await res.text()).message; } catch (_) {}
      throw new Error(msg || `HTTP ${res.status}`);
    }
    return res.json();
  }

  function mount(el, opts) {
    const base = opts.base;
    let page = 1;
    let total = 0;

    el.innerHTML = `
      <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
        ${opts.withShops ? '<div class="form-group"><label>상점</label><select class="input" id="vsShop"><option value="">전체</option></select></div>' : ''}
        <div class="form-group"><label>시작일</label><input class="input" type="date" id="vsStart"></div>
        <div class="form-group"><label>종료일</label><input class="input" type="date" id="vsEnd"></div>
        <button class="btn btn-primary" id="vsSearch" type="button">검색</button>
      </div>
      <div id="vsMeta" style="margin:8px 0;color:var(--text-muted);font-size:13px"></div>
      <div class="table-wrap" id="vsWrap"></div>
      <div id="vsPager" style="margin-top:10px;display:flex;gap:8px;align-items:center"></div>`;

    const q = id => el.querySelector('#' + id);
    q('vsSearch').addEventListener('click', () => { page = 1; load(); });

    async function loadShops() {
      if (!opts.withShops) return;
      try {
        const shops = await getJson(base + '/ship-check/shops');
        q('vsShop').insertAdjacentHTML('beforeend', shops.map(s => `<option value="${esc(s.shopCode)}">${esc(s.shopName)}</option>`).join(''));
      } catch (_) { /* 드롭다운 없이도 목록은 동작 */ }
    }

    async function load() {
      const wrap = q('vsWrap');
      wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted)">불러오는 중...</div>';
      try {
        const p = new URLSearchParams({ pPageOffset: (page - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
        if (opts.withShops && q('vsShop').value) p.set('pShopCode', q('vsShop').value);
        if (q('vsStart').value && q('vsEnd').value) { p.set('pStartDate', q('vsStart').value); p.set('pEndDate', q('vsEnd').value); }
        const r = await getJson(base + '/ship-check/list?' + p.toString());
        total = r.total || 0;
        q('vsMeta').textContent = `총 ${total.toLocaleString()}건 (발주확인이 끝난 발주서만 표시됩니다)`;
        if (!r.rows.length) { wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted)">발주 목록이 없습니다.</div>'; renderPager(); return; }
        wrap.innerHTML = `
          <table style="width:100%;font-size:13px">
            <thead><tr><th>번호</th>${opts.withShops ? '<th>상점</th>' : ''}<th>발주서</th><th>확인 시각</th><th style="text-align:right">주문 수</th><th></th></tr></thead>
            <tbody>${r.rows.map(h => `
              <tr><td>${esc(h.shipCheckNo)}</td>${opts.withShops ? `<td>${esc(h.shopName || h.shopCode)}</td>` : ''}
                <td>${esc(h.shipCheckDesc)}</td><td>${esc(h.shipCheckDate)}</td><td style="text-align:right">${esc(h.orderCount)}</td>
                <td><button class="btn btn-ghost" data-no="${esc(h.shipCheckNo)}" type="button" style="padding:2px 8px;font-size:12px">발주서 받기</button></td></tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('[data-no]').forEach(b => b.addEventListener('click', () => download(b.dataset.no, b)));
        renderPager();
      } catch (e) {
        wrap.innerHTML = `<div style="padding:16px;color:var(--danger)">${esc(e.message)}</div>`;
      }
    }

    function renderPager() {
      const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
      q('vsPager').innerHTML = `<button class="btn btn-ghost" id="vsPrev" type="button" ${page <= 1 ? 'disabled' : ''}>이전</button>
        <span style="font-size:13px">${page} / ${pages}</span>
        <button class="btn btn-ghost" id="vsNext" type="button" ${page >= pages ? 'disabled' : ''}>다음</button>`;
      q('vsPrev').addEventListener('click', () => { page--; load(); });
      q('vsNext').addEventListener('click', () => { page++; load(); });
    }

    async function download(no, btn) {
      btn.disabled = true;
      try {
        const res = await fetch(base + '/ship-check/excel?pShipCheckNo=' + encodeURIComponent(no), { credentials: 'same-origin' });
        if (!res.ok) {
          let msg = '';
          try { msg = JSON.parse(await res.text()).message; } catch (_) {}
          throw new Error(msg || `HTTP ${res.status}`);
        }
        const blob = await res.blob();
        let name = `발주서_${no}.xls`;
        const m = /filename\*=UTF-8''([^;]+)/i.exec(res.headers.get('Content-Disposition') || '');
        if (m) { try { name = decodeURIComponent(m[1]); } catch (_) {} }
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(a.href), 1000);
      } catch (e) {
        alert(e.message);
      } finally {
        btn.disabled = false;
      }
    }

    loadShops().then(load);
  }

  return { mount };
})();

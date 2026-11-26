/**
 * 발주내역 (사이드 메뉴 발주) — 레거시 발주내역(wShpShipDirectPrintList). 상점의 발주서 행(tShpShipDirectPrint)을
 *   발주일자 범위로 보고, 엑셀(출고지별 시트)로 받는다. 상점은 필수. 수령인·주소·전화번호가 나오므로
 *   조회·엑셀을 받을 때마다 개인정보 조회 기록이 남는다(서버가 기록).
 *   백엔드: /ship/print (list, excel)
 */
const PageShipPrintList = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 50;
  let currentPage = 1;
  let seq = 0;   // 늦게 도착한 이전 요청의 응답이 최신 결과를 덮어쓰지 않게 하는 순번

  const COLS = [
    ['주문번호', r => r.orderNo], ['순번', r => r.orderProdNo], ['변경', r => r.orderChangeNo], ['채널주문번호', r => r.chnlOrderNo],
    ['주문일자', r => r.orderReciptDate], ['주문자', r => r.orderName], ['상품코드', r => r.prodCode], ['상품명', r => r.prodName],
    ['속성명', r => r.itemName], ['수량', r => num(r.prodQty), 'n'], ['금액', r => num(r.amount), 'n'],
    ['수령인', r => r.recverName], ['전화', r => r.recverPhoneNo], ['핸드폰', r => r.recverMobileNo], ['우편번호', r => r.recverZipcode],
    ['주소', r => r.recverAddr], ['주문메모', r => r.orderMemo], ['전달메모', r => r.deliMemo],
    ['공급단가', r => num(r.unitPrice), 'n'], ['공급소계', r => num(r.subTotal), 'n'], ['배송비', r => num(r.deliFeeAmt), 'n'],
    ['배송유형', r => r.deliFeeType], ['주문상태', r => r.orderState], ['공급자명', r => r.shopName], ['채널명', r => r.chnlName],
    ['발주일자', r => (r.shipCheckDate || '').slice(0, 16)], ['출고지', r => r.shipPlace], ['합배송', r => (r.bundleYn ? '합배송' : '')],
  ];

  function num(n) { return Number(n) || 0; }
  function today() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function el(id) { return document.getElementById(id); }

  function render(container) {
    currentPage = 1;
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>발주내역 <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— 상점의 발주서 행 (발주일자 기준)</span></span>
          <span>
            <button class="btn btn-ghost" id="plBtnRefresh" type="button">새로고침</button>
            <button class="btn btn-primary" id="plBtnExcel" type="button">엑셀 받기</button>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">
            수령인·주소·전화번호가 표시되는 화면입니다. 조회와 엑셀 받기는 <b>개인정보 조회 기록</b>으로 남습니다.
            발주일자 시작·종료를 비우면 기간 없이 전체를 봅니다(발주일이 아직 없는 행은 기간을 넣으면 빠집니다).
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상점 *</label>
              <select class="input" id="plShop" style="width:200px"><option value="">선택</option></select></div>
            <div class="form-group"><label>발주일자 시작</label><input class="input" id="plStart" type="date" style="width:140px" value="${today()}"></div>
            <div class="form-group"><label>종료</label><input class="input" id="plEnd" type="date" style="width:140px" value="${today()}"></div>
            <button class="btn btn-primary" id="plBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="plTotal">0</strong>건</div>
          <div class="table-wrap" id="plWrap"><div style="text-align:center;padding:40px;color:var(--text-muted)">상점을 선택하고 검색하세요</div></div>
          <div id="plPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    el('plBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    el('plBtnRefresh').addEventListener('click', () => loadList());
    el('plBtnExcel').addEventListener('click', () => {
      if (!el('plShop').value) return UI.toast('상점을 선택하세요', 'error');
      download('/ship/print/excel', params());
    });
    el('plShop').addEventListener('change', () => { currentPage = 1; if (el('plShop').value) loadList(); });
    loadShops();
  }

  async function loadShops() {
    const { shops } = await UI.loadFilterOptions(svcCode());
    UI.fillOptions('plShop', shops.map(s => ({ v: s.shopCode, t: s.shopName })));
  }

  function params() {
    return { pShopCode: el('plShop').value, pShipCheckStartDate: el('plStart').value, pShipCheckEndDate: el('plEnd').value };
  }

  async function loadList() {
    const wrap = el('plWrap');
    if (!el('plShop').value) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">상점을 선택하세요</div>'; return; }
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    el('plPage').innerHTML = '';
    const mine = ++seq;
    try {
      const res = await Api.get('/ship/print/list', { ...params(), pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      if (mine !== seq) return;
      el('plTotal').textContent = Number(res.total).toLocaleString();
      if (!res.total) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>'; return; }
      wrap.innerHTML = `
        <table style="width:100%;font-size:12px;white-space:nowrap">
          <thead><tr>${COLS.map(c => `<th${c[2] === 'n' ? ' style="text-align:right"' : ''}>${c[0]}</th>`).join('')}</tr></thead>
          <tbody>${res.rows.map(r => `<tr${r.bundleYn ? ' style="background:var(--bg-subtle, #fafaf0)"' : ''}>${COLS.map(c => {
            const v = c[1](r);
            return c[2] === 'n' ? `<td style="text-align:right">${Number(v).toLocaleString()}</td>` : `<td>${esc(v)}</td>`;
          }).join('')}</tr>`).join('')}</tbody>
        </table>`;
      UI.pagination(el('plPage'), { total: res.total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      if (mine !== seq) return;
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  /** 엑셀 내려받기(fetch → blob). 발주내역2 화면도 같이 쓴다. */
  async function download(url, p) {
    try {
      const qs = new URLSearchParams(Object.entries(p).filter(([, v]) => v !== '' && v != null)).toString();
      const res = await fetch(url + '?' + qs, { credentials: 'same-origin' });
      if (!res.ok) {
        let msg = '';
        try { msg = JSON.parse(await res.text()).message; } catch (_) {}
        if (res.status === 403) msg = '이 기능을 사용할 권한이 없습니다.';
        throw new Error(msg || `HTTP ${res.status}`);
      }
      const blob = await res.blob();
      let name = '발주내역.xls';
      const m = /filename\*=UTF-8''([^;]+)/i.exec(res.headers.get('Content-Disposition') || '');
      if (m) { try { name = decodeURIComponent(m[1]); } catch (_) {} }
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
      UI.toast('엑셀을 내려받았습니다', 'success');
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  return { render, download };
})();

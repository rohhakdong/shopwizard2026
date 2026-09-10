/**
 * 판매 상품 관리 (메뉴 라벨 기준. 구 "상품 관리")
 *   shopion.tPrdProd — "상품 등록/승인"(catalog-prod.js)에서 승인되어 실제 판매 중인 상품
 * - 목록 조회 (페이지네이션)
 * - 상세보기 (읽기 전용)
 * - 판매여부 변경 / 재고수량 변경 / 삭제
 */
const PageProductProd = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList    = [];

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">판매 상품 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="flex-wrap:wrap;gap:8px">
            <div class="form-group">
              <label>상품코드</label>
              <input class="input" id="psProdCode" placeholder="상품/쇼핑몰 코드" style="width:160px">
            </div>
            <div class="form-group">
              <label>상품명</label>
              <input class="input" id="psProdName" placeholder="상품명" style="width:180px">
            </div>
            <div class="form-group">
              <label>브랜드</label>
              <input class="input" id="psBrandName" placeholder="브랜드명" style="width:130px">
            </div>
            <div class="form-group">
              <label>쇼핑몰</label>
              <select class="input" id="psShopCode" style="width:150px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>판매여부</label>
              <select class="input" id="psSaleYn" style="width:90px">
                <option value="">전체</option>
                <option value="1">판매</option>
                <option value="0">미판매</option>
              </select>
            </div>
            <div class="form-group">
              <label>재고</label>
              <select class="input" id="psSupplyQty" style="width:90px">
                <option value="">전체</option>
                <option value="1">재고있음</option>
              </select>
            </div>
            <button class="btn btn-primary" id="psBtnSearch" style="margin-top:18px">검색</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="psTotalCountLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="psTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="psPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('psBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['psProdCode','psProdName','psBrandName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });

    loadShopList().then(() => { currentPage = 1; loadList(); });
  }

  // ── 쇼핑몰 목록 ────────────────────────────────────────────────────
  async function loadShopList() {
    try {
      shopList = await Api.get('/company/shop', {});
      const sel = document.getElementById('psShopCode');
      if (!sel) return;
      shopList.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.shopCode;
        opt.textContent = s.shopName || s.shopCode;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pProdCode:   document.getElementById('psProdCode').value.trim(),
      pProdName:   document.getElementById('psProdName').value.trim(),
      pBrandName:  document.getElementById('psBrandName').value.trim(),
      pShopCode:   document.getElementById('psShopCode').value,
      pSaleYn:     document.getElementById('psSaleYn').value,
      pSupplyQty:  document.getElementById('psSupplyQty').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('psTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('psPagination').innerHTML = '';

    try {
      const params = getParams();

      totalCount = await Api.get('/product/prod/count', params);
      document.getElementById('psTotalCountLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/product/prod/list', {
        ...params,
        pPageOffset: (currentPage - 1) * PAGE_SIZE,
        pPageSize:   PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('psTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(p => `
      <tr>
        <td style="font-size:11px;${ell}">
          <code>${p.prodCode || ''}</code><br>
          <span style="color:var(--text-muted)">${p.shopProdCode || ''}</span>
        </td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${p.prodName || ''}">${p.prodName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${p.brandName || ''}">${p.brandName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${p.shopName || ''}">${p.shopName || ''}</td>
        <td style="font-size:12px;${ell}" title="${p.supplyName || ''}">${p.supplyName || ''}</td>
        <td style="text-align:right;font-size:13px;font-weight:500">
          ${p.salePrice != null ? p.salePrice.toLocaleString() + '원' : '-'}
        </td>
        <td style="text-align:right;font-size:13px">
          ${p.supplyQty != null ? p.supplyQty.toLocaleString() : '-'}
        </td>
        <td style="text-align:center">
          <span class="badge ${p.saleYn === 1 ? 'badge-green' : 'badge-gray'}">${p.saleYn === 1 ? '판매' : '미판매'}</span>
        </td>
        <td style="font-size:11px;color:var(--text-muted);${ell}">${p.registDate ? p.registDate.substring(0,10) : ''}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="detail" data-code="${p.prodCode}">상세</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:${p.saleYn === 1 ? 'var(--danger)' : 'var(--primary)'}"
            data-action="saleyn" data-code="${p.prodCode}" data-yn="${p.saleYn}">
            ${p.saleYn === 1 ? '판매중지' : '판매시작'}
          </button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="qty" data-code="${p.prodCode}" data-qty="${p.supplyQty ?? 0}">재고</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${p.prodCode}" data-name="${p.prodName}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("상품명")은 지정된 열들의 폭 합계가
    // 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블 자체는
    // width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:955px">
        <colgroup>
          <col style="width:130px"><col style="width:200px"><col style="width:90px"><col style="width:90px">
          <col style="width:90px"><col style="width:70px"><col style="width:55px">
          <col style="width:80px"><col style="width:150px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">상품코드</th><th style="${thEll}">상품명</th><th style="${thEll}">쇼핑몰</th><th style="${thEll}">협력사</th>
            <th style="${thEll};text-align:right">판매가</th>
            <th style="${thEll};text-align:right">재고</th>
            <th style="${thEll};text-align:center">판매</th>
            <th style="${thEll}">등록일</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 상세
    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const prod = await Api.get('/product/prod', { pProdCode: btn.dataset.code });
          openDetailModal(Array.isArray(prod) ? prod[0] : prod);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    // 판매여부 변경
    wrap.querySelectorAll('[data-action=saleyn]').forEach(btn => {
      btn.addEventListener('click', () => {
        const newYn = btn.dataset.yn === '1' ? 0 : 1;
        const label = newYn === 1 ? '판매 시작' : '판매 중지';
        UI.confirm(`[${btn.dataset.code}] ${label} 처리하시겠습니까?`, async close => {
          try {
            await Api.put('/product/prod/saleyn', { prodCode: btn.dataset.code, saleYn: newYn });
            UI.toast(`${label} 처리되었습니다`, 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });

    // 재고수량 변경
    wrap.querySelectorAll('[data-action=qty]').forEach(btn => {
      btn.addEventListener('click', () => {
        const input = document.createElement('div');
        input.innerHTML = `
          <div class="form-group" style="margin:8px 0">
            <label>재고수량</label>
            <input class="input" id="qtyInput" type="number" min="0" value="${btn.dataset.qty}" style="width:120px">
          </div>`;
        UI.modal({
          title: `재고 수량 변경 – ${btn.dataset.code}`,
          body: input,
          confirmText: '변경',
          onConfirm: async close => {
            const qty = parseInt(document.getElementById('qtyInput').value);
            if (isNaN(qty) || qty < 0) { UI.toast('올바른 수량을 입력하세요', 'error'); return; }
            try {
              await Api.put('/product/prod/supplyqty', { prodCode: btn.dataset.code, supplyQty: qty });
              UI.toast('재고수량이 변경되었습니다', 'success');
              loadList();
            } catch (e) { UI.toast(e.message, 'error'); }
            close();
          },
        });
      });
    });

    // 삭제
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상품을 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/product/prod', { prodCode: btn.dataset.code });
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 페이지네이션 ───────────────────────────────────────────────────
  function renderPagination() {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    if (totalPages <= 1) return;

    const el = document.getElementById('psPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0)
      html += `<button class="page-btn" data-page="${start - 1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages)
      html += `<button class="page-btn" data-page="${end + 1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentPage = parseInt(btn.dataset.page);
        loadList();
      });
    });
  }

  // ── 상세 모달 (읽기 전용) ──────────────────────────────────────────
  function openDetailModal(p) {
    if (!p) { UI.toast('상품 정보를 불러올 수 없습니다', 'error'); return; }

    const row = (label, value, full = false) => `
      <div class="form-group ${full ? 'full' : ''}">
        <label>${label}</label>
        <div class="input" style="background:#f8fafc;color:var(--text);min-height:34px;line-height:34px;padding:0 10px;border-radius:var(--radius);border:1px solid var(--border)">${value ?? '-'}</div>
      </div>`;

    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">기본 정보</div>
      <div class="form-grid" style="margin-bottom:16px">
        ${row('상품코드', p.prodCode)}
        ${row('쇼핑몰상품코드', p.shopProdCode)}
        ${row('상품명', p.prodName, true)}
        ${row('쇼핑몰', p.shopName)}
        ${row('협력사', p.supplyName)}
        ${row('브랜드', p.brandName)}
        ${row('모델명', p.modelName || '-')}
        ${row('성인상품', p.adultYn === 1 ? '예' : '아니오')}
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">가격 / 재고</div>
      <div class="form-grid" style="margin-bottom:16px">
        ${row('정가', p.listPrice != null ? p.listPrice.toLocaleString() + '원' : null)}
        ${row('판매가', p.salePrice != null ? p.salePrice.toLocaleString() + '원' : null)}
        ${row('공급가', p.supplyPrice != null ? p.supplyPrice.toLocaleString() + '원' : null)}
        ${row('재고수량', p.supplyQty != null ? p.supplyQty.toLocaleString() + '개' : null)}
        ${row('리드타임', p.leadTime != null ? p.leadTime + '일' : null)}
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">배송 / 판매</div>
      <div class="form-grid">
        ${row('판매여부', p.saleYn === 1 ? '판매' : '미판매')}
        ${row('배송비유형', p.deliFeeType || '-')}
        ${row('등록일', p.registDate ? p.registDate.substring(0,10) : null)}
        ${row('등록자', p.registName || '-')}
      </div>`;

    UI.modal({
      title: `상품 상세 – ${p.prodCode}`,
      confirmText: null,
      cancelText: '닫기',
      body,
    });
  }

  return { render };
})();

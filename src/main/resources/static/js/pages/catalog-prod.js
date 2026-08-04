/**
 * 카탈로그 상품 관리
 * - 목록 조회 (페이지네이션)
 * - 상세보기 (읽기 전용)
 * - 승인 / 삭제
 */
const PageCatalogProd = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList    = [];

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">카탈로그 상품</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="flex-wrap:wrap;gap:8px">
            <div class="form-group">
              <label>상품코드</label>
              <input class="input" id="sProdCode" placeholder="상품/쇼핑몰/IMPA 코드" style="width:180px">
            </div>
            <div class="form-group">
              <label>상품명</label>
              <input class="input" id="sProdName" placeholder="상품명" style="width:180px">
            </div>
            <div class="form-group">
              <label>쇼핑몰</label>
              <select class="input" id="sShopCode" style="width:160px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>판매여부</label>
              <select class="input" id="sSaleYn" style="width:90px">
                <option value="">전체</option>
                <option value="1">판매</option>
                <option value="0">미판매</option>
              </select>
            </div>
            <div class="form-group">
              <label>승인</label>
              <select class="input" id="sApprovDate" style="width:90px">
                <option value="">전체</option>
                <option value="NOTNULL">승인</option>
                <option value="NULL">미승인</option>
              </select>
            </div>
            <button class="btn btn-primary" id="btnSearch" style="margin-top:18px">검색</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="totalCountLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="prodTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="prodPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('btnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['sProdCode','sProdName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });

    loadShopList().then(() => { currentPage = 1; loadList(); });
  }

  // ── 쇼핑몰 목록 ────────────────────────────────────────────────────
  async function loadShopList() {
    try {
      shopList = await Api.get('/company/shop', {});
      const sel = document.getElementById('sShopCode');
      shopList.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.shopCode;
        opt.textContent = s.shopName || s.shopCode;
        sel.appendChild(opt);
      });
    } catch (_) {}
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  function getParams() {
    return {
      pProdCode:   document.getElementById('sProdCode').value.trim(),
      pProdName:   document.getElementById('sProdName').value.trim(),
      pShopCode:   document.getElementById('sShopCode').value,
      pSaleYn:     document.getElementById('sSaleYn').value,
      pApprovDate: document.getElementById('sApprovDate').value,
    };
  }

  async function loadList() {
    const wrap = document.getElementById('prodTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('prodPagination').innerHTML = '';

    try {
      const params = getParams();

      // 건수 조회
      totalCount = await Api.get('/catalog/prod/count', params);
      document.getElementById('totalCountLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      // 목록 조회
      const list = await Api.get('/catalog/prod/list', {
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
    const wrap = document.getElementById('prodTableWrap');

    const rows = list.map(p => `
      <tr>
        <td style="font-size:11px">
          <code>${p.prodCode || ''}</code><br>
          <span style="color:var(--text-muted)">${p.shopProdCode || ''}</span>
        </td>
        <td>
          <div style="font-weight:500;font-size:13px">${p.prodName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted)">${p.brandName || ''}</div>
        </td>
        <td style="font-size:12px">${p.shopName || ''}</td>
        <td style="font-size:12px">${p.cateName || ''}</td>
        <td style="text-align:right;font-size:13px;font-weight:500">
          ${p.salePrice != null ? p.salePrice.toLocaleString() + '원' : '-'}
        </td>
        <td style="text-align:center">
          <span class="badge ${p.saleYn === 1 ? 'badge-green' : 'badge-gray'}">${p.saleYn === 1 ? '판매' : '미판매'}</span>
        </td>
        <td style="text-align:center">
          ${p.approvDate
            ? `<span class="badge badge-blue" title="${p.approvDate}">승인</span>`
            : `<span class="badge badge-gray">미승인</span>`}
        </td>
        <td style="font-size:11px;color:var(--text-muted)">${p.registDate ? p.registDate.substring(0, 10) : ''}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="detail" data-code="${p.prodCode}">상세</button>
          ${!p.approvDate ? `<button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--primary)"
            data-action="approv" data-code="${p.prodCode}" data-name="${p.prodName}">승인</button>` : ''}
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${p.prodCode}" data-name="${p.prodName}">삭제</button>
        </td>
      </tr>`).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:100%">
        <colgroup>
          <col style="width:130px"><col><col style="width:90px"><col style="width:90px">
          <col style="width:90px"><col style="width:55px"><col style="width:55px">
          <col style="width:80px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th>상품코드</th><th>상품명</th><th>쇼핑몰</th><th>카테고리</th>
            <th style="text-align:right">판매가</th><th style="text-align:center">판매</th>
            <th style="text-align:center">승인</th><th>등록일</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 상세
    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const prod = await Api.get('/catalog/prod/' + btn.dataset.code);
          openDetailModal(prod);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    // 승인
    wrap.querySelectorAll('[data-action=approv]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상품을 승인하시겠습니까?`, async close => {
          try {
            await Api.put('/catalog/prod/approv', {
              prodCode:   btn.dataset.code,
              approvId:   info?.loginId,
              approvName: info?.name,
            });
            UI.toast('승인되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });

    // 삭제
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상품을 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/catalog/prod', { prodCode: btn.dataset.code });
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

    const el = document.getElementById('prodPagination');
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
        ${row('진열명', p.prodDsplName, true)}
        ${row('쇼핑몰', p.shopName)}
        ${row('카테고리', p.cateName)}
        ${row('브랜드', p.brandName)}
        ${row('제조사', p.makerName)}
        ${row('원산지', p.originName)}
        ${row('모델명', p.modelName)}
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">가격 정보</div>
      <div class="form-grid" style="margin-bottom:16px">
        ${row('정가', p.listPrice != null ? p.listPrice.toLocaleString() + '원' : null)}
        ${row('판매가', p.salePrice != null ? p.salePrice.toLocaleString() + '원' : null)}
        ${row('공급가', p.supplyPrice != null ? p.supplyPrice.toLocaleString() + '원' : null)}
        ${row('할인가', p.discntPrice != null ? p.discntPrice.toLocaleString() + '원' : null)}
        ${row('할인기간', p.discntStartDate ? `${p.discntStartDate} ~ ${p.discntEndDate}` : null)}
        ${row('VAT율', p.vatRate != null ? p.vatRate + '%' : null)}
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">배송 / 판매</div>
      <div class="form-grid">
        ${row('판매여부', p.saleYn === 1 ? '판매' : '미판매')}
        ${row('승인일', p.approvDate || '미승인')}
        ${row('배송방법', p.deliMethod)}
        ${row('배송비유형', p.deliFeeType)}
        ${row('리드타임', p.leadTime != null ? p.leadTime + '일' : null)}
        ${row('성인상품', p.adultYn === 1 ? '예' : '아니오')}
        ${row('등록일', p.registDate ? p.registDate.substring(0,10) : null)}
        ${row('등록자', p.registName)}
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

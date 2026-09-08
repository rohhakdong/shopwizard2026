/**
 * 카탈로그 상품 관리 (tCatProd — shopwizard 스키마, 승인 워크플로우의 시작점)
 * - 목록 조회 (페이지네이션) / 신규 등록 / 수정 / 상세보기(읽기 전용) / 승인 / 삭제
 * - 승인된 상품은 별도 API(copy2shopion, 이 화면에는 노출 안 함)로 실제 판매 스키마
 *   (shopion.tPrdProd, "상품 관리"/product-prod.js 화면)로 복사되는 구조로 보인다.
 * - 브랜드/제조사/원산지는 tCatProd에 자유 텍스트로 저장되며 tCatBrand/Maker/Origin
 *   마스터와 DB상 FK로 연결돼 있지 않다 — 카테고리(CateCode)만 실제 FK. 값 자체는
 *   여전히 텍스트로 저장되지만, 선택 실수를 줄이도록 마스터 관리 화면에 등록된 이름만
 *   고를 수 있는 select로 제공한다(자유 입력은 막고 마스터 등록을 먼저 하도록 유도).
 * - 부가세율(VatRate), 배송비유형(DeliFeeType), 판매여부(SaleYn)는 공통코드
 *   (wizardn.tCodConstrVal의 cTaxType/cDeliFeeType/cSaleYnType)를 select 옵션으로
 *   사용한다. cTaxType은 코드값 자체가 세율(%) 문자열(0=면세, 10=과세)이라 그대로
 *   VatRate에 저장해도 의미가 맞아떨어진다.
 */
const PageCatalogProd = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let shopList    = [];
  let brandOptions   = [];
  let makerOptions   = [];
  let originOptions  = [];
  let taxTypeOptions = [];
  let deliFeeTypeOptions = [];
  let saleYnTypeOptions  = [];

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
              <label>상점</label>
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
            <button class="btn btn-ghost" id="btnNew" style="margin-top:18px">+ 신규</button>
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
    document.getElementById('btnNew').addEventListener('click', () => openEditModal(null));
    ['sProdCode','sProdName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });

    loadShopList().then(() => { currentPage = 1; loadList(); });
    loadMasterOptions();
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

  // 브랜드/제조사/원산지 마스터 + 부가세율/배송비유형 공통코드 — 등록/수정 모달의
  // select 옵션용 (전체를 한 번에 불러온다).
  async function loadMasterOptions() {
    try { brandOptions  = await Api.get('/catalog/brand/list',  { pPageOffset: 0, pPageSize: 2000 }); } catch (_) { brandOptions  = []; }
    try { makerOptions  = await Api.get('/catalog/maker/list',  { pPageOffset: 0, pPageSize: 2000 }); } catch (_) { makerOptions  = []; }
    try { originOptions = await Api.get('/catalog/origin/list', { pPageOffset: 0, pPageSize: 2000 }); } catch (_) { originOptions = []; }
    // cTaxType 코드값 자체가 세율(%) 문자열이다 (0=면세, 10=과세 — 국세청 부가세율 체계와
    // 일치). ConstrValSeq 오름차순 정렬로 받아온다.
    try { taxTypeOptions     = await Api.get('/code/constr-val', { pConstrCode: 'cTaxType',     sidx: 'ConstrValSeq', sord: 'ASC' }); } catch (_) { taxTypeOptions     = []; }
    try { deliFeeTypeOptions = await Api.get('/code/constr-val', { pConstrCode: 'cDeliFeeType', sidx: 'ConstrValSeq', sord: 'ASC' }); } catch (_) { deliFeeTypeOptions = []; }
    try { saleYnTypeOptions  = await Api.get('/code/constr-val', { pConstrCode: 'cSaleYnType',  sidx: 'ConstrValSeq', sord: 'ASC' }); } catch (_) { saleYnTypeOptions  = []; }
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
        <td style="font-size:12px;${ell}" title="${p.cateName || ''}">${p.cateName || ''}</td>
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
        <td style="font-size:11px;color:var(--text-muted);${ell}">${p.registDate ? p.registDate.substring(0, 10) : ''}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="detail" data-code="${p.prodCode}">상세</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${p.prodCode}">수정</button>
          ${!p.approvDate ? `<button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--primary)"
            data-action="approv" data-code="${p.prodCode}" data-name="${p.prodName}">승인</button>` : ''}
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
      <table style="table-layout:fixed;width:890px">
        <colgroup>
          <col style="width:130px"><col style="width:200px"><col style="width:90px"><col style="width:90px">
          <col style="width:90px"><col style="width:55px"><col style="width:55px">
          <col style="width:80px"><col style="width:100px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">상품코드</th><th style="${thEll}">상품명</th><th style="${thEll}">상점</th><th style="${thEll}">카테고리</th>
            <th style="${thEll};text-align:right">판매가</th><th style="${thEll};text-align:center">판매</th>
            <th style="${thEll};text-align:center">승인</th><th style="${thEll}">등록일</th><th></th>
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

    // 수정
    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const prod = await Api.get('/catalog/prod/' + btn.dataset.code);
          openEditModal(prod);
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
        ${p.imgUrl ? `
        <div class="form-group full">
          <label>대표이미지</label>
          <img src="${p.imgUrl100 || p.imgUrl}" style="width:100px;height:100px;object-fit:cover;border:1px solid var(--border);border-radius:var(--radius)">
        </div>` : ''}
        ${row('상점', p.shopName)}
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
        ${row('부가세율', p.vatRate != null ? p.vatRate + '%' : null)}
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">배송 / 판매</div>
      <div class="form-grid" style="margin-bottom:16px">
        ${row('판매여부', saleYnTypeOptions.find(s => s.constrVal === String(p.saleYn))?.constrValDesc ?? p.saleYn)}
        ${row('승인일', p.approvDate || '미승인')}
        ${row('배송방법', p.deliMethod)}
        ${row('배송비유형', p.deliFeeType)}
        ${row('리드타임', p.leadTime != null ? p.leadTime + '일' : null)}
        ${row('성인상품', p.adultYn === 1 ? '예' : '아니오')}
        ${row('등록일', p.registDate ? p.registDate.substring(0,10) : null)}
        ${row('등록자', p.registName)}
      </div>
      ${p.prodDesc ? `
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;text-transform:uppercase;letter-spacing:.05em">상품설명</div>
      <div style="border:1px solid var(--border);border-radius:var(--radius);padding:12px;background:#fff">${p.prodDesc}</div>` : ''}`;

    UI.modal({
      title: `상품 상세 – ${p.prodCode}`,
      confirmText: null,
      cancelText: '닫기',
      body,
    });
  }

  // ── 카테고리 검색 모달 ─────────────────────────────────────────────
  // 카테고리는 대/중/소/세 4단 트리라 목록 전체를 select에 넣을 수 없다. 이미 준비돼 있던
  // GET /catalog/cate/list-final-table(리프 카테고리를 "대>중>소>세" 경로 포함해서 검색하는
  // 전용 쿼리 — catalog-cate.js가 만들어지기 전부터 백엔드에 있었지만 어디서도 안 쓰이고
  // 있었다)를 이용해 이름으로 검색한 뒤 하나를 선택하는 방식으로 구현한다.
  function openCateSearchModal(svcCode, onSelect) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="search-bar" style="margin-bottom:10px">
        <div class="form-group full">
          <label>분류명 검색</label>
          <input class="input" id="cateSearchKeyword" placeholder="분류명을 입력하세요 (대/중/소/세분류 통합 검색)">
        </div>
        <button type="button" class="btn btn-primary" id="cateSearchBtn" style="margin-top:18px">검색</button>
      </div>
      <div id="cateSearchResult" style="max-height:320px;overflow-y:auto;border:1px solid var(--border);border-radius:8px">
        <div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">검색어를 입력하세요</div>
      </div>`;

    async function runSearch() {
      const keyword = document.getElementById('cateSearchKeyword').value.trim();
      const resultEl = document.getElementById('cateSearchResult');
      if (!keyword) { resultEl.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">검색어를 입력하세요</div>'; return; }
      resultEl.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">검색 중...</div>';
      try {
        const list = await Api.get('/catalog/cate/list-final-table', { pSvcCode: svcCode, pCateName: keyword });
        if (!list.length) {
          resultEl.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:13px">일치하는 분류가 없습니다</div>';
          return;
        }
        resultEl.innerHTML = list.map(c => {
          const path = [c.cateName1, c.cateName2, c.cateName3, c.cateName4].filter(Boolean).join(' > ');
          return `<div class="cate-pick" data-code="${c.cateCode}" data-path="${path.replace(/"/g,'&quot;')}"
                    style="padding:8px 12px;cursor:pointer;border-bottom:1px solid var(--border);font-size:13px">${path}</div>`;
        }).join('');
        resultEl.querySelectorAll('.cate-pick').forEach(el => {
          el.addEventListener('click', () => {
            onSelect(el.dataset.code, el.dataset.path);
            close();
          });
          el.addEventListener('mouseenter', () => el.style.background = '#f8fafc');
          el.addEventListener('mouseleave', () => el.style.background = '');
        });
      } catch (e) {
        resultEl.innerHTML = `<div style="padding:16px;color:var(--danger)">${e.message}</div>`;
      }
    }

    const { close } = UI.modal({
      title: '분류 선택',
      body,
      confirmText: null,
      cancelText: '닫기',
    });
    document.getElementById('cateSearchBtn').addEventListener('click', runSearch);
    document.getElementById('cateSearchKeyword').addEventListener('keydown', e => { if (e.key === 'Enter') runSearch(); });
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  async function openEditModal(prod) {
    const isNew = !prod;
    const v = prod || {};
    let selectedCateCode = v.cateCode || '';
    let selectedCatePath = v.cateName || '';

    // 대표이미지: 서버가 업로드된 원본으로부터 만들어준 6단계 썸네일 URL을 여기 모아뒀다가
    // 저장 시 그대로 payload에 실어 보낸다 (사용자가 직접 URL을 입력하지 않는다).
    let imgUrls = {
      imgUrl: v.imgUrl || '', imgUrl50: v.imgUrl50 || '', imgUrl80: v.imgUrl80 || '', imgUrl100: v.imgUrl100 || '',
      imgUrl160: v.imgUrl160 || '', imgUrl220: v.imgUrl220 || '', imgUrl280: v.imgUrl280 || '',
    };

    // 상품코드 자동 채번: PK가 순번 숫자(char(10))라 목록을 ProdCode 내림차순 1건만 조회해
    // 최댓값+1을 계산한다 (전용 /max 엔드포인트가 따로 없어 기존 목록 API를 재사용).
    let newProdCode = '';
    if (isNew) {
      try {
        const maxList = await Api.get('/catalog/prod/list', { pPageOffset: 0, pPageSize: 1 });
        const maxCode = maxList[0]?.prodCode;
        newProdCode = maxCode ? String(parseInt(maxCode, 10) + 1) : '1000000001';
      } catch (_) { newProdCode = ''; }
    }

    const shopOpts = shopList.map(s =>
      `<option value="${s.shopCode}" ${v.shopCode === s.shopCode ? 'selected' : ''}>${s.shopName || s.shopCode} (${s.shopCode})</option>`
    ).join('');
    const brandOpts  = brandOptions.map(b =>
      `<option value="${b.brandKorName}" ${v.brandName === b.brandKorName ? 'selected' : ''}>${b.brandKorName}</option>`
    ).join('');
    const makerOpts  = makerOptions.map(m =>
      `<option value="${m.makerKorName}" ${v.makerName === m.makerKorName ? 'selected' : ''}>${m.makerKorName}</option>`
    ).join('');
    const originOpts = originOptions.map(o =>
      `<option value="${o.originKorName}" ${v.originName === o.originKorName ? 'selected' : ''}>${o.originKorName}</option>`
    ).join('');
    const taxTypeOpts = taxTypeOptions.map(t =>
      `<option value="${t.constrVal}" ${String(v.vatRate ?? 10) === t.constrVal ? 'selected' : ''}>${t.constrValDesc} (${t.constrVal}%)</option>`
    ).join('');
    const deliFeeTypeOpts = deliFeeTypeOptions.map(d =>
      `<option value="${d.constrVal}" ${v.deliFeeType === d.constrVal ? 'selected' : ''}>${d.constrValDesc}</option>`
    ).join('');
    const saleYnOpts = saleYnTypeOptions.map(s =>
      `<option value="${s.constrVal}" ${String(v.saleYn ?? 1) === s.constrVal ? 'selected' : ''}>${s.constrValDesc}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>상품코드</label>
          <input class="input" value="${isNew ? newProdCode : v.prodCode}" readonly style="background:#f8fafc;font-family:monospace">
        </div>
        <div class="form-group">
          <label>상점 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="pFShopCode" ${!isNew ? 'disabled style="background:#f8fafc"' : ''}>
            <option value="">-- 선택 --</option>
            ${shopOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>분류(카테고리) <span style="color:var(--danger)">*</span></label>
          <div style="display:flex;gap:8px">
            <input class="input" id="pFCatePath" value="${selectedCatePath}" readonly placeholder="분류를 선택하세요" style="background:#f8fafc;flex:1">
            <button type="button" class="btn btn-ghost" id="pFCateBtn">분류 선택</button>
          </div>
        </div>
        <div class="form-group full">
          <label>상품명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="pFProdName" value="${v.prodName || ''}">
        </div>
        <div class="form-group full">
          <label>진열명</label>
          <input class="input" id="pFProdDsplName" value="${v.prodDsplName || ''}">
        </div>
        <div class="form-group">
          <label>쇼핑몰상품코드</label>
          <input class="input" id="pFShopProdCode" value="${v.shopProdCode || ''}">
        </div>
        <div class="form-group">
          <label>모델명</label>
          <input class="input" id="pFModelName" value="${v.modelName || ''}">
        </div>
        <div class="form-group">
          <label>브랜드명</label>
          <select class="input" id="pFBrandName">
            <option value="">-- 선택 안 함 --</option>
            ${brandOpts}
          </select>
        </div>
        <div class="form-group">
          <label>제조사명</label>
          <select class="input" id="pFMakerName">
            <option value="">-- 선택 안 함 --</option>
            ${makerOpts}
          </select>
        </div>
        <div class="form-group">
          <label>원산지명</label>
          <select class="input" id="pFOriginName">
            <option value="">-- 선택 안 함 --</option>
            ${originOpts}
          </select>
        </div>
        <div class="form-group">
          <label>정가</label>
          <input class="input" id="pFListPrice" type="number" min="0" value="${v.listPrice ?? ''}">
        </div>
        <div class="form-group">
          <label>판매가</label>
          <input class="input" id="pFSalePrice" type="number" min="0" value="${v.salePrice ?? ''}">
        </div>
        <div class="form-group">
          <label>공급가</label>
          <input class="input" id="pFSupplyPrice" type="number" min="0" value="${v.supplyPrice ?? ''}">
        </div>
        <div class="form-group">
          <label>매입가</label>
          <input class="input" id="pFBuyPrice" type="number" min="0" value="${v.buyPrice ?? ''}">
        </div>
        <div class="form-group">
          <label>부가세율</label>
          <select class="input" id="pFVatRate">
            ${taxTypeOpts}
          </select>
        </div>
        <div class="form-group">
          <label>재고수량</label>
          <input class="input" id="pFSupplyQty" type="number" min="0" value="${v.supplyQty ?? 9999}">
        </div>
        <div class="form-group">
          <label>판매여부</label>
          <select class="input" id="pFSaleYn">
            ${saleYnOpts}
          </select>
        </div>
        <div class="form-group">
          <label>성인상품</label>
          <select class="input" id="pFAdultYn">
            <option value="0" ${(v.adultYn ?? 0) === 0 ? 'selected' : ''}>아니오</option>
            <option value="1" ${v.adultYn === 1 ? 'selected' : ''}>예</option>
          </select>
        </div>
        <div class="form-group">
          <label>배송방법</label>
          <input class="input" id="pFDeliMethod" value="${v.deliMethod || ''}">
        </div>
        <div class="form-group">
          <label>배송비유형</label>
          <select class="input" id="pFDeliFeeType">
            <option value="">-- 선택 안 함 --</option>
            ${deliFeeTypeOpts}
          </select>
        </div>
        <div class="form-group">
          <label>배송비</label>
          <input class="input" id="pFDeliFeeAmt" type="number" min="0" value="${v.deliFeeAmt ?? 0}">
        </div>
        <div class="form-group">
          <label>리드타임(일)</label>
          <input class="input" id="pFLeadTime" type="number" min="0" value="${v.leadTime ?? ''}">
        </div>
        <div class="form-group full">
          <label>대표이미지</label>
          <div style="display:flex;align-items:center;gap:12px">
            <img id="pFImgPreview" src="${imgUrls.imgUrl}" style="width:72px;height:72px;object-fit:cover;border:1px solid var(--border);border-radius:var(--radius);background:#f8fafc;${imgUrls.imgUrl ? '' : 'display:none'}">
            <div>
              <input type="file" id="pFImgFile" accept="image/*">
              <div id="pFImgStatus" style="font-size:12px;color:var(--text-muted);margin-top:4px">${imgUrls.imgUrl ? '등록된 이미지가 있습니다. 변경하려면 새 파일을 선택하세요.' : '이미지를 선택하면 50/80/100/160/220/280px 썸네일이 자동 생성됩니다.'}</div>
            </div>
          </div>
        </div>
        <div class="form-group full">
          <label>상품설명</label>
          <div id="pFProdDescEditor" style="background:#fff"></div>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="pFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 쇼핑몰 선택이 바뀌면 그 쇼핑몰의 서비스코드로 분류 검색 범위를 맞춘다.
    function currentSvcCode() {
      const shopCode = document.getElementById('pFShopCode').value;
      const shop = shopList.find(s => s.shopCode === shopCode);
      return shop?.svcCode || '';
    }

    // 이미지 선택 즉시 업로드 (등록/수정 저장을 누르기 전에 먼저 서버에 반영해두고,
    // 결과 URL만 payload에 실어 보낸다 — 신규 등록 중이라도 위에서 미리 계산해둔
    // newProdCode를 상품코드로 넘기면 되므로 문제 없다).
    body.querySelector('#pFImgFile').addEventListener('change', async e => {
      const file = e.target.files[0];
      if (!file) return;

      const shopCode = document.getElementById('pFShopCode').value;
      if (!shopCode) { UI.toast('쇼핑몰을 먼저 선택하세요', 'error'); e.target.value = ''; return; }

      const statusEl = document.getElementById('pFImgStatus');
      statusEl.textContent = '업로드 중...';

      const formData = new FormData();
      formData.append('file', file);
      formData.append('shopCode', shopCode);
      formData.append('prodCode', isNew ? newProdCode : v.prodCode);

      try {
        const uploaded = await Api.upload('/catalog/prod/img/upload', formData);
        imgUrls = uploaded;
        const preview = document.getElementById('pFImgPreview');
        preview.src = uploaded.imgUrl;
        preview.style.display = '';
        statusEl.textContent = '업로드 완료 (50/80/100/160/220/280px 썸네일 자동 생성됨)';
      } catch (err) {
        statusEl.textContent = '업로드 실패';
        UI.toast(err.message, 'error');
      }
    });

    body.querySelector('#pFCateBtn').addEventListener('click', () => {
      const svcCode = currentSvcCode();
      if (!svcCode) { UI.toast('쇼핑몰을 먼저 선택하세요', 'error'); return; }
      openCateSearchModal(svcCode, (cateCode, path) => {
        selectedCateCode = cateCode;
        selectedCatePath = path;
        document.getElementById('pFCatePath').value = path;
      });
    });

    // Quill 인스턴스는 UI.modal()이 body를 실제 document에 붙인 뒤에 생성해야 한다
    // (모달 열기 전에는 #pFProdDescEditor가 아직 문서에 붙어있지 않은 상태).
    let quill;

    UI.modal({
      title: isNew ? '상품 신규 등록' : `상품 수정 – ${v.prodCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        // Quill은 root.innerHTML로 HTML을 읽어오는데, 아무것도 입력하지 않은 상태의
        // 기본값이 빈 문자열이 아니라 '<p><br></p>'이므로 그대로 저장하면 DB에
        // 의미없는 태그만 남는다 — 빈 값으로 정규화한다.
        const prodDescHtml = quill.root.innerHTML;
        const prodDesc = prodDescHtml === '<p><br></p>' ? '' : prodDescHtml;
        const shopCode  = document.getElementById('pFShopCode').value;
        const prodName  = document.getElementById('pFProdName').value.trim();

        if (!shopCode)          { UI.toast('쇼핑몰을 선택하세요', 'error'); return; }
        if (!selectedCateCode)  { UI.toast('분류를 선택하세요', 'error'); return; }
        if (!prodName)          { UI.toast('상품명을 입력하세요', 'error'); return; }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          prodCode:      isNew ? newProdCode : v.prodCode,
          shopCode,
          cateCode:      selectedCateCode,
          prodName,
          prodDsplName:  document.getElementById('pFProdDsplName').value.trim(),
          shopProdCode:  document.getElementById('pFShopProdCode').value.trim(),
          modelName:     document.getElementById('pFModelName').value.trim(),
          brandName:     document.getElementById('pFBrandName').value.trim(),
          makerName:     document.getElementById('pFMakerName').value.trim(),
          originName:    document.getElementById('pFOriginName').value.trim(),
          listPrice:     parseInt(document.getElementById('pFListPrice').value) || 0,
          salePrice:     parseInt(document.getElementById('pFSalePrice').value) || 0,
          supplyPrice:   parseInt(document.getElementById('pFSupplyPrice').value) || 0,
          buyPrice:      parseInt(document.getElementById('pFBuyPrice').value) || 0,
          vatRate:       parseInt(document.getElementById('pFVatRate').value) || 0,
          supplyQty:     parseInt(document.getElementById('pFSupplyQty').value) || 0,
          saleYn:        parseInt(document.getElementById('pFSaleYn').value),
          adultYn:       parseInt(document.getElementById('pFAdultYn').value),
          deliMethod:    document.getElementById('pFDeliMethod').value.trim(),
          deliFeeType:   document.getElementById('pFDeliFeeType').value.trim(),
          deliFeeAmt:    parseInt(document.getElementById('pFDeliFeeAmt').value) || 0,
          leadTime:      parseInt(document.getElementById('pFLeadTime').value) || 0,
          ...imgUrls,
          prodDesc,
          remark:        document.getElementById('pFRemark').value.trim(),
          state:         v.state ?? 1,
          registId, registName,
          changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/prod', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/prod', payload);
            UI.toast('저장되었습니다', 'success');
          }
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });

    // 모달이 열려 body가 document에 붙은 뒤 Quill을 초기화하고 기존 값(HTML)을 채운다.
    quill = new Quill(body.querySelector('#pFProdDescEditor'), {
      theme: 'snow',
      placeholder: '상품 상세 설명을 입력하세요...',
    });
    if (v.prodDesc) quill.clipboard.dangerouslyPasteHTML(v.prodDesc);
  }

  return { render };
})();

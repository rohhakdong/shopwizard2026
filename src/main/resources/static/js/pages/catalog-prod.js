/**
 * 상품 등록/승인 (메뉴 라벨 기준. 구 "카탈로그 상품")
 *   tCatProd — shopwizard 스키마, 승인 워크플로우의 시작점
 * - 목록 조회 (페이지네이션) / 신규 등록 / 수정 / 상세보기(읽기 전용) / 옵션 / 승인 / 삭제
 * - 승인하면 상품+옵션이 실제 판매 스키마(shopion.tPrdProd/tPrdProdItem,
 *   "판매 상품 관리"/product-prod.js 화면)로 즉시 복사되어 shop.html에 노출된다
 *   (approv 직후 copy2shopion 호출 — publishToShopion 참고).
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

  // 외부/사용자 입력이 섞이는 값은 innerHTML 에 넣기 전에 반드시 esc() 를 거친다.
  const esc = UI.esc;

  // 승인 권한(역할에 "상품승인" 메뉴가 있는지) — 서버가 승인/쇼핑몰 반영 API 를 403 으로 막고, 여기선 버튼만 숨긴다.
  // index.html 의 전역 info.screens 를 쓰며, 서버가 screens 를 내려주지 않으면(권한 검사 off) 모두 허용.
  const canApprove = () => typeof info === 'undefined' || !info || !info.screens || !!info.screens['catalog-prod-approve'];

  const PAGE_SIZE = 20;
  // 상품 등록 시 서버(ProdService.insert)가 항상 자동으로 만들어두는 "선택사항없음" 기본 옵션(tCatProdItem).
  // 옵션 관리 화면에서 이 항목은 실제 등록 옵션이 아니므로 숨긴다. 기본 행은 ItemCode(20000)가 아니라
  // 옵션값("선택사항없음")으로 알아본다 — 레거시 옵션 상품은 기본 행 없이 20000 이 실제 첫 번째 옵션이라,
  // 코드로 숨기면 그 옵션이 관리 화면에서 보이지도 고칠 수도 없게 된다.
  const PLACEHOLDER_ATTR_VAL = '선택사항없음';
  const isPlaceholderItem = it => String(it.attrVal1 || '').trim() === PLACEHOLDER_ATTR_VAL;
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
        <div class="card-header">상품 등록/승인</div>
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
            <button class="btn btn-ghost" id="btnExcel" style="margin-top:18px">엑셀 일괄등록</button>
            <button class="btn btn-ghost" id="btnExcelDown" style="margin-top:18px" title="현재 검색 조건의 상품을 엑셀 양식으로 내려받습니다. 고쳐서 [엑셀 일괄등록]으로 올리면 수정됩니다.">엑셀 다운로드</button>
          </div>

          <!-- 건수 + 일괄승인 -->
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
            <div style="font-size:12px;color:var(--text-muted)">
              총 <strong id="totalCountLabel">0</strong>건
            </div>
            <div style="display:flex;gap:6px">
              <button class="btn btn-ghost" id="btnSlink" style="padding:4px 10px;font-size:12px;${canApprove() ? '' : 'display:none'}" title="승인(게시)된 샵피온 상품을 샵링커에 등록/수정합니다">선택 샵링커 전송</button>
              <button class="btn btn-primary" id="btnBulkApprov" style="padding:4px 10px;font-size:12px;${canApprove() ? '' : 'display:none'}">선택 일괄승인</button>
            </div>
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
    document.getElementById('btnExcel').addEventListener('click', openExcelModal);
    document.getElementById('btnExcelDown').addEventListener('click', downloadExcel);
    document.getElementById('btnBulkApprov').addEventListener('click', bulkApprov);
    document.getElementById('btnSlink').addEventListener('click', sendToShoplinker);
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
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
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
        <td style="text-align:center">
          ${canApprove() ? `<input type="checkbox" class="row-check" data-code="${esc(p.prodCode)}" data-approved="${p.approvDate ? '1' : '0'}">` : ''}
        </td>
        <td style="font-size:11px;${esc(ell)}">
          <code>${esc(p.prodCode || '')}</code><br>
          <span style="color:var(--text-muted)">${esc(p.shopProdCode || '')}</span>
        </td>
        <td style="${esc(ell)}">
          <div style="font-weight:500;font-size:13px;${esc(ell)}" title="${esc(p.prodName || '')}">${esc(p.prodName || '')}</div>
          <div style="font-size:11px;color:var(--text-muted);${esc(ell)}" title="${esc(p.brandName || '')}">${esc(p.brandName || '')}</div>
        </td>
        <td style="font-size:12px;${esc(ell)}" title="${esc(p.shopName || '')}">${esc(p.shopName || '')}</td>
        <td style="font-size:12px;${esc(ell)}" title="${esc(p.cateName || '')}">${esc(p.cateName || '')}</td>
        <td style="text-align:right;font-size:13px;font-weight:500">
          ${esc(p.salePrice != null ? p.salePrice.toLocaleString() + '원' : '-')}
        </td>
        <td style="text-align:center">
          <span class="badge ${esc(p.saleYn === 1 ? 'badge-green' : 'badge-gray')}">${esc(p.saleYn === 1 ? '판매' : '미판매')}</span>
        </td>
        <td style="text-align:center">
          ${p.approvDate
            ? `<span class="badge badge-blue" title="${esc(p.approvDate)}">승인</span>`
            : `<span class="badge badge-gray">미승인</span>`}
        </td>
        <td style="font-size:11px;color:var(--text-muted);${esc(ell)}">${esc(p.registDate ? p.registDate.substring(0, 10) : '')}</td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="detail" data-code="${esc(p.prodCode)}">상세</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${esc(p.prodCode)}">수정</button>
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="option" data-code="${esc(p.prodCode)}" data-name="${esc(p.prodName)}">옵션</button>
          ${!p.approvDate && canApprove() ? `<button class="btn btn-ghost" style="padding:2px 7px;font-size:11px;color:var(--primary)"
            data-action="approv" data-code="${esc(p.prodCode)}" data-name="${esc(p.prodName)}">승인</button>` : ''}
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="slinklog" data-code="${esc(p.prodCode)}" data-name="${esc(p.prodName)}" title="샵링커 전송 이력">샵링커</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${esc(p.prodCode)}" data-name="${esc(p.prodName)}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("상품명")은 지정된 열들의 폭 합계가
    // 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블 자체는
    // width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:1030px">
        <colgroup>
          <col style="width:30px">
          <col style="width:130px"><col style="width:200px"><col style="width:90px"><col style="width:90px">
          <col style="width:90px"><col style="width:55px"><col style="width:55px">
          <col style="width:80px"><col style="width:210px">
        </colgroup>
        <thead>
          <tr>
            <th style="text-align:center"><input type="checkbox" id="checkAll"></th>
            <th style="${esc(thEll)}">상품코드</th><th style="${esc(thEll)}">상품명</th><th style="${esc(thEll)}">상점</th><th style="${esc(thEll)}">카테고리</th>
            <th style="${esc(thEll)};text-align:right">판매가</th><th style="${esc(thEll)};text-align:center">판매</th>
            <th style="${esc(thEll)};text-align:center">승인</th><th style="${esc(thEll)}">등록일</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 전체선택: 현재 페이지에 보이는 체크박스 전부. [선택 일괄승인]은 그중 미승인 행만, [선택 샵링커 전송]은 선택한 전부를 처리한다.
    const checkAll = wrap.querySelector('#checkAll');
    if (checkAll) {
      checkAll.addEventListener('change', () => {
        wrap.querySelectorAll('.row-check').forEach(cb => { cb.checked = checkAll.checked; });
      });
    }

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

    // 옵션(사이즈/색상 등) 관리
    wrap.querySelectorAll('[data-action=option]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const prod = await Api.get('/catalog/prod/' + btn.dataset.code);
          openProdItemModal(prod);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    // 승인
    // 주의: 백엔드 ProdMapper.xml의 approv 쿼리는 ProdCode IN (...) 을 위해
    // 파라미터 맵에 반드시 "prodCodes"(배열)를 요구한다 — 예전에는 여기서 "prodCode"
    // (단수)를 보내고 있어서 MyBatis가 "The expression 'prodCodes' evaluated to a
    // null value" 예외를 던지며 버튼을 누를 때마다 500이 났다(한 번도 성공한 적이
    // 없었던 것으로 보인다). 배열로 보내도록 고치면서, 아래 일괄승인과 완전히 같은
    // 엔드포인트/파라미터 모양을 쓰도록 통일했다.
    wrap.querySelectorAll('[data-action=approv]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상품을 승인하시겠습니까? (승인 즉시 쇼핑몰에 노출됩니다)`, async close => {
          try {
            await Api.put('/catalog/prod/approv', {
              prodCodes:  [btn.dataset.code],
              approvId:   info?.loginId,
              approvName: info?.name,
            });
            await publishToShopion([btn.dataset.code]);
            UI.toast('승인되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });

    // 샵링커 전송 이력
    wrap.querySelectorAll('[data-action=slinklog]').forEach(btn => {
      btn.addEventListener('click', () => openSlinkLogModal(btn.dataset.code, btn.dataset.name));
    });

    // 삭제
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 상품을 삭제하시겠습니까? (쇼핑몰에 게시된 상품이면 판매 상품에서도 함께 삭제됩니다. 주문 이력이 있으면 삭제하지 않고 완전품절로 처리합니다)`, async close => {
          try {
            const r = await Api.delete('/catalog/prod', { prodCode: btn.dataset.code });
            // 주문 이력이 있으면 서버가 지우지 않고 완전품절로만 바꾼다 — 그 사실을 그대로 알려준다.
            UI.toast(r && r.deleted === false && r.message ? r.message : '삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 샵링커 전송 ────────────────────────────────────────────────────
  // 승인(게시)된 샵피온 상품을 선택해 샵링커에 등록/수정한다(수동 전송 — 자동 전송은 없다).
  // 실제 호출은 운영 샵링커에 상품이 등록·수정되므로 서버 기본 모드는 "시험"(요청만 만들어 이력에 남기고 보내지 않음)이다.
  // 미승인 상품·샵피온이 아닌 서비스·필수값(상점상품코드/이미지/분류) 누락 상품은 서버가 사유와 함께 건너뛴다.
  async function sendToShoplinker() {
    const codes = Array.from(document.querySelectorAll('.row-check:checked')).map(cb => cb.dataset.code);
    if (codes.length === 0) { UI.toast('샵링커로 전송할 상품을 먼저 선택하세요', 'error'); return; }

    let modeInfo;
    try { modeInfo = await Api.get('/catalog/prod/slinker/mode'); }
    catch (e) { UI.toast(e.message, 'error'); return; }
    if (codes.length > modeInfo.maxProducts) {
      UI.toast(`한 번에 최대 ${modeInfo.maxProducts}건까지 전송할 수 있습니다 (선택 ${codes.length}건)`, 'error');
      return;
    }
    const live = modeInfo.mode === 'live';
    const msg = live
      ? `샵링커에 ${codes.length}건을 실제로 전송합니다. 샵링커에 상품이 등록·수정됩니다. 계속하시겠습니까?`
      : `[시험 모드] ${codes.length}건의 전송 요청을 만들어 이력에만 남기고, 샵링커로는 보내지 않습니다. 계속하시겠습니까?`;

    UI.confirm(msg, async close => {
      try {
        const r = await Api.post('/catalog/prod/slinker/send', { prodCodes: codes });
        close();
        openSlinkResultModal(r);
      } catch (e) {
        UI.toast(e.message, 'error');
        close();
      }
    });
  }

  const SLINK_STATUS = {
    SENT:    { label: '전송 성공', badge: 'badge-green' },
    DRY:     { label: '시험(미전송)', badge: 'badge-blue' },
    FAILED:  { label: '실패', badge: 'badge-red' },
    SKIPPED: { label: '건너뜀', badge: 'badge-gray' },
  };

  function openSlinkResultModal(r) {
    const results = r.results || [];
    const count = s => results.filter(x => x.status === s).length;
    const body = document.createElement('div');
    body.innerHTML = `
      ${r.mode === 'live' ? '' : `
      <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:var(--radius);padding:10px 12px;font-size:12px;margin-bottom:10px">
        시험 모드입니다. 요청을 만들어 이력에만 남겼고 <b>샵링커로는 실제로 보내지 않았습니다.</b>
      </div>`}
      <div style="font-size:13px;margin-bottom:8px">
        총 <b>${esc(String(results.length))}</b>건 ·
        전송 성공 <b>${esc(String(count('SENT')))}</b> · 시험 <b>${esc(String(count('DRY')))}</b> ·
        <span style="color:${count('FAILED') ? 'var(--danger)' : 'inherit'}">실패 <b>${esc(String(count('FAILED')))}</b></span> ·
        건너뜀 <b>${esc(String(count('SKIPPED')))}</b>
      </div>
      <div style="max-height:380px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
        <table class="table" style="width:100%;font-size:12px">
          <thead><tr><th>상품코드</th><th>상품명</th><th>결과</th><th>메시지</th></tr></thead>
          <tbody>${results.map(x => {
            const st = SLINK_STATUS[x.status] || { label: x.status, badge: 'badge-gray' };
            return `<tr>
              <td><code>${esc(x.prodCode || '')}</code></td>
              <td style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(x.prodName || '')}">${esc(x.prodName || '')}</td>
              <td><span class="badge ${esc(st.badge)}">${esc(st.label)}</span></td>
              <td style="line-height:1.5">${esc(x.message || '')}</td>
            </tr>`;
          }).join('')}</tbody>
        </table>
      </div>`;
    const { el } = UI.modal({ title: '샵링커 전송 결과', body, confirmText: null, cancelText: '닫기' });
    el.querySelector('.modal').style.width = '900px';
  }

  async function openSlinkLogModal(prodCode, prodName) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="padding:12px;color:var(--text-muted)">불러오는 중...</div>';
    const { el } = UI.modal({ title: `샵링커 전송 이력 – ${prodName || prodCode}`, body, confirmText: null, cancelText: '닫기' });
    el.querySelector('.modal').style.width = '860px';

    const TYPE = { 'true': '성공', 'false': '실패', 'error': '오류', 'dry': '시험' };
    try {
      const logs = await Api.get('/catalog/prod/slinker/log', { prodCode, limit: 30 });
      if (!logs.length) { body.innerHTML = '<div style="padding:16px;color:var(--text-muted);text-align:center">전송 이력이 없습니다</div>'; return; }
      body.innerHTML = `
        <div style="max-height:420px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
          <table class="table" style="width:100%;font-size:12px">
            <thead><tr><th>일시</th><th>결과</th><th>메시지</th><th>처리자</th></tr></thead>
            <tbody>${logs.map(l => `<tr>
              <td style="white-space:nowrap">${esc((l.registDate || '').replace('T', ' ').substring(0, 19))}</td>
              <td>${esc(TYPE[String(l.resultType || '').substring(0, 5).replace('|', '')] || l.resultType || '')}</td>
              <td style="line-height:1.5">${esc(l.resultMessag || '')}</td>
              <td style="white-space:nowrap">${esc(l.registName || l.registId || '')}</td>
            </tr>`).join('')}</tbody>
          </table>
        </div>`;
    } catch (e) {
      body.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
    }
  }

  // ── 엑셀 다운로드 ──────────────────────────────────────────────────
  // 현재 검색 조건의 상품을 일괄등록과 같은 35열 양식으로 내려받는다. 고쳐서 [엑셀 일괄등록]으로 올리면
  // 상품코드가 있는 행은 수정으로 처리된다. 오류(예: 5,000건 초과)를 토스트로 보여주려고 fetch 로 받는다.
  async function downloadExcel() {
    const btn = document.getElementById('btnExcelDown');
    btn.disabled = true;
    try {
      const qs = new URLSearchParams(Object.entries(getParams()).filter(([, v]) => v)).toString();
      const res = await fetch('/catalog/prod/excel/export' + (qs ? '?' + qs : ''), { credentials: 'same-origin' });
      if (!res.ok) {
        let msg = '';
        try { msg = JSON.parse(await res.text()).message; } catch (_) {}
        if (res.status === 403) msg = '이 기능을 사용할 권한이 없습니다.';
        throw new Error(msg || `HTTP ${res.status}`);
      }
      const blob = await res.blob();
      const d = new Date();
      const stamp = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`;
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `prodExcel_${stamp}.xls`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
      UI.toast(`현재 검색 조건의 상품 ${totalCount.toLocaleString()}건을 내려받았습니다`, 'success');
    } catch (e) {
      UI.toast(e.message, 'error');
    } finally {
      btn.disabled = false;
    }
  }

  // ── 엑셀 일괄등록 ──────────────────────────────────────────────────
  // 예전 양식(prodExcel.xls, 35열)으로 작성한 파일을 올려 상품을 한 번에 등록/수정한다.
  // [검사] → 서버가 행별로 검사한 결과를 보여주고, 오류가 0건일 때만 [등록]이 켜진다.
  // [등록]은 같은 파일을 다시 보내고, 서버는 미리보기 결과를 믿지 않고 다시 검사한 뒤 전부 한 번에 저장한다.
  function openExcelModal() {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);line-height:1.6;margin-bottom:10px">
        엑셀 양식(.xls, .xlsx)으로 작성한 파일을 올려 상품을 한 번에 등록·수정합니다.<br>
        상품코드가 <b>비어 있으면 신규</b>(코드 자동 부여), <b>있으면 그 상품 수정</b>(빈 칸은 기존 값 유지)입니다.
        오류가 한 행이라도 있으면 아무것도 등록되지 않습니다. 새 상품은 승인 전 상태로 들어가며 [승인] 후 쇼핑몰에 노출됩니다.
        <a href="/catalog/prod/excel/template" id="xlTemplate" style="margin-left:6px">엑셀 양식 받기</a><br>
        기존 상품을 고치려면 목록의 [엑셀 다운로드]로 받은 파일을 수정해서 올리세요(같은 양식입니다).
      </div>
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
        <input type="file" id="xlFile" accept=".xls,.xlsx" style="flex:1">
        <button class="btn btn-primary" id="xlCheck">검사</button>
      </div>
      <div id="xlResult"></div>`;

    const { close, el } = UI.modal({
      title: '상품 엑셀 일괄등록',
      body,
      confirmText: '등록',
      onConfirm: async closeModal => {
        const file = fileInput.files[0];
        if (!file || !checked) { UI.toast('먼저 파일을 선택하고 검사하세요', 'error'); return; }
        confirmBtn.disabled = true;
        try {
          const r = await Api.upload('/catalog/prod/excel/import', formOf(file));
          if (r.imported) {
            UI.toast(`신규 ${r.newCount}건 등록, 수정 ${r.updateCount}건 반영되었습니다`, 'success');
            closeModal();
            loadList();
          } else {
            checked = false;
            renderResult(r);
            UI.toast('오류가 있어 등록되지 않았습니다', 'error');
          }
        } catch (e) {
          UI.toast(e.message, 'error');
          confirmBtn.disabled = !checked;
        }
      },
    });
    el.querySelector('.modal').style.width = '960px';

    const fileInput  = body.querySelector('#xlFile');
    const resultEl   = body.querySelector('#xlResult');
    const confirmBtn = el.querySelector('[data-action=confirm]');
    let checked = false;
    confirmBtn.disabled = true;

    const formOf = file => { const f = new FormData(); f.append('file', file); return f; };

    function renderResult(r) {
      checked = !!r.importable;
      confirmBtn.disabled = !checked;
      if (r.fileError) {
        resultEl.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(r.fileError)}</div>`;
        return;
      }
      const summary = `
        <div style="font-size:13px;margin-bottom:8px">
          총 <b>${esc(String((r.rows || []).length))}</b>행 ·
          신규 <b>${esc(String(r.newCount))}</b> · 수정 <b>${esc(String(r.updateCount))}</b> ·
          <span style="color:${r.errorCount ? 'var(--danger)' : 'inherit'}">오류 <b>${esc(String(r.errorCount))}</b></span> ·
          경고 <b>${esc(String(r.warningCount))}</b>
          ${r.errorCount ? '<span style="color:var(--danger);margin-left:8px">오류를 고쳐 파일을 다시 올려 주세요.</span>' : ''}
        </div>`;
      const rows = (r.rows || []).map(row => `
        <tr>
          <td style="text-align:right">${esc(String(row.rowNo))}</td>
          <td>${row.action === 'UPDATE' ? '수정' : '신규'}</td>
          <td>${esc(row.prodCode || '-')}</td>
          <td style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(row.prodName || '')}">${esc(row.prodName || '')}</td>
          <td><span class="badge ${row.status === 'OK' ? 'badge-green' : 'badge-red'}">${row.status === 'OK' ? '정상' : '오류'}</span></td>
          <td style="font-size:12px;line-height:1.5">
            ${(row.errors || []).map(m => `<div style="color:var(--danger)">${esc(m)}</div>`).join('')}
            ${(row.warnings || []).map(m => `<div style="color:#b45309">${esc(m)}</div>`).join('')}
          </td>
        </tr>`).join('');
      resultEl.innerHTML = `${summary}
        <div style="max-height:380px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
          <table class="table" style="width:100%;font-size:12px">
            <thead><tr><th>행</th><th>구분</th><th>상품코드</th><th>상품명</th><th>상태</th><th>메시지</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        </div>`;
    }

    // 파일을 바꾸면 이전 검사 결과는 더 이상 이 파일의 것이 아니다.
    fileInput.addEventListener('change', () => {
      checked = false;
      confirmBtn.disabled = true;
      resultEl.innerHTML = '';
    });

    body.querySelector('#xlCheck').addEventListener('click', async () => {
      const file = fileInput.files[0];
      if (!file) { UI.toast('엑셀 파일을 선택하세요', 'error'); return; }
      resultEl.innerHTML = '<div style="padding:12px;color:var(--text-muted)">검사 중...</div>';
      checked = false;
      confirmBtn.disabled = true;
      try {
        renderResult(await Api.upload('/catalog/prod/excel/preview', formOf(file)));
      } catch (e) {
        resultEl.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
      }
    });
  }

  // ── 선택 일괄승인 ──────────────────────────────────────────────────
  // 개별 승인 버튼과 동일한 /catalog/prod/approv 엔드포인트를 그대로 재사용한다
  // (백엔드 쿼리 자체가 처음부터 ProdCode IN (...) 이라 다건 승인을 염두에 두고
  // 만들어져 있었는데, 정작 프론트에는 여러 건을 한 번에 선택할 UI가 없었다).
  function bulkApprov() {
    // 체크박스는 승인된 행에도 있으므로(샵링커 전송용) 일괄승인은 미승인 행만 대상으로 한다.
    const codes = Array.from(document.querySelectorAll('.row-check:checked'))
      .filter(cb => cb.dataset.approved === '0').map(cb => cb.dataset.code);
    if (codes.length === 0) { UI.toast('승인할 상품(미승인)을 먼저 선택하세요', 'error'); return; }

    UI.confirm(`선택한 ${codes.length}건을 일괄승인 하시겠습니까? (승인 즉시 쇼핑몰에 노출됩니다)`, async close => {
      try {
        await Api.put('/catalog/prod/approv', {
          prodCodes:  codes,
          approvId:   info?.loginId,
          approvName: info?.name,
        });
        await publishToShopion(codes);
        UI.toast(`${codes.length}건 승인되었습니다`, 'success');
        loadList();
      } catch (e) { UI.toast(e.message, 'error'); }
      close();
    });
  }

  // ── 승인된 상품을 실제 판매 스키마(shopion)로 반영 ────────────────────
  // catalog.ProdController/ProdItemController의 copy2shopion을 그대로 재사용한다.
  // 이전까지는 두 엔드포인트 다(상품쪽은 컨트롤러까지 있었지만, 옵션쪽은 서비스
  // 레이어에만 있고 컨트롤러 자체가 없어서 호출이 원천적으로 불가능했다) 실제로
  // 어디서도 호출되지 않아서, 카탈로그에서 아무리 등록→승인해도 shop.html(고객
  // 화면)에는 영원히 나타날 수 없었다 — "승인 = 쇼핑몰 노출"이 되도록 여기서
  // 연결한다. 상품 하나가 실패해도 나머지는 계속 시도하고, 실패한 것만 모아
  // 알려준다(승인 자체는 이미 DB에 반영된 뒤이므로 조용히 삼키면 안 된다).
  async function publishToShopion(prodCodes) {
    const failed = [];
    for (const prodCode of prodCodes) {
      try {
        await Api.post('/catalog/prod/copy2shopion', { prodCode });
        await Api.post('/catalog/prod-item/copy2shopion', {
          prodCode,
          registId:   info?.loginId,
          registName: info?.name,
        });
      } catch (e) {
        failed.push(prodCode);
      }
    }
    if (failed.length > 0) {
      UI.toast(`다음 상품은 쇼핑몰 반영에 실패했습니다: ${failed.join(', ')}`, 'error');
    }
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
      html += `<button class="page-btn" data-page="${esc(start - 1)}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${esc(p === currentPage ? 'active' : '')}" data-page="${esc(p)}">${esc(p)}</button>`;
    if (end < totalPages)
      html += `<button class="page-btn" data-page="${esc(end + 1)}">›</button>`;
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
      <div class="form-group ${esc(full ? 'full' : '')}">
        <label>${esc(label)}</label>
        <div class="input" style="background:#f8fafc;color:var(--text);min-height:34px;line-height:34px;padding:0 10px;border-radius:var(--radius);border:1px solid var(--border)">${esc(value ?? '-')}</div>
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
          <img src="${esc(p.imgUrl100 || p.imgUrl)}" style="width:100px;height:100px;object-fit:cover;border:1px solid var(--border);border-radius:var(--radius)">
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
      <div style="border:1px solid var(--border);border-radius:var(--radius);padding:12px;background:#fff">${UI.sanitizeHtml(p.prodDesc)}</div>` : ''}`;  // 리치텍스트(Quill) HTML — 이스케이프하면 서식이 깨지므로 허용 목록 정화

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
          return `<div class="cate-pick" data-code="${esc(c.cateCode)}" data-path="${esc(path)}"
                    style="padding:8px 12px;cursor:pointer;border-bottom:1px solid var(--border);font-size:13px">${esc(path)}</div>`;
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
        resultEl.innerHTML = `<div style="padding:16px;color:var(--danger)">${esc(e.message)}</div>`;
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

  // ── 상품옵션(사이즈/색상 등) 관리 모달 ────────────────────────────────
  // tCatProd.AttrName1~4 (옵션명, 상품 수정화면에서 설정)에 대응하는 실제 값 조합을
  // tCatProdItem(ProdCode+ItemCode PK)에 재고/가격까지 포함해 여러 건 등록하는 화면.
  // 백엔드(ProdItemController/Service/Mapper)는 이미 있었지만 화면이 어디에도 없었다.
  async function openProdItemModal(prod) {
    const attrNames = [prod.attrName1, prod.attrName2, prod.attrName3, prod.attrName4];

    const body = document.createElement('div');
    body.innerHTML = `
      ${attrNames.every(n => !n) ? `
      <div style="background:#fffbea;border:1px solid #fde68a;border-radius:var(--radius);padding:10px 12px;font-size:12px;margin-bottom:12px">
        옵션명이 설정되지 않았습니다. '수정' 화면의 '옵션명(사이즈/색상 등)'에 이름을 먼저 입력하면
        아래 옵션값 입력란에 그 이름이 라벨로 표시됩니다. (비워도 옵션값 자체는 등록할 수 있습니다.)
      </div>` : ''}
      <div style="margin-bottom:10px">
        <button class="btn btn-primary" id="piAddBtn" style="padding:5px 12px;font-size:12px">+ 옵션 추가</button>
      </div>
      <div id="piTableWrap"></div>`;

    async function refresh() {
      const wrap = body.querySelector('#piTableWrap');
      wrap.innerHTML = '<div style="text-align:center;padding:24px;color:var(--text-muted)">불러오는 중...</div>';
      let items = [];
      try { items = await Api.get('/catalog/prod-item/list', { pProdCode: prod.prodCode }); }
      catch (e) { wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`; return; }

      // "선택사항없음" 기본 아이템은 상품 등록 시 서버가 자동으로 만들어두는 행이다(ProdService.insert 참고) —
      // 관리자가 실제로 등록한 옵션이 아니므로 목록에는 보여주지 않는다. ItemCode 채번(신규 추가 시 최댓값+1)에는
      // 계속 포함시켜야 하므로 items 자체는 그대로 두고 화면에 그릴 목록만 따로 거른다.
      const visibleItems = items.filter(it => !isPlaceholderItem(it));

      if (visibleItems.length === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:24px;color:var(--text-muted)">등록된 옵션이 없습니다</div>';
        return;
      }

      const attrLabel = (i) => attrNames[i] || `옵션값${i + 1}`;
      const rows = visibleItems.map(it => `
        <tr>
          <td style="font-size:12px">${esc(it.attrVal1 || '-')}</td>
          <td style="font-size:12px">${esc(it.attrVal2 || '-')}</td>
          <td style="font-size:12px">${esc(it.attrVal3 || '-')}</td>
          <td style="font-size:12px">${esc(it.attrVal4 || '-')}</td>
          <td style="text-align:right;font-size:12px">${esc(it.supplyQty ?? 0)}</td>
          <td style="text-align:right;font-size:12px">${esc(it.salePrice != null ? it.salePrice.toLocaleString() : '-')}</td>
          <td style="text-align:center">
            <span class="badge ${esc(it.saleYn === 1 ? 'badge-green' : 'badge-gray')}">${esc(it.saleYn === 1 ? '판매' : '중지')}</span>
          </td>
          <td style="white-space:nowrap">
            <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-action="pi-edit" data-item="${esc(it.itemCode)}">수정</button>
            <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-action="pi-del" data-item="${esc(it.itemCode)}">삭제</button>
          </td>
        </tr>`).join('');

      wrap.innerHTML = `
        <table style="width:100%;table-layout:fixed">
          <colgroup>
            <col><col><col><col><col style="width:70px"><col style="width:80px"><col style="width:60px"><col style="width:110px">
          </colgroup>
          <thead>
            <tr>
              <th style="font-size:11px;text-align:left">${esc(attrLabel(0))}</th><th style="font-size:11px;text-align:left">${esc(attrLabel(1))}</th>
              <th style="font-size:11px;text-align:left">${esc(attrLabel(2))}</th><th style="font-size:11px;text-align:left">${esc(attrLabel(3))}</th>
              <th style="font-size:11px;text-align:right">재고</th><th style="font-size:11px;text-align:right">판매가</th>
              <th style="font-size:11px;text-align:center">판매</th><th></th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>`;

      wrap.querySelectorAll('[data-action=pi-edit]').forEach(btn => {
        btn.addEventListener('click', () => {
          const item = items.find(it => String(it.itemCode) === btn.dataset.item);
          openProdItemEditModal(prod, item, items, refresh);
        });
      });
      wrap.querySelectorAll('[data-action=pi-del]').forEach(btn => {
        btn.addEventListener('click', () => {
          UI.confirm('이 옵션을 삭제하시겠습니까?', async close => {
            try {
              await Api.delete('/catalog/prod-item', { prodCode: prod.prodCode, itemCode: btn.dataset.item });
              UI.toast('삭제되었습니다', 'success');
              refresh();
            } catch (e) { UI.toast(e.message, 'error'); }
            close();
          });
        });
      });
    }

    UI.modal({
      title: `상품옵션 관리 – ${prod.prodName}`,
      body,
      confirmText: null,
      cancelText: '닫기',
    });

    body.querySelector('#piAddBtn').addEventListener('click', async () => {
      let items = [];
      try { items = await Api.get('/catalog/prod-item/list', { pProdCode: prod.prodCode }); } catch (_) { items = []; }
      openProdItemEditModal(prod, null, items, refresh);
    });

    refresh();
  }

  // 옵션 1건 등록/수정 (openProdItemModal 위에 중첩되는 모달)
  function openProdItemEditModal(prod, item, existingItems, onSaved) {
    const isNew = !item;
    const v = item || {};
    const attrNames = [prod.attrName1, prod.attrName2, prod.attrName3, prod.attrName4];

    // ItemCode는 전용 채번 엔드포인트가 없어 상품코드(ProdCode) 자동채번과 같은 방식으로,
    // 이미 불러와둔 목록에서 최댓값+1을 클라이언트에서 계산한다.
    // 상품 등록 시 서버가 항상 ItemCode=20000짜리 "선택사항없음" 기본 아이템을 미리 만들어두므로
    // (ProdService.insert 참고 — 옵션 유무와 무관하게 tCatProdItem을 항상 조인 가능하게 하는 기존
    // 데이터 전체의 확립된 관례), 실제 옵션은 자연스럽게 20001부터 이어붙는다. 혹시 그 기본
    // 아이템이 지워진 등 예외적으로 목록이 비어 있는 경우에도 1이 아닌 20001부터 시작해
    // 레거시 채번 규칙과 어긋나지 않게 한다.
    const newItemCode = isNew
      ? (existingItems.length ? Math.max(...existingItems.map(it => it.itemCode)) + 1 : 20001)
      : v.itemCode;

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>옵션코드</label>
          <input class="input" value="${esc(newItemCode)}" readonly style="background:#f8fafc;font-family:monospace">
        </div>
        <div class="form-group">
          <label>${esc(attrNames[0] || '옵션값1')}</label>
          <input class="input" id="piAttrVal1" value="${esc(v.attrVal1 || '')}">
        </div>
        <div class="form-group">
          <label>${esc(attrNames[1] || '옵션값2')}</label>
          <input class="input" id="piAttrVal2" value="${esc(v.attrVal2 || '')}">
        </div>
        <div class="form-group">
          <label>${esc(attrNames[2] || '옵션값3')}</label>
          <input class="input" id="piAttrVal3" value="${esc(v.attrVal3 || '')}">
        </div>
        <div class="form-group">
          <label>${esc(attrNames[3] || '옵션값4')}</label>
          <input class="input" id="piAttrVal4" value="${esc(v.attrVal4 || '')}">
        </div>
        <div class="form-group">
          <label>재고수량</label>
          <input class="input" id="piSupplyQty" type="number" min="0" value="${esc(v.supplyQty ?? 0)}">
        </div>
        <div class="form-group">
          <label>판매가(옵션추가금 아님)</label>
          <input class="input" id="piSalePrice" type="number" min="0" value="${esc(v.salePrice ?? prod.salePrice ?? 0)}">
        </div>
        <div class="form-group">
          <label>옵션추가금액</label>
          <input class="input" id="piOptionPrice" type="number" min="0" value="${esc(v.optionPrice ?? 0)}">
        </div>
        <div class="form-group">
          <label>공급가</label>
          <input class="input" id="piSupplyPrice" type="number" min="0" value="${esc(v.supplyPrice ?? 0)}">
        </div>
        <div class="form-group">
          <label>매입가</label>
          <input class="input" id="piBuyPrice" type="number" min="0" value="${esc(v.buyPrice ?? 0)}">
        </div>
        <div class="form-group">
          <label>판매여부</label>
          <select class="input" id="piSaleYn">
            <option value="1" ${esc((v.saleYn ?? 1) === 1 ? 'selected' : '')}>판매</option>
            <option value="0" ${esc(v.saleYn === 0 ? 'selected' : '')}>중지</option>
          </select>
        </div>
      </div>`;

    UI.modal({
      title: isNew ? '옵션 추가' : `옵션 수정 – #${v.itemCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const attrVal1 = document.getElementById('piAttrVal1').value.trim();
        const attrVal2 = document.getElementById('piAttrVal2').value.trim();
        const attrVal3 = document.getElementById('piAttrVal3').value.trim();
        const attrVal4 = document.getElementById('piAttrVal4').value.trim();
        if (!attrVal1 && !attrVal2 && !attrVal3 && !attrVal4) {
          UI.toast('옵션값을 최소 1개 이상 입력하세요', 'error'); return;
        }

        const registId   = (typeof info !== 'undefined' && info?.loginId) || '';
        const registName = (typeof info !== 'undefined' && info?.name) || '';

        const payload = {
          prodCode: prod.prodCode,
          itemCode: newItemCode,
          attrVal1, attrVal2, attrVal3, attrVal4,
          supplyQty:   parseInt(document.getElementById('piSupplyQty').value) || 0,
          salePrice:   parseInt(document.getElementById('piSalePrice').value) || 0,
          optionPrice: parseInt(document.getElementById('piOptionPrice').value) || 0,
          supplyPrice: parseInt(document.getElementById('piSupplyPrice').value) || 0,
          buyPrice:    parseInt(document.getElementById('piBuyPrice').value) || 0,
          saleYn:      parseInt(document.getElementById('piSaleYn').value),
          state: 1,
          registId, registName, changeId: registId, changeName: registName,
        };

        try {
          if (isNew) {
            await Api.post('/catalog/prod-item', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/catalog/prod-item', payload);
            UI.toast('저장되었습니다', 'success');
          }
          onSaved();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
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
      `<option value="${esc(s.shopCode)}" ${esc(v.shopCode === s.shopCode ? 'selected' : '')}>${esc(s.shopName || s.shopCode)} (${esc(s.shopCode)})</option>`
    ).join('');
    const brandOpts  = brandOptions.map(b =>
      `<option value="${esc(b.brandKorName)}" ${esc(v.brandName === b.brandKorName ? 'selected' : '')}>${esc(b.brandKorName)}</option>`
    ).join('');
    // 제조사는 필수다. 기존 상품의 제조사가 마스터에 없는 값(레거시 데이터)이면 목록에 "현재값"으로 끼워 넣어,
    // 수정할 때 그 값이 조용히 비워지거나 마스터의 다른 값으로 바뀌지 않게 한다.
    const makerInMaster = makerOptions.some(m => m.makerKorName === v.makerName);
    const makerCurrentOpt = v.makerName && !makerInMaster
      ? `<option value="${esc(v.makerName)}" selected>${esc(v.makerName)} (현재값)</option>` : '';
    const makerOpts  = makerCurrentOpt + makerOptions.map(m =>
      `<option value="${esc(m.makerKorName)}" ${esc(v.makerName === m.makerKorName ? 'selected' : '')}>${esc(m.makerKorName)}</option>`
    ).join('');
    const originOpts = originOptions.map(o =>
      `<option value="${esc(o.originKorName)}" ${esc(v.originName === o.originKorName ? 'selected' : '')}>${esc(o.originKorName)}</option>`
    ).join('');
    const taxTypeOpts = taxTypeOptions.map(t =>
      `<option value="${esc(t.constrVal)}" ${esc(String(v.vatRate ?? 10) === t.constrVal ? 'selected' : '')}>${esc(t.constrValDesc)} (${esc(t.constrVal)}%)</option>`
    ).join('');
    const deliFeeTypeOpts = deliFeeTypeOptions.map(d =>
      `<option value="${esc(d.constrVal)}" ${esc(v.deliFeeType === d.constrVal ? 'selected' : '')}>${esc(d.constrValDesc)}</option>`
    ).join('');
    const saleYnOpts = saleYnTypeOptions.map(s =>
      `<option value="${esc(s.constrVal)}" ${esc(String(v.saleYn ?? 1) === s.constrVal ? 'selected' : '')}>${esc(s.constrValDesc)}</option>`
    ).join('');

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>상품코드</label>
          <input class="input" value="${esc(isNew ? newProdCode : v.prodCode)}" readonly style="background:#f8fafc;font-family:monospace">
        </div>
        <div class="form-group">
          <label>상점 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="pFShopCode" ${esc(!isNew ? 'disabled style="background:#f8fafc"' : '')}>
            <option value="">-- 선택 --</option>
            ${shopOpts}
          </select>
        </div>
        <div class="form-group full">
          <label>분류(카테고리) <span style="color:var(--danger)">*</span></label>
          <div style="display:flex;gap:8px">
            <input class="input" id="pFCatePath" value="${esc(selectedCatePath)}" readonly placeholder="분류를 선택하세요" style="background:#f8fafc;flex:1">
            <button type="button" class="btn btn-ghost" id="pFCateBtn">분류 선택</button>
          </div>
        </div>
        <div class="form-group full">
          <label>상품명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="pFProdName" value="${esc(v.prodName || '')}">
        </div>
        <div class="form-group full">
          <label>진열명</label>
          <input class="input" id="pFProdDsplName" value="${esc(v.prodDsplName || '')}">
        </div>
        <div class="form-group">
          <label>쇼핑몰상품코드</label>
          <input class="input" id="pFShopProdCode" value="${esc(v.shopProdCode || '')}">
        </div>
        <div class="form-group">
          <label>모델명</label>
          <input class="input" id="pFModelName" value="${esc(v.modelName || '')}">
        </div>
        <div class="form-group">
          <label>브랜드명</label>
          <select class="input" id="pFBrandName">
            <option value="">-- 선택 안 함 --</option>
            ${brandOpts}
          </select>
        </div>
        <div class="form-group">
          <label>제조사명 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="pFMakerName">
            <option value="">-- 선택 --</option>
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
          <input class="input" id="pFListPrice" type="number" min="0" value="${esc(v.listPrice ?? '')}">
        </div>
        <div class="form-group">
          <label>판매가</label>
          <input class="input" id="pFSalePrice" type="number" min="0" value="${esc(v.salePrice ?? '')}">
        </div>
        <div class="form-group">
          <label>공급가</label>
          <input class="input" id="pFSupplyPrice" type="number" min="0" value="${esc(v.supplyPrice ?? '')}">
        </div>
        <div class="form-group">
          <label>매입가</label>
          <input class="input" id="pFBuyPrice" type="number" min="0" value="${esc(v.buyPrice ?? '')}">
        </div>
        <div class="form-group">
          <label>부가세율</label>
          <select class="input" id="pFVatRate">
            ${taxTypeOpts}
          </select>
        </div>
        <div class="form-group">
          <label>재고수량</label>
          <input class="input" id="pFSupplyQty" type="number" min="0" value="${esc(v.supplyQty ?? 9999)}">
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
            <option value="0" ${esc((v.adultYn ?? 0) === 0 ? 'selected' : '')}>아니오</option>
            <option value="1" ${esc(v.adultYn === 1 ? 'selected' : '')}>예</option>
          </select>
        </div>
        <div class="form-group">
          <label>배송방법</label>
          <input class="input" id="pFDeliMethod" value="${esc(v.deliMethod || '')}">
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
          <input class="input" id="pFDeliFeeAmt" type="number" min="0" value="${esc(v.deliFeeAmt ?? 0)}">
        </div>
        <div class="form-group">
          <label>리드타임(일)</label>
          <input class="input" id="pFLeadTime" type="number" min="0" value="${esc(v.leadTime ?? '')}">
        </div>
        <div class="form-group full">
          <label>옵션명 (사이즈/색상 등, 최대 4개)</label>
          <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px">
            <input class="input" id="pFAttrName1" placeholder="예: 사이즈" value="${esc(v.attrName1 || '')}">
            <input class="input" id="pFAttrName2" placeholder="예: 색상" value="${esc(v.attrName2 || '')}">
            <input class="input" id="pFAttrName3" placeholder="옵션3명" value="${esc(v.attrName3 || '')}">
            <input class="input" id="pFAttrName4" placeholder="옵션4명" value="${esc(v.attrName4 || '')}">
          </div>
          <div style="font-size:11px;color:var(--text-muted);margin-top:4px">
            여기 입력한 이름이 아래 '옵션 관리'에서 사이즈/색상 등 실제 옵션값(재고·가격 포함)을
            등록할 때 항목명으로 쓰입니다. 옵션이 필요 없는 상품은 비워두세요.
          </div>
        </div>
        <div class="form-group full">
          <label>대표이미지</label>
          <div style="display:flex;align-items:center;gap:12px">
            <img id="pFImgPreview" src="${esc(imgUrls.imgUrl)}" style="width:72px;height:72px;object-fit:cover;border:1px solid var(--border);border-radius:var(--radius);background:#f8fafc;${esc(imgUrls.imgUrl ? '' : 'display:none')}">
            <div>
              <input type="file" id="pFImgFile" accept="image/*">
              <div id="pFImgStatus" style="font-size:12px;color:var(--text-muted);margin-top:4px">${esc(imgUrls.imgUrl ? '등록된 이미지가 있습니다. 변경하려면 새 파일을 선택하세요.' : '이미지를 선택하면 50/80/100/160/220/280px 썸네일이 자동 생성됩니다.')}</div>
            </div>
          </div>
        </div>
        <div class="form-group full">
          <label>상품설명</label>
          <div id="pFProdDescEditor" style="background:#fff"></div>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="pFRemark" value="${esc(v.remark || '')}">
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
        // 제조사는 필수 — 비어 있으면 샵링커 전송이 "제조업체 누락"으로 거절된다. 서버도 한 번 더 막는다.
        if (!document.getElementById('pFMakerName').value.trim()) { UI.toast('제조사를 선택하세요', 'error'); return; }

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
          attrName1:     document.getElementById('pFAttrName1').value.trim(),
          attrName2:     document.getElementById('pFAttrName2').value.trim(),
          attrName3:     document.getElementById('pFAttrName3').value.trim(),
          attrName4:     document.getElementById('pFAttrName4').value.trim(),
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
    // Quill 의 dangerouslyPasteHTML 은 받은 HTML 을 먼저 DOM 에 넣어 변환하기 때문에 <img onerror> 같은 값이
    // 그 순간 실행된다 — 반드시 정화한 HTML 을 넘긴다.
    if (v.prodDesc) quill.clipboard.dangerouslyPasteHTML(UI.sanitizeHtml(v.prodDesc));
  }

  return { render };
})();

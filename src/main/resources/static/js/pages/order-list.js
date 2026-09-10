/**
 * 주문관리 (메뉴 라벨 기준. 구 "주문 목록")
 *   shopion.tOrdOrderProd(주문상품 라인) 기준 목록 — 한 행 = 주문 1건의 상품 1줄(순번).
 *   백엔드는 /order/orderprod/list, /order/orderprod/list/count (OrderProdMapper.selectList).
 *   이 쿼리는 pSvcCode가 있어야 결제(OPay)/상품(P)/상점(S) 조인이 붙는다 —
 *   관리자(info.svcCode, 보통 SHP001)를 항상 실어 보낸다.
 * - 기간유형: '' 주문일자(OrderReciptDate) / P 지불일자(PayCmpletDate) /
 *   S 배송일자(ShipCmpletDate) / R 환불일자(RefundCmpletDate)
 * - 검색: 주문번호/주문자명/수취인명/주문상태 + 상품명/상품코드/전화번호/채널주문번호
 * - 행 클릭 또는 [상세]로 주문 상세 모달, [취소]로 주문(전체) 취소
 */
const PageOrderList = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;

  // ── 날짜 유틸 ──────────────────────────────────────────────────────
  function toDateStr(date) { return date.toISOString().slice(0, 10); }
  function defaultStartDate() { const d = new Date(); d.setDate(d.getDate() - 30); return toDateStr(d); }
  function defaultEndDate() { return toDateStr(new Date()); }

  // ── 표시 유틸 ──────────────────────────────────────────────────────
  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function fmtDt(s) { return s ? String(s).replace('T', ' ').substring(0, 16) : ''; }
  // 마진율/순마진율/채널수수료율 — 저장 컬럼이 없어 (판매가 × 수량) 대비 비율로 계산한다.
  function rate(part, o) {
    const base = (Number(o.salePrice) || 0) * (Number(o.prodQty) || 0);
    if (!base || part == null) return '';
    return (Number(part) / base * 100).toFixed(1) + '%';
  }

  // ── 컬럼 정의 ──────────────────────────────────────────────────────
  // 사용자 요청 순서 그대로. get(o, i) → 셀 값. align:'right'는 우측정렬, badge는 상태배지,
  // img는 썸네일. "옵션가"는 tOrdOrderProd에 대응 컬럼이 없어 표시만 하고 값은 비운다.
  const COLS = [
    { h: '번호',        w: 46,  get: (o, i) => (currentPage - 1) * PAGE_SIZE + i + 1, align: 'right' },
    { h: '주문번호',    w: 78,  get: o => o.orderNo },
    { h: '순번',        w: 40,  get: o => o.orderProdNo, align: 'right' },
    { h: '상담여부',    w: 58,  get: o => (o.custConsltYn === 1 ? '상담' : '미상담') },
    { h: '주문자',      w: 66,  get: o => o.orderName },
    { h: '수취인',      w: 66,  get: o => o.recverName },
    { h: '주문일자',    w: 120, get: o => fmtDt(o.orderReciptDate) },
    { h: '지불일자',    w: 120, get: o => fmtDt(o.payCmpletDate) },
    { h: '배송일자',    w: 120, get: o => fmtDt(o.shipCmpletDate) },
    { h: '환불일자',    w: 120, get: o => fmtDt(o.refundCmpletDate) },
    { h: '채널주문번호', w: 130, get: o => o.chnlOrderNo },
    { h: '주문상태',    w: 78,  get: o => o.orderState, badge: true },
    { h: '상품명',      w: 240, get: o => o.prodName },
    { h: '상품코드',    w: 88,  get: o => o.prodCode },
    { h: '상품옵션',    w: 180, get: o => o.itemName },
    { h: '세율',        w: 46,  get: o => (o.vatRate != null ? o.vatRate + '%' : ''), align: 'right' },
    { h: '업체',        w: 88,  get: o => o.shopName },
    { h: '상품담당',    w: 66,  get: o => o.shopChrgName },
    { h: '수량',        w: 44,  get: o => o.prodQty, align: 'right' },
    { h: '판매가',      w: 84,  get: o => won(o.salePrice), align: 'right' },
    { h: '옵션가',      w: 70,  get: () => '', align: 'right' },
    { h: '채널공급가',  w: 84,  get: o => won(o.supplyPrice), align: 'right' },
    { h: '원가',        w: 78,  get: o => won(o.buyPrice), align: 'right' },
    { h: '등록원가',    w: 78,  get: o => won(o.registBuyPrice), align: 'right' },
    { h: '개별배송구분', w: 66,  get: o => (o.bundleYn === 1 ? '묶음' : '개별') },
    { h: '배송비',      w: 70,  get: o => won(o.deliFeeAmt), align: 'right' },
    { h: '판촉비',      w: 70,  get: o => won(o.promotFeeAmt), align: 'right' },
    { h: '마진',        w: 80,  get: o => won(o.prodMargin), align: 'right' },
    { h: '마진율',      w: 58,  get: o => rate(o.prodMargin, o), align: 'right' },
    { h: '순마진',      w: 80,  get: o => won(o.netMargin), align: 'right' },
    { h: '순마진율',    w: 60,  get: o => rate(o.netMargin, o), align: 'right' },
    { h: '채널수수료',  w: 80,  get: o => won(o.chnlMargin), align: 'right' },
    { h: '채널수수료율', w: 66,  get: o => rate(o.chnlMargin, o), align: 'right' },
    { h: '채널담당',    w: 66,  get: o => o.chnlChrgName },
    { h: '채널',        w: 130, get: o => o.chnlName },
    { h: '전화번호1',   w: 106, get: o => o.orderPhoneNo },
    { h: '휴대번호1',   w: 106, get: o => o.orderMobileNo },
    { h: '전화번호2',   w: 106, get: o => o.recverPhoneNo },
    { h: '휴대번호2',   w: 106, get: o => o.recverMobileNo },
    { h: '우편번호',    w: 58,  get: o => o.recverZipcode },
    { h: '주소1',       w: 240, get: o => o.recverAddr1 },
    { h: '주소2',       w: 150, get: o => o.recverAddr2 },
    { h: '배송메모',    w: 150, get: o => o.deliMemo },
    { h: '배송회사',    w: 88,  get: o => (o.deliCompDesc || o.deliCompName) },
    { h: '송장번호',    w: 110, get: o => o.invNo },
    { h: '이미지',      w: 44,  get: o => o.prodImg, img: true },
    { h: '제조사',      w: 84,  get: o => o.makerName },
    { h: '모델',        w: 100, get: o => o.modelName },
    { h: '단위',        w: 46,  get: o => o.prodUnit },
    { h: '채널상품코드', w: 106, get: o => o.shopProdCode },
    { h: '상품코드',    w: 88,  get: o => o.prodCode },
    { h: '단품코드',    w: 76,  get: o => o.itemCode },
    { h: '공급사',      w: 96,  get: o => (o.supplyName || o.supplyCode) },
    { h: '결제방법',    w: 76,  get: o => o.payTypeName },
    { h: '결제회사',    w: 84,  get: o => (o.payCompName || o.cardComp || o.reciptBank) },
    { h: '승인번호',    w: 116, get: o => o.approvNo },
    { h: '상태',        w: 44,  get: o => (o.state == null ? '' : o.state) },
    { h: '비고',        w: 140, get: o => o.remark },
    { h: '등록아이디',  w: 84,  get: o => o.registId },
    { h: '등록명칭',    w: 76,  get: o => o.registName },
    { h: '등록일자',    w: 120, get: o => fmtDt(o.registDate) },
    { h: '변경아이디',  w: 84,  get: o => o.changeId },
    { h: '변경명칭',    w: 76,  get: o => o.changeName },
    { h: '변경일자',    w: 120, get: o => fmtDt(o.changeDate) },
  ];
  const ACT_W = 96;
  const TABLE_W = COLS.reduce((s, c) => s + c.w, 0) + ACT_W;

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">주문관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>기간 유형</label>
              <select class="input" id="olDateType" style="width:100px">
                <option value="">주문일자</option>
                <option value="P">지불일자</option>
                <option value="S">배송일자</option>
                <option value="R">환불일자</option>
              </select>
            </div>
            <div class="form-group">
              <label>시작일</label>
              <input class="input" id="olStartDate" type="date" value="${defaultStartDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>종료일</label>
              <input class="input" id="olEndDate" type="date" value="${defaultEndDate()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문상태</label>
              <select class="input" id="olOrderState" style="width:100px">
                <option value="">전체</option>
                <option value="주문접수">주문접수</option>
                <option value="지불완료">지불완료</option>
                <option value="출고지시">출고지시</option>
                <option value="발주확인">발주확인</option>
                <option value="배송시작">배송시작</option>
                <option value="배송완료">배송완료</option>
                <option value="주문취소">주문취소</option>
                <option value="반품요청">반품요청</option>
                <option value="반품완료">반품완료</option>
                <option value="환불완료">환불완료</option>
                <option value="결제오류">결제오류</option>
              </select>
            </div>
            <div class="form-group">
              <label>주문번호</label>
              <input class="input" id="olOrderNo" placeholder="주문번호" style="width:110px">
            </div>
            <div class="form-group">
              <label>주문자명</label>
              <input class="input" id="olOrderName" placeholder="주문자명" style="width:100px">
            </div>
            <div class="form-group">
              <label>수취인명</label>
              <input class="input" id="olRecverName" placeholder="수취인명" style="width:100px">
            </div>
            <div class="form-group">
              <label>상품명</label>
              <input class="input" id="olProdName" placeholder="상품명" style="width:150px">
            </div>
            <div class="form-group">
              <label>상품코드</label>
              <input class="input" id="olProdCode" placeholder="상품코드(정확히)" style="width:120px">
            </div>
            <div class="form-group">
              <label>전화번호</label>
              <input class="input" id="olPhoneNo" placeholder="주문/수취인 전화" style="width:130px">
            </div>
            <div class="form-group">
              <label>채널주문번호</label>
              <input class="input" id="olChnlOrderNo" placeholder="채널주문번호" style="width:130px">
            </div>
            <button class="btn btn-primary" id="olBtnSearch">검색</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="olTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="olTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="olPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('olBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['olOrderNo', 'olOrderName', 'olRecverName', 'olProdName', 'olProdCode', 'olPhoneNo', 'olChnlOrderNo'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });

    loadList();
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    const v = id => document.getElementById(id).value;
    return {
      pStartDate:     v('olStartDate'),
      pEndDate:       v('olEndDate'),
      pOrderDateType: v('olDateType'),
      pOrderState:    v('olOrderState'),
      pOrderNo:       v('olOrderNo').trim(),
      pOrderName:     v('olOrderName').trim(),
      pRecverName:    v('olRecverName').trim(),
      pProdName:      v('olProdName').trim(),
      pProdCode:      v('olProdCode').trim(),
      pPhoneNo:       v('olPhoneNo').trim(),
      pChnlOrderNo:   v('olChnlOrderNo').trim(),
      pSvcCode:       (typeof info !== 'undefined' && info?.svcCode) || 'SHP001',
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('olTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('olPagination').innerHTML = '';

    try {
      const params = getParams();

      totalCount = await Api.get('/order/orderprod/list/count', params);
      document.getElementById('olTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/order/orderprod/list', {
        ...params,
        topCnt:       (currentPage - 1) * PAGE_SIZE,
        countPerPage: PAGE_SIZE,
      });

      renderTable(list);
      renderPagination();
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  // ── 상태 배지 ─────────────────────────────────────────────────────
  function stateBadge(state) {
    const map = {
      '주문접수': 'badge-gray', '지불완료': 'badge-blue',
      '출고지시': 'badge-orange', '발주확인': 'badge-orange', '발주시작': 'badge-orange',
      '배송시작': 'badge-green', '배송완료': 'badge-green',
      '주문취소': 'badge-red', '반품요청': 'badge-red', '반품완료': 'badge-red',
      '환불완료': 'badge-red', '결제오류': 'badge-red',
    };
    return `<span class="badge ${map[state] || 'badge-gray'}">${esc(state) || '-'}</span>`;
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('olTableWrap');
    // 고정폭(table-layout:fixed) 셀은 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 옆 칸으로 삐져나온다. 열 폭 합계(TABLE_W)를 테이블 width로 그대로 주고,
    // 카드가 좁으면 table-wrap의 overflow-x:auto가 가로 스크롤을 띄운다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';
    const thEll = `${ell};max-width:0`;

    const rows = list.map((o, i) => {
      const tds = COLS.map(c => {
        const raw = c.get(o, i);
        if (c.img) {
          return `<td style="text-align:center;padding:2px">${raw
            ? `<img src="${esc(raw)}" style="width:32px;height:32px;object-fit:cover;border-radius:3px" onerror="this.style.display='none'">`
            : ''}</td>`;
        }
        const txt = raw == null ? '' : String(raw);
        if (c.badge) return `<td style="text-align:center">${stateBadge(txt)}</td>`;
        return `<td style="font-size:11px;${ell}${c.align === 'right' ? ';text-align:right' : ''}" title="${esc(txt)}">${esc(txt)}</td>`;
      }).join('');

      const canCancel = o.orderState !== '주문취소' && o.orderState !== '환불완료';
      return `
        <tr style="cursor:pointer" data-orderno="${o.orderNo}">
          ${tds}
          <td style="white-space:nowrap;text-align:center">
            <button class="btn btn-ghost" style="padding:2px 6px;font-size:11px" data-action="detail" data-orderno="${o.orderNo}">상세</button>
            ${canCancel ? `<button class="btn btn-danger" style="padding:2px 6px;font-size:11px" data-action="cancel" data-orderno="${o.orderNo}">취소</button>` : ''}
          </td>
        </tr>`;
    }).join('');

    wrap.innerHTML = `
      <table style="table-layout:fixed;width:${TABLE_W}px">
        <colgroup>
          ${COLS.map(c => `<col style="width:${c.w}px">`).join('')}
          <col style="width:${ACT_W}px">
        </colgroup>
        <thead>
          <tr>
            ${COLS.map(c => `<th style="${thEll}${c.align === 'right' ? ';text-align:right' : ''}">${c.h}</th>`).join('')}
            <th>관리</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=detail]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        openDetailModal(parseInt(btn.dataset.orderno));
      });
    });

    wrap.querySelectorAll('[data-action=cancel]').forEach(btn => {
      btn.addEventListener('click', e => {
        e.stopPropagation();
        UI.confirm(`주문번호 [${btn.dataset.orderno}]를 취소하시겠습니까?`, async close => {
          try {
            await Api.put('/order/order/cancel', { orderNo: parseInt(btn.dataset.orderno) });
            UI.toast('주문이 취소되었습니다', 'success');
            loadList();
          } catch (err) { UI.toast(err.message, 'error'); }
          close();
        });
      });
    });

    wrap.querySelectorAll('tbody tr').forEach(tr => {
      tr.addEventListener('click', () => openDetailModal(parseInt(tr.dataset.orderno)));
    });
  }

  // ── 페이지네이션 ───────────────────────────────────────────────────
  function renderPagination() {
    const totalPages = Math.ceil(totalCount / PAGE_SIZE);
    if (totalPages <= 1) return;

    const el = document.getElementById('olPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start - 1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end + 1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        currentPage = parseInt(btn.dataset.page);
        loadList();
      });
    });
  }

  // ── 주문 상세 모달 ─────────────────────────────────────────────────
  async function openDetailModal(orderNo) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';

    UI.modal({ title: `주문 상세 – #${orderNo}`, body, confirmText: null, cancelText: '닫기' });

    try {
      const svcCode = (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
      const [order, prods] = await Promise.all([
        Api.get('/order/order', { orderNo }),
        Api.get('/order/orderprod/list', { pOrderNo: orderNo, pSvcCode: svcCode }),
      ]);

      const row = (label, value) => `
        <div class="form-group">
          <label>${label}</label>
          <div class="input" style="background:#f8fafc;min-height:34px;line-height:34px;padding:0 10px;border:1px solid var(--border);border-radius:var(--radius)">${esc(value) || '-'}</div>
        </div>`;

      const prodRows = (prods || []).map(p => `
        <tr>
          <td style="font-size:12px">${esc(p.prodName)}</td>
          <td style="font-size:11px;color:var(--text-muted)">${esc(p.itemName)}</td>
          <td style="text-align:right;font-size:12px">${p.prodQty || ''}</td>
          <td style="text-align:right;font-size:12px">${p.salePrice != null ? won(p.salePrice) + '원' : '-'}</td>
          <td style="text-align:center;font-size:12px">${esc(p.invNo) || '-'}</td>
          <td style="text-align:center">${stateBadge(p.orderState)}</td>
        </tr>`).join('');

      body.innerHTML = `
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;letter-spacing:.05em">주문 정보</div>
        <div class="form-grid" style="margin-bottom:16px">
          ${row('주문번호', order.orderNo)}
          ${row('주문일자', fmtDt(order.orderReciptDate))}
          ${row('주문자', order.orderName)}
          ${row('연락처', order.orderMobileNo || order.orderPhoneNo)}
          ${row('수취인', order.recverName)}
          ${row('수취인 연락처', order.recverMobileNo || order.recverPhoneNo)}
          ${row('배송지', (order.recverAddr1 || '') + ' ' + (order.recverAddr2 || ''))}
          ${row('배송 메모', order.deliMemo)}
          ${row('주문상태', order.orderState)}
          ${row('결제수단', order.payTypeName || order.payType)}
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:8px;letter-spacing:.05em">주문 상품</div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>상품명</th><th>옵션</th>
                <th style="text-align:right">수량</th>
                <th style="text-align:right">금액</th>
                <th style="text-align:center">송장번호</th>
                <th style="text-align:center">상태</th>
              </tr>
            </thead>
            <tbody>${prodRows || '<tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:16px">주문 상품 없음</td></tr>'}</tbody>
          </table>
        </div>`;
    } catch (e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  return { render };
})();

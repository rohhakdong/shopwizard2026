/**
 * 주문 상세 (주문관리의 [상세] / 행 클릭) — 레거시 주문상세(orderDetailGridView.jsp)처럼 한 화면에 위아래로 쌓는다.
 *   기본정보(주문·지불·지시·발주·출고·수령·취소 일자) / 고객정보 / 고객상담(등록·처리내용 수정) / 주문내역(송장 조회 링크) /
 *   배송·반품내역(출고·반품 사본 전체) / 결제내역.
 * 상담에는 고객 연락처가 들어 있어 상점 제한 계정(info.shopCode)에게는 상담 패널을 보이지 않는다(서버도 403).
 * 백엔드: /order/order, /order/orderprod/list, /order/order/history, /order/order/conslt, /code/constr-val
 */
const PageOrderDetail = (() => {

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  const won = n => (n == null || n === '') ? '' : Number(n).toLocaleString();
  const num = n => Number(n) || 0;
  const fmt = s => s ? String(s).replace('T', ' ').substring(0, 16) : '';
  const dash = v => (v == null || v === '') ? '-' : esc(v);
  const hideConslt = () => typeof info !== 'undefined' && !!info && !!info.shopCode;

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

  const cell = (label, value) => `
    <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:2px">${esc(label)}</div>
    <div style="font-size:13px;min-height:18px">${dash(value)}</div></div>`;
  const grid = (cols, cells) => `<div style="display:grid;grid-template-columns:repeat(${cols},1fr);gap:10px 16px">${cells.join('')}</div>`;
  const section = (title, inner, extra) => `
    <div style="margin-top:22px">
      <div style="display:flex;align-items:center;justify-content:space-between;font-size:13px;font-weight:600;border-bottom:2px solid var(--border);padding-bottom:6px;margin-bottom:10px">
        <span>${title}</span>${extra || ''}
      </div>${inner}
    </div>`;
  const empty = (msg, cols) => `<tr><td colspan="${cols}" style="text-align:center;color:var(--text-muted);padding:14px">${msg}</td></tr>`;
  const table = (heads, rows) => `
    <div class="table-wrap" style="border:1px solid var(--border);border-radius:var(--radius)">
      <table style="width:100%;font-size:12px"><thead><tr>${heads.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows}</tbody></table>
    </div>`;

  // ── 송장 조회 링크 ─────────────────────────────────────────────────
  // 택배사 조회 주소(linkUrl)가 http(s)일 때만 링크로 만든다. invNoVar 가 있는 택배사는 그 이름의 POST 값으로 보내고,
  // 없으면 조회 주소 뒤에 송장번호(하이픈 제거)를 붙인다.
  function trackLink(invNo, linkUrl, invNoVar) {
    if (!invNo) return '-';
    if (!linkUrl || !/^https?:\/\//i.test(linkUrl)) return esc(invNo);
    return `<a href="#" data-track="1" data-inv="${esc(invNo)}" data-url="${esc(linkUrl)}" data-var="${esc(invNoVar || '')}">${esc(invNo)}</a>`;
  }
  function bindTrackLinks(root) {
    root.querySelectorAll('a[data-track]').forEach(a => {
      if (a.dataset.bound) return;
      a.dataset.bound = '1';
      a.addEventListener('click', e => {
        e.preventDefault();
        const inv = a.dataset.inv.replace(/-/g, '');
        if (a.dataset.var) {
          const f = document.createElement('form');
          f.method = 'POST'; f.action = a.dataset.url; f.target = '_blank';
          const i = document.createElement('input');
          i.type = 'hidden'; i.name = a.dataset.var; i.value = inv;
          f.appendChild(i); document.body.appendChild(f); f.submit(); f.remove();
        } else {
          window.open(a.dataset.url + encodeURIComponent(inv), '_blank', 'noopener');
        }
      });
    });
  }

  // ── 열기 ───────────────────────────────────────────────────────────
  /** @param onChanged 상담 등록처럼 주문 목록에 영향을 주는 변경이 있었을 때 부르는 콜백(목록 새로고침) */
  async function open(orderNo, onChanged) {
    const body = document.createElement('div');
    body.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-muted)">불러오는 중...</div>';
    const modal = UI.modal({ title: `주문 상세 – #${orderNo}`, body, confirmText: null, cancelText: '닫기' });
    modal.el.querySelector('.modal').style.width = '1000px';

    try {
      const svcCode = (typeof info !== 'undefined' && info?.svcCode) || 'SHP001';
      const [order, prods, history] = await Promise.all([
        Api.get('/order/order', { orderNo }),
        Api.get('/order/orderprod/list', { pOrderNo: orderNo, pSvcCode: svcCode, pProdJoin: 'LEFT' }),
        Api.get('/order/order/history', { orderNo }),
      ]);
      render(body, orderNo, order || {}, prods || [], history || {}, onChanged);
    } catch (e) {
      body.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function render(body, orderNo, order, prods, history, onChanged) {
    const ships = history.ships || [], returns = history.returns || [], pays = history.pays || [];
    const showConslt = !hideConslt();

    const basic = grid(4, [
      cell('주문번호', order.orderNo), cell('주문상태', order.orderState),
      cell('채널', (prods[0] && prods[0].chnlName) || order.chnlCode), cell('결제수단', pays[0] && (pays[0].payTypeName || pays[0].payType)),
      cell('주문일자', fmt(order.orderReciptDate)), cell('지불일자', fmt(order.payCmpletDate)),
      cell('지시일자', fmt(order.shipDirectDate)), cell('발주일자', fmt(order.shipCheckDate)),
      cell('출고일자', fmt(order.shipCmpletDate)), cell('수령일자', fmt(order.deliCmpletDate)),
      cell('취소일자', fmt(order.orderCancelDate)), cell('환불완료일자', fmt(order.refundCmpletDate)),
    ]);

    const addr = [order.recverZipcode, order.recverAddr1, order.recverAddr2].filter(Boolean).join(' ');
    const customer = grid(4, [
      cell('주문자 아이디', order.loginId), cell('주문자', order.orderName),
      cell('주문자 전화', order.orderPhoneNo), cell('주문자 휴대폰', order.orderMobileNo),
      cell('주문자 이메일', order.orderEmail), cell('수취인', order.recverName),
      cell('수취인 전화', order.recverPhoneNo), cell('수취인 휴대폰', order.recverMobileNo),
    ]) + `<div style="margin-top:10px">${grid(1, [cell('배송지', addr)])}</div>`
       + `<div style="margin-top:10px">${grid(2, [cell('주문메모', order.orderMemo), cell('배송메모', order.deliMemo)])}</div>`;

    const prodRows = prods.map(p => {
      const ship = ships.find(s => String(s.orderProdNo) === String(p.orderProdNo) && s.invNo === p.invNo);
      const inv = trackLink(p.invNo, (ship && ship.linkUrl) || p.linkUrl, (ship && ship.invNoVar) || p.invNoVar);
      return `
        <tr>
          <td style="text-align:right">${esc(p.orderProdNo)}</td>
          <td style="max-width:240px">${esc(p.prodName)}<div style="font-size:11px;color:var(--text-muted)">${esc(p.shopProdCode)}</div></td>
          <td style="color:var(--text-muted)">${esc(p.itemName)}</td>
          <td style="text-align:right">${esc(p.prodQty)}</td>
          <td style="text-align:right">${p.salePrice != null ? won(num(p.salePrice) + num(p.nvPrice)) + '원' : '-'}</td>
          <td>${esc(p.chnlOrderNo)}</td>
          <td style="text-align:center">${stateBadge(p.orderState)}</td>
          <td style="text-align:center">${inv}</td>
          <td style="text-align:center">${p.custConsltYn === 1 ? '<span class="badge badge-blue">상담</span>' : ''}</td>
        </tr>`;
    }).join('');
    const prodTable = table(['순번', '상품명', '옵션', '수량', '금액', '채널주문번호', '상태', '송장번호', '상담'],
      prodRows || empty('주문 상품 없음', 9));

    const shipTable = table(['줄-차수', '상품', '수량', '택배사', '송장번호', '일자 (지시 / 발주확인 / 출고 / 배송완료 / 취소)', '상태'],
      ships.map(s => `
        <tr>
          <td style="white-space:nowrap">${esc(s.orderProdNo)}-${esc(s.orderChangeNo)}</td>
          <td>${esc(s.prodName)}<div style="font-size:11px;color:var(--text-muted)">${esc(s.itemName)}</div></td>
          <td style="text-align:right">${esc(s.prodQty)}</td>
          <td>${dash(s.deliCompName)}</td>
          <td>${trackLink(s.invNo, s.linkUrl, s.invNoVar)}
            ${s.prevInvNo ? `<div style="font-size:11px;color:var(--text-muted)">이전 ${esc(s.prevInvNo)}${s.invChangeReason ? ' · ' + esc(s.invChangeReason) : ''}${s.invChangeReasonDetail ? ' (' + esc(s.invChangeReasonDetail) + ')' : ''}</div>` : ''}</td>
          <td style="line-height:1.6;white-space:nowrap">
            지시 ${dash(fmt(s.shipDirectDate))}<br>발주확인 ${dash(fmt(s.shipCheckDate))}<br>출고 ${dash(fmt(s.shipCmpletDate))}<br>배송완료 ${dash(fmt(s.deliCmpletDate))}
            ${(s.shipCancelRqstDate || s.shipCancelCmpletDate) ? `<br><span style="color:var(--danger)">취소 ${dash(fmt(s.shipCancelRqstDate))} / ${dash(fmt(s.shipCancelCmpletDate))}</span>` : ''}</td>
          <td style="text-align:center">${stateBadge(s.orderState)}</td>
        </tr>`).join('') || empty('출고 내역 없음', 7));

    const returnTable = returns.length ? table(['줄-차수', '상품', '수량', '택배사', '송장번호', '반품사유', '일자 (접수 / 요청 / 확인 / 완료)', '상태'],
      returns.map(r => `
        <tr>
          <td style="white-space:nowrap">${esc(r.orderProdNo)}-${esc(r.orderChangeNo)}</td>
          <td>${esc(r.prodName)}<div style="font-size:11px;color:var(--text-muted)">${esc(r.itemName)}</div></td>
          <td style="text-align:right">${esc(r.prodQty)}</td>
          <td>${dash(r.deliCompName)}</td>
          <td>${trackLink(r.invNo, r.linkUrl, r.invNoVar)}</td>
          <td>${dash(r.returnReason)}${r.returnReasonDetail ? `<div style="font-size:11px;color:var(--text-muted)">${esc(r.returnReasonDetail)}</div>` : ''}</td>
          <td style="line-height:1.6;white-space:nowrap">${dash(fmt(r.returnReciptDate))} / ${dash(fmt(r.returnRqstDate))} / ${dash(fmt(r.returnConfrmDate))} / ${dash(fmt(r.returnCmpletDate))}</td>
          <td style="text-align:center">${stateBadge(r.orderState)}</td>
        </tr>`).join('')) : '';

    const payTable = table(['번호', '결제수단', '결제예정금액', '결제금액', '금융기관', '승인번호', '결제일', 'PG'],
      pays.map(p => `
        <tr>
          <td style="text-align:right">${esc(p.payNo)}</td>
          <td>${dash(p.payTypeName || p.payType)}</td>
          <td style="text-align:right">${p.payExpectAmt != null ? won(p.payExpectAmt) + '원' : '-'}</td>
          <td style="text-align:right">${p.payAmt != null ? won(p.payAmt) + '원' : '-'}</td>
          <td>${dash(p.payCompName)}</td>
          <td>${dash(p.approvNo)}</td>
          <td style="white-space:nowrap">${dash(fmt(p.payDate || p.approvDate))}</td>
          <td>${dash(p.pgCode)}</td>
        </tr>`).join('') || empty('결제 내역 없음', 8));

    body.innerHTML =
      section('기본정보', basic) +
      section('고객정보', customer) +
      (showConslt ? section('고객상담 <span id="odConsltCount" style="font-weight:400;color:var(--text-muted);font-size:12px"></span>',
        '<div id="odConsltBox"><div style="color:var(--text-muted);padding:10px">불러오는 중...</div></div>',
        '<button class="btn btn-ghost" id="odConsltAdd" type="button">+ 상담 등록</button>') : '') +
      section('주문내역', prodTable) +
      section('배송·반품내역', `<div style="font-size:12px;color:var(--text-muted);margin-bottom:6px">출고</div>${shipTable}`
        + (returnTable ? `<div style="font-size:12px;color:var(--text-muted);margin:12px 0 6px">반품</div>${returnTable}` : '')) +
      section('결제내역', payTable);

    bindTrackLinks(body);
    if (showConslt) initConslt(body, orderNo, prods, order, onChanged);
  }

  // ── 고객상담 ───────────────────────────────────────────────────────
  async function initConslt(body, orderNo, prods, order, onChanged) {
    const box = body.querySelector('#odConsltBox');
    const addBtn = body.querySelector('#odConsltAdd');
    let codes = null;   // { type, reason, media } — 사용 중인 코드만

    async function loadCodes() {
      if (codes) return codes;
      const get = c => Api.get('/code/constr-val', { pConstrCode: c, sidx: 'ConstrValSeq', sord: 'ASC' })
        .then(l => (l || []).filter(x => String(x.state ?? '1') === '1')).catch(() => []);
      const [type, reason, media] = await Promise.all([get('cCustConsltType'), get('cCustConsltReasonType'), get('cConsltMediaType')]);
      codes = { type, reason, media };
      return codes;
    }

    async function reload() {
      try {
        const list = await Api.get('/order/order/conslt', { orderNo });
        body.querySelector('#odConsltCount').textContent = `(${list.length}건)`;
        box.querySelectorAll('.od-conslt-list').forEach(n => n.remove());
        const wrap = document.createElement('div');
        wrap.className = 'od-conslt-list';
        wrap.innerHTML = table(['번호', '접수일시', '구분 / 사유', '상품', '상담내용', '처리내용', '담당', ''],
          list.map(c => `
            <tr>
              <td style="text-align:right">${esc(c.custConsltNo)}</td>
              <td style="white-space:nowrap">${dash(fmt(c.consltDate))}</td>
              <td>${dash(c.custConsltTypeName || c.custConsltType)}<div style="font-size:11px;color:var(--text-muted)">${esc(c.custConsltReasonTypeName || c.custConsltReasonType)}${c.consltMediaTypeName ? ' · ' + esc(c.consltMediaTypeName) : ''}</div></td>
              <td style="max-width:160px">${c.orderProdNo ? esc(c.prodName || c.prodCode) + '<div style="font-size:11px;color:var(--text-muted)">' + esc(c.itemName) + '</div>' : '<span style="color:var(--text-muted)">주문 전체</span>'}</td>
              <td style="white-space:pre-wrap;max-width:260px">${esc(c.consltCntnts)}</td>
              <td style="white-space:pre-wrap;max-width:260px">${c.answrCntnts ? esc(c.answrCntnts) + '<div style="font-size:11px;color:var(--text-muted)">' + esc(fmt(c.answrDate)) + '</div>' : '<span style="color:var(--text-muted)">-</span>'}</td>
              <td style="white-space:nowrap">${dash(c.mngrName || c.mngrId)}</td>
              <td><button class="btn btn-ghost" data-answer="${esc(c.custConsltNo)}" type="button">${c.answrCntnts ? '처리 수정' : '처리 등록'}</button></td>
            </tr>`).join('') || empty('등록된 상담이 없습니다', 8));
        box.appendChild(wrap);
        wrap.querySelectorAll('[data-answer]').forEach(b => b.addEventListener('click', () => {
          const c = list.find(x => String(x.custConsltNo) === b.dataset.answer);
          openAnswerModal(c, async () => { await reload(); });
        }));
      } catch (e) {
        box.innerHTML = `<div style="color:var(--danger);padding:10px">${esc(e.message)}</div>`;
      }
    }

    function openAnswerModal(c, done) {
      const b = document.createElement('div');
      b.innerHTML = `
        <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">상담 #${esc(c.custConsltNo)} · ${esc(c.custConsltTypeName || c.custConsltType)}</div>
        <div style="white-space:pre-wrap;font-size:12px;background:#f8fafc;border:1px solid var(--border);border-radius:var(--radius);padding:8px;margin-bottom:10px;max-height:120px;overflow:auto">${esc(c.consltCntnts)}</div>
        <div class="form-group"><label>처리내용 *</label><textarea class="input" id="odAnswer" rows="5" maxlength="10000" style="resize:vertical">${esc(c.answrCntnts || '')}</textarea></div>`;
      UI.modal({
        title: c.answrCntnts ? '처리내용 수정' : '처리내용 등록', body: b, confirmText: '저장',
        onConfirm: async close => {
          const text = b.querySelector('#odAnswer').value.trim();
          if (!text) { UI.toast('처리내용을 입력하세요', 'error'); return; }
          try {
            await Api.put('/order/order/conslt', { custConsltNo: c.custConsltNo, answrCntnts: text });
            UI.toast('처리내용을 저장했습니다', 'success');
            close();
            await done();
          } catch (e) { UI.toast(e.message, 'error'); }
        },
      });
    }

    async function toggleForm() {
      const existing = box.querySelector('#odConsltForm');
      if (existing) { existing.remove(); return; }
      const cd = await loadCodes();
      const opts = list => list.map(x => `<option value="${esc(x.constrVal)}">${esc(x.constrValDesc)}</option>`).join('');
      const lineOpts = prods.map(p => `<option value="${esc(p.orderProdNo)}">${esc(p.orderProdNo)}. ${esc(p.prodName)}${p.itemName ? ' / ' + esc(p.itemName) : ''}</option>`).join('');
      const form = document.createElement('div');
      form.id = 'odConsltForm';
      form.style.cssText = 'border:1px solid var(--border);border-radius:var(--radius);padding:12px;margin-bottom:12px;background:#f8fafc';
      form.innerHTML = `
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px 14px">
          <div class="form-group"><label>상품</label><select class="input" id="odcLine"><option value="">주문 전체</option>${lineOpts}</select></div>
          <div class="form-group"><label>상담구분 *</label><select class="input" id="odcType"><option value="">선택</option>${opts(cd.type)}</select></div>
          <div class="form-group"><label>상담사유 *</label><select class="input" id="odcReason"><option value="">선택</option>${opts(cd.reason)}</select></div>
          <div class="form-group"><label>상담매체</label><select class="input" id="odcMedia"><option value="">선택 안 함</option>${opts(cd.media)}</select></div>
          <div class="form-group"><label>고객명</label><input class="input" id="odcName" maxlength="50" value="${esc(order.orderName)}"></div>
          <div class="form-group"><label>연락처</label><input class="input" id="odcMobile" maxlength="50" value="${esc(order.orderMobileNo || order.orderPhoneNo)}"></div>
        </div>
        <div class="form-group" style="margin-top:10px"><label>상담내용 *</label><textarea class="input" id="odcContent" rows="3" maxlength="10000" style="resize:vertical"></textarea></div>
        <div class="form-group" style="margin-top:10px"><label>처리내용</label><textarea class="input" id="odcAnswer" rows="2" maxlength="10000" style="resize:vertical"></textarea></div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:10px">
          <button class="btn btn-ghost" id="odcCancel" type="button">취소</button>
          <button class="btn btn-primary" id="odcSave" type="button">등록</button>
        </div>`;
      box.prepend(form);
      form.querySelector('#odcCancel').addEventListener('click', () => form.remove());
      form.querySelector('#odcSave').addEventListener('click', async () => {
        const v = id => form.querySelector('#' + id).value.trim();
        if (!v('odcType')) return UI.toast('상담구분을 선택하세요', 'error');
        if (!v('odcReason')) return UI.toast('상담사유를 선택하세요', 'error');
        if (!v('odcContent')) return UI.toast('상담내용을 입력하세요', 'error');
        const save = form.querySelector('#odcSave');
        save.disabled = true;
        try {
          await Api.post('/order/order/conslt', {
            orderNo, orderProdNo: v('odcLine') ? Number(v('odcLine')) : null,
            custConsltType: v('odcType'), custConsltReasonType: v('odcReason'), consltMediaType: v('odcMedia') || null,
            custName: v('odcName'), custMobileNo: v('odcMobile'), consltCntnts: v('odcContent'), answrCntnts: v('odcAnswer') || null,
          });
          UI.toast('상담을 등록했습니다', 'success');
          form.remove();
          await reload();
          if (typeof onChanged === 'function') onChanged();   // 목록의 "상담여부"
        } catch (e) {
          UI.toast(e.message, 'error');
          save.disabled = false;
        }
      });
    }

    addBtn.addEventListener('click', toggleForm);
    await reload();
  }

  return { open };
})();

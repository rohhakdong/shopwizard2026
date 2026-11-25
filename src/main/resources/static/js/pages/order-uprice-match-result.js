/**
 * 단가매칭결과 (사이드 메뉴 발주) — 레거시 wOrdOrderUpriceMatchList 의 결과 조회
 *   상품명+옵션을 발주단가코드×수량 구성으로 분해한 규칙(shopwizard.tOrdUpriceMatch)을 규칙별로 조회·등록·수정·삭제한다.
 *   [발주단가매칭] 화면이 저장하는 규칙이고, 출고지시가 이 규칙으로 원가(BuyPrice)를 계산한다 — 규칙이 없으면 출고지시가 막힌다.
 *   원가 합계는 출고지시와 같이 "사용 중인" 발주단가코드만 센다. 사용 중지·삭제된 코드가 섞인 규칙은 경고로 표시한다.
 *   삭제하면 그 상품은 다시 [발주단가매칭] 의 미매칭 목록에 나타난다(이미 들어온 주문의 원가는 출고지시 때 다시 계산된다).
 *   상품명·옵션은 주문 줄 원문과 정확히 비교되므로 주문관리에서 복사한 그대로 넣는다.
 *   백엔드: /order/uprice-match (list, count, lines, DELETE ?prodName&itemName, POST /save), /order/uprice/list(코드 선택),
 *   POST /order/uprice-match/legacy-migrate/preview|commit (레거시 이관 — 제한 없는 계정만)
 */
const PageOrderUpriceMatchResult = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;
  const won = n => (n == null || n === '') ? '' : Number(n).toLocaleString();

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>단가매칭결과</span>
          <span>
            <button class="btn btn-ghost" id="umrBtnLegacy" type="button">레거시 이관</button>
            <button class="btn btn-primary" id="umrBtnNew" type="button">+ 등록</button>
          </span>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="font-size:12px;color:var(--text-muted);line-height:1.6;margin-bottom:10px">
            상품명+옵션별 발주단가 구성입니다. 출고지시가 이 규칙으로 원가를 계산하며, 규칙이 없는 주문은 출고지시할 수 없습니다.
            이미 들어온 미매칭 주문은 <b>발주단가매칭</b> 화면에서 연결하세요.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상품명</label><input class="input" id="umrName" style="width:240px"></div>
            <div class="form-group"><label>옵션</label><input class="input" id="umrItem" style="width:180px"></div>
            <button class="btn btn-primary" id="umrBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="umrTotal">0</strong>건</div>
          <div class="table-wrap" id="umrWrap"></div>
          <div id="umrPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('umrBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('umrBtnNew').addEventListener('click', () => openModal(null));
    document.getElementById('umrBtnLegacy').addEventListener('click', openLegacyModal);
    ['umrName', 'umrItem'].forEach(id =>
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadList();
  }

  // ── 레거시 발주단가매칭 이관 ────────────────────────────────────────
  // 레거시가 주문 라인마다 풀어 둔 구성(tOrdCurrOrderProd)을 원본 주문 라인의 상품명·옵션(새 규칙 키)으로 바꿔 미리 보고,
  // 이관 대상(NEW)만 저장한다. 이미 있는 규칙은 덮어쓰지 않고, 사용 중지 코드를 쓰는 규칙은 "검토 필요"로 두고 이관하지 않는다.
  const STATUS_TEXT = {
    NEW: '이관 대상', SAME: '이미 같음', CONFLICT_EXISTING: '기존 규칙과 다름(건너뜀)', CONFLICT_LEGACY: '레거시 안에서 구성 불일치',
    INACTIVE_CODE: '사용 중지 코드(검토 필요)', MISSING_CODE: '코드 없음', ANOMALY_QTY: '수량 이상', TOO_MANY: '30줄 초과',
  };

  function openLegacyModal() {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);line-height:1.6;margin-bottom:10px">
        레거시 발주단가매칭 데이터를 새 규칙으로 옮깁니다. 키는 <b>레거시가 풀어 둔 주문 라인의 상품명·옵션</b>이고, 수량은 레거시 라인 합계를 주문수량으로 나눈 <b>상품 1개분</b>입니다.
        먼저 [미리보기]로 확인한 뒤 [이관 확정]을 누르세요. 이미 있는 규칙은 덮어쓰지 않으며, 사용 중지 코드가 섞인 규칙은 이관하지 않습니다(발주단가표를 정리한 뒤 다시 실행).
        '힣' 사은품 구성은 일반 줄로 이관됩니다. 되돌리려면 등록자가 MIGRATE-LEGACY 인 규칙을 지우면 됩니다.
      </div>
      <div style="display:flex;gap:8px;align-items:flex-end;margin-bottom:10px">
        <div class="form-group" style="margin:0"><label>상품명 시작(선택)</label><input class="input" id="umrLgPrefix" style="width:220px" placeholder="비우면 전체"></div>
        <button class="btn btn-ghost" id="umrLgPreview" type="button">미리보기</button>
        <button class="btn btn-primary" id="umrLgCommit" type="button" disabled>이관 확정 (<span id="umrLgNew">0</span>건)</button>
      </div>
      <div id="umrLgResult" style="font-size:12px"></div>`;
    const modal = UI.modal({ title: '레거시 발주단가매칭 이관', body, confirmText: null, cancelText: '닫기' });
    modal.el.querySelector('.modal').style.width = '900px';
    const q = id => body.querySelector('#' + id);
    const prefix = () => q('umrLgPrefix').value.trim();

    q('umrLgPreview').addEventListener('click', async () => {
      q('umrLgResult').innerHTML = '<div style="padding:16px;color:var(--text-muted)">계산 중...</div>';
      q('umrLgCommit').disabled = true;
      try {
        const r = await Api.post('/order/uprice-match/legacy-migrate/preview', { keyPrefix: prefix() });
        const n = (r.counts && r.counts.NEW) || 0;
        q('umrLgNew').textContent = n;
        q('umrLgCommit').disabled = n === 0;
        const counts = Object.entries(r.counts || {}).map(([k, v]) => `${STATUS_TEXT[k] || k} <b>${v}</b>`).join(' · ');
        q('umrLgResult').innerHTML = `
          <div style="margin-bottom:8px">레거시 규칙 ${won(r.legacyHeaders)}건 중 새 키로 복원된 <b>${won(r.keys)}</b>개 — ${counts || '없음'}
            ${r.unmappable && r.unmappable.length ? `<br><span style="color:#b45309">⚠ 주문 행이 없어 키를 복원하지 못한 레거시 규칙 ${r.unmappable.length}건은 발주단가매칭 화면에서 다시 등록하세요.</span>` : ''}</div>
          <div class="table-wrap" style="max-height:42vh;border:1px solid var(--border);border-radius:var(--radius)">
            <table style="width:100%;font-size:12px"><thead><tr><th>상태</th><th>상품명</th><th>옵션</th><th>구성(코드 × 1개분 수량)</th><th style="text-align:right">주문</th></tr></thead>
              <tbody>${r.rows.map(c => `<tr><td>${esc(STATUS_TEXT[c.status] || c.status)}</td><td style="max-width:260px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(c.prodName)}">${esc(c.prodName)}</td>
                <td>${esc(c.itemName)}</td><td>${esc((c.lines || []).map(l => `${l.code} × ${l.qty}`).join(' + '))}${c.giftLines ? ` <span style="color:var(--text-muted)">(사은품 ${c.giftLines}줄)</span>` : ''}</td>
                <td style="text-align:right">${esc(c.orders)}</td></tr>`).join('')}</tbody></table></div>
          ${r.truncated ? '<div style="color:var(--text-muted);margin-top:6px">표에는 앞 1,000개만 보여 줍니다.</div>' : ''}`;
      } catch (e) { q('umrLgResult').innerHTML = `<div style="color:var(--danger)">${esc(e.message)}</div>`; }
    });

    q('umrLgCommit').addEventListener('click', () => {
      UI.confirm(`이관 대상 ${q('umrLgNew').textContent}건을 새 규칙으로 저장하시겠습니까? (이미 있는 규칙은 건드리지 않습니다)`, async close => {
        try {
          const r = await Api.post('/order/uprice-match/legacy-migrate/commit', { keyPrefix: prefix() });
          UI.toast(`${r.inserted}건 이관 · 건너뜀 ${r.skipped} · 실패 ${r.failed}`, r.failed ? 'error' : 'success');
          close();
          q('umrLgPreview').click();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); close(); }
      });
    });
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('umrName')) p.pProdName = v('umrName');
    if (v('umrItem')) p.pItemName = v('umrItem');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('umrWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/uprice-match/count', params);
      document.getElementById('umrTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('umrPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/uprice-match/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('umrPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('umrWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>상품명</th><th>옵션</th><th>구성 (발주단가코드 x 수량)</th><th style="text-align:right">원가 합계</th><th>상태</th><th>등록일</th><th></th></tr></thead>
        <tbody>${list.map((r, i) => `
          <tr>
            <td style="max-width:260px">${esc(r.prodName)}</td>
            <td style="max-width:160px;color:var(--text-muted)">${esc(r.itemName)}</td>
            <td style="max-width:300px">${esc(r.composition)}</td>
            <td style="text-align:right">${won(r.totalBuy)}원</td>
            <td>${Number(r.invalidCnt) > 0 ? `<span class="badge badge-red" title="사용 중지되었거나 발주단가표에서 지워진 코드는 원가에 반영되지 않습니다">사용 불가 코드 ${esc(r.invalidCnt)}</span>` : '<span class="badge badge-green">정상</span>'}</td>
            <td style="white-space:nowrap;color:var(--text-muted)">${esc((r.registDate || '').substring(0, 10))}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const r = list[+b.dataset.del];
      UI.confirm(`[${r.prodName}${r.itemName ? ' / ' + r.itemName : ''}] 단가매칭 규칙을 삭제하시겠습니까? 이 상품은 다시 발주단가매칭 대상이 되고, 규칙이 없으면 출고지시가 막힙니다.`, async close => {
        try {
          await Api.delete('/order/uprice-match', { prodName: r.prodName, itemName: r.itemName || '' });
          UI.toast('삭제되었습니다', 'success');
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
        close();
      });
    }));
  }

  // ── 등록/수정 모달 ─────────────────────────────────────────────────
  async function openModal(rule) {
    const isNew = !rule;
    // 각 줄: { matchUpriceCode, keywrd, capa, uprice, useYn, qty }
    const lines = [];
    if (!isNew) {
      try {
        const existing = await Api.get('/order/uprice-match/lines', { prodName: rule.prodName, itemName: rule.itemName || '' });
        (existing || []).forEach(d => lines.push({
          matchUpriceCode: d.matchUpriceCode, keywrd: d.keywrd, capa: d.capa, uprice: d.uprice, useYn: d.upriceUseYn, qty: d.qty,
        }));
      } catch (e) { return UI.toast(e.message, 'error'); }
    }
    if (!lines.length) lines.push({ matchUpriceCode: '', qty: 1 });

    const lock = isNew ? '' : 'readonly style="background:#f1f5f9"';
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full"><label>상품명 *</label><input class="input" id="umrFName" maxlength="255" value="${esc(rule && rule.prodName)}" ${lock} placeholder="주문관리의 상품명 그대로"></div>
        <div class="form-group full"><label>옵션</label><input class="input" id="umrFItem" maxlength="255" value="${esc(rule && rule.itemName)}" ${lock} placeholder="주문의 옵션(없으면 비움)"></div>
      </div>
      <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin:14px 0 6px">발주단가 구성</div>
      <table style="width:100%;font-size:12px">
        <thead><tr><th>발주단가코드</th><th style="text-align:right;width:90px">단가</th><th style="width:80px">수량</th><th style="width:50px"></th></tr></thead>
        <tbody id="umrLines"></tbody>
      </table>
      <button class="btn btn-ghost" id="umrAddLine" type="button" style="margin-top:6px;font-size:11px">+ 줄 추가</button>
      <div id="umrSummary" style="margin-top:10px;font-size:12px;font-weight:600;color:var(--text-muted)"></div>
      ${isNew ? '' : '<div style="font-size:12px;color:var(--text-muted);margin-top:8px">상품명·옵션은 규칙의 기본키라 수정할 수 없습니다. 바꾸려면 새로 등록하세요.</div>'}`;
    const q = id => body.querySelector('#' + id);

    function renderLines() {
      q('umrLines').innerHTML = lines.map((l, i) => `
        <tr>
          <td>
            <div style="display:flex;gap:6px;align-items:center">
              <div class="input" style="flex:1;font-size:12px;background:#f8fafc;min-height:30px">
                ${l.matchUpriceCode ? esc(l.matchUpriceCode) : '<span style="color:var(--text-muted)">미선택</span>'}
                ${l.matchUpriceCode && l.useYn !== 1 ? '<span class="badge badge-red" style="margin-left:6px">' + (l.useYn == null ? '삭제된 코드' : '사용 중지') + '</span>' : ''}
              </div>
              <button class="btn btn-ghost" data-pick="${i}" type="button" style="font-size:11px;white-space:nowrap">선택</button>
            </div>
          </td>
          <td style="text-align:right">${l.uprice != null ? won(l.uprice) : '-'}</td>
          <td><input class="input" data-qty="${i}" type="number" min="1" max="9999" value="${esc(l.qty)}" style="font-size:12px"></td>
          <td style="text-align:center"><button class="btn btn-ghost" data-del="${i}" type="button" style="font-size:11px" ${lines.length <= 1 ? 'disabled' : ''}>삭제</button></td>
        </tr>`).join('');
      q('umrLines').querySelectorAll('[data-qty]').forEach(el => el.addEventListener('input', e => { lines[+e.target.dataset.qty].qty = parseInt(e.target.value) || 0; updateSummary(); }));
      q('umrLines').querySelectorAll('[data-pick]').forEach(b => b.addEventListener('click', () => openCodePicker(picked => {
        lines[+b.dataset.pick] = { ...lines[+b.dataset.pick], matchUpriceCode: picked.matchUpriceCode, keywrd: picked.keywrd, capa: picked.capa, uprice: picked.uprice, useYn: 1 };
        renderLines();
      })));
      q('umrLines').querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => { lines.splice(+b.dataset.del, 1); renderLines(); }));
      updateSummary();
    }
    function updateSummary() {
      const total = lines.reduce((s, l) => s + (l.useYn === 1 ? (l.qty || 0) * (l.uprice || 0) : 0), 0);
      q('umrSummary').textContent = `원가 합계 ${won(total)}원 (사용 중인 코드 기준)`;
    }
    q('umrAddLine').addEventListener('click', () => { lines.push({ matchUpriceCode: '', qty: 1 }); renderLines(); });
    renderLines();

    UI.modal({
      title: isNew ? '단가매칭 규칙 등록' : '단가매칭 규칙 수정',
      body, confirmText: '저장',
      onConfirm: async close => {
        const name = q('umrFName').value, item = q('umrFItem').value;
        if (!name.trim()) return UI.toast('상품명을 입력하세요', 'error');
        for (const l of lines) {
          if (!l.matchUpriceCode) return UI.toast('모든 줄에 발주단가코드를 선택하세요', 'error');
          if (!l.qty || l.qty <= 0) return UI.toast('수량은 1 이상이어야 합니다', 'error');
        }
        try {
          await Api.post('/order/uprice-match/save', {
            prodName: name, itemName: item,
            lines: lines.map(l => ({ matchUpriceCode: l.matchUpriceCode, qty: l.qty })),
          });
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    }).el.querySelector('.modal').style.width = '720px';
  }

  // ── 발주단가코드 선택(중첩 모달) — 사용 중인 코드만 ──────────────────
  function openCodePicker(onPick) {
    const pb = document.createElement('div');
    pb.innerHTML = `
      <div style="display:flex;gap:6px;margin-bottom:8px">
        <input class="input" id="umrQKeywrd" placeholder="키워드로 검색" style="flex:1">
        <button class="btn btn-ghost" id="umrQBtn" type="button">검색</button>
      </div>
      <div class="table-wrap" id="umrQWrap" style="max-height:40vh;border:1px solid var(--border);border-radius:var(--radius)">
        <div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색하세요</div>
      </div>
      <div style="font-size:11px;color:var(--text-muted);margin-top:8px">없는 코드는 <b>발주단가표</b> 화면에서 먼저 등록하세요.</div>`;
    const sub = UI.modal({ title: '발주단가코드 선택', body: pb, confirmText: null, cancelText: '닫기' });
    sub.el.querySelector('.modal').style.width = '640px';
    const doSearch = async () => {
      const wrap = pb.querySelector('#umrQWrap');
      wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색 중...</div>';
      try {
        const params = { pUseYn: 1, pPageOffset: 0, pPageSize: 30 };
        const kw = pb.querySelector('#umrQKeywrd').value.trim();
        if (kw) params.pKeywrd = kw;
        const list = await Api.get('/order/uprice/list', params);
        if (!list || !list.length) {
          wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">결과 없음</div>';
          return;
        }
        wrap.innerHTML = `
          <table style="font-size:11px">
            <thead><tr><th>코드</th><th>키워드</th><th>용량</th><th style="text-align:right">단가</th></tr></thead>
            <tbody>${list.map((u, i) => `
              <tr style="cursor:pointer" data-i="${i}"><td>${esc(u.matchUpriceCode)}</td><td>${esc(u.keywrd)}</td><td>${esc(u.capa)}</td><td style="text-align:right">${won(u.uprice)}</td></tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('tbody tr').forEach(tr => tr.addEventListener('click', () => { onPick(list[+tr.dataset.i]); sub.close(); }));
      } catch (e) {
        wrap.innerHTML = `<div style="color:var(--danger);padding:16px;font-size:12px">${esc(e.message)}</div>`;
      }
    };
    pb.querySelector('#umrQBtn').addEventListener('click', doSearch);
    pb.querySelector('#umrQKeywrd').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    doSearch();
  }

  return { render };
})();

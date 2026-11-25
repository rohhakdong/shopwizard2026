/**
 * 상품매칭결과 (사이드 메뉴 발주) — 레거시 wOrdOrderProdMatchList 의 결과 조회
 *   상품명+옵션이 어떤 상품코드로 매칭되는지의 규칙(shopwizard.tOrdProdCodeMatch)을 조회·등록·수정·삭제한다.
 *   [상품 매칭] 화면이 "이 상품명을 기억" 으로 저장하는 규칙, 수집 주문·엑셀 주문 업로드의 자동 매칭이 읽는 규칙과 같은 테이블이다.
 *   규칙은 앞으로 들어오는 주문에 쓰인다 — 이미 들어온 미매칭 주문은 [상품 매칭]에서 연결한다.
 *   상품명·옵션은 주문 줄 원문과 정확히 비교되므로 주문관리에서 복사한 그대로 넣는다(앞뒤 공백도 그대로 저장).
 *   백엔드: /order/prod-code-match (list, count, POST, PUT, DELETE ?prodName&prodOption), /product/prod/list(상품 선택)
 */
const PageOrderProdMatchResult = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  let currentPage = 1;

  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
          <span>상품매칭결과</span>
          <button class="btn btn-primary" id="pmrBtnNew" type="button">+ 등록</button>
        </div>
        <div class="card-body" style="padding:12px 16px">
          <div style="font-size:12px;color:var(--text-muted);line-height:1.6;margin-bottom:10px">
            상품명+옵션 → 상품코드 매칭 규칙입니다. 여기서 등록·수정·삭제한 규칙은 <b>앞으로</b> 수집·엑셀로 들어오는 주문의 자동 매칭에 쓰입니다.
            이미 들어온 미매칭 주문은 <b>상품 매칭</b> 화면에서 연결하세요.
          </div>
          <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
            <div class="form-group"><label>상품명</label><input class="input" id="pmrName" style="width:220px"></div>
            <div class="form-group"><label>옵션</label><input class="input" id="pmrOption" style="width:170px"></div>
            <div class="form-group"><label>상품코드</label><input class="input" id="pmrCode" style="width:130px"></div>
            <button class="btn btn-primary" id="pmrBtnSearch" type="button">검색</button>
          </div>
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">총 <strong id="pmrTotal">0</strong>건</div>
          <div class="table-wrap" id="pmrWrap"></div>
          <div id="pmrPage" style="margin-top:12px"></div>
        </div>
      </div>`;
    document.getElementById('pmrBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('pmrBtnNew').addEventListener('click', () => openModal(null));
    ['pmrName', 'pmrOption', 'pmrCode'].forEach(id =>
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } }));
    loadList();
  }

  function getParams() {
    const v = id => document.getElementById(id).value.trim();
    const p = {};
    if (v('pmrName')) p.pProdName = v('pmrName');
    if (v('pmrOption')) p.pProdOption = v('pmrOption');
    if (v('pmrCode')) p.pProdCode = v('pmrCode');
    return p;
  }

  async function loadList() {
    const wrap = document.getElementById('pmrWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    try {
      const params = getParams();
      const total = await Api.get('/order/prod-code-match/count', params);
      document.getElementById('pmrTotal').textContent = Number(total).toLocaleString();
      if (!total) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        document.getElementById('pmrPage').innerHTML = '';
        return;
      }
      const list = await Api.get('/order/prod-code-match/list', { ...params, pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
      renderTable(list);
      UI.pagination(document.getElementById('pmrPage'), { total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const wrap = document.getElementById('pmrWrap');
    wrap.innerHTML = `
      <table style="width:100%;font-size:12px">
        <thead><tr><th>상품명</th><th>옵션</th><th>상품코드</th><th>연결된 상품</th><th>상점</th><th>등록일</th><th></th></tr></thead>
        <tbody>${list.map((m, i) => `
          <tr>
            <td style="max-width:300px">${esc(m.prodName)}</td>
            <td style="max-width:180px;color:var(--text-muted)">${esc(m.prodOption)}</td>
            <td>${esc(m.prodCode)}</td>
            <td style="max-width:240px">${m.matchedProdName ? esc(m.matchedProdName) : '<span style="color:var(--danger)">판매 상품 없음</span>'}</td>
            <td>${esc(m.shopName)}</td>
            <td style="white-space:nowrap;color:var(--text-muted)">${esc((m.registDate || '').substring(0, 10))}</td>
            <td style="white-space:nowrap">
              <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px" data-edit="${i}" type="button">수정</button>
              <button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button>
            </td>
          </tr>`).join('')}</tbody>
      </table>`;
    wrap.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openModal(list[+b.dataset.edit])));
    wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
      const m = list[+b.dataset.del];
      UI.confirm(`[${m.prodName}${m.prodOption ? ' / ' + m.prodOption : ''}] 매칭 규칙을 삭제하시겠습니까? 앞으로 이 상품명의 주문은 자동 매칭되지 않습니다.`, async close => {
        try {
          await Api.delete('/order/prod-code-match', { prodName: m.prodName, prodOption: m.prodOption || '' });
          UI.toast('삭제되었습니다', 'success');
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
        close();
      });
    }));
  }

  // ── 등록/수정 모달 ─────────────────────────────────────────────────
  function openModal(row) {
    const isNew = !row;
    const v = row || {};
    const lock = isNew ? '' : 'readonly style="background:#f1f5f9"';
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full"><label>상품명 *</label><input class="input" id="pmrFName" maxlength="255" value="${esc(v.prodName)}" ${lock}
          placeholder="주문관리의 상품명 그대로"></div>
        <div class="form-group full"><label>옵션</label><input class="input" id="pmrFOption" maxlength="255" value="${esc(v.prodOption)}" ${lock}
          placeholder="주문의 옵션(없으면 비움)"></div>
        <div class="form-group"><label>상품코드 *</label>
          <div style="display:flex;gap:6px"><input class="input" id="pmrFCode" maxlength="255" value="${esc(v.prodCode)}" style="flex:1">
          <button class="btn btn-ghost" id="pmrFPick" type="button" style="white-space:nowrap">상품 검색</button></div></div>
        <div class="form-group"><label>연결될 상품</label>
          <div class="input" id="pmrFProd" style="background:#f8fafc;min-height:34px;line-height:34px">${esc(v.matchedProdName) || '-'}</div></div>
      </div>
      ${isNew ? '' : '<div style="font-size:12px;color:var(--text-muted);margin-top:8px">상품명·옵션은 규칙의 기본키라 수정할 수 없습니다. 바꾸려면 새로 등록하세요.</div>'}`;
    const q = id => body.querySelector('#' + id);
    q('pmrFPick').addEventListener('click', () => openProdPicker(p => {
      q('pmrFCode').value = p.prodCode;
      q('pmrFProd').textContent = `${p.prodName} (${p.shopName || ''})`;
    }));

    UI.modal({
      title: isNew ? '상품매칭 규칙 등록' : '상품매칭 규칙 수정',
      body, confirmText: '저장',
      onConfirm: async close => {
        const name = q('pmrFName').value, option = q('pmrFOption').value, code = q('pmrFCode').value.trim();
        if (!name.trim()) return UI.toast('상품명을 입력하세요', 'error');
        if (!code) return UI.toast('상품코드를 입력하세요', 'error');
        const payload = { prodName: name, prodOption: option, prodCode: code };
        try {
          if (isNew) await Api.post('/order/prod-code-match', payload); else await Api.put('/order/prod-code-match', payload);
          UI.toast(isNew ? '등록되었습니다' : '저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  // ── 상품 선택(중첩 모달) — 판매 상품 검색 ───────────────────────────
  function openProdPicker(onPick) {
    let picked = null;
    const pb = document.createElement('div');
    pb.innerHTML = `
      <div style="display:flex;gap:6px;margin-bottom:8px">
        <input class="input" id="pmrQName" placeholder="상품명" style="flex:1">
        <input class="input" id="pmrQCode" placeholder="상품코드/상점상품코드" style="width:180px">
        <button class="btn btn-ghost" id="pmrQBtn" type="button" style="white-space:nowrap">검색</button>
      </div>
      <div class="table-wrap" id="pmrQWrap" style="max-height:40vh;border:1px solid var(--border);border-radius:var(--radius)">
        <div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">상품명이나 코드로 검색하세요</div>
      </div>
      <div id="pmrQPicked" style="margin-top:8px;font-size:12px;min-height:16px"></div>`;
    const sub = UI.modal({
      title: '상품 선택', body: pb, confirmText: '선택', cancelText: '닫기',
      onConfirm: async close => {
        if (!picked) return UI.toast('상품을 선택하세요', 'error');
        onPick(picked);
        close();
      },
    });
    sub.el.querySelector('.modal').style.width = '720px';
    const doSearch = async () => {
      const wrap = pb.querySelector('#pmrQWrap');
      wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">검색 중...</div>';
      try {
        const params = { pPageOffset: 0, pPageSize: 30 };
        const nm = pb.querySelector('#pmrQName').value.trim();
        const cd = pb.querySelector('#pmrQCode').value.trim();
        if (nm) params.pProdName = nm;
        if (cd) params.pProdCode = cd;
        const list = await Api.get('/product/prod/list', params);
        if (!list || !list.length) {
          wrap.innerHTML = '<div style="padding:20px;text-align:center;color:var(--text-muted);font-size:12px">결과 없음</div>';
          return;
        }
        wrap.innerHTML = `
          <table style="font-size:11px">
            <thead><tr><th></th><th>상품코드</th><th>상품명</th><th>상점</th></tr></thead>
            <tbody>${list.map((p, i) => `
              <tr style="cursor:pointer" data-i="${i}">
                <td><input type="radio" name="pmrpick" value="${i}"></td>
                <td>${esc(p.prodCode)}</td><td>${esc(p.prodName)}</td><td>${esc(p.shopName)}</td>
              </tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('tbody tr').forEach(tr => tr.addEventListener('click', () => {
          picked = list[+tr.dataset.i];
          wrap.querySelector(`input[value="${tr.dataset.i}"]`).checked = true;
          pb.querySelector('#pmrQPicked').innerHTML = `선택: <strong>${esc(picked.prodCode)}</strong> ${esc(picked.prodName)}`;
        }));
      } catch (e) {
        wrap.innerHTML = `<div style="color:var(--danger);padding:16px;font-size:12px">${esc(e.message)}</div>`;
      }
    };
    pb.querySelector('#pmrQBtn').addEventListener('click', doSearch);
    pb.querySelector('#pmrQName').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    pb.querySelector('#pmrQCode').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
    pb.querySelector('#pmrQName').focus();
  }

  return { render };
})();

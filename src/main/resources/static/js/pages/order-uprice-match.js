/**
 * 발주단가매칭 (주문 > 발주단가매칭)
 *   채널 상품명(상품명+옵션)이 세트/묶음이라 실제 발주수량·원가가 주문 데이터만으론 안 보일 때,
 *   "이 채널상품명은 발주단가코드 A 2개 + B 1개로 구성된다" 같은 분해 규칙을 저장한다.
 *   세트가 아닌 단품도 항상 1줄로 저장 — 규칙 적용(출고지시 게이트)을 통일하기 위함.
 *
 * - 목록: /order/uprice-match/unmatched/list(+/count) — 지불완료 주문 중 규칙 없는 (상품명,옵션) 그룹
 * - [매칭]: 상세 줄(키워드/규격/수량/발주단가코드) 여러 개 편집 → POST /order/uprice-match/save
 *   발주단가코드는 /order/uprice/list 로 검색하거나 그 자리에서 POST /order/uprice 로 신규 등록
 * - 저장된 규칙이 없으면 "출고지시" 화면에서 처리가 막힌다 (ShipDirectIssueService 게이트).
 */
const PageOrderUpriceMatch = (() => {

  const PAGE_SIZE = 30;
  let currentPage = 1;
  let totalCount = 0;
  let chnlList = [];

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }
  function won(n) { return (n == null || n === '') ? '' : Number(n).toLocaleString(); }
  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function toDateStr(d) { return d.toISOString().slice(0, 10); }
  function defaultStart() { const d = new Date(); d.setDate(d.getDate() - 90); return toDateStr(d); }
  function defaultEnd() { return toDateStr(new Date()); }

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">발주단가매칭</div>
        <div class="card-body" style="padding:12px 16px">

          <div class="search-bar" style="flex-wrap:wrap;gap:8px;align-items:flex-end">
            <div class="form-group">
              <label>채널</label>
              <select class="input" id="umChnl" style="width:150px"><option value="">전체</option></select>
            </div>
            <div class="form-group">
              <label>주문일 시작</label>
              <input class="input" id="umStart" type="date" value="${defaultStart()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>주문일 종료</label>
              <input class="input" id="umEnd" type="date" value="${defaultEnd()}" style="width:140px">
            </div>
            <div class="form-group">
              <label>상품명</label>
              <input class="input" id="umProdName" placeholder="상품명 일부" style="width:200px">
            </div>
            <button class="btn btn-primary" id="umSearch">검색</button>
          </div>

          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            발주단가 미매칭 <strong id="umTotal">0</strong>종 — 세트가 아니어도 1줄로 매칭해야 출고지시가 가능합니다.
          </div>

          <div class="table-wrap" id="umTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <div id="umPagination" style="margin-top:12px"></div>
        </div>
      </div>`;

    document.getElementById('umSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    document.getElementById('umProdName').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });

    loadChannels();
    loadList();
  }

  async function loadChannels() {
    try {
      const chnls = await Api.get('/company/chnl', { pSvcCode: svcCode(), pPageOffset: 0, pPageSize: 2000 });
      chnlList = (chnls || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'));
      const el = document.getElementById('umChnl');
      chnlList.forEach(c => el.insertAdjacentHTML('beforeend', `<option value="${esc(c.chnlCode)}">${esc(c.chnlName)}</option>`));
    } catch (_) { /* 옵션 로드 실패해도 목록은 동작 */ }
  }

  function getParams() {
    const v = id => document.getElementById(id).value;
    return {
      pSvcCode:   svcCode(),
      pChnlCode:  v('umChnl'),
      pStartDate: v('umStart'),
      pEndDate:   v('umEnd'),
      pProdName:  v('umProdName').trim(),
    };
  }

  async function loadList() {
    const wrap = document.getElementById('umTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('umPagination').innerHTML = '';
    try {
      const params = getParams();
      totalCount = await Api.get('/order/uprice-match/unmatched/list/count', params);
      document.getElementById('umTotal').textContent = totalCount.toLocaleString();
      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">미매칭 상품이 없습니다</div>';
        return;
      }
      const list = await Api.get('/order/uprice-match/unmatched/list', {
        ...params, topCnt: (currentPage - 1) * PAGE_SIZE, countPerPage: PAGE_SIZE,
      });
      renderTable(list);
      UI.pagination(document.getElementById('umPagination'), {
        total: totalCount, page: currentPage, pageSize: PAGE_SIZE,
        onChange: p => { currentPage = p; loadList(); },
      });
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
    }
  }

  function renderTable(list) {
    const rows = list.map((g, i) => `
      <tr>
        <td style="font-size:12px">${esc(g.prodName)}</td>
        <td style="font-size:11px;color:var(--text-muted)">${esc(g.itemName) || '-'}</td>
        <td style="font-size:11px">${esc(g.chnlNames)}</td>
        <td style="text-align:right">${g.cnt}</td>
        <td style="text-align:right;font-size:11px">${esc(g.sampleOrderNo)}</td>
        <td style="text-align:center">
          <button class="btn btn-primary" style="padding:2px 10px;font-size:11px" data-i="${i}">매칭</button>
        </td>
      </tr>`).join('');

    const wrap = document.getElementById('umTableWrap');
    wrap.innerHTML = `
      <table>
        <thead><tr>
          <th>상품명</th><th>옵션</th><th>채널</th>
          <th style="text-align:right">건수</th><th style="text-align:right">대표주문</th>
          <th style="text-align:center">매칭</th>
        </tr></thead>
        <tbody>${rows}</tbody>
      </table>`;
    wrap.querySelectorAll('button[data-i]').forEach(btn => {
      btn.addEventListener('click', () => openMatchModal(list[+btn.dataset.i]));
    });
  }

  // ── 매칭 모달 ──────────────────────────────────────────────────────
  function openMatchModal(group) {
    // 각 줄: { matchUpriceCode, keywrd, capa, uprice, qty }
    const lines = [{ matchUpriceCode: '', keywrd: group.prodName || '', capa: '', uprice: 0, qty: 1 }];

    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;margin-bottom:10px">
        <div><strong>${esc(group.prodName)}</strong></div>
        <div style="color:var(--text-muted)">옵션: ${esc(group.itemName) || '-'} · ${group.cnt}건 · ${esc(group.chnlNames)}</div>
      </div>
      <table style="width:100%">
        <thead>
          <tr>
            <th style="font-size:11px">키워드</th><th style="font-size:11px">규격</th>
            <th style="font-size:11px;width:70px">수량</th><th style="font-size:11px">발주단가코드</th>
            <th style="width:40px"></th>
          </tr>
        </thead>
        <tbody id="umLines"></tbody>
      </table>
      <button class="btn btn-ghost" id="umAddLine" style="margin-top:6px;font-size:11px">+ 줄 추가</button>
      <div id="umSummary" style="margin-top:10px;font-size:12px;font-weight:600;color:var(--text-muted)"></div>
      <div id="umMsg" style="margin-top:8px;font-size:12px"></div>`;

    const modal = UI.modal({
      title: '발주단가매칭',
      body,
      confirmText: '저장',
      cancelText: '닫기',
      onConfirm: async (close) => {
        const msgEl = body.querySelector('#umMsg');
        for (const l of lines) {
          if (!l.matchUpriceCode) { msgEl.innerHTML = '<span style="color:var(--danger)">모든 줄에 발주단가코드를 선택하세요</span>'; return; }
          if (!l.qty || l.qty <= 0) { msgEl.innerHTML = '<span style="color:var(--danger)">수량은 1 이상이어야 합니다</span>'; return; }
        }
        const btn = modal.el.querySelector('[data-action=confirm]');
        if (btn) btn.disabled = true;
        try {
          await Api.post('/order/uprice-match/save', {
            prodName: group.prodName,
            itemName: group.itemName,
            lines: lines.map(l => ({ matchUpriceCode: l.matchUpriceCode, qty: l.qty })),
            registId: info?.loginId || '',
            registName: info?.name || '',
          });
          UI.toast('발주단가매칭이 저장되었습니다', 'success');
          close();
          loadList();
        } catch (e) {
          msgEl.innerHTML = `<span style="color:var(--danger)">${esc(e.message)}</span>`;
          if (btn) btn.disabled = false;
        }
      },
    });

    function renderLines() {
      const tbody = body.querySelector('#umLines');
      tbody.innerHTML = lines.map((l, i) => `
        <tr>
          <td><input class="input um-keywrd" data-i="${i}" value="${esc(l.keywrd)}" style="font-size:11px"></td>
          <td><input class="input um-capa" data-i="${i}" value="${esc(l.capa)}" placeholder="예: 1kg" style="font-size:11px"></td>
          <td><input class="input um-qty" data-i="${i}" type="number" min="1" value="${l.qty}" style="font-size:11px"></td>
          <td>
            <div style="display:flex;gap:4px;align-items:center">
              <div class="input" style="flex:1;font-size:11px;background:#f8fafc" data-role="code-display">
                ${l.matchUpriceCode ? esc(l.matchUpriceCode) + ' (' + won(l.uprice) + '원)' : '미선택'}
              </div>
              <button class="btn btn-ghost um-pick" data-i="${i}" style="font-size:11px;white-space:nowrap">선택</button>
            </div>
          </td>
          <td style="text-align:center">
            <button class="btn btn-ghost um-del" data-i="${i}" style="font-size:11px" ${lines.length <= 1 ? 'disabled' : ''}>삭제</button>
          </td>
        </tr>`).join('');

      tbody.querySelectorAll('.um-keywrd').forEach(el => el.addEventListener('input', e => { lines[+e.target.dataset.i].keywrd = e.target.value; }));
      tbody.querySelectorAll('.um-capa').forEach(el => el.addEventListener('input', e => { lines[+e.target.dataset.i].capa = e.target.value; }));
      tbody.querySelectorAll('.um-qty').forEach(el => el.addEventListener('input', e => { lines[+e.target.dataset.i].qty = parseInt(e.target.value) || 0; updateSummary(); }));
      tbody.querySelectorAll('.um-pick').forEach(btn => btn.addEventListener('click', () => openCodePicker(+btn.dataset.i)));
      tbody.querySelectorAll('.um-del').forEach(btn => btn.addEventListener('click', () => {
        lines.splice(+btn.dataset.i, 1);
        renderLines();
        updateSummary();
      }));
      updateSummary();
    }

    function updateSummary() {
      const totalQty = lines.reduce((s, l) => s + (l.qty || 0), 0);
      const totalBuy = lines.reduce((s, l) => s + (l.qty || 0) * (l.uprice || 0), 0);
      body.querySelector('#umSummary').textContent = `합계 — 총 ${totalQty}개, 총원가 ${won(totalBuy)}원`;
    }

    body.querySelector('#umAddLine').addEventListener('click', () => {
      lines.push({ matchUpriceCode: '', keywrd: group.prodName || '', capa: '', uprice: 0, qty: 1 });
      renderLines();
    });

    // 기존 저장된 규칙이 있으면 불러오기 (수정 진입)
    Api.get('/order/uprice-match/detail', { prodName: group.prodName, itemName: group.itemName })
      .then(existing => {
        if (existing && existing.length > 0) {
          lines.length = 0;
          existing.forEach(d => lines.push({
            matchUpriceCode: d.matchUpriceCode, keywrd: d.keywrd, capa: d.capa, uprice: d.uprice, qty: d.qty,
          }));
        }
        renderLines();
      })
      .catch(() => renderLines());

    // ── 발주단가코드 선택/신규등록 (중첩 모달) ─────────────────────────
    function openCodePicker(lineIdx) {
      const line = lines[lineIdx];
      const pbody = document.createElement('div');
      pbody.innerHTML = `
        <div style="display:flex;gap:6px;margin-bottom:8px">
          <input class="input" id="umPickKeywrd" placeholder="키워드로 검색" value="${esc(line.keywrd)}" style="flex:1">
          <button class="btn btn-ghost" id="umPickSearch">검색</button>
        </div>
        <div class="table-wrap" id="umPickWrap" style="max-height:30vh;border:1px solid var(--border);border-radius:var(--radius)">
          <div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색하세요</div>
        </div>
        <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin:14px 0 8px">또는 새 발주단가코드 등록</div>
        <div class="form-grid">
          <div class="form-group"><label>키워드</label><input class="input" id="umNewKeywrd" value="${esc(line.keywrd)}"></div>
          <div class="form-group"><label>규격</label><input class="input" id="umNewCapa" value="${esc(line.capa)}" placeholder="예: 1kg"></div>
          <div class="form-group"><label>개당 발주단가</label><input class="input" id="umNewUprice" type="number" placeholder="원"></div>
          <div class="form-group"><label>단위명 (선택)</label><input class="input" id="umNewUnitName"></div>
        </div>
        <div id="umPickMsg" style="margin-top:8px;font-size:12px"></div>`;

      const pmodal = UI.modal({
        title: '발주단가코드 선택',
        body: pbody,
        confirmText: '새 코드 등록',
        cancelText: '닫기',
        onConfirm: async (pclose) => {
          const keywrd = pbody.querySelector('#umNewKeywrd').value.trim();
          const capa = pbody.querySelector('#umNewCapa').value.trim();
          const uprice = parseInt(pbody.querySelector('#umNewUprice').value) || 0;
          const unitName = pbody.querySelector('#umNewUnitName').value.trim();
          const msgEl = pbody.querySelector('#umPickMsg');
          if (!keywrd || !capa) { msgEl.innerHTML = '<span style="color:var(--danger)">키워드와 규격을 입력하세요</span>'; return; }
          const matchUpriceCode = `${keywrd}-${capa}`;
          try {
            await Api.post('/order/uprice', {
              matchUpriceCode, shopCode: '', orderProdName: group.prodName, keywrd, capa,
              uprice, unitName, useYn: 1, state: 1,
              registId: info?.loginId || '', registName: info?.name || '',
            });
            applyPick(lineIdx, { matchUpriceCode, keywrd, capa, uprice });
            pclose();
          } catch (e) {
            msgEl.innerHTML = `<span style="color:var(--danger)">${esc(e.message)}</span>`;
          }
        },
      });

      const doSearch = async () => {
        const wrap = pbody.querySelector('#umPickWrap');
        wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">검색 중...</div>';
        try {
          const kw = pbody.querySelector('#umPickKeywrd').value.trim();
          const params = { pUseYn: 1, pPageOffset: 0, pPageSize: 30 };
          if (kw) params.pKeywrd = kw;
          const list = await Api.get('/order/uprice/list', params);
          if (!list || list.length === 0) {
            wrap.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text-muted);font-size:12px">결과 없음 — 아래에서 신규 등록하세요</div>';
            return;
          }
          wrap.innerHTML = `
            <table>
              <thead><tr><th style="font-size:11px">코드</th><th style="font-size:11px">키워드</th><th style="font-size:11px">규격</th><th style="text-align:right;font-size:11px">단가</th></tr></thead>
              <tbody>${list.map((u, i) => `
                <tr style="cursor:pointer" data-i="${i}">
                  <td style="font-size:11px">${esc(u.matchUpriceCode)}</td>
                  <td style="font-size:11px">${esc(u.keywrd)}</td>
                  <td style="font-size:11px">${esc(u.capa)}</td>
                  <td style="text-align:right;font-size:11px">${won(u.uprice)}</td>
                </tr>`).join('')}</tbody>
            </table>`;
          wrap.querySelectorAll('tbody tr').forEach(tr => {
            tr.addEventListener('click', () => {
              const u = list[+tr.dataset.i];
              applyPick(lineIdx, { matchUpriceCode: u.matchUpriceCode, keywrd: u.keywrd, capa: u.capa, uprice: u.uprice });
              pmodal.close();
            });
          });
        } catch (e) {
          wrap.innerHTML = `<div style="color:var(--danger);padding:16px;font-size:12px">${esc(e.message)}</div>`;
        }
      };
      pbody.querySelector('#umPickSearch').addEventListener('click', doSearch);
      pbody.querySelector('#umPickKeywrd').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });
      doSearch();
    }

    function applyPick(lineIdx, picked) {
      lines[lineIdx] = { ...lines[lineIdx], ...picked };
      renderLines();
    }
  }

  return { render };
})();

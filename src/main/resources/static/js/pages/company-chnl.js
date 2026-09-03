/**
 * 채널 관리 (거래처 - 채널)
 * - 목록 조회 (페이지네이션)
 * - 등록 / 수정 (판매회사 선택 — 소속 회사는 판매회사에 종속되어 자동 결정된다)
 * - 삭제
 */
const PageCompanyChnl = (() => {

  const PAGE_SIZE = 20;
  let currentPage = 1;
  let totalCount  = 0;
  let saleCompOptions = []; // { saleCompCode, saleCompName, compCode } — 판매회사 선택 시 compCode도 여기서 가져온다

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">채널 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 검색바 -->
          <div class="search-bar" style="gap:8px;flex-wrap:wrap">
            <div class="form-group">
              <label>채널코드</label>
              <input class="input" id="chChnlCode" placeholder="채널코드" style="width:130px">
            </div>
            <div class="form-group">
              <label>채널명</label>
              <input class="input" id="chChnlName" placeholder="채널명" style="width:160px">
            </div>
            <div class="form-group">
              <label>판매사</label>
              <select class="input" id="chSaleCompCode" style="width:180px">
                <option value="">전체</option>
              </select>
            </div>
            <div class="form-group">
              <label>상태</label>
              <select class="input" id="chState" style="width:90px">
                <option value="">전체</option>
                <option value="1">정상</option>
                <option value="0">중지</option>
              </select>
            </div>
            <button class="btn btn-primary" id="chBtnSearch" style="margin-top:18px">검색</button>
            <button class="btn btn-ghost" id="chBtnNew" style="margin-top:18px">+ 신규</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="chTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="chTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">검색하세요</div>
          </div>

          <!-- 페이지네이션 -->
          <div id="chPagination" style="margin-top:12px"></div>

        </div>
      </div>`;

    document.getElementById('chBtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
    ['chChnlCode','chChnlName'].forEach(id => {
      document.getElementById(id).addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
    });
    document.getElementById('chBtnNew').addEventListener('click', () => openEditModal(null));

    loadOptions().then(() => {
      const sel = document.getElementById('chSaleCompCode');
      sel.innerHTML = '<option value="">전체</option>' + saleCompOptions.map(s =>
        `<option value="${s.saleCompCode}">${s.saleCompName || '(판매사명 미입력)'} (${s.saleCompCode})</option>`
      ).join('');
    });
    loadList();
  }

  // 판매회사 선택용 목록 (전체를 한 번에 불러온다)
  async function loadOptions() {
    try {
      saleCompOptions = await Api.get('/company/sale-comp', { pPageOffset: 0, pPageSize: 1000 });
    } catch (_) { saleCompOptions = []; }
  }

  // ── 검색 파라미터 ──────────────────────────────────────────────────
  function getParams() {
    return {
      pChnlCode:     document.getElementById('chChnlCode').value.trim(),
      pChnlName:     document.getElementById('chChnlName').value.trim(),
      pSaleCompCode: document.getElementById('chSaleCompCode').value,
      pState:        document.getElementById('chState').value,
    };
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('chTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
    document.getElementById('chPagination').innerHTML = '';

    try {
      const params = getParams();
      totalCount = await Api.get('/company/chnl/count', params);
      document.getElementById('chTotalLabel').textContent = totalCount.toLocaleString();

      if (totalCount === 0) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      const list = await Api.get('/company/chnl', {
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
    const wrap = document.getElementById('chTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(c => `
      <tr>
        <td style="font-size:11px;font-family:monospace;${ell}" title="${c.chnlCode || ''}">${c.chnlCode || ''}</td>
        <td style="${ell}">
          <div style="font-weight:500;font-size:13px;${ell}" title="${c.chnlName || ''}">${c.chnlName || ''}</div>
          <div style="font-size:11px;color:var(--text-muted);${ell}" title="${c.saleCompName || ''}">${c.saleCompName || ''}</div>
        </td>
        <td style="font-size:12px;${ell}" title="${c.chnlPolicyName || ''}">${c.chnlPolicyName || ''}</td>
        <td style="font-size:12px;${ell}" title="${c.mngrName || ''}">${c.mngrName || ''}</td>
        <td style="font-size:12px;color:var(--text-muted);${ell}" title="${c.mngrEmail || ''}">${c.mngrEmail || ''}</td>
        <td style="text-align:center">
          <span class="badge ${c.state === 1 ? 'badge-green' : 'badge-gray'}">${c.state === 1 ? '정상' : '중지'}</span>
        </td>
        <td style="white-space:nowrap">
          <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
            data-action="edit" data-code="${c.chnlCode}">수정</button>
          <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
            data-action="del" data-code="${c.chnlCode}" data-name="${(c.chnlName||'').replace(/"/g,'&quot;')}">삭제</button>
        </td>
      </tr>`).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("채널명/판매회사")은 지정된 열들의
    // 폭 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블
    // 자체는 width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:820px">
        <colgroup>
          <col style="width:110px"><col style="width:200px"><col style="width:110px">
          <col style="width:90px"><col style="width:160px">
          <col style="width:60px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">채널코드</th><th style="${thEll}">채널명 / 판매회사</th><th style="${thEll}">채널정책</th>
            <th style="${thEll}">담당자</th><th style="${thEll}">담당자 이메일</th>
            <th style="${thEll};text-align:center">상태</th><th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const chnl = await Api.get(`/company/chnl/${btn.dataset.code}`);
          openEditModal(chnl);
        } catch (e) { UI.toast(e.message, 'error'); }
      });
    });

    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 채널을 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete(`/company/chnl/${btn.dataset.code}`);
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

    const el = document.getElementById('chPagination');
    const block = Math.floor((currentPage - 1) / 10);
    const start = block * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    let html = `<div class="pagination">`;
    if (block > 0) html += `<button class="page-btn" data-page="${start-1}">‹</button>`;
    for (let p = start; p <= end; p++)
      html += `<button class="page-btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`;
    if (end < totalPages) html += `<button class="page-btn" data-page="${end+1}">›</button>`;
    html += `</div>`;

    el.innerHTML = html;
    el.querySelectorAll('.page-btn').forEach(btn => {
      btn.addEventListener('click', () => { currentPage = parseInt(btn.dataset.page); loadList(); });
    });
  }

  // ── 신규/수정 모달 ─────────────────────────────────────────────────
  function openEditModal(chnl) {
    const isNew = !chnl;
    const v = chnl || {};

    const saleCompOpts = saleCompOptions.map(s =>
      `<option value="${s.saleCompCode}" data-comp-code="${s.compCode || ''}" data-comp-name="${(s.comp?.compName || '').replace(/"/g,'&quot;')}" ${v.saleCompCode === s.saleCompCode ? 'selected' : ''}>${s.saleCompName} (${s.saleCompCode})</option>`
    ).join('');
    const selectedSaleComp = saleCompOptions.find(s => s.saleCompCode === v.saleCompCode);

    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group">
          <label>채널코드 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="chFChnlCode" value="${v.chnlCode || ''}" ${!isNew ? 'readonly style="background:#f8fafc"' : ''} placeholder="채널코드">
        </div>
        <div class="form-group">
          <label>판매회사 <span style="color:var(--danger)">*</span></label>
          <select class="input" id="chFSaleCompCode">
            <option value="">-- 선택 --</option>
            ${saleCompOpts}
          </select>
          <!-- 소속 회사는 판매회사에 딸린 값이라 따로 입력받지 않는다 — 판매회사를 고르면
               자동으로 정해지므로 참고용으로만 표시한다 (공급사 선택 시 상점의 소속이
               자동으로 정해지는 것과 동일한 관계: 판매회사 → 소속 회사). -->
          <div id="chSelectedCompName" style="font-size:12px;color:var(--text-muted);margin-top:4px">
            ${selectedSaleComp ? `소속 회사: ${selectedSaleComp.comp?.compName || '(회사명 미입력)'} (${selectedSaleComp.compCode || ''})` : ''}
          </div>
        </div>
        <div class="form-group full">
          <label>채널명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="chFChnlName" value="${v.chnlName || ''}">
        </div>
        <div class="form-group full">
          <label>채널 URL</label>
          <input class="input" id="chFChnlUrl" value="${v.chnlUrl || ''}">
        </div>
        <div class="form-group">
          <label>담당자명</label>
          <input class="input" id="chFMngrName" value="${v.mngrName || ''}">
        </div>
        <div class="form-group">
          <label>담당 MD</label>
          <input class="input" id="chFMngrMd" value="${v.mngrMd || ''}">
        </div>
        <div class="form-group">
          <label>담당자 휴대폰</label>
          <input class="input" id="chFMngrMobileNo" value="${v.mngrMobileNo || ''}">
        </div>
        <div class="form-group">
          <label>담당자 전화번호</label>
          <input class="input" id="chFMngrPhoneNo" value="${v.mngrPhoneNo || ''}">
        </div>
        <div class="form-group full">
          <label>담당자 이메일</label>
          <input class="input" id="chFMngrEmail" value="${v.mngrEmail || ''}">
        </div>
        <div class="form-group">
          <label>운영시작일</label>
          <input class="input" id="chFMallStartDate" type="date" value="${v.mallStartDate ? v.mallStartDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>운영종료일</label>
          <input class="input" id="chFMallEndDate" type="date" value="${v.mallEndDate ? v.mallEndDate.substring(0,10) : ''}">
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="chFState">
            <option value="1" ${(v.state ?? 1) === 1 ? 'selected' : ''}>정상</option>
            <option value="0" ${v.state === 0 ? 'selected' : ''}>중지</option>
          </select>
        </div>
        <div class="form-group full">
          <label>비고</label>
          <input class="input" id="chFRemark" value="${v.remark || ''}">
        </div>
      </div>`;

    // 판매회사를 바꿀 때마다 참고용 소속 회사 표시를 갱신한다.
    body.querySelector('#chFSaleCompCode').addEventListener('change', e => {
      const opt = e.target.selectedOptions[0];
      const label = document.getElementById('chSelectedCompName');
      label.textContent = opt && opt.value
        ? `소속 회사: ${opt.dataset.compName || '(회사명 미입력)'} (${opt.dataset.compCode || ''})`
        : '';
    });

    UI.modal({
      title: isNew ? '채널 신규 등록' : `채널 수정 – ${v.chnlCode}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const chnlCode     = document.getElementById('chFChnlCode').value.trim();
        const saleCompCode = document.getElementById('chFSaleCompCode').value;
        const chnlName     = document.getElementById('chFChnlName').value.trim();

        if (!chnlCode)     { UI.toast('채널코드를 입력하세요', 'error'); return; }
        if (!saleCompCode) { UI.toast('판매회사를 선택하세요', 'error'); return; }
        if (!chnlName)     { UI.toast('채널명을 입력하세요', 'error'); return; }

        // 소속 회사코드는 입력받지 않고 선택된 판매회사의 compCode를 그대로 따라간다.
        const selected = saleCompOptions.find(s => s.saleCompCode === saleCompCode);
        const compCode = selected?.compCode || '';

        const payload = {
          chnlCode,
          saleCompCode,
          chnlName,
          // 채널정책은 등록화면에서 더 이상 입력받지 않는다 (tCmpChnlPolicy가 비어있어
          // 실질적으로 고를 게 없는 상태) — 기존에 저장돼 있던 값은 수정 시 그대로 보존해
          // 화면에서 손대지 않은 값을 의도치 않게 지우는 일이 없도록 한다.
          chnlPolicyCode: isNew ? null : (v.chnlPolicyCode || null),
          compCode,
          chnlUrl:        document.getElementById('chFChnlUrl').value.trim(),
          mngrName:       document.getElementById('chFMngrName').value.trim(),
          mngrMd:         document.getElementById('chFMngrMd').value.trim(),
          mngrMobileNo:   document.getElementById('chFMngrMobileNo').value.trim(),
          mngrPhoneNo:    document.getElementById('chFMngrPhoneNo').value.trim(),
          mngrEmail:      document.getElementById('chFMngrEmail').value.trim(),
          mallStartDate:  document.getElementById('chFMallStartDate').value || null,
          mallEndDate:    document.getElementById('chFMallEndDate').value || null,
          state:          parseInt(document.getElementById('chFState').value),
          remark:         document.getElementById('chFRemark').value.trim(),
          registId:       (typeof info !== 'undefined' && info?.loginId) || '',
          registName:     (typeof info !== 'undefined' && info?.name) || '',
          changeId:       (typeof info !== 'undefined' && info?.loginId) || '',
          changeName:     (typeof info !== 'undefined' && info?.name) || '',
        };

        try {
          if (isNew) {
            await Api.post('/company/chnl', payload);
            UI.toast('등록되었습니다', 'success');
          } else {
            await Api.put('/company/chnl', payload);
            UI.toast('저장되었습니다', 'success');
          }
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

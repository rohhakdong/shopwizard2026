/**
 * 스토어 관리
 * - 채널 선택 → 스토어 트리 테이블 조회
 * - 스토어 수정 / 삭제
 */
const PageStoreStore = (() => {

  let chnlList = [];
  let selectedChnlCode = '';

  // ── 진입점 ─────────────────────────────────────────────────────────
  function render(container) {
    container.innerHTML = `
      <div class="card">
        <div class="card-header">스토어 관리</div>
        <div class="card-body" style="padding:12px 16px">

          <!-- 채널 선택 -->
          <div class="search-bar" style="gap:8px;margin-bottom:12px">
            <div class="form-group">
              <label>채널</label>
              <select class="input" id="stChnlCode" style="width:220px">
                <option value="">채널을 선택하세요</option>
              </select>
            </div>
            <button class="btn btn-primary" id="stBtnSearch" style="margin-top:18px">조회</button>
          </div>

          <!-- 건수 -->
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            총 <strong id="stTotalLabel">0</strong>건
          </div>

          <!-- 목록 -->
          <div class="table-wrap" id="stTableWrap">
            <div style="text-align:center;padding:40px;color:var(--text-muted)">채널을 선택하고 조회하세요</div>
          </div>

        </div>
      </div>`;

    document.getElementById('stBtnSearch').addEventListener('click', () => {
      selectedChnlCode = document.getElementById('stChnlCode').value;
      if (!selectedChnlCode) { UI.toast('채널을 선택하세요', 'error'); return; }
      loadList();
    });

    loadChnlList();
  }

  // ── 채널 목록 ──────────────────────────────────────────────────────
  async function loadChnlList() {
    try {
      chnlList = await Api.get('/store/chnl/list', {});
      const sel = document.getElementById('stChnlCode');
      if (!sel) return;
      chnlList.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.chnlCode;
        opt.textContent = c.chnlName || c.chnlCode;
        sel.appendChild(opt);
      });
      if (chnlList.length === 1) {
        sel.value = chnlList[0].chnlCode;
        selectedChnlCode = chnlList[0].chnlCode;
        loadList();
      }
    } catch (_) {}
  }

  // ── 목록 조회 ─────────────────────────────────────────────────────
  async function loadList() {
    const wrap = document.getElementById('stTableWrap');
    wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';

    try {
      const list = await Api.get('/store/store/table/list', { pChnlCode: selectedChnlCode });
      document.getElementById('stTotalLabel').textContent = list.length.toLocaleString();

      if (!list.length) {
        wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">데이터가 없습니다</div>';
        return;
      }

      renderTable(list);
    } catch (e) {
      wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${e.message}</div>`;
    }
  }

  // ── 테이블 렌더 ────────────────────────────────────────────────────
  function renderTable(list) {
    const wrap = document.getElementById('stTableWrap');
    // 고정폭(table-layout:fixed) 셀에 overflow:hidden + ellipsis가 없으면 긴 텍스트가
    // 다음 칸 위로 그대로 삐져나와 겹쳐 보인다.
    const ell = 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap';

    const rows = list.map(s => {
      // 경로 표시: 비어 있지 않은 레벨들을 ' > ' 로 연결
      const pathParts = [
        s.storeName1 || null,
        s.storeCode2 ? (s.storeName2 || null) : null,
        s.storeCode3 ? (s.storeName3 || null) : null,
        s.storeCode4 ? (s.storeName4 || null) : null,
      ].filter(Boolean);
      const path = pathParts.slice(0, -1).join(' > ');
      const lastName = pathParts[pathParts.length - 1] || s.storeName;
      const indent = (s.storeLevel || 0) * 16;

      return `
        <tr>
          <td style="font-size:12px;padding-left:${8 + indent}px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${s.storeName || ''}">
            ${'└'.repeat(s.storeLevel > 0 ? 1 : 0)}
            <strong>${lastName || s.storeName}</strong>
            ${path ? `<div style="font-size:10px;color:var(--text-muted);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${path}">${path}</div>` : ''}
          </td>
          <td style="font-size:11px;font-family:monospace;${ell}" title="${s.storeCode}">${s.storeCode}</td>
          <td style="font-size:12px;color:var(--text-muted);${ell}" title="${s.storeDesc || ''}">${s.storeDesc || ''}</td>
          <td style="text-align:center;font-size:12px">${s.storeLevel ?? ''}</td>
          <td style="text-align:center;font-size:12px">${s.storeSeq ?? ''}</td>
          <td style="text-align:center">
            <span class="badge ${s.state === 1 ? 'badge-green' : 'badge-gray'}">${s.state === 1 ? '사용' : '미사용'}</span>
          </td>
          <td style="white-space:nowrap">
            <button class="btn btn-ghost" style="padding:2px 7px;font-size:11px"
              data-action="edit"
              data-chnl="${s.chnlCode}"
              data-code="${s.storeCode}"
              data-name="${(s.storeName || '').replace(/"/g, '&quot;')}"
              data-desc="${(s.storeDesc || '').replace(/"/g, '&quot;')}"
              data-seq="${s.storeSeq ?? ''}"
              data-adult="${s.adultYn ?? 0}"
              data-state="${s.state ?? 1}"
              >수정</button>
            <button class="btn btn-danger" style="padding:2px 7px;font-size:11px"
              data-action="del"
              data-chnl="${s.chnlCode}"
              data-code="${s.storeCode}"
              data-name="${(s.storeName || '').replace(/"/g, '&quot;')}"
              >삭제</button>
          </td>
        </tr>`;
    }).join('');

    // table-layout:fixed + width:100%에서 폭 미지정 열("스토어명")은 지정된 열들의 폭
    // 합계가 카드 폭을 넘는 순간 강제로 찌부러진다. 모든 열에 고정폭을 주고 테이블 자체는
    // width:100% 대신 열 합계 그대로 두면, 카드가 좁을 때 열이 찌그러지는 대신
    // table-wrap의 가로 스크롤(overflow-x:auto)이 뜬다 — 항상 읽을 수 있는 쪽을 택함.
    const thEll = `${ell};max-width:0`;
    wrap.innerHTML = `
      <table style="table-layout:fixed;width:735px">
        <colgroup>
          <col style="width:220px"><col style="width:110px"><col style="width:140px">
          <col style="width:55px"><col style="width:55px">
          <col style="width:65px"><col style="width:90px">
        </colgroup>
        <thead>
          <tr>
            <th style="${thEll}">스토어명</th><th style="${thEll}">스토어코드</th><th style="${thEll}">설명</th>
            <th style="${thEll};text-align:center">레벨</th>
            <th style="${thEll};text-align:center">순서</th>
            <th style="${thEll};text-align:center">상태</th>
            <th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>`;

    // 수정
    wrap.querySelectorAll('[data-action=edit]').forEach(btn => {
      btn.addEventListener('click', () => openEditModal(btn));
    });

    // 삭제
    wrap.querySelectorAll('[data-action=del]').forEach(btn => {
      btn.addEventListener('click', () => {
        UI.confirm(`[${btn.dataset.name}] 스토어를 삭제하시겠습니까?`, async close => {
          try {
            await Api.delete('/store/store', {
              pChnlCode: btn.dataset.chnl,
              pStoreCode: btn.dataset.code,
            });
            UI.toast('삭제되었습니다', 'success');
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
          close();
        });
      });
    });
  }

  // ── 수정 모달 ─────────────────────────────────────────────────────
  function openEditModal(btn) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div class="form-grid">
        <div class="form-group full">
          <label>스토어명 <span style="color:var(--danger)">*</span></label>
          <input class="input" id="stEditName" value="${btn.dataset.name}">
        </div>
        <div class="form-group full">
          <label>설명</label>
          <input class="input" id="stEditDesc" value="${btn.dataset.desc}">
        </div>
        <div class="form-group">
          <label>순서</label>
          <input class="input" id="stEditSeq" type="number" value="${btn.dataset.seq}" style="width:80px">
        </div>
        <div class="form-group">
          <label>성인여부</label>
          <select class="input" id="stEditAdult">
            <option value="0" ${btn.dataset.adult === '0' ? 'selected' : ''}>아니오</option>
            <option value="1" ${btn.dataset.adult === '1' ? 'selected' : ''}>예</option>
          </select>
        </div>
        <div class="form-group">
          <label>상태</label>
          <select class="input" id="stEditState">
            <option value="1" ${btn.dataset.state === '1' ? 'selected' : ''}>사용</option>
            <option value="0" ${btn.dataset.state === '0' ? 'selected' : ''}>미사용</option>
          </select>
        </div>
      </div>`;

    UI.modal({
      title: `스토어 수정 – ${btn.dataset.code}`,
      body,
      confirmText: '저장',
      onConfirm: async close => {
        const storeName = document.getElementById('stEditName').value.trim();
        if (!storeName) { UI.toast('스토어명을 입력하세요', 'error'); return; }

        try {
          await Api.put('/store/store', {
            chnlCode:      btn.dataset.chnl,
            storeCode:     btn.dataset.code,
            storeName,
            storeDesc:     document.getElementById('stEditDesc').value.trim(),
            storeSeq:      parseInt(document.getElementById('stEditSeq').value) || 0,
            adultYn:       parseInt(document.getElementById('stEditAdult').value),
            state:         parseInt(document.getElementById('stEditState').value),
            changeId:      (typeof info !== 'undefined' && info?.loginId) || '',
            changeName:    (typeof info !== 'undefined' && info?.name) || '',
          });
          UI.toast('저장되었습니다', 'success');
          loadList();
          close();
        } catch (e) { UI.toast(e.message, 'error'); }
      },
    });
  }

  return { render };
})();

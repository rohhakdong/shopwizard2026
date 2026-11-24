/**
 * 주문관리 [엑셀 가격수정] (레거시 주문엑셀가격수정)
 *   엑셀(priceExcel.xls 양식)로 기존 주문 줄(주문번호+순번)의 판매액·채널공급가·원가·판촉비·배송형태·배송비를 고친다.
 *   수량·상품은 바꾸지 않는다. [검사]에서 행마다 변경/변경 없음/오류와 바뀌는 값(전→후)을 보고, [등록]하면 변경 행만 고쳐진다.
 *   정산마감된 줄·환불완료 줄·쇼핑몰(PG) 결제 주문은 서버가 거절한다. 출고지시 이후 줄은 출고 사본·마감 전 정산자료도 함께 바뀐다.
 *   서버는 [등록] 때 파일을 다시 검사하므로 검사 결과를 믿지 않는다.
 *   백엔드: /order/price-excel/{template,preview,import}
 */
const PageOrderPriceExcel = (() => {

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }

  const STATUS = {
    OK:    { cls: 'badge-blue', text: '변경' },
    SAME:  { cls: 'badge-gray', text: '변경 없음' },
    ERROR: { cls: 'badge-red',  text: '오류' },
  };

  async function downloadTemplate() {
    try {
      const res = await fetch('/order/price-excel/template', { credentials: 'same-origin' });
      if (!res.ok) throw new Error(res.status === 403 ? '이 기능을 사용할 권한이 없습니다.' : `HTTP ${res.status}`);
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'priceExcel.xls';
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    } catch (e) { UI.toast(e.message, 'error'); }
  }

  /** @param onDone 수정이 한 건이라도 반영된 뒤 부르는 콜백(주문관리 목록 새로고침) */
  function openModal(onDone) {
    const body = document.createElement('div');
    body.innerHTML = `
      <div style="font-size:12px;color:var(--text-muted);line-height:1.7;margin-bottom:10px">
        엑셀 양식(.xls, .xlsx)으로 <b>주문번호 + 순번</b>의 기존 주문 줄 가격을 고칩니다. 한 번에 최대 300행까지 올릴 수 있습니다.<br>
        판매액·채널공급가·원가는 <b>수량을 곱하기 전 1개 가격</b>이고, 칸을 비우면 현재 값이 유지됩니다. <b>수량·상품은 바꾸지 않습니다</b>(수량이 다르면 오류).<br>
        <b>정산마감된 줄, 환불완료 줄, 쇼핑몰(PG) 결제 주문</b>은 고칠 수 없습니다. 출고지시 이후 줄은 출고 사본과 마감 전 정산자료도 함께 바뀝니다.
        <a href="#" id="pxTemplate" style="margin-left:6px">엑셀 양식 받기</a>
      </div>
      <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
        <input type="file" id="pxFile" accept=".xls,.xlsx" style="flex:1">
        <button class="btn btn-ghost" id="pxCheck" type="button">검사</button>
      </div>
      <div id="pxResult"></div>`;

    const { el } = UI.modal({
      title: '엑셀 가격수정',
      body,
      confirmText: '등록',
      cancelText: '닫기',
      onConfirm: async closeModal => {
        const file = fileInput.files[0];
        if (!file || !checked) { UI.toast('먼저 파일을 선택하고 검사하세요', 'error'); return; }
        confirmBtn.disabled = true;
        checkBtn.disabled = true;
        resultEl.innerHTML = '<div style="padding:12px;color:var(--text-muted)">수정 중... (행마다 저장하므로 시간이 걸릴 수 있습니다)</div>';
        try {
          const r = await Api.upload('/order/price-excel/import', formOf(file));
          renderResult(r, true);
          checked = false;   // 같은 파일을 또 [등록]하면 이미 바뀐 값이라 "변경 없음"이 되므로 다시 검사하게 한다
          confirmBtn.disabled = true;
          if (r.imported) {
            UI.toast(`주문 줄 ${r.appliedCount}건의 가격을 수정했습니다`, r.appliedCount ? 'success' : 'info');
            if (typeof onDone === 'function') onDone();
          }
        } catch (e) {
          resultEl.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
        } finally {
          checkBtn.disabled = false;
        }
      },
    });
    el.querySelector('.modal').style.width = '1000px';

    const fileInput  = body.querySelector('#pxFile');
    const resultEl   = body.querySelector('#pxResult');
    const checkBtn   = body.querySelector('#pxCheck');
    const confirmBtn = el.querySelector('[data-action=confirm]');
    let checked = false;
    confirmBtn.disabled = true;

    const formOf = file => { const f = new FormData(); f.append('file', file); return f; };

    function renderResult(r, afterImport) {
      if (r.fileError) {
        resultEl.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(r.fileError)}</div>`;
        return;
      }
      if (!afterImport) {
        checked = !!r.importable;
        confirmBtn.disabled = !checked;
      }
      const n = v => esc(String(v));
      const summary = `
        <div style="font-size:13px;margin-bottom:8px;line-height:1.7">
          총 <b>${n((r.rows || []).length)}</b>행 ·
          ${afterImport ? `<span style="color:#15803d">수정 <b>${n(r.appliedCount)}</b></span>` : `변경 <b>${n(r.okCount)}</b>`} ·
          변경 없음 <b>${n(r.sameCount)}</b> ·
          <span style="color:${r.errorCount ? 'var(--danger)' : 'inherit'}">오류 <b>${n(r.errorCount)}</b></span> ·
          경고 <b>${n(r.warningCount)}</b>
          ${!afterImport && !r.okCount ? '<span style="color:var(--danger);margin-left:8px">바뀌는 값이 있는 정상 행이 없습니다.</span>' : ''}
          ${!afterImport && r.okCount && (r.errorCount || r.sameCount) ? '<span style="color:var(--text-muted);margin-left:8px">[등록]하면 변경 행만 수정되고 오류·변경 없음 행은 건너뜁니다.</span>' : ''}
          ${afterImport && r.message ? `<div style="color:var(--text-muted)">${esc(r.message)}</div>` : ''}
        </div>`;
      const rows = (r.rows || []).map(row => {
        const st = STATUS[row.status] || STATUS.ERROR;
        const done = afterImport && row.applied;
        return `
          <tr>
            <td style="text-align:right">${n(row.rowNo)}</td>
            <td style="white-space:nowrap">${esc(row.orderNo != null ? row.orderNo + '-' + (row.orderProdNo ?? 1) : '')}</td>
            <td style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${esc(row.prodName)}">${esc(row.prodName)}</td>
            <td style="white-space:nowrap">${esc(row.orderState)}</td>
            <td><span class="badge ${done ? 'badge-green' : st.cls}">${done ? '수정됨' : st.text}</span></td>
            <td style="font-size:12px;line-height:1.5">
              ${(row.changes || []).map(m => `<div>${esc(m)}</div>`).join('')}
              ${(row.errors || []).map(m => `<div style="color:var(--danger)">${esc(m)}</div>`).join('')}
              ${(row.warnings || []).map(m => `<div style="color:#b45309">${esc(m)}</div>`).join('')}
            </td>
          </tr>`;
      }).join('');
      resultEl.innerHTML = `${summary}
        <div style="max-height:420px;overflow:auto;border:1px solid var(--border);border-radius:var(--radius)">
          <table class="table" style="width:100%;font-size:12px">
            <thead><tr><th>행</th><th>주문-순번</th><th>상품명</th><th>상태</th><th>결과</th><th>바뀌는 값 / 메시지</th></tr></thead>
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
    body.querySelector('#pxTemplate').addEventListener('click', e => { e.preventDefault(); downloadTemplate(); });

    checkBtn.addEventListener('click', async () => {
      const file = fileInput.files[0];
      if (!file) { UI.toast('엑셀 파일을 선택하세요', 'error'); return; }
      resultEl.innerHTML = '<div style="padding:12px;color:var(--text-muted)">검사 중...</div>';
      checked = false;
      confirmBtn.disabled = true;
      try {
        renderResult(await Api.upload('/order/price-excel/preview', formOf(file)), false);
      } catch (e) {
        resultEl.innerHTML = `<div style="padding:12px;color:var(--danger)">${esc(e.message)}</div>`;
      }
    });
  }

  return { openModal };
})();

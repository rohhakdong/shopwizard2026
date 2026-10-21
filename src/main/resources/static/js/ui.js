/**
 * 공통 UI 유틸 – 토스트, 모달, 페이지네이션
 */
const UI = (() => {

  // ── 토스트 ──────────────────────────────────────────────────────────
  function toast(message, type = 'info', duration = 3000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }

    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    container.appendChild(el);

    requestAnimationFrame(() => {
      requestAnimationFrame(() => el.classList.add('show'));
    });

    setTimeout(() => {
      el.classList.remove('show');
      setTimeout(() => el.remove(), 200);
    }, duration);
  }

  // ── 모달 ────────────────────────────────────────────────────────────
  function modal({ title, body, onConfirm, confirmText = '확인', cancelText = '취소' }) {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';
    backdrop.innerHTML = `
      <div class="modal">
        <div class="modal-header">
          <span>${esc(title)}</span>
          <button class="modal-close">&times;</button>
        </div>
        <div class="modal-body">${typeof body === 'string' ? body : ''}</div>
        <div class="modal-footer">
          <button class="btn btn-ghost" data-action="cancel">${cancelText}</button>
          ${confirmText != null ? `<button class="btn btn-primary" data-action="confirm">${confirmText}</button>` : ''}
        </div>
      </div>`;

    if (typeof body !== 'string' && body instanceof HTMLElement) {
      backdrop.querySelector('.modal-body').appendChild(body);
    }

    function close() { backdrop.remove(); }

    backdrop.querySelector('.modal-close').addEventListener('click', close);
    backdrop.querySelector('[data-action=cancel]').addEventListener('click', close);
    backdrop.querySelector('[data-action=confirm]')?.addEventListener('click', async () => {
      if (onConfirm) await onConfirm(close);
    });
    backdrop.addEventListener('click', e => { if (e.target === backdrop) close(); });

    document.body.appendChild(backdrop);
    return { close, el: backdrop };
  }

  // title / message 는 텍스트로 취급해 이스케이프한다(호출부가 `[${이름}]` 처럼 데이터를 끼워 넣는 경우가 많다).
  // 마크업이 필요하면 modal({ body: '<...>' }) 를 직접 쓰고 값은 UI.esc 로 감싼다.
  function confirm(message, onConfirm) {
    return modal({ title: '확인', body: `<p style="margin:8px 0">${esc(message)}</p>`, onConfirm, confirmText: '확인', cancelText: '취소' });
  }

  // ── 페이지네이션 ─────────────────────────────────────────────────────
  function pagination(container, { total, page, pageSize = 20, onChange }) {
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    const group = Math.floor((page - 1) / 10);
    const start = group * 10 + 1;
    const end   = Math.min(start + 9, totalPages);

    container.innerHTML = '';
    const wrap = document.createElement('div');
    wrap.className = 'pagination';

    const btn = (label, p, disabled = false) => {
      const b = document.createElement('button');
      b.textContent = label;
      if (p === page) b.classList.add('active');
      if (disabled) b.disabled = true;
      b.addEventListener('click', () => !disabled && p !== page && onChange(p));
      return b;
    };

    wrap.appendChild(btn('«', 1,        page === 1));
    wrap.appendChild(btn('‹', page - 1, page === 1));
    for (let i = start; i <= end; i++) wrap.appendChild(btn(i, i));
    wrap.appendChild(btn('›', page + 1, page === totalPages));
    wrap.appendChild(btn('»', totalPages, page === totalPages));

    const info = document.createElement('span');
    info.style.cssText = 'font-size:12px;color:var(--text-muted);margin-left:8px;';
    info.textContent = `총 ${total.toLocaleString()}건`;
    wrap.appendChild(info);

    container.appendChild(wrap);
  }

  // ── 로딩 스피너 ───────────────────────────────────────────────────────
  function loading(container, show) {
    let el = container.querySelector('.loading-spinner');
    if (show) {
      if (!el) {
        el = document.createElement('div');
        el.className = 'loading-spinner';
        el.style.cssText = 'text-align:center;padding:40px;color:var(--text-muted);font-size:13px;';
        el.textContent = '불러오는 중...';
        container.appendChild(el);
      }
    } else {
      if (el) el.remove();
    }
  }

  // ── HTML 이스케이프 ─────────────────────────────────────────────────
  // 외부 채널(샵링커 등)에서 온 상품명·수취인·메모 같은 값을 innerHTML 템플릿에 넣을 때 사용한다.
  // 속성값("...")에도 안전하도록 따옴표까지 치환한다. null/undefined 는 빈 문자열.
  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // ── 목록 화면 공통 필터(채널/상점/담당MD) 옵션 ───────────────────────────
  // 배송·반품 화면들이 같은 드롭다운을 쓴다. 옵션 로드가 실패해도 목록 자체는 동작해야 하므로
  // 실패 시 빈 목록을 돌려준다.
  async function loadFilterOptions(svcCode) {
    let chnls = [], shops = [];
    try {
      const [c, s] = await Promise.all([
        Api.get('/company/chnl', { pSvcCode: svcCode, pPageOffset: 0, pPageSize: 2000 }),
        Api.get('/company/shop', {}),
      ]);
      chnls = (c || []).slice().sort((a, b) => (a.chnlName || '').localeCompare(b.chnlName || '', 'ko'));
      shops = (s || []).filter(x => x.svcCode === svcCode)
        .sort((a, b) => (a.shopName || '').localeCompare(b.shopName || '', 'ko'));
    } catch (_) { /* 옵션 없이도 동작 */ }
    const mds = [...new Set(shops.map(x => x.mngrMd).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ko'));
    return { chnls, shops, mds };
  }

  /** select 요소에 [{v, t}] 옵션을 추가한다(기존 "전체" 옵션 뒤에). 값/표시는 이스케이프된다. */
  function fillOptions(selectId, opts) {
    const el = document.getElementById(selectId);
    if (!el) return;
    el.insertAdjacentHTML('beforeend', opts.map(o => `<option value="${esc(o.v)}">${esc(o.t)}</option>`).join(''));
  }

  /** 리치텍스트 HTML(상품설명 등)을 허용 목록으로 정화한 문자열. 구현은 safe-html.js. */
  function sanitizeHtml(html) { return SafeHtml.sanitize(html); }

  return { toast, modal, confirm, pagination, loading, esc, sanitizeHtml, loadFilterOptions, fillOptions };
})();

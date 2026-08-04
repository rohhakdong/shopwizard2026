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
          <span>${title}</span>
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

  function confirm(message, onConfirm) {
    return modal({ title: '확인', body: `<p style="margin:8px 0">${message}</p>`, onConfirm, confirmText: '확인', cancelText: '취소' });
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

  return { toast, modal, confirm, pagination, loading };
})();

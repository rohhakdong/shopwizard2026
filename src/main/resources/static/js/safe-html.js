/**
 * 리치텍스트(상품설명 등) HTML 정화기.
 *
 * 상품설명은 Quill 에디터가 만든 HTML 이라 이스케이프하면 서식이 깨진다. 그래서 허용 목록(태그/속성/CSS)에 있는
 * 것만 남기고 나머지(스크립트, 이벤트 핸들러 on*, javascript: URL, iframe/object/embed 등)는 버린다.
 * DOMParser 로 파싱한 비활성 문서에서 걸러내므로 정화 중에는 스크립트 실행이나 이미지 로딩이 일어나지 않는다.
 *
 * 관리자 화면(UI.sanitizeHtml)과 스토어프론트(shop.html)가 같이 쓴다. 외부 라이브러리 없음.
 */
const SafeHtml = (() => {

  const ALLOWED_TAGS = new Set([
    'p', 'br', 'div', 'span', 'strong', 'b', 'em', 'i', 'u', 's', 'strike', 'sub', 'sup',
    'ul', 'ol', 'li', 'blockquote', 'pre', 'code', 'hr',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'a', 'img',
    'table', 'thead', 'tbody', 'tfoot', 'tr', 'td', 'th', 'colgroup', 'col', 'caption',
    'figure', 'figcaption',
  ]);

  // 태그와 그 자식까지 통째로 버리는 것들(내용이 텍스트로 새어 나오지도 않게)
  const DROP_WITH_CONTENT = new Set([
    'script', 'style', 'iframe', 'frame', 'frameset', 'object', 'embed', 'applet', 'link', 'meta', 'base',
    'form', 'input', 'button', 'textarea', 'select', 'option', 'svg', 'math', 'template', 'noscript', 'video', 'audio', 'source',
  ]);

  const GLOBAL_ATTRS = new Set(['class', 'style', 'title']);
  const TAG_ATTRS = {
    a: new Set(['href', 'target', 'rel']),
    img: new Set(['src', 'alt', 'width', 'height']),
    td: new Set(['colspan', 'rowspan']),
    th: new Set(['colspan', 'rowspan']),
    col: new Set(['span', 'width']),
  };

  const ALLOWED_CSS = new Set([
    'color', 'background-color', 'text-align', 'text-decoration', 'font-size', 'font-weight', 'font-style', 'font-family',
    'line-height', 'width', 'height', 'max-width', 'margin', 'margin-top', 'margin-bottom', 'margin-left', 'margin-right',
    'padding', 'padding-top', 'padding-bottom', 'padding-left', 'padding-right', 'text-indent',
  ]);

  function safeUrl(value, { allowData = false } = {}) {
    const v = String(value || '').trim().replace(/[\u0000-\u001f\u007f\s]+/g, '');   // 제어문자/공백으로 스킴을 숨기는 수법 제거
    if (!v) return null;
    if (/^(https?:|mailto:|tel:)/i.test(v)) return value.trim();
    if (/^(\/(?!\/)|\.\/|\.\.\/|#)/.test(v)) return value.trim();           // 사이트 내부 상대경로, 앵커
    if (allowData && /^data:image\/(png|jpe?g|gif|webp);base64,[a-z0-9+/=]+$/i.test(v)) return value.trim();
    return null;                                                             // javascript:, vbscript:, data:text/html ...
  }

  function safeStyle(css) {
    const kept = [];
    String(css || '').split(';').forEach(decl => {
      const i = decl.indexOf(':');
      if (i < 0) return;
      const prop = decl.slice(0, i).trim().toLowerCase();
      const val = decl.slice(i + 1).trim();
      if (!ALLOWED_CSS.has(prop)) return;
      if (/url\s*\(|expression|javascript:|@import|[<>\\]|\/\*/i.test(val)) return;
      kept.push(`${prop}:${val}`);
    });
    return kept.join(';');
  }

  function clean(node, out) {
    for (const child of [...node.childNodes]) {
      if (child.nodeType === Node.TEXT_NODE) { out.appendChild(out.ownerDocument.createTextNode(child.nodeValue)); continue; }
      if (child.nodeType !== Node.ELEMENT_NODE) continue;                    // 주석 등은 버린다
      const tag = child.tagName.toLowerCase();
      if (DROP_WITH_CONTENT.has(tag)) continue;
      if (!ALLOWED_TAGS.has(tag)) { clean(child, out); continue; }          // 모르는 태그는 벗기고 내용만 유지

      const el = out.ownerDocument.createElement(tag);
      const tagAttrs = TAG_ATTRS[tag] || new Set();
      for (const attr of [...child.attributes]) {
        const name = attr.name.toLowerCase();
        if (name.startsWith('on')) continue;
        if (!GLOBAL_ATTRS.has(name) && !tagAttrs.has(name)) continue;
        let value = attr.value;
        if (name === 'href') { value = safeUrl(value); if (value == null) continue; }
        else if (name === 'src') { value = safeUrl(value, { allowData: true }); if (value == null) continue; }
        else if (name === 'style') { value = safeStyle(value); if (!value) continue; }
        else if (name === 'class') { value = value.replace(/[^\w\- ]/g, '').trim(); if (!value) continue; }
        else if (name === 'target') { value = '_blank'; }
        el.setAttribute(name, value);
      }
      if (tag === 'a') { el.setAttribute('rel', 'noopener noreferrer'); }
      if (tag === 'img' && !el.hasAttribute('src')) continue;                // 안전한 src 가 없는 이미지는 버린다
      clean(child, el);
      out.appendChild(el);
    }
  }

  /** 허용 목록으로 정화된 HTML 문자열을 돌려준다. 문자열이 아니거나 비어 있으면 ''. */
  function sanitize(html) {
    if (html == null || html === '') return '';
    const src = new DOMParser().parseFromString('<!doctype html><body>' + String(html), 'text/html');
    const dst = document.implementation.createHTMLDocument('');
    clean(src.body, dst.body);
    return dst.body.innerHTML;
  }

  return { sanitize };
})();

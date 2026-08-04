/**
 * API 공통 fetch wrapper
 * 모든 REST 요청은 이 함수를 통해 처리
 */
const Api = (() => {

  async function request(method, url, data) {
    const opts = {
      method,
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
    };
    if (data !== undefined) {
      if (method === 'GET' || method === 'DELETE') {
        if (data && typeof data === 'object') {
          const qs = new URLSearchParams(
            Object.fromEntries(Object.entries(data).filter(([, v]) => v !== undefined && v !== null && v !== ''))
          ).toString();
          if (qs) url += (url.includes('?') ? '&' : '?') + qs;
        }
      } else {
        opts.body = JSON.stringify(data);
      }
    }

    const res = await fetch(url, opts);

    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
    }

    const contentType = res.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      return res.json();
    }
    return res.text();
  }

  return {
    get:    (url, params)  => request('GET',    url, params),
    post:   (url, body)    => request('POST',   url, body),
    put:    (url, body)    => request('PUT',    url, body),
    delete: (url, params)  => request('DELETE', url, params),
  };
})();

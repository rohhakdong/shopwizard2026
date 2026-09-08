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

  // 파일 업로드 전용 (multipart/form-data). Content-Type은 boundary가 필요해서
  // 브라우저가 자동으로 붙이도록 헤더를 직접 지정하지 않는다 — request()의 JSON 강제와는 별도 경로.
  async function upload(url, formData) {
    const res = await fetch(url, { method: 'POST', body: formData, credentials: 'same-origin' });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
    }
    return res.json();
  }

  return {
    get:    (url, params)  => request('GET',    url, params),
    post:   (url, body)    => request('POST',   url, body),
    put:    (url, body)    => request('PUT',    url, body),
    delete: (url, params)  => request('DELETE', url, params),
    upload,
  };
})();

/**
 * 인증 유틸 – /auth/mngr/me API 기반 (쿠키는 HttpOnly라 JS에서 직접 읽기 불가)
 */
const Auth = (() => {

  // 마지막으로 확인한 세션 정보 (페이지 내 캐시)
  let _info = null;

  async function fetchMe() {
    const res = await fetch('/auth/mngr/me', { credentials: 'same-origin' });
    return res.json();
  }

  async function isLoggedIn() {
    const me = await fetchMe();
    return me.authenticated === true;
  }

  async function getInfo() {
    if (_info) return _info;
    const me = await fetchMe();
    if (!me.authenticated) return null;
    _info = {
      loginId:    me.loginId    || '',
      name:       me.name       || '',
      roleUid:    me.roleUid    || '',
      svcCode:    me.svcCode    || '',
      chnlCode:   me.chnlCode   || '',
      compCode:   me.compCode   || '',
      shopCode:   me.shopCode   || '',
      warehsCode: me.warehsCode || '',
    };
    return _info;
  }

  async function login(loginId, passwd, chnlCode) {
    const res = await fetch('/auth/mngr/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ loginId, passwd, chnlCode: chnlCode || '' }),
    });
    return res.json();
  }

  async function logout() {
    _info = null;
    await fetch('/auth/mngr/logout', { method: 'POST', credentials: 'same-origin' });
  }

  /** 비로그인 상태면 login.html로 리다이렉트 */
  async function requireLogin() {
    const ok = await isLoggedIn();
    if (!ok) location.href = '/login.html';
    return ok;
  }

  /** 로그인 상태면 index.html로 리다이렉트 */
  async function requireGuest() {
    const ok = await isLoggedIn();
    if (ok) location.href = '/index.html';
  }

  return { isLoggedIn, getInfo, login, logout, requireLogin, requireGuest };
})();

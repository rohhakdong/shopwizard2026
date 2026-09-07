/**
 * 창고(거래처) 계정 인증 유틸 – /auth/warehs/me API 기반.
 * 관리자용 Auth(js/auth.js), 상점용 ShopAuth(js/shop-auth.js)와 완전히 분리된 별도 세션
 * (warehs_code 쿠키)을 사용한다. 현재 범위: 로그인 + 내 정보 확인까지.
 */
const WarehsAuth = (() => {

  let _info = null;

  async function fetchMe() {
    const res = await fetch('/auth/warehs/me', { credentials: 'same-origin' });
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
      warehsCode: me.warehsCode || '',
      name:       me.name       || '',
      loginId:    me.loginId    || '',
    };
    return _info;
  }

  async function login(loginId, passwd) {
    const res = await fetch('/auth/warehs/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ loginId, passwd }),
    });
    return res.json();
  }

  async function logout() {
    _info = null;
    await fetch('/auth/warehs/logout', { method: 'POST', credentials: 'same-origin' });
  }

  /** 비로그인 상태면 warehs-login.html로 리다이렉트 */
  async function requireLogin() {
    const ok = await isLoggedIn();
    if (!ok) location.href = '/warehs-login.html';
    return ok;
  }

  /** 로그인 상태면 warehs-portal.html로 리다이렉트 */
  async function requireGuest() {
    const ok = await isLoggedIn();
    if (ok) location.href = '/warehs-portal.html';
  }

  return { isLoggedIn, getInfo, login, logout, requireLogin, requireGuest };
})();

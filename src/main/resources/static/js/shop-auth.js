/**
 * 상점(거래처) 계정 인증 유틸 – /auth/shop/me API 기반.
 * 관리자용 Auth(js/auth.js)와 완전히 분리된 별도 세션(shop_code 쿠키)을 사용한다.
 */
const ShopAuth = (() => {

  let _info = null;

  async function fetchMe() {
    const res = await fetch('/auth/shop/me', { credentials: 'same-origin' });
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
      shopCode:   me.shopCode   || '',
      name:       me.name       || '',
      loginId:    me.loginId    || '',
      supplyCode: me.supplyCode || '',
      svcCode:    me.svcCode    || '',
    };
    return _info;
  }

  async function login(loginId, passwd) {
    const res = await fetch('/auth/shop/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ loginId, passwd }),
    });
    return res.json();
  }

  async function logout() {
    _info = null;
    await fetch('/auth/shop/logout', { method: 'POST', credentials: 'same-origin' });
  }

  /** 비로그인 상태면 shop-login.html로 리다이렉트 */
  async function requireLogin() {
    const ok = await isLoggedIn();
    if (!ok) location.href = '/shop-login.html';
    return ok;
  }

  /** 로그인 상태면 shop-portal.html로 리다이렉트 */
  async function requireGuest() {
    const ok = await isLoggedIn();
    if (ok) location.href = '/shop-portal.html';
  }

  return { isLoggedIn, getInfo, login, logout, requireLogin, requireGuest };
})();

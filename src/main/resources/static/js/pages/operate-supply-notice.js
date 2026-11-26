/**
 * 공급사 공지 (사이드 메뉴 운영) — 레거시 공급회사공지/상점공지(wCmpSupplyCompNoticeList, shopwizard.tCmpSupplyCompNotice).
 *   채널·회사 구분 없이 관리자·공급사 화면에 보이는 전역 공지를 등록·수정·삭제한다. 쓰기는 상점·채널·서비스 제한이 없는 계정만 가능하다
 *   (서버가 막고, 조회는 화면 권한만). 화면 구성은 공지사항 화면과 같다(NoticeScreen 은 operate-notice.js).
 *   백엔드: /company/supply-comp-notice (list, GET {no}, POST, PUT, DELETE {no})
 */
const PageOperateSupplyNotice = NoticeScreen.create({
  prefix: 'osn',
  title: '공급사 공지',
  subtitle: '관리자·공급사 전체 공지 (채널·회사 구분 없음)',
  hasChnl: false,
  titleParam: 'pTitle',
  linkMax: 255,
  note: '채널·회사 구분 없이 모두에게 보이는 공지입니다. <b>등록·수정·삭제는 제한 없는 계정만</b> 할 수 있습니다. 게시기간은 날짜 단위로, 종료일 당일까지 게시됩니다.',
  api: {
    list: p => Api.get('/company/supply-comp-notice/list', p),
    get: no => Api.get('/company/supply-comp-notice/' + no),
    create: b => Api.post('/company/supply-comp-notice', b),
    update: b => Api.put('/company/supply-comp-notice', b),
    remove: no => Api.delete('/company/supply-comp-notice/' + no),
  },
  norm: r => ({
    no: r.noticeNo, title: r.title, content: r.cntnts || '', startDate: r.startDate, endDate: r.endDate, popupYn: r.popupYn,
    horiznLoc: r.horiznLoc, verticLoc: r.verticLoc, popupWidth: r.popupWidth, popupLenght: r.popupLenght, linkAddr: r.linkAddr,
    chnlCode: '', chnlName: '', compCode: '', status: r.status, registName: r.registName, registDate: r.registDate,
  }),
});

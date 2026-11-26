/**
 * 공지사항 (사이드 메뉴 운영) — 레거시 공지사항(wOprNoticeList, shopion.tOprNotice). 쇼핑몰 공지를 채널(·회사)별로 등록·수정·삭제한다.
 *   게시기간은 날짜 단위(종료일 당일까지 게시)이고, 목록의 상태는 게시중/예정/종료로 보여 준다. 내용은 일반 텍스트(줄바꿈 유지).
 *   이번 단계는 관리 화면만 — 쇼핑몰(스토어프론트) 표시는 아직 없다.
 *   백엔드: /operate/notice (list, GET {no}, POST, PUT, DELETE {no})
 *
 * 같은 모양의 공급사 공지 화면(operate-supply-notice.js)이 {@link NoticeScreen} 을 함께 쓴다 — API 호출은 각 화면 파일에 리터럴로 두어
 * 화면 권한 소유표 검사(ScreenRegistryTest)가 보게 한다.
 */
const NoticeScreen = (() => {

  const esc = UI.esc;
  const PAGE_SIZE = 20;
  const STATUS = { ING: ['게시중', '#15803d', '#dcfce7'], WAIT: ['예정', '#1d4ed8', '#dbeafe'], END: ['종료', '#6b7280', '#f3f4f6'] };

  function svcCode() { return (typeof info !== 'undefined' && info?.svcCode) || 'SHP001'; }
  function today() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10); }
  function day(s) { return s ? String(s).slice(0, 10) : ''; }
  function badge(status) {
    const b = STATUS[status] || ['-', '#6b7280', '#f3f4f6'];
    return `<span style="font-size:11px;padding:2px 8px;border-radius:10px;color:${b[1]};background:${b[2]}">${b[0]}</span>`;
  }

  /**
   * cfg: { prefix, title, subtitle, hasChnl, note, api:{list,get,create,update,remove},
   *        norm(row) → {no,title,content,startDate,endDate,popupYn,horiznLoc,verticLoc,popupWidth,popupLenght,linkAddr,chnlCode,chnlName,compCode,status,registName,registDate},
   *        titleParam, linkMax }
   */
  function create(cfg) {
    const P = cfg.prefix;
    const id = s => document.getElementById(P + s);
    let currentPage = 1;
    let seq = 0;
    let chnls = [];

    function render(container) {
      currentPage = 1;
      container.innerHTML = `
        <div class="card">
          <div class="card-header" style="display:flex;align-items:center;justify-content:space-between">
            <span>${esc(cfg.title)} <span style="font-weight:400;font-size:12px;color:var(--text-muted)">— ${esc(cfg.subtitle)}</span></span>
            <button class="btn btn-primary" id="${P}BtnNew" type="button">+ 공지 등록</button>
          </div>
          <div class="card-body" style="padding:12px 16px">
            <div style="background:#fffbeb;border:1px solid #fde68a;border-radius:var(--radius);padding:8px 12px;margin-bottom:12px;font-size:12px;line-height:1.7">${cfg.note}</div>
            <div class="search-bar" style="gap:8px;flex-wrap:wrap;align-items:flex-end">
              ${cfg.hasChnl ? `<div class="form-group"><label>채널</label><select class="input" id="${P}Chnl" style="width:170px"><option value="">전체</option></select></div>` : ''}
              <div class="form-group"><label>게시 상태</label>
                <select class="input" id="${P}Status" style="width:100px"><option value="">전체</option><option value="ING">게시중</option><option value="WAIT">예정</option><option value="END">종료</option></select></div>
              <div class="form-group"><label>제목</label><input class="input" id="${P}Title" style="width:150px"></div>
              ${cfg.hasChnl ? `<div class="form-group"><label>내용</label><input class="input" id="${P}Content" style="width:150px"></div>` : ''}
              <div class="form-group"><label>게시기간이 겹치는 기간</label>
                <span><input class="input" id="${P}Start" type="date" style="width:135px"> ~ <input class="input" id="${P}End" type="date" style="width:135px"></span></div>
              <button class="btn btn-primary" id="${P}BtnSearch" type="button">검색</button>
            </div>
            <div style="font-size:12px;color:var(--text-muted);margin:8px 0">총 <strong id="${P}Total">0</strong>건</div>
            <div class="table-wrap" id="${P}Wrap"></div>
            <div id="${P}Page" style="margin-top:12px"></div>
          </div>
        </div>`;
      id('BtnSearch').addEventListener('click', () => { currentPage = 1; loadList(); });
      id('BtnNew').addEventListener('click', () => openForm(null));
      id('Title').addEventListener('keydown', e => { if (e.key === 'Enter') { currentPage = 1; loadList(); } });
      if (cfg.hasChnl) {
        UI.loadFilterOptions(svcCode()).then(o => {
          chnls = o.chnls;
          UI.fillOptions(P + 'Chnl', chnls.map(c => ({ v: c.chnlCode, t: c.chnlName })));
        });
      }
      loadList();
    }

    function params() {
      const p = { pStatus: id('Status').value, [cfg.titleParam]: id('Title').value.trim(), pStartDate: id('Start').value, pEndDate: id('End').value };
      if (cfg.hasChnl) { p.pChnlCode = id('Chnl').value; p.pNoticeCntnts = id('Content').value.trim(); }
      return p;
    }

    async function loadList() {
      const wrap = id('Wrap');
      wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">불러오는 중...</div>';
      id('Page').innerHTML = '';
      const mine = ++seq;
      try {
        const res = await cfg.api.list({ ...params(), pPageOffset: (currentPage - 1) * PAGE_SIZE, pPageSize: PAGE_SIZE });
        if (mine !== seq) return;
        id('Total').textContent = Number(res.total).toLocaleString();
        if (!res.total) { wrap.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-muted)">공지가 없습니다</div>'; return; }
        const rows = res.rows.map(cfg.norm);
        wrap.innerHTML = `
          <table style="width:100%;font-size:12px">
            <thead><tr><th>번호</th>${cfg.hasChnl ? '<th>채널</th><th>회사</th>' : ''}<th>제목</th><th>게시기간</th><th>상태</th><th>팝업</th><th>등록자</th><th>등록일</th><th></th></tr></thead>
            <tbody>${rows.map((r, i) => `
              <tr style="cursor:pointer" data-i="${i}">
                <td>${esc(r.no)}</td>${cfg.hasChnl ? `<td>${esc(r.chnlName || r.chnlCode)}</td><td>${esc(r.compCode)}</td>` : ''}
                <td><b>${esc(r.title)}</b></td>
                <td style="white-space:nowrap">${esc(day(r.startDate))} ~ ${esc(day(r.endDate) === '2999-12-31' ? '무기한' : day(r.endDate))}</td>
                <td>${badge(r.status)}</td><td>${r.popupYn ? '팝업' : ''}</td><td>${esc(r.registName)}</td><td>${esc(day(r.registDate))}</td>
                <td><button class="btn btn-danger" style="padding:2px 7px;font-size:11px" data-del="${i}" type="button">삭제</button></td>
              </tr>`).join('')}</tbody>
          </table>`;
        wrap.querySelectorAll('tr[data-i]').forEach(tr => tr.addEventListener('click', e => {
          if (e.target.closest('[data-del]')) return;
          openForm(rows[+tr.dataset.i].no);
        }));
        wrap.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
          const r = rows[+b.dataset.del];
          UI.confirm(`공지 "${r.title}" 를 삭제하시겠습니까? 되돌릴 수 없습니다.`, async close => {
            try {
              await cfg.api.remove(r.no);
              UI.toast('삭제되었습니다', 'success');
              loadList();
            } catch (e) { UI.toast(e.message, 'error'); }
            close();
          });
        }));
        UI.pagination(id('Page'), { total: res.total, page: currentPage, pageSize: PAGE_SIZE, onChange: p => { currentPage = p; loadList(); } });
      } catch (e) {
        if (mine !== seq) return;
        wrap.innerHTML = `<div style="color:var(--danger);padding:16px">${esc(e.message)}</div>`;
      }
    }

    /** 등록(no = null)·수정 모달. 수정은 내용까지 있는 단건을 읽어 온다. */
    async function openForm(no) {
      let v = { title: '', content: '', startDate: today(), endDate: '', popupYn: 0, horiznLoc: 50, verticLoc: 50, popupWidth: 386, popupLenght: 200, linkAddr: '', chnlCode: '', compCode: '' };
      if (no != null) {
        try { v = cfg.norm(await cfg.api.get(no)); } catch (e) { return UI.toast(e.message, 'error'); }
      }
      const endShown = day(v.endDate) === '2999-12-31' ? '' : day(v.endDate);
      const body = document.createElement('div');
      body.innerHTML = `
        ${cfg.hasChnl ? `<div class="form-group"><label>채널 *</label>
          <select class="input" id="${P}FChnl"><option value="">선택</option>${chnls.map(c => `<option value="${esc(c.chnlCode)}" ${c.chnlCode === v.chnlCode ? 'selected' : ''}>${esc(c.chnlName)}</option>`).join('')}</select></div>
          <div class="form-group" style="margin-top:8px"><label>회사코드 <span style="font-weight:400;color:var(--text-muted)">(비우면 모든 고객)</span></label>
            <input class="input" id="${P}FComp" maxlength="10" value="${esc(v.compCode || '')}"></div>` : ''}
        <div class="form-group" style="margin-top:8px"><label>제목 * <span style="font-weight:400;color:var(--text-muted)">(50자 이내)</span></label>
          <input class="input" id="${P}FTitle" maxlength="50" value="${esc(v.title)}"></div>
        <div style="display:flex;gap:8px;margin-top:8px">
          <div class="form-group" style="flex:1"><label>게시 시작일 *</label><input class="input" id="${P}FStart" type="date" value="${esc(day(v.startDate))}"></div>
          <div class="form-group" style="flex:1"><label>게시 종료일 <span style="font-weight:400;color:var(--text-muted)">(비우면 무기한)</span></label><input class="input" id="${P}FEnd" type="date" value="${esc(endShown)}"></div>
        </div>
        <div class="form-group" style="margin-top:8px"><label>내용</label>
          <textarea class="input" id="${P}FContent" rows="8" style="width:100%" maxlength="20000">${esc(v.content)}</textarea></div>
        <div style="margin-top:8px"><label><input type="checkbox" id="${P}FPopup" ${v.popupYn ? 'checked' : ''}> 팝업으로 띄우기</label></div>
        <div id="${P}FPopupBox" style="display:${v.popupYn ? 'flex' : 'none'};gap:8px;margin-top:6px;flex-wrap:wrap">
          <div class="form-group"><label>가로 위치</label><input class="input" id="${P}FH" type="number" min="0" max="3000" style="width:90px" value="${esc(v.horiznLoc ?? 50)}"></div>
          <div class="form-group"><label>세로 위치</label><input class="input" id="${P}FV" type="number" min="0" max="3000" style="width:90px" value="${esc(v.verticLoc ?? 50)}"></div>
          <div class="form-group"><label>폭</label><input class="input" id="${P}FW" type="number" min="0" max="3000" style="width:90px" value="${esc(v.popupWidth ?? 386)}"></div>
          <div class="form-group"><label>길이</label><input class="input" id="${P}FL" type="number" min="0" max="3000" style="width:90px" value="${esc(v.popupLenght ?? 200)}"></div>
        </div>
        <div class="form-group" style="margin-top:8px"><label>링크 주소 <span style="font-weight:400;color:var(--text-muted)">(${cfg.linkMax}자 이내)</span></label>
          <input class="input" id="${P}FLink" maxlength="${cfg.linkMax}" value="${esc(v.linkAddr || '')}"></div>`;
      const q = s => body.querySelector('#' + P + s);
      q('FPopup').addEventListener('change', () => { q('FPopupBox').style.display = q('FPopup').checked ? 'flex' : 'none'; });
      UI.modal({
        title: no == null ? '공지 등록' : `공지 수정 #${no}`, body, confirmText: '저장',
        onConfirm: async close => {
          const payload = {
            noticeNo: no, title: q('FTitle').value.trim(), content: q('FContent').value, startDate: q('FStart').value, endDate: q('FEnd').value,
            popupYn: q('FPopup').checked ? 1 : 0, horiznLoc: +q('FH').value, verticLoc: +q('FV').value, popupWidth: +q('FW').value, popupLenght: +q('FL').value,
            linkAddr: q('FLink').value.trim(),
          };
          if (cfg.hasChnl) { payload.chnlCode = q('FChnl').value; payload.compCode = q('FComp').value.trim(); }
          if (!payload.title) return UI.toast('제목을 입력하세요', 'error');
          if (cfg.hasChnl && !payload.chnlCode) return UI.toast('채널을 선택하세요', 'error');
          if (!payload.startDate) return UI.toast('게시 시작일을 입력하세요', 'error');
          try {
            if (no == null) await cfg.api.create(payload); else await cfg.api.update(payload);
            UI.toast('저장되었습니다', 'success');
            close();
            if (no == null) currentPage = 1;
            loadList();
          } catch (e) { UI.toast(e.message, 'error'); }
        },
      });
    }

    return { render };
  }

  return { create };
})();

const PageOperateNotice = NoticeScreen.create({
  prefix: 'on',
  title: '공지사항',
  subtitle: '쇼핑몰 공지 (채널·회사별)',
  hasChnl: true,
  titleParam: 'pNoticeTitle',
  linkMax: 50,
  note: '채널(·회사)별 쇼핑몰 공지를 등록합니다. 게시기간은 <b>날짜 단위</b>로, 종료일 당일까지 게시됩니다. 내용은 일반 텍스트입니다. '
    + '<b>아직 쇼핑몰 화면에는 표시되지 않습니다</b>(관리 화면만 제공).',
  api: {
    list: p => Api.get('/operate/notice/list', p),
    get: no => Api.get('/operate/notice/' + no),
    create: b => Api.post('/operate/notice', b),
    update: b => Api.put('/operate/notice', b),
    remove: no => Api.delete('/operate/notice/' + no),
  },
  norm: r => ({
    no: r.noticeNo, title: r.noticeTitle, content: r.noticeCntnts || '', startDate: r.startDate, endDate: r.endDate, popupYn: r.popupYn,
    horiznLoc: r.horiznLoc, verticLoc: r.verticLoc, popupWidth: r.popupWidth, popupLenght: r.popupLenght, linkAddr: r.linkAddr,
    chnlCode: r.chnlCode, chnlName: r.chnlName, compCode: r.compCode, status: r.status, registName: r.registName, registDate: r.registDate,
  }),
});

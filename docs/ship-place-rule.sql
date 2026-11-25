-- 배송제한 규칙 (자동발주 "배송 데이터 생성"에서 발주서 행의 출고지=엑셀 시트명과 수취인 표시를 바꾼다)
-- 1) tShpShipPlaceRule: 상점별 규칙(우편번호 구분 OR 주소 LIKE 패턴 → 시트명·수취인 표시, 상품명 포함·제외 채널은 AND)
-- 2) tCmpShop.ShipRstrctYn: 상점별 사용 여부(기본 0=끔)
-- 3) 피죤(3018508386A1) 레거시 rCreateShipPigeon3.xml 의 규칙 시드 — 상점 플래그는 꺼진 채라 켜기 전에는 적용되지 않는다.
--    특정 주문자 이름·전화번호로 박혀 있던 예외 1건(개인정보)은 옮기지 않았다.
-- 우편번호 구분(DeliFeeUp*, *JerrycanLimit)의 우편번호는 tShpDeliRstrctZipcodeAddr 에 "배송제한우편번호" 화면으로 등록한다.
-- 되돌리기: DROP TABLE shopwizard.tShpShipPlaceRule; ALTER TABLE shopwizard.tCmpShop DROP COLUMN ShipRstrctYn;

CREATE TABLE IF NOT EXISTS shopwizard.tShpShipPlaceRule (
  RuleNo INT(11) NOT NULL AUTO_INCREMENT,
  ShopCode CHAR(12) NOT NULL,
  RuleName VARCHAR(50) NOT NULL DEFAULT '',
  ZipSect VARCHAR(50) NOT NULL DEFAULT '',
  AddrPatterns VARCHAR(4000) NOT NULL DEFAULT '',
  ExceptAddrPatterns VARCHAR(2000) NOT NULL DEFAULT '',
  ProdName VARCHAR(50) NOT NULL DEFAULT '',
  ExceptChnlCodes VARCHAR(500) NOT NULL DEFAULT '',
  ShipPlace VARCHAR(50) NOT NULL,
  NamePrefix VARCHAR(10) NOT NULL DEFAULT '',
  State DECIMAL(1,0) NOT NULL DEFAULT 1,
  Remark VARCHAR(50) DEFAULT NULL,
  RegistId VARCHAR(50) DEFAULT NULL,
  RegistName VARCHAR(50) DEFAULT NULL,
  RegistDate DATETIME DEFAULT NULL,
  ChangeId VARCHAR(50) DEFAULT NULL,
  ChangeName VARCHAR(50) DEFAULT NULL,
  ChangeDate DATETIME DEFAULT NULL,
  PRIMARY KEY (RuleNo),
  KEY IX_tShpShipPlaceRule_Shop (ShopCode, State)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE shopwizard.tCmpShop ADD COLUMN ShipRstrctYn INT(1) NOT NULL DEFAULT 0;

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '기본 출고지(보물유통)', '', '', '', '', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '보물유통', '', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '택배비인상 ★', 'DeliFeeUpStar', '%서울%강남구%
%서울%노원구%
%서울%도봉구%방학동%
%서울%송파구%삼전동%
%서울%종로구%숭인2동%
%서울%종로구%혜화동%
%서울%중구%무학동%
%경기%광주%중대동%
%경기%부천%내동%
%경기%성남%수정구%
%경기%성남%중원구%
%경기%수원%권선%곡반정동%
%경기%안양%만안구%안양_동%
%경기%평택%오성면%
%경기도%하남시%하안동%
%대구%북구%산격동%
%포항%남구%오천읍%
%전라북도%전주시%완산구%서신동%', '%경기%부천%송내동%', '', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '택배비인상', '★', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '택배비인상 ■', 'DeliFeeUpSquare', '', '', '', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '택배비인상', '■', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '택배비인상 ★★', 'DeliFeeUpDoubleStar', '%경기%광주%장지동%
%서울%송파구%가락동%
%경기%안양시%호계동%
%경%북%포항시%북구%흥해읍%', '', '', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '택배비인상', '★★', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '택배비인상 ■■', 'DeliFeeUpDoubleSquare', '', '', '', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '택배비인상', '■■', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '배송불가 말통', 'JerrycanLimit', '%서울%노원구%공릉동%
%서울%노원구%월계동%
%서울%노원구%월계로%
%서울%송파구%가락동%
%서울%송파구%삼전동%
%서울%송파구%삼전로%
%서울%송파구%백제고분로%
%서울%송파구%잠실동%
%서울%중랑구%용마산로%
%경기%고양시%원흥동%
%경기%광주시%곤지암읍%
%경기%광주시%장지동%
%경기%부천%내동%
%경기%수원%권선구%곡반정동%
%경기%수원%권선구%권선동%
%경기%시흥%신천동%
%경기%양주시%덕정동%
%경기%의정부%충의로37번길%
%경기%의정부%의정부동%
%경기%의정부%호국로%
%경기%광주시%회덕동%
%부산%사하구%낙동대로%
%대구%수성구%상동%
%강원%춘천시%퇴계동%
%강원%홍천%내촌면%
%경상남%진주시%봉래동%
%경상북%안동시%예안면%
%경상북%영천시%청솔길%
%경상북%영천시%망정동%
%경상북%포항시%오천읍%
%경상북%포항시%흥해읍%
%경상북%울릉군%
%전라남%고흥군%동강면%
%제주%제주시%
%제주%서귀포시%
%충청남%공주시%유구읍%
%충청남%당진시%계성3길%
%충청남%당진시%읍내동%
%충청북%청주시%상당구%영동%', '%경기%부천%송내동%', '말통', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '배송불가', '', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

INSERT INTO shopwizard.tShpShipPlaceRule (ShopCode, RuleName, ZipSect, AddrPatterns, ExceptAddrPatterns, ProdName, ExceptChnlCodes, ShipPlace, NamePrefix, State, Remark, RegistId, RegistName, RegistDate)
VALUES ('3018508386A1', '배송불가 18리터', '18JerrycanLimit', '%경상북%안동시%예안면%
%전라북%전주시%덕진구%금암동%', '', '18리터', '1208755227Z1,1208755227Z3,1208755227Z5,1208800767Z1,2118841856Z1,2118841856Z3', '배송불가', '', 1, '레거시 rCreateShipPigeon3 이식', 'SYSTEM', '시스템', NOW());

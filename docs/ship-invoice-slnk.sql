-- 송장입력 → 샵링커 송장 전송: 택배사명 매핑
-- tCmpDeliComp.SlnkDeliName: 샵링커에 보낼 택배사 이름(비우면 DeliCompName 을 그대로 보낸다).
--   레거시는 택배사 코드 없이 "이름" 만 보내고 우리 택배사명과 샵링커 이름이 같다고 가정했다. 이름이 다른 택배사만 여기에 적는다.
-- 되돌리기: ALTER TABLE shopwizard.tCmpDeliComp DROP COLUMN SlnkDeliName;

ALTER TABLE shopwizard.tCmpDeliComp ADD COLUMN SlnkDeliName VARCHAR(50) NULL AFTER InvNoVar;

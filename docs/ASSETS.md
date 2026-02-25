# Assets Catalog

`core/assets/` 하위 전체 에셋 목록. 약 500+ 파일.

## gfx/ — 그래픽

### models/machines/ — 아군 머신
- `bodies.atlas` + `bodies.png` — 머신 본체 스프라이트
- `feet.atlas` + `feet.png` — 발/다리 애니메이션
- `weapons.atlas` + `weapons.png` — 무기 스프라이트
- `P_Wpn_Sni_101~104.png` — 스나이퍼 무기 개별 스프라이트

### models/effects/ — 이펙트
**아틀라스:**
- `effects.atlas` + `effects.png`, `effects2.png` — 폭발/연기 등 통합 아틀라스

**개별 이미지:**
- `bullet-trail.png` — 투사체 궤적 (64×1 흰색 선)
- `flame.png` — 화염
- `smoke-trail.png` — 연기 트레일 (작은 연기 뭉치)
- `smoke-effect.png` — 연기 이펙트 (작은 구름)
- `smoke-effect2.png` — 연기 이펙트 (큰 흰색 구름)
- `trail-01~04.png` — 다양한 트레일 선
- `nuclear.png` — 핵폭발
- `shotgun.png` — 산탄 이펙트
- `item.png`, `item2.png` — 아이템 이펙트
- `ion-01~07.png` — 이온 무기 이펙트 (7프레임)
- `level-up.jpg` — 레벨업

**애니메이션 (ZIP 스프라이트시트):**
- `damage-split.zip` — 데미지 숫자
- `damage-hmg-split.zip` — HMG 데미지
- `explode-01~04-split.zip` — 폭발 4종
- `explode-boss-split.zip` — 보스 폭발
- `explode-ground-split.zip` — 지면 폭발
- `explode-ion-split.zip` — 이온 폭발
- `explode-npc-split.zip` — NPC 폭발
- `explode-plasma-split.zip` — 플라즈마 폭발
- `fire-split.zip` — 화염
- `lightning-split.zip` — 번개
- `plasma-split.zip` — 플라즈마
- `rail-split.zip`, `railgun-split.zip`, `railgun-ex-split.zip` — 레일건
- `smoke-split.zip` — 연기 (64×64, 16프레임)

### models/bullets/ — 투사체
- `ally.atlas` + `ally.png` — 아군 투사체
- `enemy-bullet.atlas` + `enemy-bullet.png` — 적 투사체
- `shell.atlas` + `shell.png` — 셸/탄피

### models/planes/ — 비행기
- `plane.png` — 아군 비행기
- `enemy-planes.atlas` + `enemy-planes.png` — 적 비행기

### models/tanks/ — 적 탱크
- `enemy-bodies.atlas` + `enemy-bodies.png` — 적 탱크 차체
- `enemy-weapons.atlas` + `enemy-weapons.png` — 적 탱크 무기

### models/turrets/ — 터렛
- `ally-bodies.atlas` + `ally-bodies.png` — 아군 터렛 본체
- `ally-weapons.atlas` + `ally-weapons.png` — 아군 터렛 무기
- `enemy-bodies.atlas` + `enemy-bodies.png` — 적 터렛 본체
- `enemy-weapons.atlas` + `enemy-weapons.png` — 적 터렛 무기
- `transporter.png` — 수송 유닛

### models/boss/ — 보스
- `bosses-bodies.atlas` + `bosses-bodies.png` — 보스 차체
- `bosses-weapons.atlas` + `bosses-weapons.png` — 보스 무기

### models/base/ — 기지
- `ally-base.atlas` + `ally-base.png` — 아군 기지
- `enemy-base.atlas` + `enemy-base.png` — 적 기지

### models/other/
- `ships.atlas` + `ships.png` — 선박

### terrains/ — 전장 배경 (27종)

`terrains/01/` ~ `terrains/27/`, 각각:
- `01.jpg`, `02.jpg` — 주/부 배경
- `01-alt.jpg`, `02-alt.jpg` — 대체 배경
- `01-small.png`, `02-small.png` — 썸네일

총 162파일. 80개 레벨에 27종이 순환 배치 (map-list.json).

### ui/ — UI 그래픽

**buttons/**
- `common.atlas` — 범용 버튼
- `stage.atlas` — 스테이지 버튼
- `stage-select.atlas` — 레벨 선택 버튼
- `base-upgrades.atlas` — 기지 업그레이드 버튼
- `upgrade-equip.atlas` — 장비 업그레이드 버튼
- `sys-menu.atlas` — 시스템 메뉴 버튼

**game/**
- `controls.png` — 조작 오버레이
- `hp-gauge.png`, `player-hp-gauge.png`, `enemy-hp-gauge.png` — HP 바
- `mach-slot.png`, `build-slot.png` — 슬롯 UI
- `upgrade-cell.png`, `upgrades-left.png`, `upgrades-right.png` — 업그레이드 패널
- `menu-buttons.atlas` — 인게임 메뉴 버튼
- `title.atlas` — 타이틀 로고
- `build-queue/background.png`, `gauge.png` — 빌드 큐
- `build-slot/background.png`, `cover.png`, `foreground.png`, `upgrade.png` 등 — 빌드 슬롯 상세
- `build-slot/icons-special.atlas` — 특수 스킬 아이콘
- `radar/background.png`, `border.png`, `marks.atlas` — 레이더

**기타 UI:**
- `loading/top.png`, `bottom.png`, `left.png`, `right.png`, `center.png` — 로딩 화면
- `level/background.jpg`, `foreground.png` — 레벨 선택
- `slots/background.jpg`, `slots.atlas` — 세이브 슬롯
- `splash/tap_to_start.png`, `title.jpg` — 타이틀
- `dialog/sys-menu/background.png`, `slider-*.png`, `volume-options.png` — 다이얼로그
- `flags.atlas` — 국기
- `items.atlas` — 아이템 아이콘
- `nIcon57.png`, `nIcon72.png`, `nIcon114.png` — 앱 아이콘

---

## sfx/ — 사운드 (79파일)

### machine/ — 무기 발사음
- `rifle/01~10.wav` (10종)
- `missile/01~10.wav` (10종)
- `hmg/01~10.wav` (10종)
- `plasma/01~05.wav` (5종)
- `ion/01~03.wav` (3종)
- `shotgun/01~03.wav` (3종)

### weapon/ — 특수무기
- `rifle.wav`, `missile.wav`, `cannon.wav`, `gun.wav`, `default.wav`

### effects/ — 피격/폭발
- `body-hit-01~04.wav` (4종) — 적 피격
- `ground-hit.wav` — 지면 충돌
- `ground-explosion.wav` — 지면 폭발
- `mach-explosion.wav` — 머신 폭발
- `airstrike.wav` — 공습
- `on-message.wav` — 알림
- `quick-slot.wav` — 퀵슬롯

### game/ — 게임 이벤트
- `build-started.wav`, `build-complete.wav` — 빌드
- `upgrade-mach.wav`, `upgrade-special.wav`, `upgrade-base.wav` — 업그레이드
- `buy-special.wav` — 스킬 구매
- `install.wav`, `uninstall.wav` — 장비 장착/해제
- `get-item.wav` — 아이템 획득
- `low-hp.wav` — 저HP 경고
- `plasma-damage.wav` — 플라즈마 피격

### ui/ — UI
- `button-ok.wav`, `button-cancel.wav` — 확인/취소
- `button-special.wav`, `button-formation.wav`, `button-build-mach.wav` — 기능 버튼
- `click.wav` — 일반 클릭
- `background-music.mp3` — 게임플레이 BGM

### misc/ — 기타
- `intro.mp3` — 인트로 음악
- `gameplay.mp3` — 게임플레이 음악
- `title-in.wav`, `title-bang.wav`, `title-tap-to-start.wav` — 타이틀 화면
- `loading-open.wav`, `loading-close.wav` — 로딩 화면

---

## fonts/ — 비트맵 폰트 (14종)

**Trebuchet MS Bold:**
- `trebuchet_ms_bd_11_white` (11pt 흰색)
- `trebuchet_ms_bd_12_white` (12pt 흰색)
- `trebuchet_ms_bd_16_white` (16pt 흰색)
- `trebuchet_ms_bd_18` (18pt)
- `trebuchet_ms_bd_28_blue` (28pt 파랑)
- `trebuchet_ms_bd_28_black` (28pt 검정)
- `trebuchet_ms_20_blue` (20pt 파랑)
- `trebuchet_ms_26_blue` (26pt 파랑)

**특수 폰트:**
- `cone_14` — 14pt
- `level` — 레벨 표시
- `mineral` — 자원 카운터
- `stage` — 스테이지 이름
- `set_01b` — 세트 표시
- `credits` — 크레딧

각 폰트는 `.fnt` (정의) + `.png`/`.tga` (텍스처) 쌍.

---

## data/ — 설정 데이터

### 핵심 JSON
- `build-info.json` (12.9KB) — 머신/터렛 빌드 비용, 스탯, 레벨별 데이터
- `special-info.json` — 특수 스킬 데미지/레벨 데이터
- `upgrades.json` — 업그레이드 진행 데이터
- `map-list.json` — 80레벨 → 27배경 매핑

### models/machines/ — 머신 좌표 (80파일)
각 머신 종류별 10레벨 × 8종:
- `rifle/rifle-01~10.json`
- `missile/missile-01~10.json`
- `heavy-missile/heavy-missile-01~10.json`
- `hmg/hmg-01~10.json`
- `plasma/plasma-01~10.json`
- `ion/ion-01~10.json`
- `shotgun/shotgun-01~10.json`
- `tanker/tanker-01~10.json`

각 JSON: body 크기, weapon 좌/우 (크기+오프셋), feet (리전+오프셋)

### models/bases/ — 기지 설정 (21파일)
`base-01/` ~ `base-07/`, 각각:
- `base.json` — 기지 본체
- `radar.json` — 레이더
- `weapon.json` — 기지 무기

### waves/ — 웨이브 데이터
- `level-01~03.json` — 초기 레벨 웨이브 (이후 레벨은 WaveData.kt에서 동적 생성)

---

## ai/ — AI 행동 트리

- `machine.btree` — 아군 머신 행동 트리 정의

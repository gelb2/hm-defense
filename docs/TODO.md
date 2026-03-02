# TODO

전체 프로젝트 점검 결과 정리. 우선순위별로 분류.

---

## 1. 크래시 위험 (TODO 스텁)

런타임에 `NotImplementedError` 던지는 코드. 세이브에 터렛 슬롯이 있으면 즉시 크래시.

- [x] ~~**TurretBuildSlot.updateBuildingNumber()** — `TODO("Not yet implemented")`~~ (building=0으로 대체)
- [x] ~~**BuildMachineIfPossible 터렛 분기** — `is TurretBuildSlot -> TODO()`~~ (else→return으로 대체, 셀 차감 방지)
- [x] ~~**StageScreen 빌드 슬롯 else 분기** — `else -> TODO()`~~ (continue로 대체)
- [x] ~~**StageScreen 스페셜 슬롯 else 분기** — `else -> TODO()`~~ (continue로 대체)

## 2. 비기능 UI (버튼 있으나 동작 없음)

화면에 보이고 누를 수 있지만 아무 일도 안 하는 요소들.

- [x] ~~**SupportButton** — 리스너 주석 처리~~ ("Coming Soon" 팝업 연결)
- [x] ~~**SystemMenu Help 버튼** — 리스너 없음~~ ("Coming Soon" 팝업 연결)
- [x] ~~**SystemMenu Leader Board 버튼** — 리스너 없음~~ ("Coming Soon" 팝업 연결)
- [x] ~~**UpgradeEquipment 버튼** — addListener 없이 배치~~ ("Coming Soon" 팝업 연결)

## 3. 미구현 게임 메카닉

데이터/코드가 존재하지만 실제 게임플레이에 반영되지 않는 것들.

### 터렛 시스템
- [ ] 터렛 6종(RIFLE, MISSILE, VULCAN, PLASMA, ION, LASER) 정의만 존재
- [ ] `GameSave.turretUpgrades` 저장하지만 읽는 곳 없음
- [ ] `BuildTurretItem` 클래스 존재하나 인스턴스 생성 없음
- [ ] 터렛 에셋(atlas) 매 스테이지 로딩되지만 렌더링 안 됨

### 업그레이드 효과 미반영
- [x] ~~**CELL_RESEARCH** — multiplier 읽지만 CellCounter 채굴 속도에 미적용~~ (BASE_MINING_SPEED/multiplier 적용)
- [x] ~~**CR_RESEARCH** — multiplier 읽지만 크레딧 보상에 미적용~~ (적 처치 보상에 적용)
- [ ] **BASE_CANNON** — 업그레이드 시 스킨만 변경, 실제 사격 안 함 (`BaseEntity`: `range=null`, `attackSpeed=0f`)
- [x] ~~**BASE_DEFENSE** — 업그레이드 시 스킨만 변경, 기지 HP 항상 500 하드코딩~~ (upgrades.json defense 값 적용, 적 기지 10000 HP)

### 경제 시스템
- [x] ~~**크레딧 획득** — 적 처치 시 크레딧 보상 없음~~ (maxHp/10 * CR_RESEARCH 보상 구현)
- [ ] **스페셜 보충** — DEV 모드 99개 외에 보충 수단 없음

### 미구현 스킬
- [ ] **Laser** — `SpecialKind.kt` TODO 주석 (`0204 -> laser`)
- [ ] **Flamethrower** — `SpecialKind.kt` TODO 주석 (`0211 -> flamethrower`)

### 기타
- [ ] **UpgradeSelectedItem 비활성화 루프** — 크레딧 부족 시 버튼 비활성화 주석 처리 (`UpgradeSelectedItem.kt:98`)

## 4. 예외 처리 부재

### 에셋 로딩
- [ ] `unsafeRegion()` — `findRegion()` null 반환 시 NPE (`StageAssetsManager.kt:248`)
- [ ] `PreparationAssetsManager.bodyRegion()` / `feetRegion()` — 동일 문제

### 세이브/데이터 역직렬화
- [x] ~~`SavesSelectionScreen` — `Json.decodeFromString` try-catch 없음~~ (try-catch + checkValid() 추가)
- [ ] `StageScreen` — upgrades/builds/specials JSON 로딩에 try-catch 없음 (`:106-110`) — 번들 에셋이라 실질 위험 낮음
- [x] ~~`GameSave.checkValid()` — 정의만 있고 호출하는 곳 없음~~ (SavesSelectionScreen에서 호출)

### 안전하지 않은 접근
- [ ] `SlotKinds.kt:91` — 이중 `!!` 강제 언래핑, 세이브에 키 없으면 NPE
- [x] ~~`MapList.kt:14` — `assert`만으로 레벨 범위 검증~~ (require로 대체)
- [x] ~~`BuildMachineIfPossible.kt:25` — 빠른 더블탭 시 cells 음수 가능~~ (balance guard 추가)

## 5. 미사용 / 죽은 코드

정리 대상. 기능에 영향 없이 삭제 가능.

- [x] ~~`SoundEffectManager` — 미사용 클래스 + `TODO()` dispose~~ (삭제됨)
- [x] ~~`DialogKind` enum — 어디서도 참조 안 됨~~ (파일 삭제됨)
- [x] ~~`CancelButton` — 정의만 있고 인스턴스 없음~~ (삭제됨)
- [x] ~~`MachineModel.toPositionedBody()` — 호출부 주석 처리됨~~ (메서드 + 미사용 import 삭제)
- [x] ~~`entities/Machine.kt` 주석 블록 — 60줄 옛 Ashley ECS 코드~~ (삭제됨)
- [x] ~~`Slots.kt` — 빈 파일, 패키지 선언만~~ (파일 삭제됨)
- [x] ~~`Batcher` + `Drawable` — Scene2D로 대체된 커스텀 렌더링 레이어~~ (UIWorld에서 제거, 파일 삭제, internal/ 디렉토리 삭제)
- [x] ~~`MachinePart.LEFT_FOOT/RIGHT_FOOT` — 미사용 enum 값~~ (삭제됨)
- [ ] `GameSave.turretUpgrades` — 읽거나 쓰는 곳 없음 (터렛 시스템 구현 시까지 유지)
- [x] ~~`ENEMY_PLANE` / `SHIPS` 에셋 — 매 스테이지 로딩하지만 사용처 없음~~ (allAtlases()에서 제거, 메모리 절약)
- [x] ~~`Radar Border.init` — 자식 추가 주석 처리~~ (삭제됨)

## 6. 기존 진행 중 항목

- [ ] 미사일 투사체 트레일 이펙트 (하얀 굵은 실/연기) — 시도 2회 롤백, 방법 미정
- [ ] 창 리사이즈 시 UI 깨짐 — FitViewport 리사이즈 처리 문제
- [ ] 유닛 충돌 회피 잔여 떨림 — 대량(20+) 밀집 시. 파라미터 튜닝 또는 적 유닛 vel.x 유지 검토
- [ ] NavGrid 패스파인딩 — 32×32 타일 그리드 기반 A* 구현
- [ ] **아군 머신 사선 정렬** — 장애물 근처에서 발생 가능. buildFlowField()를 cardinal central difference 연속 그래디언트로 교체 완료. 열린 지형에서는 해결, 장애물 근처 효과는 실행 확인 필요. adjustUnitVelocities의 separation X-force가 2차 원인으로 남아있음

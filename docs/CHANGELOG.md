# Changelog

## 2026-03-02 — 유닛테스트 엣지케이스 보강

- **GameSave 엣지케이스** (+8개 → 총 24개)
  - TurretSlot 검증 경로 (잠긴 터렛, 혼합 슬롯, 누락 업그레이드 키)
  - Long.MAX_VALUE 크레딧 직렬화 round-trip
  - 부분 JSON 역직렬화 (누락 필드 → 기본값 적용 확인)
- **WaveData 엣지케이스** (+6개 → 총 14개)
  - 범위 밖 레벨 (0, -10, 999) 크래시 안전성
  - 웨이브 수 단조증가 보장 (레벨 1→80)
  - 적 속도 > 0, 스폰 간격 ≥ 0.8, 후속 웨이브 딜레이 증가
- **WaveManager 엣지케이스** (+6개 → 총 14개)
  - 음수 delta, 대형 delta (버스트 방지), zero interval
  - 다중 그룹 웨이브 동시 스폰, 빈 그룹 즉시 완료, count=0 처리
- **NavGrid 엣지케이스** (+7개 → 총 21개)
  - OOB 월드좌표 안전성 (음수, 초대형), isBlockedAt 정확성
  - seed row 플로우 존재 보장, 도달 불가 타일 null 확인
  - 대각선 코너커팅 방지 검증

## 2026-03-02 — 유닛테스트 도입 + 크레딧 보상

- **유닛테스트 인프라 구축** (build.gradle, core/src/test/)
  - JUnit 5 + kotlin-test-junit5 + gdx-backend-headless 의존성 추가
  - `./gradlew :core:test` 로 실행 가능
- **GameSave 테스트** (16개) — checkValid() 경계값, 직렬화 round-trip, 기본값
- **WaveData 테스트** (8개) — 전 레벨(1-80) 유효성, 결정론성, 스탯 스케일링
- **WaveManager 테스트** (8개) — 스폰 타이밍, 멀티웨이브, 루프, 엣지케이스
- **NavGrid 테스트** (14개) — 플로우필드 방향, 벽+갭 라우팅, 유닛 이동 시뮬레이션(오실레이션/스턱 검출)
- **스테이지 클리어 크레딧 보상** (StageScreen.kt)
  - 승리 시 `(500 + level × 100) × CR_RESEARCH` 크레딧 지급
  - 세이브 항상 persist (기존: 신규 스테이지일 때만)

## 2026-03-02 — 죽은 코드 정리 (추가)

- **Batcher + Drawable 제거** (UIWorld.kt, internal/ 디렉토리)
  - UIWorld에서 매 프레임 빈 SpriteBatch begin/end 호출하던 Batcher 삭제
  - `internal/Batcher.kt`, `internal/Drawable.kt` 파일 삭제, 디렉토리 정리
- **MachineModel.toPositionedBody() 삭제** (MachineModel.kt, -68줄)
  - Scene2D로 대체된 옛 Box2D 조인트 기반 머신 생성 메서드 제거
  - 미사용 import 10개 정리 (Vector2, Body, BodyDef, Joint, World, MachinePart, PPM 등)
- **ENEMY_PLANE / SHIPS 에셋 로딩 제거** (StageAssetsManager.kt)
  - allAtlases()에서 제외 → 매 스테이지 불필요한 TextureAtlas 2개 로딩 방지

## 2026-03-02 — 게임 메카닉 구현

- **적 처치 크레딧 보상** (StageScreen.kt)
  - 적 사망 시 `maxHp / 10 * crResearchMultiplier` 크레딧 지급
  - CR_RESEARCH 업그레이드 (1.0~3.0배) 자동 반영
- **CELL_RESEARCH 채굴 속도** (CellCounter.kt)
  - `BASE_MINING_SPEED / multiplier` 간격으로 셀 채굴 (1.0~2.2배 가속)
- **BASE_DEFENSE 기지 HP** (StageScreen.kt)
  - 아군 기지: upgrades.json의 defense 값 적용 (레벨 1 = 10,000 HP ~ 레벨 7 = 100,000 HP)
  - 적 기지: 10,000 HP 고정 (기존 500 → 밸런스 조정)

## 2026-03-02 — 안정성 개선 + Coming Soon 팝업

- **크래시 위험 TODO 스텁 제거** (4건)
  - `TurretBuildSlot.updateBuildingNumber()`: `TODO()` → `building=0` (터렛 미구현)
  - `BuildMachineIfPossible` 터렛 분기: `TODO()` → early return (셀 차감 방지)
  - `StageScreen` 빌드/스페셜 슬롯 else 분기: `TODO()` → `continue`
- **비기능 버튼에 "Coming Soon" 팝업 연결** (4건)
  - `InGameDialog.showComingSoon()` 메서드 추가
  - `ShowComingSoon` 재사용 ClickListener 클래스 추가
  - SupportButton, Help, Leader Board, UpgradeEquipment 버튼에 적용

## 2026-03-02 — 게임 Pause/Resume

- **SystemMenu 열기 시 전체 게임플레이 정지** (StageScreen.kt, Terrain.kt, SystemMenu.kt)
  - `Terrain.act()` 오버라이드: pause 시 모든 자식 액터에 `delta=0` 전달
  - Box2D 물리, AI 행동트리, 적 스폰, 투사체, 이펙트, 보행 애니메이션 모두 동결
  - UI (메뉴, 볼륨 슬라이더, 버튼)는 정상 작동
  - Resume Game 클릭 시 즉시 재개

## 2026-03-02 — NavGrid 플로우 필드 연속 그래디언트

- **buildFlowField() 그래디언트 개선** (NavGrid.kt)
  - 기존: 단일 최적 이웃 선택 → 이산 8방향 벡터 (0°, 45°, 90°, ... 45° 양자화)
  - 변경: Cardinal central difference → 연속 그래디언트
  - `gradCol = dist[right] - dist[left]`, `gradRow = dist[down] - dist[up]`
  - Blocked/OOB 이웃은 myDist로 폴백 → 해당 축 기여 0
  - 결과: 장애물 근처에서 45° 급변 대신 부드러운 곡선 방향
  - 쌍선형 보간과 결합하여 타일 경계 전환도 매끄러움

## 2026-03-02 — 몸체 회전 필터 + NavGrid 지형 수정

- **적 탱크 몸체 회전 — 4중 필터** (EnemyTankEntity.kt)
  - Layer 1: Flow field 방향 — 쌍선형 보간된 NavGrid flow를 시각 회전에 사용 (velocity noise 원천 차단)
  - Layer 2: EMA 방향 스무딩 — 지수이동평균 (λ=8, α≈0.125, half-life≈0.087s)
  - Layer 3: Angular dead zone with hysteresis — enter 8° / exit 3° (미세 진동 차단)
  - Layer 4: Rate-limited rotation — 180°/s
  - 포탑(weaponImage)은 독립적으로 타겟을 조준 (body 기준 상대 회전)
- **아군 머신 몸체 회전 — 2중 필터** (MachineEntity.kt)
  - Layer 1: Flow field 방향 (적과 동일)
  - Layer 2: Rate-limited rotation — 240°/s (적보다 빠름)
  - EMA/Dead Zone 미적용 — 전체 Group이 회전(body+feet+weapons)하여 정렬 불일치가 즉시 가시적
  - aimAt() 즉시 스냅 → 타겟 소실 후 240°/s로 빠르게 flow 방향 복귀
  - 참고: Context Steering, ORCA, Rory Driscoll EMA, iforce2d hysteresis, Dota 2 turn rate
- **NavGrid 지형 4건 수정** (generate_navgrid.py)
  - Terrain 6: FORCE_ALL_WALKABLE 추가 (순수 초원인데 자동 분류 오류)
  - Terrain 19: 물 웅덩이 + 구조물 복합 영역 수동 블록 지정
  - Terrain 21(Stage 14): 협곡 양 벽의 물 경계를 정밀 all_walkable_then_block으로 재작성
  - Terrain 27: 좌측 호수 영역 수동 블록 지정
  - 전 27개 navgrid JSON 재생성 + BFS 연결성 검증 통과

## 2026-03-02 — 스티어링 시스템 전면 도입

- **NavGrid 8방향 BFS + 쌍선형 보간** (NavGrid.kt)
  - 4방향 → 8방향(대각선 포함) BFS로 업그레이드, float 거리 (√2 대각선 비용)
  - 대각선 코너 커팅 방지 (인접 cardinal 타일 walkable 여부 확인)
  - getFlowToBase/getFlowToTop에 쌍선형 보간 적용: 타일 경계에서 4개 이웃 벡터를 가중 평균
  - 보간 불가 시(blocked 타일 인접) discrete 조회 폴백
  - 재사용 Vector2로 프레임당 GC 부하 제거
- **가감속 시스템** (EnemyTankEntity.kt, Machine.kt, MachineEntity.kt)
  - speedFactor 0.0↔1.0 곡선: accelRate=3.3(~0.3초 가속), decelRate=5.0(~0.2초 감속)
  - 적 유닛: walk()에서 가속, stopInPlace()에서 Arrive 스타일 감속
  - 아군 머신: smooth 이동(Tanker 등) + step 이동(발 애니메이션) 모두 speedFactor 적용
  - MachineEntity.stopInPlace()는 velocity 직접 제로화 대신 Machine Action의 감속에 위임
  - EMP 마비 시 speedFactor=0 리셋 (마비 해제 후 부드러운 재가속)
- **adjustUnitVelocities 리팩토링** (GameWorld.kt)
  - **공간 해싱**: O(n²) → O(n·k) 이웃 탐색 (CELL_SIZE=4.0wu, HashMap 기반)
  - **방향성 Separation**: 속도 벡터에 수직인 방향으로 밀침 (flow field 방향 보존)
  - **전방 감속**: 이동 방향 벡터 기반 (기존 vel.y 부호 → dot product)
  - **XY 스무딩**: X축뿐 아니라 Y축도 프레임 간 스무딩 (FloatArray[2])
  - **메모리 릭 수정**: 파괴된 Body의 lastVelocity 엔트리 정리 (retainAll)
  - 상수 리네이밍: LATERAL_REPULSION → SEPARATION_STRENGTH, LATERAL_SMOOTHING → VELOCITY_SMOOTHING
- **알려진 제한**: Machine sway(actor.x +=)가 물리 위치 동기화로 덮어써지는 기존 이슈 유지

## 2026-03-02 — NavGrid 지형 장애물 회피 + Foreground 오버레이

- **NavGrid 시스템 구현** (NavGrid.kt, generate_navgrid.py)
  - 16×64 타일(32px) BFS flow field 경로 탐색
  - flowToBase(적용) / flowToTop(아군용) 이중 flow field
  - 27개 지형 전부 navgrid JSON 생성, BFS 유효성 검증 통과
  - 지형별 수동 오버라이드 (다리, 건물, 물 등) + 자동 픽셀 분석
- **적/아군 NavGrid 연동** (EnemyTankEntity.kt, Machine.kt, StageScreen.kt)
  - 적 유닛: walk()에서 navGrid.getFlowToBase()로 이동 방향 결정
  - 아군 머신: smooth/burst 이동 모두 navGrid.getFlowToTop() 적용
  - NavGrid 없으면 기존 직선 이동으로 fallback
- **adjustUnitVelocities flow field 보존 버그 수정** (GameWorld.kt)
  - 기존: `speed = abs(vel.y)` → flow field X 성분 완전 소실
  - 수정: `totalSpeed = sqrt(vel.x² + vel.y²)`, `latVel = vel.x + proximityLat + overlapVel`
  - isMoving 판정도 전체 속도 벡터 기반으로 변경
- **Foreground 오버레이 시스템** (StageAssetsManager.kt, Terrain.kt, GameWorld.kt)
  - 01-fg.png / 02-fg.png 존재 시 자동 로드
  - 유닛 위, 기지 아래로 z-order 렌더링 (건물 뒤로 지나가는 효과)
  - terrain 25(공장): 전면 walkable + foreground 오버레이 적용
- **도구** (tools/)
  - generate_navgrid.py: navgrid JSON + 디버그 이미지 생성
  - extract_black_regions.py: 사용자 어노테이션 이미지에서 blocked 영역 추출
  - generate_foreground.py: blocked 영역에서 foreground PNG 오버레이 생성
- **알려진 이슈**: terrain 25 foreground 마스크가 32px 타일 단위로 과도 — 정밀도 개선 필요

## 2026-02-26 — 유닛 충돌 회피 시스템 + 이동속도 차등

- **충돌 회피 시스템 구현** (GameWorld.kt)
  - 반발 벡터(repulsion vector) 모델: 인접 유닛으로부터 연속적 lateral repulsion 누적
  - velocity-only 통합 아키텍처: proximity repulsion + overlap separation 모두 속도 기반
  - setTransform(위치 텔레포트) 완전 제거 → 속도 시스템과의 충돌(떨림 원인) 해소
  - 화면 내: 스무딩 적용 (LATERAL_SMOOTHING=0.3), 화면 밖: 즉시 보정 (×30 속도)
  - 전방 감속: X축 겹침 + Y축 전방에 있는 유닛에 의한 속도 감쇠
  - terrain X 경계 하드 클램프 (벽 밖 돌출 방지)
- **머신 종류별 이동속도 차등** (MachineKind.kt)
  - ION(17.5) > SHOTGUN(16) > RIFLE=HMG(14.4) > PLASMA(12.5) > MISSILE=HEAVY_MISSILE(11) > TANKER(9.5) px/s
  - 속도 차이로 자연스러운 Y축 유닛 분산 → 밀집도 감소
- **오프스크린 스폰** (StageScreen.kt)
  - 아군 Y=-200, 적 Y=2250 (화면 밖)에서 스폰 후 걸어서 진입
  - 화면 밖에서 회피 스킵 + 강한 분리로 진입 전 간격 확보
- **기지 레이어 상위 배치**: 유닛이 기지 건물에서 나오는 연출 (toFront)
- **Machine.kt**: 걷는 동안 vel.x 유지 (vel.x=0 리셋 제거)

## 2026-02-26 — 적 웨이브 스폰 다양성 개선

- 적 웨이브 생성 알고리즘 리라이트 (generateDefaultWaves)
- 4티어 시스템 도입: SCOUT(빠른 잡졸) / MEDIUM(중간) / HEAVY(강적) / ELITE(보스급)
- 32종 탱크 모델 전 범위 활용 (기존: 5종 슬라이딩 윈도우)
- 매 웨이브 2~3개 그룹 동시 스폰으로 적 다양성 확보
- 티어별 스탯 차등: SCOUT(HP×0.3, 속도×1.3) ~ ELITE(HP×1.5, 속도×0.7)
- 시드 기반 랜덤으로 레벨 간 조합 다양화 (같은 레벨은 결정론적)

## 2026-02-26 — 머신 업그레이드 UI 리뉴얼

- 3×3 그리드 + 확인 오버레이 레이아웃으로 전면 개편
- 빌드슬롯 배경 에셋 적용 (gfx/ui/game/build-slot/background.png)
- 그리드 셀에 완전한 머신 미리보기 표시 (body + weapons + feet)
- 머신 90° 회전 (위를 향하도록) + 셀 중앙 배치
- 업그레이드 확인 오버레이에도 완전한 머신 미리보기 적용 (body + weapons + feet, 90° 회전)
- 업그레이드 확인 오버레이 딤 처리 강화 (0.6 → 0.85)
- 머신 이동 속도 15% 증가 (12.5 → 14.4 px/s)
- PreparationAssetsManager에 weapons.atlas, feet.atlas, build-slot 에셋 추가
- 스테이지 선택 화면 배경/전경 에셋 재활용 (ref counting 활용)

## 2026-02-26 — 승리 일러스트 화면 + 다음 스테이지 직행

- 스테이지 승리 시 타이틀 일러스트(title.jpg) 페이드인 표시 후 로딩 도어 닫힘 → 다음 스테이지 전환
- 승리 시 다음 스테이지 전투로 직행 (PreparationScreen 건너뜀)
- KtxGame 같은 타입 스크린 전환 제한 우회 (수동 remove/add/set)
- 마지막 스테이지(80) 클리어 또는 패배 시에는 기존대로 스테이지 선택 화면 복귀

## 2026-02-25 — 머신 업그레이드 준비 화면

- 스테이지 선택과 전투 사이에 PreparationScreen 추가
- 머신 8종 리스트: 현재 레벨 아이콘, 다음 레벨 아이콘, 업그레이드 비용 표시
- 더블클릭/더블탭으로 업그레이드 확인 다이얼로그 호출
- 업그레이드 비용: baseCost × currentLevel (종류별 차등)
- 크레딧 차감 + 세이브 flush + UI 즉시 갱신
- DEV 모드: 크레딧 0일 때 20000 자동 지급
- 스테이지 선택 화면의 OK 버튼 및 더블탭 모두 PreparationScreen 경유
- PreparationAssetsManager 신규 (bodies.atlas + stage-select.atlas)

## 2026-02-25 — 웨이브 번호 UI 표시

- 전투 중 현재 웨이브 번호 표시 (WAVE n / total)
- WaveManager에 currentWave/totalWaves public getter 추가
- WaveCounter UI 컴포넌트 신규 생성

## 2026-02-25 — 문서화

- CLAUDE.md 프로젝트 가이드 생성 (빌드, 구조, 아키텍처, 컨벤션)
- docs/RESTORATION.md 프로젝트 복원 스토리 문서 작성
- README.md에 Restoration (2026) 섹션 추가
- docs/Gameplay.md 게임플레이 메카닉 문서 완성
- docs/CHANGELOG.md 변경 이력 문서 생성
- docs/ASSETS.md 에셋 카탈로그 문서 생성

## 2026-02-24 — 특수 스킬 이펙트 (PR #8)

- 공습 스킬 4종(BOMB/MISSILE/NUKE/EMP)에 비행기 플라이오버 연출 추가
- 비행기 사선 비행, 기수 방향, 원근감(스케일 변화) 구현
- 비행기에서 투사체를 발사하여 목표물에 착탄하는 시스템 구현
- 비행기 비행 속도 조정 (5.0초, pow3In 가속)
- 폭발 이펙트 개선 (additive 블렌딩)
- 미사일 발사 사운드 적용

## 2026-02-24 — DEV 모드 & 사운드 통합 (PR #7)

- DEV 모드 전 스테이지 해금
- 스테이지 진행 저장 기능
- 전 80레벨 자동 웨이브 생성
- 사운드 이펙트 전면 통합

## 2026-02-24 — 사운드 이펙트 (PR #6)

- UI/생산 사운드 10종+ 추가
- 머신 무기별 발사음 (rifle, missile, hmg, plasma, ion, shotgun)
- 적 탱크 발사/피격/파괴 사운드
- 아군 머신 파괴 사운드
- 도어 개폐, 스테이지 BGM
- 랜덤 사운드 풀 시스템

## 2026-02-24 — 머신 데이터 자동화 & 빌드 타입 (PR #5)

- 3단계 빌드 시스템 (dev/inhouse/release)
- 머신 8종 완전 지원
- DEV 모드: 전 머신 해금 (1크레딧)
- 슬롯 스크롤 지원
- 탱커 이동 로직
- 머신 종류별 투사체 분기
- 미사일 스모크 트레일

## 2026-02-24 — 전투 시스템 (PR #4)

- AI 기반 타겟팅 및 사격
- 투사체 물리 시스템
- 적 스폰/웨이브 루프
- 충돌 감지 및 피격 처리
- 건물 HP/파괴 시스템
- HP 바 통합
- 스폰 겹침 방지
- 행동 트리 이동 버그 수정

## 2026-02-24 — 유닛 빌드 큐 & 맵 이동 (PR #3)

- 빌드 큐 시스템 완성
- 맵 이동 메카닉
- 머신 애니메이션 개선 (발 애니메이션)
- 스텝 기반 이동 시스템
- 스폰 방향 수정

## 2026-02-24 — Scala 로직 이식 (PR #2)

- rework-scala 브랜치에서 로직 포팅
- macOS 호환성 버그 수정

## 2026-02-24 — 의존성 수정 (PR #1)

- 프로젝트 의존성 정리 (Claude 협업 시작)
- 의존성 마이그레이션: LibGDX 1.10.0→1.13.1, Kotlin 1.6.0→2.1.0, LWJGL2→LWJGL3

---

## 원본 프로젝트 (Mesabloo, 2021-2025)

### 2025-01 — AI & 머신 시스템 탐색
- AI 행동 트리 구현 탐색
- 스티어링 행동 테스트
- 에셋 추출 및 데이터 테이블 해독

### 2024-01 — Scala 기반 프로젝트 재작성 시도
- `rework-scala` 브랜치에서 Scala + LibGDX 1.13.0 기반 전면 재작성 (146 커밋)
- AI 행동 트리, 전투 시스템, 스티어링 행동 구현
- 메인 브랜치와 미통합 상태로 종료

### 2022-01 ~ 2022-06 — UI 프레임워크
- Scene2D 기반 UI 마이그레이션
- 비트맵 폰트, 뷰포트 설정
- 세이브 시스템 (Preferences)
- 업그레이드 메뉴, 일시정지 메뉴, HP 게이지
- 기지 시스템 (레이더 포함)
- 빌드 슬롯 및 장비 시스템

### 2021-12 — 프로젝트 초기 설정
- LibGDX 1.10.0 + Kotlin 1.6.0 + LWJGL2 프로젝트 생성
- 원작 IPA에서 에셋 추출
- 기본 프로젝트 구조 구축

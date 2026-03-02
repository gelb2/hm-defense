# Gameplay — Heavy MACH: Defense

실시간 터렛 디펜스 게임. 아군 기지를 방어하면서 적 기지를 파괴하는 것이 목표.

## 게임 흐름

```
타이틀 → 세이브 선택 → 스테이지 선택 (1-80) → 머신 업그레이드 → 전투
                                                                          ↑        ↓
                                                                          └─ 승리 ─┘
```

각 스테이지는 직사각형 전장(512×2048px) 위에 아군 기지(하단)와 적 기지(상단)가 배치된다.
적은 웨이브 단위로 스폰되며, 모든 웨이브를 처리하고 적 기지를 파괴하면 승리.
아군 기지 HP가 0이 되면 패배.

승리 시 타이틀 일러스트 표시 후 다음 스테이지 전투로 직행한다.
마지막 스테이지(80) 클리어 또는 패배 시에는 스테이지 선택 화면으로 복귀한다.

## 기지 (Base)

아군/적 기지 모두 HP를 가지며, 0이 되면 파괴된다.

- **초기 HP**: 500
- 아군 기지는 자체 무기를 보유하여 접근하는 적을 공격
- 아군 기지만 전투 중 업그레이드 가능 (적 기지는 스테이지별 고정)

## 머신 (Machines) — 8종

아군 이동 유닛. 전방으로 걸어가며 사거리 내 첫 번째 적을 공격한다.

| 종류 | 설명 |
|------|------|
| **RIFLE** | 표준 보병. 저렴, 다수 배치 가능 |
| **MISSILE** | 중거리 폭발형 |
| **HEAVY_MISSILE** | 중화기 포병 |
| **ION** | 에너지 기반 |
| **HMG** | 고연사 중기관총 |
| **PLASMA** | 플라즈마 기반 |
| **SHOTGUN** | 근거리 산탄 |
| **TANKER** | 탱크형 지원 |

### 머신 업그레이드 (PreparationScreen)

전투 진입 전 준비 화면에서 크레딧으로 머신 레벨을 올릴 수 있다.

- 업그레이드 비용 = baseCost × 현재 레벨
- baseCost: RIFLE 100, MISSILE 150, HMG 200, SHOTGUN 175, PLASMA 250, ION 300, HEAVY_MISSILE 250, TANKER 150
- 최대 레벨: 10 (MAX 표시)
- 더블클릭/더블탭 → 확인 다이얼로그 → Yes → 크레딧 차감 + 레벨 증가
- 크레딧 부족 시 해당 머신 행 비활성화 표시
- DEV 모드: 크레딧이 0 이하일 때 20,000cr 자동 지급

### 머신 이동속도

종류별로 다른 이동속도를 가진다. 빠른 유닛이 먼저 전선에 도달하여 자연스러운 Y축 분산을 만든다.

| 종류 | 속도 (px/s) |
|------|-----------|
| ION | 17.5 |
| SHOTGUN | 16.0 |
| RIFLE | 14.4 |
| HMG | 14.4 |
| PLASMA | 12.5 |
| MISSILE | 11.0 |
| HEAVY_MISSILE | 11.0 |
| TANKER | 9.5 |

걷기 애니메이션은 스텝 기반 버스트 (0.45초 이동 → 0.25초 정지 반복, 1.4초 주기).
버스트 속도로 보상하여 평균 속도 = MachineKind.speed 유지.

### 머신 공통 속성
- HP, 이동속도, 공격력, 사거리, 감지거리, 재장전 속도
- 레벨업(1-10)으로 공격력, HP 등 상승

### 머신 스탯 예시

**RIFLE** (레벨 1 → 10):
| | Lv.1 | Lv.10 |
|--|------|-------|
| 비용 | 60 | 500 |
| 빌드시간 | 3.0s | 5.0s |
| 최대배치 | 5 | 3 |
| HP | 80 | 300 |
| 공격력 | 8 | 35 |
| 속도 | 1.2 | 1.2 |
| 사거리 | 100 | 140 |
| 감지거리 | 150 | 210 |

**TANKER** (레벨 1 → 10):
| | Lv.1 | Lv.10 |
|--|------|-------|
| 비용 | 200 | 640 |
| 빌드시간 | 6.0s | 2.0s |
| 최대배치 | 1 | 1 |
| HP | 400 | 1280 |
| 공격력 | 15 | 60 |
| 속도 | 0.5 | 0.5 |
| 사거리 | 80 | 120 |
| 감지거리 | 130 | 190 |

### 투사체 속성 (머신별)

| 머신 | 투사체 속도 | 피격 이펙트 |
|------|-----------|------------|
| Rifle | 350 px/s | damage |
| HMG | 400 px/s | damage-hmg |
| Missile | 250 px/s | explode-01 |
| Heavy Missile | 200 px/s | explode-02 |
| ION | 450 px/s | explode-ion |
| Plasma | 300 px/s | explode-plasma |
| Shotgun | 380 px/s | — |
| Tanker | 300 px/s | explode-01 |

## 스티어링 + 유닛 충돌 회피 시스템

유닛(머신, 적 탱크)의 이동과 겹침 방지를 velocity 기반 스티어링 시스템으로 관리한다.

### 아키텍처: Velocity-Only + Steering Concepts

```
매 프레임 실행 순서:
1. 행동트리 → walk()/stopInPlace() → speedFactor 가감속 → body.setLinearVelocity()
2. adjustUnitVelocities() → 방향성 separation + overlap separation + forward slowdown
3. world.step(1/60) → 속도 기반 위치 이동
4. Position sync → body.position → actor.position + terrain X 클램프
```

모든 이동이 velocity를 통해 이루어지며, setTransform(위치 텔레포트)를 사용하지 않는다.
gdx-ai SteeringBehavior를 직접 사용하지 않고, 동일한 스티어링 개념을 기존 직접 velocity 제어 패턴에 통합했다.

### 가감속 시스템 (Arrive 행동)

유닛은 즉시 최대 속도가 아닌 가감속 곡선으로 이동한다.

```
speedFactor: 0.0 (정지) ↔ 1.0 (최대 속도)
가속: speedFactor += ACCEL_RATE * dt → ~0.3초에 풀스피드
감속: speedFactor -= DECEL_RATE * dt → ~0.2초에 정지
```

- **적 유닛**: walk()에서 가속, stopInPlace()에서 Arrive 스타일 감속
  - 감속 중에도 flow field 방향 유지 (갑작스런 정지 방지)
  - EMP 마비 시 speedFactor=0 리셋 → 마비 해제 후 부드럽게 재가속
- **아군 머신**: smooth/step 이동 모두 speedFactor 적용
  - step 이동(발 애니메이션)에서 sway도 speedFactor에 비례

### 경로 탐색: NavGrid Flow Field

8방향 BFS(cardinal + diagonal)로 flow field를 생성하고, 쌍선형 보간으로 타일 경계를 부드럽게 전환한다.

```
타일 중심 좌표에서 4개 이웃 타일의 flow 벡터를 가중 평균:
  f(x,y) = Σ weight_i × flow_i / Σ weight_i
  weight = bilinear coefficient (서브타일 위치 기반)
```

- 대각선 이동 비용 √2 (정확한 최단 경로)
- 코너 커팅 방지: 대각선 이동 시 인접 두 cardinal 타일 모두 walkable 필요

### 충돌 회피: 세 가지 힘

**1. 방향성 Separation** (화면 내 이동 중인 유닛만)

이동 방향에 수직인 축으로 인접 유닛을 밀어낸다 (flow field 방향 보존).

```
perpendicular = (-velDir.y, velDir.x)  // 속도 벡터에 수직
perpDot = dot(neighbor_offset, perpendicular)
pushSign = opposite of perpDot sign
separation += perpendicular × pushSign × proximity × SEPARATION_STRENGTH(0.3)
```

**2. Overlap Separation** (모든 유닛: 이동/정지, 화면 내/외)

실제로 겹친 유닛을 속도로 밀어내는 힘. gapX < SEPARATION_BUFFER(0.1wu) && gapY < 0 일 때 작동.

```
overlapVel = overlapPush × OVERLAP_SEPARATION_VEL(3.0)    // 화면 내
overlapVel = overlapPush × OFFSCREEN_SEPARATION_VEL(30.0)  // 화면 밖
```

**3. Forward Slowdown** (화면 내 이동 중인 유닛만)

이동 방향 전방에 유닛이 있으면 감속. 이동 방향 벡터 기반 dot product로 "전방" 판정.

```
toOtherAlongVel = dot(to_neighbor, velDir)
forwardScale = 1 - proximity × FORWARD_SLOWDOWN(0.6)
```

### 공간 해싱 (Spatial Hashing)

O(n²) → O(n·k) 이웃 탐색 최적화 (k = 셀당 평균 유닛 수).

```
CELL_SIZE = 4.0 wu (~64px)
key = (floor(x/CELL_SIZE) << 32) | floor(y/CELL_SIZE)
query: ±1 cell (3×3 = 9 cells 검사)
```

### XY 스무딩

X축과 Y축 모두 프레임 간 스무딩 적용 (VELOCITY_SMOOTHING=0.3).
HashMap<Body, FloatArray[2]>로 이전 프레임 [x, y] 값 보존.
파괴된 Body 엔트리는 매 프레임 정리 (메모리 릭 방지).

### 몸체 회전 (Body Rotation)

유닛과 머신의 스프라이트가 이동 방향을 따라 자연스럽게 회전한다.
적 탱크와 아군 머신은 구조적 차이로 인해 다른 필터 전략을 사용한다.

**적 탱크** (EnemyTankEntity) — **4중 필터**:

```
Layer 1 — Flow Field Direction (노이즈 원천 차단)
  NavGrid의 쌍선형 보간된 flow 방향을 시각 회전 타겟으로 사용.
  collision avoidance로 인한 velocity noise가 시각 회전에 영향을 주지 않음.

Layer 2 — EMA Smoothing (잔여 노이즈 필터)
  방향 벡터에 지수이동평균 적용 (λ=8, α≈0.125/frame, half-life ≈ 0.087s).
  셀 경계에서의 flow 방향 점프도 부드럽게 전환.

Layer 3 — Angular Dead Zone with Hysteresis (미세 진동 차단)
  enter threshold: 8° (이 이상 차이나면 회전 시작)
  exit threshold: 3° (이 이하면 회전 정지)
  히스테리시스 밴드가 경계 근처 떨림 방지.

Layer 4 — Rate-Limited Rotation — 180°/s
```

- 포탑(weaponImage)은 독립적으로 타겟을 조준 (body 기준 상대 회전)
- 정지 시 마지막 방향 유지, 마비(EMP) 시 회전 중단
- EMA+Dead Zone이 효과적인 이유: 포탑이 독립 회전하므로 몸체 회전은 순수 장식적 → 느려도 무방

**아군 머신** (MachineEntity) — **2중 필터**:

```
Layer 1 — Flow Field Direction (적과 동일)
Layer 2 — Rate-Limited Rotation — 240°/s
```

- 전체 Group(body+feet+weapons)이 함께 회전 → 정렬 불일치가 즉시 가시적
- EMA/Dead Zone을 사용하면 aimAt()→walk() 전환 시 ~0.5초 "crab-walking" 발생
- 240°/s의 빠른 턴레이트로 타겟 소실 후 즉시 flow 방향 복귀 (30° 차이 → 0.125초)
- Flow field 자체가 이미 쌍선형 보간으로 부드러워 추가 스무딩 불필요

**참고한 선진 사례:**
- Context Steering (Andrew Firth, Game AI Pro 2) — interest/danger map 패턴
- ORCA (UNC GAMMA Lab) — 진동 없는 상호 회피 수학적 보장
- Frame-Rate Independent Damping (Rory Driscoll) — EMA 감쇠 공식
- iforce2d Box2D Rotating to Angle — angular hysteresis 패턴
- Dota 2 Turn Rate System — turn rate 기반 시각 디커플링

### 오프스크린 스폰

유닛은 화면 밖에서 스폰되어 걸어서 진입한다:
- 아군: Y=-200px (terrain 하단 밖), 적: Y=2250px (terrain 상단 밖)
- 화면 밖(Y<0 또는 Y>2048): directional separation 스킵, overlap separation만 강하게 적용 (×30)
- 기지(StaticBody)를 toFront()로 유닛 위에 렌더링 → 건물에서 나오는 연출

### 경계 처리

- terrain X 경계: velocity 클램프 + position 하드 클램프 (벽 밖 돌출 방지)
- 정지 중(speed≈0) 겹침: 부드러운 분리 속도 부여

### 상수 요약

| 상수 | 값 | 역할 |
|------|---|------|
| INFLUENCE_RADIUS | 2.0 wu | 반발 작동 범위 (~32px) |
| SEPARATION_STRENGTH | 0.3 | 방향성 분리 강도 |
| FORWARD_SLOWDOWN | 0.6 | 전방 감속 강도 |
| MAX_SEPARATION_RATIO | 0.8 | 최대 분리이동 비율 (전진속도 대비) |
| VELOCITY_SMOOTHING | 0.3 | XY 속도 스무딩 계수 |
| OVERLAP_SEPARATION_VEL | 3.0 | 겹침→속도 변환 (화면 내) |
| OFFSCREEN_SEPARATION_VEL | 30.0 | 겹침→속도 변환 (화면 밖) |
| SEPARATION_BUFFER | 0.1 wu | 유닛 간 유지 간격 (~1.6px) |
| CELL_SIZE | 4.0 wu | 공간 해시 셀 크기 (~64px) |
| ACCEL_RATE | 3.3 | 가속률 (~0.3초에 풀스피드) |
| DECEL_RATE | 5.0 | 감속률 (~0.2초에 정지) |

### 알려진 제한

- Machine sway(actor.x +=)가 물리 위치 동기화에 의해 덮어써짐 — 시각적 영향만, 물리에는 무영향
- 적 유닛 velocity는 btree 실행 후 world.step 전에 설정 → 1프레임 지연 (실질적 영향 미미)

## 터렛 (Turrets) — 6종

고정 방어 유닛. 높은 HP, 사거리 내 적 자동 공격.
터렛을 빈틈없이 배치하면 적 진행을 물리적으로 차단할 수 있다.

| 종류 | 레벨 |
|------|------|
| **RIFLE** | 1-5 |
| **MISSILE** | 1-5 |
| **VULCAN** | 1-5 |
| **PLASMA** | 1-5 |
| **ION** | 1-5 |
| **LASER** | 1-5 |

## 업그레이드 — 6종

스테이지 간(세이브 화면) 또는 전투 중 적용 가능한 글로벌 업그레이드.

| 종류 | 효과 | 범위 |
|------|------|------|
| **BASE_CANNON** | 기지 무기 강화 (재장전 2.5→1.9s, 탄수 10→24) | — |
| **BASE_DEFENSE** | 기지 HP 증가 (10,000→100,000) | — |
| **BUILD_TIME** | 빌드 시간 감소 (×1.0→×0.64) | — |
| **CELL_STORAGE** | 셀 저장량 증가 (500→4,700) | — |
| **CELL_RESEARCH** | 셀 수입 배율 (×1.0→×3.0) | — |
| **CR_RESEARCH** | CR 수입 배율 (×1.0→×3.0) | — |

## 슬롯 시스템

- **빌드 슬롯** (최대 7개): 머신 또는 터렛 장착
- **스페셜 슬롯** (5개 고정): 특수 공격 장착

## 특수 공격 (Specials) — 5종

소모성 스킬. 전투 중 사용하며, 비행기 플라이오버 → 투사체 발사 → 착탄 순서로 연출.

### AIRSTRIKE_BOMB — 폭격

다발 투사체. 비행기가 목표 상공에서 폭탄 투하.

| Lv | 데미지 | 탄수 | 범위 |
|----|--------|------|------|
| 1 | 300 | 10 | 120 |
| 2 | 400 | 11 | 130 |
| 3 | 500 | 12 | 140 |
| 4 | 600 | 13 | 150 |
| 5 | 700 | 14 | 160 |

### AIRSTRIKE_MISSILE — 미사일 공습

다발 투사체. 폭격보다 높은 데미지와 넓은 범위.

| Lv | 데미지 | 탄수 | 범위 |
|----|--------|------|------|
| 1 | 400 | 10 | 150 |
| 2 | 500 | 11 | 160 |
| 3 | 600 | 12 | 170 |
| 4 | 700 | 13 | 180 |
| 5 | 800 | 14 | 190 |

### AIRSTRIKE_NUKE — 핵공습

단발 고위력 광역. 거대 폭발 + 주변 소폭발.

| Lv | 데미지 | 범위 |
|----|--------|------|
| 1 | 1,500 | 300 |
| 2 | 2,000 | 320 |
| 3 | 2,700 | 360 |
| 4 | 3,700 | 400 |
| 5 | 5,000 | 450 |

### AIRSTRIKE_EMP — EMP 공습

단발 광역. 데미지 + 마비(이동/공격 정지).

| Lv | 데미지 | 범위 | 마비시간 |
|----|--------|------|----------|
| 1 | 100 | 120 | 12초 |
| 2 | 120 | 150 | 18초 |
| 3 | 150 | 200 | 26초 |
| 4 | 200 | 250 | 36초 |

### CROSSFIRE_MISSILE — 십자포화

아군 머신에서 미사일 직접 발사.

| Lv | 데미지 | 범위 | 미사일수 |
|----|--------|------|----------|
| 1 | 300 | 100 | 2 |
| 2 | 300 | 105 | 3 |
| 3 | 300 | 110 | 4 |
| 4 | 300 | 115 | 5 |
| 5 | 300 | 120 | 6 |
| 6 | 300 | 125 | 7 |
| 7 | 300 | 130 | 8 |
| 8 | 300 | 135 | 9 |

## 웨이브 시스템

적은 웨이브 단위로 스폰. 레벨이 올라갈수록 적의 종류, 수, 스탯이 증가.
전투 중 화면 하단에 현재 웨이브 번호가 "WAVE n / total" 형식으로 표시된다.

### 스케일링 공식 (`progress = (level - 1) / 79`)

| 속성 | Lv.1 | Lv.80 |
|------|------|-------|
| 웨이브 수 | 3 | 6 |
| 적 HP | 80 | 1,000 |
| 적 공격력 | 8 | 80 |
| 적 속도 | 6 | 10 px/s |
| 적 공속 | 0.6 | 1.2 |
| 적 사거리 | 100 | 140 |
| 적 감지거리 | 160 | 200 |
| 그룹당 적 수 | 2 | 10 |
| 스폰 간격 | 3초 | 1초 |
| 첫 웨이브 딜레이 | 5초 | 2초 |
| 이후 웨이브 딜레이 | 15초 | 5초 |

- 적 탱크 종류: Tank01(Lv.1) ~ Tank32(Lv.80)까지 점진 해금
- 전장 배경: 27종이 80개 레벨에 순환 배치

## 세이브 시스템

`Preferences("hm-defense/saves")` 기반.

저장 항목:
- 최종 클리어 스테이지 (0-79)
- 크레딧 (재화)
- 머신/터렛/업그레이드/스페셜 레벨
- 빌드 슬롯 구성 (최대 7개)
- 스페셜 슬롯 구성 (5개)
- 스페셜 잔여 수량

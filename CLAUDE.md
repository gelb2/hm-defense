# Heavy MACH: Defense — Project Guide

iOS 원작(We Made Entertainment, ~2008)을 LibGDX/Kotlin으로 재현하는 프로젝트.
Mesabloo의 기반 코드 위에 게임플레이를 복원 중.

## Build & Run

```bash
# 컴파일 확인
./gradlew :desktop:classes

# 실행 (DEV 모드 — 전 스테이지 해금)
./gradlew :desktop:run -PreleaseType=dev

# 실행 (일반)
./gradlew :desktop:run

# 이펙트 텍스처 패킹
./gradlew packEffects

# 배포용 JAR
./gradlew :desktop:dist
```

JAVA_HOME: `~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`

## Tech Stack

- **LibGDX 1.13.1** + LibKTX 1.13.1-rc1
- **Kotlin 2.1.0**, kotlinx.serialization 1.7.3
- **Box2D** (물리), **Scene2D** (UI/렌더링), **gdx-ai 1.8.2** (행동 트리)
- Universal Tween Engine 6.3.3
- Desktop: LWJGL3 backend, 768×1024, 60fps

## Project Structure

```
core/
  src/kotlin/fr/mesabloo/heavymachdefense/
    MainGame.kt              # KtxGame 메인 앱
    Constants.kt             # PPM=16f, MachinePart enum
    Options.kt               # DEBUG, DEV, INHOUSE 플래그
    screens/
      StageScreen.kt         # 게임플레이 핵심 (전투, 스킬, 스폰)
      StageSelectionScreen.kt
      SavesSelectionScreen.kt
      StartScreen.kt
    ai/                      # AI 행동 트리, GameObject, Entity
      GameObject.kt          # Steerable 기반 추상 클래스
      MachineEntity.kt       # 아군 유닛 AI
      EnemyTankEntity.kt     # 적 유닛 AI
      BaseEntity.kt          # 기지 AI
    data/                    # 데이터 모델, 열거형, 직렬화
      MachineKind.kt         # 8종: RIFLE, MISSILE, ION 등
      SpecialKind.kt         # 5종: AIRSTRIKE_BOMB/MISSILE/NUKE/EMP, CROSSFIRE_MISSILE
      TurretKind.kt, UpgradeKind.kt
      GameSave.kt            # 세이브 데이터 (Preferences 기반)
      WaveData.kt            # 웨이브 동적 생성 (레벨 1-80)
    managers/
      assets/
        StageAssetsManager.kt  # 스테이지 에셋 로딩 (텍스처, 사운드)
      WaveManager.kt         # 적 웨이브 스폰 스케줄러
      SoundManager.kt
      FontManager.kt         # 14종 비트맵 폰트
    ui/stage/
      Machine.kt             # 아군 머신 비주얼 (body+feet+weapons)
      EnemyTank.kt           # 적 탱크 비주얼
      Terrain.kt             # 전장 컨테이너
      game/
        Bullet.kt            # 투사체 (아크 궤적, 스모크 트레일, onHit/onMiss)
        ExplosionEffect.kt   # 폭발 이펙트
    world/
      GameWorld.kt           # Box2D 물리 월드
      UIWorld.kt             # Scene2D 스테이지 (FitViewport 768×1024)
    listeners/               # 이벤트 핸들러 (빌드, 업그레이드, 스킬 사용 등)
  assets/
    gfx/                     # 텍스처, 아틀라스
      models/machines/       # bodies.atlas, weapons.atlas, feet.atlas
      models/effects/        # effects.atlas (폭발, 연기 등)
      models/bullets/        # 투사체 스프라이트
      models/planes/         # 공습 비행기
      terrains/01-27/        # 레벨별 배경
      ui/                    # UI 그래픽
    sfx/                     # 사운드 에셋
      machine/               # 무기별 발사음 (rifle, missile, plasma 등)
      weapon/                # 특수무기 사운드 (missile.wav 등)
      effects/               # 폭발, 피격음
      game/                  # 게임 이벤트 사운드
    data/
      special-info.json      # 스킬 데미지/레벨 데이터
      build-info.json        # 빌드 설정
      map-list.json          # 레벨 메타데이터 (1-80)
      models/machines/       # 머신별 JSON (body/weapon/feet 좌표)
desktop/
  src/kotlin/.../desktop/
    DesktopLauncher.kt       # main() 진입점
```

## Architecture

### MVC 패턴
- **Model** → `data/` (GameSave, WaveData, MachineKind 등 데이터 모델)
- **View** → `ui/stage/` (Machine, EnemyTank, Bullet 등 Scene2D 액터)
- **Controller** → `ai/` (Entity + 행동 트리) + `listeners/` (이벤트 핸들러)

### 렌더링: Two-World 시스템
- **GameWorld** — Box2D 물리 시뮬레이션, 충돌 감지
- **UIWorld** — Scene2D 스테이지, FitViewport, 카메라 제어
- 물리 바디 위치 → Scene2D 액터 위치 동기화

### AI 시스템
- gdx-ai 행동 트리 기반
- `GameObject` → `MachineEntity` / `EnemyTankEntity` / `BaseEntity`
- 타겟 락온 → 사거리 내 사격 → 투사체 생성

### 화면 전환
```
StartScreen → SavesSelectionScreen → StageSelectionScreen → StageScreen
```

### 투사체 시스템 (Bullet.kt)
- 시작점/끝점, speed, arcHeight (사인 커브 수직 오프셋)
- trailRegions로 스모크 트레일
- onHit: 타겟 생존 시 콜백, onMiss: 타겟 사망/부재 시 콜백

### 공습 스킬 (StageScreen.kt)
- `spawnPlaneFlyover()` — 비행기 플라이오버 + onDrop 콜백
- onDrop: 비행기가 targetY-700px 도달 시 Bullet 투사체 발사
- BOMB/MISSILE: 다발 투사체, NUKE/EMP: 단발 투사체

## Key Constants

- `PPM = 16f` — 픽셀↔Box2D 미터 변환
- `UI_WIDTH = 768f`, `UI_HEIGHT = 1024f`
- Terrain: 512×2048 pixels
- 비행기 비행시간: 5.0초 (Interpolation.pow3In)

## Conventions

- 패키지: `fr.mesabloo.heavymachdefense`
- 매니저: lazy 싱글턴 패턴
- 에셋 키: `StageAssetsManager`의 companion object 상수
- 사운드: `randomSpecialImpactSound()` 등 랜덤 풀 패턴
- 세이브: `Preferences("hm-defense/saves")`
- 굵직한 기능 구현 완료 시 `docs/RESTORATION.md`의 복원 진행 섹션에 반영할 것
- 게임 메카닉 변경 시 `docs/Gameplay.md` 업데이트 (스탯, 유닛, 스킬 등)
- 기능 구현/버그 수정 완료 시 `docs/CHANGELOG.md`에 항목 추가
- 에셋 추가/변경 시 `docs/ASSETS.md` 업데이트

## TODO (미해결/진행중)

- [ ] 미사일 투사체 트레일 이펙트 (하얀 굵은 실/연기) — 시도 2회 롤백, 방법 미정

## Notes

- 에셋 좌표계: 원작 IPA 추출물 → LibGDX 좌표계 변환 필요할 수 있음
- DEV 모드에서 전 스테이지 해금됨 (Options.kt의 DEV 플래그)
- 원작 저작권: We Made Entertainment — 수익화 금지, 비상업적 복원 목적

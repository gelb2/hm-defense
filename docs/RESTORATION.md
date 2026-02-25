# Heavy MACH: Defense - Restoration Story

## Why This Project Exists

2008년, 한 대학생이 아이팟터치에서 *Heavy MACH*을 처음 만났다. 틸트 기반 컨트롤로 전차를 조종하고, 사격하며, 한 스테이지가 끝날 때마다, 세계적인 아티스트 페이퍼블루의 일러스트를 보여주는 이 게임은 개발자 변해준, 아티스트 페이퍼블루의 손에서 탄생하고, *Heavy MACH 2*, *Heavy MACH: Defense* 라는 속편으로 세상에 이어졌다. 
*Heavy MACH 시리즈*는 훗날 그 대학생이 2015년의 잡지사 기자를 거쳐, 2016년의 필리핀 공장 관리자 생활 끝에, 2017년에 iOS 개발자로 전업을 하는 데 동기부여를 준 여러 사건 중 하나였다.

*Heavy MACH 시리즈*가 세상에 나온 지 약 18년이 지나, 원작은 앱스토어에서 사라졌다. iOS 7 이후의 호환성 문제로 새 세이브 파일을 생성할 수 없게 되었고, 원작 개발사 [We Made Entertainment](https://wemade.com)는 업데이트를 포기했다. 게임은 영원히 사라질 운명이었다.

그리고 2026년, iOS 개발자로 개발 직무를 시작한 그 대학생은 캐나다의 개발자 Ghilain Bergeron(Mesabloo)의 *Heavy MACH 시리즈* 의 마지막 작품인 *Heavy MACH: Defense* 재현 프로젝트를 우연히 발견했다.

---

## The Fork

2021년 12월, 캐나다의 개발자 Ghilain Bergeron(Mesabloo)이 이 게임을 처음부터 재현하는 프로젝트를 시작했다. LibGDX + Kotlin 기반으로 데스크톱/안드로이드 환경을 타겟으로 했으며, 원작 IPA에서 에셋을 추출하고 GDB 스크립트로 게임 데이터를 역분석하는 등 진지한 시도였다.

하지만 프로젝트는 미완성 상태로 멈췄다. UI 프레임워크와 에셋 파이프라인은 구축되었으나, 핵심 게임플레이 — 유닛 생산, 전투, 웨이브 시스템, 스페셜 공격 — 는 대부분 구현되지 않았다. 별도의 `rework-scala` 브랜치에 Scala로 작성된 전투 로직이 존재했지만, 왜 Kotlin 프로젝트에 Scala 브랜치가 있는지, 그 안의 로직이 어떤 의도인지는 불분명했다.

2026년 2월 24일, Jun Young Jee가 이 레포지토리를 포크했다.

---

## The Challenge

포크한 사람의 배경은 게임 개발과 거리가 멀었다:
- iOS 앱 개발
- Android 앱 인수인계 및 유지보수
- Vue.js 프론트엔드

LibGDX, Box2D, Scene2D, gdx-ai 행동 트리, Kotlin DSL 기반 물리 엔진 — 이 모든 것이 처음이었다. 게임 개발 경험이 전무한 상태에서 미완성 게임 엔진의 아키텍처를 파악하고, 그 위에 게임플레이를 올리는 것은 심리적으로도 기술적으로도 높은 벽이었다.

### Technical Hurdles

- **언어 스택**: Kotlin 메인 브랜치 + Scala 전투 로직 브랜치. 두 언어 간 로직 포팅이 필요했다
- **아틀라스 좌표 불일치**: 원작 IPA에서 추출한 에셋의 좌표계와 LibGDX의 좌표계가 달랐다
- **미완성 아키텍처**: UI 프레임워크는 있지만 게임 루프, 생산 큐, AI 시스템은 스텁 상태였다
- **문서 부재**: 기획서는 미완성, 코드 주석은 최소한, 원작자의 의도를 코드에서 역추적해야 했다

---

## The Approach: AI-Collaborative Development

이 프로젝트의 복원은 전통적인 수동 코딩이 아니다. **Claude(Anthropic)와의 협업을 통한 AI-assisted development** — 이른바 "바이브 코딩(Vibe Coding)"의 산물이다.

### What This Means

- **아키텍처 설계는 인간이, 구현은 AI와 함께**: 어떤 기능이 필요한지, 어떤 순서로 구현할지, 어떤 트레이드오프를 선택할지는 인간이 결정했다. 그 결정을 코드로 변환하는 과정에서 Claude가 코드베이스를 분석하고, 패턴을 파악하고, 구현을 제안했다.

- **프롬프트 엔지니어링이 곧 설계**: "크로스파이어 미사일이 메크에서 발사되어 타원 궤적으로 적에게 날아가게 해줘"라는 한 문장이, Bullet 클래스의 arcHeight 파라미터 설계, 사인 커브 기반 수직 오프셋 계산, MachineEntity 이터레이션 로직으로 변환되었다.

- **Scala 브랜치 해독**: Previous Maintainer가 남긴 Scala 브랜치의 전투 관련 로직을 Claude와 함께 기술 검토했다. AI가 Scala 코드를 분석하여 전투 시스템의 핵심 로직(타겟팅, 데미지 계산, AI 행동 트리)을 추출하고, 이를 기존 Kotlin 코드베이스의 아키텍처에 맞게 포팅했다.

### What This Is NOT

- 이것은 "AI가 만든 게임"이 아니다. AI는 인간의 의도를 코드로 변환하는 도구였다.
- 모든 기술적 결정, 게임플레이 설계, 에셋 선택, 우선순위 판단은 인간이 했다.
- AI는 제안했고, 인간은 테스트하고, 판단하고, 방향을 수정했다.

---

## What Was Restored

Previous Maintainer(Mesabloo)의 작업물 위에 복원/구현된 항목:

### Core Gameplay
- 유닛 생산 시스템 (빌드 큐)
- 맵 스크롤 및 이동
- 머신 스폰, 방향, 걷기 애니메이션 (feet 기반 스텝 이동)
- 전투 시스템: AI 행동 트리 구동, 타겟팅, 투사체, 폭발 이펙트
- 적 스폰 및 웨이브 시스템 (전 레벨 자동 생성)

### Special Attacks (5 Types)
- **Airstrike Bomb**: 비행기 플라이오버 → 복수 폭탄 투사체 투하
- **Airstrike Missile**: 비행기 → 미사일 투사체 발사 (아크 궤적)
- **Airstrike Nuke**: 비행기 → 단발 대형 폭탄 → 범위 폭발 (가산 블렌딩)
- **Airstrike EMP**: 비행기 → EMP탄 → 번개 이펙트 + 적 마비
- **Crossfire Missile**: 아군 머신에서 타원 궤적 미사일 발사

### Visual Effects
- 비행기 플라이오버 연출 (사선 비행, 기수 방향, 가속 커브)
- 투사체 발사 및 낙하 연출 (smoke trail, arc trajectory)
- 핵 폭발 가산 블렌딩 (검은 사각형 아티팩트 수정)
- 폭발/착탄 이펙트 시스템

### Sound System
- 무기별 랜덤 사운드 풀 (발사음, 피격음)
- 스킬 폭발음 (미스 시에도 지면 착탄음)
- UI 효과음 (빌드, 업그레이드, 메뉴, 기지 경고)

### Infrastructure
- DEV 모드 전체 스테이지 해금(원활한 테스트 목적)
- 스테이지 진행 저장 시스템
- macOS 호환성 수정

---

## Credits

**Original Game**: *Heavy MACH: Defense* by [We Made Entertainment](https://wemade.com) (iOS, ~2008)

**Reconstruction Project**: Ghilain Bergeron ([Mesabloo](https://github.com/Mesabloo)) — LibGDX/Kotlin 기반 프로젝트 설계, UI 프레임워크, 에셋 파이프라인, 원작 데이터 역분석 (2021-2022)

**Restoration & Completion**: Jun Young Jee ([gelb2](https://github.com/gelb2)) — AI 협업을 통한 게임플레이 구현, 전투 시스템, 스페셜 공격, 사운드 시스템, 비주얼 이펙트 (2026)

**AI Collaboration**: Claude (Anthropic) — 코드 분석, 아키텍처 설계 지원, Scala→Kotlin 로직 포팅, 구현 협업

---

## License

All code is licensed under the BSD 3-clause license.
Original copyright © 2021 Ghilain Bergeron (Mesabloo).

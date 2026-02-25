# Heavy MACH: Defense — Restoration Story

---

## 법적 고지 및 저작권 안내

본 레포지토리는 리버스 엔지니어링, 소프트웨어 아키텍처 연구 및 AI 협업 개발(바이브 코딩)의 기술적 증명을 위해 제작된 **비상업적, 교육용 포트폴리오 프로젝트**입니다.

원작 게임 *Heavy MACH: Defense*의 타이틀, 캐릭터, 아트워크, 사운드 및 기본 게임 디자인을 포함한 모든 지적재산권(IP)과 저작권은 **[위메이드(We Made Entertainment)](https://wemade.com)** 및 원저작권자에게 있습니다.

* **비상업적 목적:** 본 프로젝트는 어떠한 수익도 창출하지 않으며, 상업적 출시를 목적으로 하지 않습니다.
* **배포 금지:** 원작의 저작물이 포함된 컴파일된 빌드 파일(APK/IPA 등)은 본 레포지토리에서 절대 배포하지 않습니다.
* **삭제 요청:** 원저작권자 측에서 본 레포지토리의 전시가 저작권을 침해한다고 판단하시어 에셋 삭제 혹은 비공개 전환을 요청하실 경우, 즉각적으로 조치하겠습니다.

본 프로젝트는 18년 전 훌륭했던 고전 게임에 대한 순수한 오마주이자, 현대 AI 개발론의 기술적 실험(Proof-of-Concept)으로만 존재합니다.

---

## 이 프로젝트가 존재하는 이유

2008년, 한 대학생이 아이팟터치에서 *Heavy MACH*을 처음 만났다. 틸트 기반 컨트롤로 전차를 조종하고, 사격하며, 한 스테이지가 끝날 때마다, 세계적인 아티스트 페이퍼블루의 일러스트를 보여주는 이 게임은 개발자 변해준, 아티스트 페이퍼블루의 손에서 탄생하고, *Heavy MACH 2*, *Heavy MACH: Defense* 라는 속편으로 세상에 이어졌다.

*Heavy MACH 시리즈*는 훗날 그 대학생이 2015년의 잡지사 기자를 거쳐, 2016년의 필리핀 공장 관리자 생활 끝에, 2017년에 iOS 개발자로 전업을 하는 데 동기부여를 준 여러 사건 중 하나였다.

*Heavy MACH 시리즈*가 세상에 나온 지 약 18년이 지나, 원작은 앱스토어에서 사라졌다. iOS 7 이후의 호환성 문제로 새 세이브 파일을 생성할 수 없게 되었고, 원작 개발사 [We Made Entertainment](https://wemade.com)는 업데이트를 포기했다. 게임은 영원히 사라질 운명이었다.

그리고 2026년, iOS 개발자로 개발 직무를 시작한 그 대학생은 프랑스의 개발자 Ghilain Bergeron(Mesabloo)의 *Heavy MACH 시리즈* 의 마지막 작품인 *Heavy MACH: Defense* 재현 프로젝트를 우연히 발견했다.

---

## 포크

2021년 12월, 프랑스의 개발자 Ghilain Bergeron(Mesabloo)이 이 게임을 처음부터 재현하는 프로젝트를 시작했다. LibGDX 1.10.0 + Kotlin 1.6.0 기반으로 데스크톱/안드로이드 환경을 타겟으로 했으며, 원작 IPA에서 에셋을 추출하고 GDB 스크립트로 게임 데이터를 역분석하는 등 진지한 시도였다.

2022년 상반기까지 Scene2D 기반 UI 프레임워크, 세이브 시스템, 업그레이드 메뉴, 기지 시스템 등이 구축되었다. 이후 2024년 1월, Mesabloo는 `rework-scala` 브랜치에서 Scala 기반으로 프로젝트 전면 재작성을 시도했다(146 커밋). 이 브랜치에는 AI 행동 트리, 스티어링 행동, 전투 시스템의 핵심 로직이 포함되어 있었다. 2025년 1월에는 AI 행동 트리 구현 탐색과 데이터 테이블 해독 작업이 이어졌다.

하지만 프로젝트는 미완성 상태로 멈췄다. Kotlin 메인 브랜치의 UI 프레임워크와 에셋 파이프라인은 견고했으나, 핵심 게임플레이 — 유닛 생산, 전투, 웨이브 시스템, 스페셜 공격 — 는 대부분 구현되지 않았다. Scala 브랜치에 본격적인 전투 로직이 존재했지만, 메인 브랜치와 통합되지 않은 상태였다.

> 원작자의 프로젝트 동기와 배경은 [README.md](../README.md)에서 확인할 수 있다.

2026년 2월 24일, Jun Young Jee가 이 레포지토리를 포크했다.

---

## 도전

포크한 사람의 배경은 게임 개발과 거리가 멀었다:
- iOS 앱 개발
- Android 앱 인수인계 및 유지보수
- Vue.js 프론트엔드

LibGDX, Box2D, Scene2D, gdx-ai 행동 트리, Kotlin DSL 기반 물리 엔진 — 이 모든 것이 처음이었다. 게임 개발 경험이 전무한 상태에서 미완성 게임 엔진의 아키텍처를 파악하고, 그 위에 게임플레이를 올리는 것은 심리적으로도 기술적으로도 높은 벽이었다.

### 기술적 난관

- **언어 스택**: Kotlin 메인 브랜치 + Scala 전투 로직 브랜치. 두 언어 간 로직 포팅이 필요했다
- **아틀라스 좌표 불일치**: 원작 IPA에서 추출한 에셋의 좌표계와 LibGDX의 좌표계가 달랐다
- **미완성 아키텍처**: UI 프레임워크는 있지만 게임 루프, 생산 큐, AI 시스템은 스텁 상태였다
- **문서 부재**: 기획서는 미완성, 코드 주석은 최소한, 원작자의 의도를 코드에서 역추적해야 했다

---

## 접근 방법: AI 협업 개발

이 프로젝트의 복원은 전통적인 수동 코딩이 아니다. **Claude(Anthropic)와의 협업을 통한 AI-assisted development** — 이른바 "바이브 코딩(Vibe Coding)"의 산물이다.

### 이것이 의미하는 것

- **아키텍처 설계는 인간이, 구현은 AI와 함께**: 어떤 기능이 필요한지, 어떤 순서로 구현할지, 어떤 트레이드오프를 선택할지는 인간이 결정했다. 그 결정을 코드로 변환하는 과정에서 Claude가 코드베이스를 분석하고, 패턴을 파악하고, 구현을 제안했다.

- **프롬프트 엔지니어링이 곧 설계**: "크로스파이어 미사일이 메크에서 발사되어 타원 궤적으로 적에게 날아가게 해줘"라는 한 문장이, Bullet 클래스의 arcHeight 파라미터 설계, 사인 커브 기반 수직 오프셋 계산, MachineEntity 이터레이션 로직으로 변환되었다.

- **Scala 브랜치 해독 및 선택적 포팅**: Previous Maintainer가 남긴 `rework-scala` 브랜치(146 커밋, 프로젝트 전면 재작성 시도)의 전투 관련 로직을 Claude와 함께 기술 검토했다. AI가 Scala 코드를 분석하여 전투 시스템의 핵심 로직(타겟팅, 데미지 계산, AI 행동 트리)을 추출하고, 이 중 필요한 부분만 기존 Kotlin 코드베이스의 아키텍처에 맞게 포팅했다. Scala 브랜치를 통째로 병합한 것이 아니라, 전투 시스템에 필요한 설계와 로직만 선별하여 Kotlin으로 재구현했다.

### 이것이 아닌 것

- 이것은 "AI가 만든 게임"이 아니다. AI는 인간의 의도를 코드로 변환하는 도구였다.
- 모든 기술적 결정, 게임플레이 설계, 에셋 선택, 우선순위 판단은 인간이 했다.
- AI는 제안했고, 인간은 테스트하고, 판단하고, 방향을 수정했다.

---

## 복원된 항목

Previous Maintainer(Mesabloo)의 작업물 위에 복원/구현된 항목:

### 핵심 게임플레이
- 유닛 생산 시스템 (빌드 큐)
- 맵 스크롤 및 이동
- 머신 스폰, 방향, 걷기 애니메이션 (feet 기반 스텝 이동)
- 전투 시스템: AI 행동 트리 구동, 타겟팅, 투사체, 폭발 이펙트
- 적 스폰 및 웨이브 시스템 (전 레벨 자동 생성)
- 머신 업그레이드 준비 화면 (스테이지 선택과 전투 사이 PreparationScreen)

### 특수 공격 (5종)
- **Airstrike Bomb**: 비행기 플라이오버 → 복수 폭탄 투사체 투하
- **Airstrike Missile**: 비행기 → 미사일 투사체 발사 (아크 궤적)
- **Airstrike Nuke**: 비행기 → 단발 대형 폭탄 → 범위 폭발 (가산 블렌딩)
- **Airstrike EMP**: 비행기 → EMP탄 → 번개 이펙트 + 적 마비
- **Crossfire Missile**: 아군 머신에서 타원 궤적 미사일 발사

### 비주얼 이펙트
- 비행기 플라이오버 연출 (사선 비행, 기수 방향, 가속 커브)
- 투사체 발사 및 낙하 연출 (smoke trail, arc trajectory)
- 핵 폭발 가산 블렌딩 (검은 사각형 아티팩트 수정)
- 폭발/착탄 이펙트 시스템

### 사운드 시스템
- 무기별 랜덤 사운드 풀 (발사음, 피격음)
- 스킬 폭발음 (미스 시에도 지면 착탄음)
- UI 효과음 (빌드, 업그레이드, 메뉴, 기지 경고)

### 인프라
- 의존성 마이그레이션: LibGDX 1.10.0→1.13.1, Kotlin 1.6.0→2.1.0, LWJGL2→LWJGL3
- DEV 모드 전체 스테이지 해금 (원활한 테스트 목적)
- 스테이지 진행 저장 시스템
- macOS 호환성 수정

---

## 크레딧

**원작 게임**: *Heavy MACH: Defense* — [We Made Entertainment](https://wemade.com) (iOS, ~2008)

**재현 프로젝트**: Ghilain Bergeron ([Mesabloo](https://github.com/Mesabloo)) — LibGDX/Kotlin 프로젝트 설계, UI 프레임워크, 에셋 파이프라인, 원작 데이터 역분석, Scala 기반 전투 시스템 재작성 시도 (2021-2025)

**복원 및 완성**: Jun Young Jee ([gelb2](https://github.com/gelb2)) — AI 협업을 통한 게임플레이 구현, 전투 시스템, 특수 공격, 사운드 시스템, 비주얼 이펙트 (2026)

**AI 협업**: Claude (Anthropic) — 코드 분석, 아키텍처 설계 지원, Scala→Kotlin 로직 포팅, 구현 협업

---

## 라이선스

모든 코드는 BSD 3-clause 라이선스로 배포됩니다.
원본 저작권 © 2021 Ghilain Bergeron (Mesabloo).

---
---

# Heavy MACH: Defense — Restoration Story (English)

---

## Legal Disclaimer & Copyright Notice

This repository is a non-commercial, educational, and portfolio project created solely for the purpose of studying reverse engineering, software architecture, and AI-assisted development (Vibe Coding).

All rights to the original game *Heavy MACH: Defense*, including but not limited to its title, characters, artwork, audio, and underlying game design, are the exclusive property of [We Made Entertainment](https://wemade.com) or its respective copyright holders.

* **No Commercial Intent:** This project does not generate any revenue, nor is it intended for commercial release.
* **No Distribution:** No compiled binaries (APK/IPA/EXE) containing original copyrighted assets are provided or distributed in this repository.
* **Takedown Request:** If you are the copyright holder and believe this repository infringes upon your rights, please contact me. I will immediately remove the requested assets or take the repository down without hesitation.

This project exists simply as a tribute to a childhood memory and a technical proof-of-concept for modern AI collaboration.

---

## Why This Project Exists

In 2008, a college student first encountered *Heavy MACH* on an iPod Touch. The game — controlled via tilt mechanics to maneuver a tank, fire weapons, and rewarded with illustrations by the renowned artist Paperblue after each stage — was born from the hands of developer Byun Haejun and artist Paperblue. It continued through sequels: *Heavy MACH 2* and *Heavy MACH: Defense*.

The *Heavy MACH series* was one of many catalysts that later motivated that college student — after working as a magazine journalist in 2015 and a factory manager in the Philippines in 2016 — to transition into a career as an iOS developer in 2017.

Approximately 18 years after the *Heavy MACH series* was released, the original game vanished from the App Store. Compatibility issues after iOS 7 made it impossible to create new save files, and the original developer [We Made Entertainment](https://wemade.com) abandoned updates. The game was destined to be lost forever.

Then, in 2026, that former college student — now a professional developer who had started his career in iOS development — stumbled upon a reconstruction project of the final installment of the *Heavy MACH series*, *Heavy MACH: Defense*, by French developer Ghilain Bergeron (Mesabloo).

---

## The Fork

In December 2021, French developer Ghilain Bergeron (Mesabloo) began a project to reconstruct this game from scratch. Built on LibGDX 1.10.0 + Kotlin 1.6.0 and targeting desktop/Android platforms, it was a serious effort that included extracting assets from the original IPA and reverse-engineering game data using GDB scripts.

By the first half of 2022, a Scene2D-based UI framework, save system, upgrade menus, and base systems were established. Then in January 2024, Mesabloo attempted a full project rewrite on the `rework-scala` branch using Scala (146 commits). This branch contained core combat system logic including AI behavior trees, steering behaviors, and battle mechanics. Work continued into January 2025 with AI behavior tree exploration and data table deciphering.

However, the project stalled in an incomplete state. The Kotlin main branch's UI framework and asset pipeline were solid, but core gameplay — unit production, combat, wave systems, special attacks — remained largely unimplemented. The Scala branch contained substantial combat logic, but it had not been integrated into the main branch.

> The original author's project motivation and background can be found in [README.md](../README.md).

On February 24, 2026, Jun Young Jee forked this repository.

---

## The Challenge

The person who forked the project had a background far removed from game development:
- iOS app development
- Android app handover and maintenance
- Vue.js frontend

LibGDX, Box2D, Scene2D, gdx-ai behavior trees, Kotlin DSL-based physics engine — all of these were completely new. Grasping the architecture of an incomplete game engine and building gameplay on top of it, with zero game development experience, was a formidable barrier both psychologically and technically.

### Technical Hurdles

- **Language stack**: Kotlin main branch + Scala combat logic branch. Porting logic between two languages was required
- **Atlas coordinate mismatch**: The coordinate system of assets extracted from the original IPA differed from LibGDX's coordinate system
- **Incomplete architecture**: The UI framework existed, but the game loop, production queue, and AI systems were in a stub state
- **No documentation**: Design documents were incomplete, code comments were minimal, and the original author's intent had to be reverse-engineered from the code

---

## The Approach: AI-Collaborative Development

The restoration of this project is not traditional manual coding. It is a product of **AI-assisted development through collaboration with Claude (Anthropic)** — so-called "Vibe Coding."

### What This Means

- **Humans design the architecture, AI assists with implementation**: What features were needed, in what order they should be implemented, and which trade-offs to accept — these decisions were made by the human. In the process of translating those decisions into code, Claude analyzed the codebase, identified patterns, and proposed implementations.

- **Prompt engineering is design**: A single sentence like "Make the crossfire missiles launch from mechs and fly to enemies in an elliptical trajectory" was transformed into Bullet class arcHeight parameter design, sine-curve-based vertical offset calculations, and MachineEntity iteration logic.

- **Deciphering the Scala branch and selective porting**: The combat-related logic in the `rework-scala` branch left by the previous maintainer (146 commits, a full project rewrite attempt) was technically reviewed together with Claude. The AI analyzed the Scala code to extract the core combat system logic (targeting, damage calculation, AI behavior trees), and only the necessary parts were ported to fit the existing Kotlin codebase architecture. Rather than merging the Scala branch wholesale, the design and logic needed for the combat system were selectively re-implemented in Kotlin.

### What This Is NOT

- This is not a "game made by AI." The AI was a tool for translating human intent into code.
- All technical decisions, gameplay design, asset selection, and priority judgments were made by the human.
- The AI proposed; the human tested, judged, and adjusted direction.

---

## What Was Restored

Items restored/implemented on top of Previous Maintainer (Mesabloo)'s work:

### Core Gameplay
- Unit production system (build queue)
- Map scrolling and movement
- Machine spawning, direction, walking animation (feet-based step movement)
- Combat system: AI behavior tree driven, targeting, projectiles, explosion effects
- Enemy spawning and wave system (auto-generated for all levels)
- Machine upgrade preparation screen (PreparationScreen between stage selection and combat)

### Special Attacks (5 Types)
- **Airstrike Bomb**: Plane flyover → multiple bomb projectile drops
- **Airstrike Missile**: Plane → missile projectile launch (arc trajectory)
- **Airstrike Nuke**: Plane → single large bomb → area explosion (additive blending)
- **Airstrike EMP**: Plane → EMP shell → lightning effect + enemy paralysis
- **Crossfire Missile**: Elliptical trajectory missiles launched from allied machines

### Visual Effects
- Plane flyover presentation (diagonal flight, nose direction, acceleration curve)
- Projectile launch and descent presentation (smoke trail, arc trajectory)
- Nuclear explosion additive blending (black rectangle artifact fix)
- Explosion/impact effect system

### Sound System
- Per-weapon random sound pools (fire sounds, hit sounds)
- Skill explosion sounds (ground impact sound even on miss)
- UI sound effects (build, upgrade, menu, base warning)

### Infrastructure
- Dependency migration: LibGDX 1.10.0→1.13.1, Kotlin 1.6.0→2.1.0, LWJGL2→LWJGL3
- DEV mode full stage unlock (for smooth testing)
- Stage progress save system
- macOS compatibility fixes

---

## Credits

**Original Game**: *Heavy MACH: Defense* by [We Made Entertainment](https://wemade.com) (iOS, ~2008)

**Reconstruction Project**: Ghilain Bergeron ([Mesabloo](https://github.com/Mesabloo)) — LibGDX/Kotlin project design, UI framework, asset pipeline, original data reverse-engineering, Scala-based combat system rewrite attempt (2021-2025)

**Restoration & Completion**: Jun Young Jee ([gelb2](https://github.com/gelb2)) — Gameplay implementation through AI collaboration, combat system, special attacks, sound system, visual effects (2026)

**AI Collaboration**: Claude (Anthropic) — Code analysis, architecture design support, Scala→Kotlin logic porting, implementation collaboration

---

## License

All code is licensed under the BSD 3-clause license.
Original copyright © 2021 Ghilain Bergeron (Mesabloo).

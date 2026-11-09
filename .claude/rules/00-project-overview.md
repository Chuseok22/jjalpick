# Project Overview

## Purpose

- 목적: 전역 단축키로 밈(이미지·GIF)을 검색해 클립보드에 복사하고, 사용 중인 앱에 붙여 넣어 주는 데스크톱 서비스. 서비스명은 가칭 `MimeFit`이며 추후 바뀔 수 있다. 표시명은 중앙 설정으로 관리하고 코드·DB·API 경로는 서비스명에 의존하지 않는다.
- 해결하려는 문제: 채팅 중 밈을 찾고 저장하고 붙여 넣는 번거로움.
- 사용자: 비회원도 검색·복사·삽입을 쓸 수 있다. 로그인(Google만)은 즐겨찾기 동기화와 공개 제출에 필요하다.
- 우선순위: 삽입 신뢰성(실패해도 복사 상태 유지) > 검색 반응 속도 > 공개 콘텐츠 안전(승인·신고) > 운영 단순성(1인 개발, NAS 운영).
- 범위: Windows 11 x64와 macOS Apple Silicon 데스크톱 앱, 공개 웹사이트, 서버와 관리자 페이지. 모바일 앱은 범위 밖이다.

## Primary Stack

- Backend: Java 21, Spring Boot 4.1.x(`spring-boot-starter-webmvc`, 가상 스레드), Gradle Groovy DSL, 단일 모듈
- Database: PostgreSQL 17, Spring Data JPA, Flyway
- Admin: Spring Boot 내부 Thymeleaf SSR (`api.<domain>/admin`)
- Desktop: Tauri 2 + Rust + React + Vite + TypeScript
- Website: React + Vite + TypeScript (정적 빌드, 독립 배포)
- Storage: Cloudflare R2
- JS 패키지 매니저: pnpm (lockfile 커밋)
- CI/CD: GitHub Actions, trunk-based(`main` 단일)

## Important directories

- `backend/`: Spring Boot 앱
- `desktop/`: Tauri 앱(React UI는 `desktop/src`, Rust는 `desktop/src-tauri`)
- `website/`: 공개 웹사이트
- `.report/`: 작업 보고서(gitignore)
- 최상위 폴더는 서로 독립된 프로젝트이며 공유 패키지를 두지 않는다.

## Main commands

- Backend 빌드·테스트: `cd backend && ./gradlew build`
- Backend 일부 테스트: `cd backend && ./gradlew test --tests '*ClassName'`
- Website·Desktop 준비: `cd <website|desktop> && pnpm install --frozen-lockfile`
- Website·Desktop 검증: `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build`, `pnpm exec prettier --check .`
- Desktop Rust 검사: `cd desktop/src-tauri && cargo check` (cargo가 PATH에 없으면 `$HOME/.cargo/bin`을 추가한다)
- Tauri CLI: `cd desktop && pnpm tauri <명령>` (`tauri` 스크립트 존재와 `--version`만 확인했고 `tauri dev`·`tauri build`는 실행해 보지 못했다)

## Project-specific constraints

- 도메인 상세(엔드포인트, 필드, 에러 코드, 상태, 정책 값)는 문서에 적지 않는다. 원천은 코드이고 결정은 각 이슈 구현 전 브레인스토밍에서 확정한다. 확정 전에는 임의로 구현하지 않는다. 결정 이력은 vault(second-brain)에 남긴다. 문서와 코드가 어긋나면 코드가 맞다.
- 이슈 본문은 브레인스토밍 시작 때 맥락으로 한 번 가볍게 읽는 용도다. 이슈를 갱신하거나 근거 문서로 쓰지 않는다.
- 새 의존성은 필요해지는 시점에 추가한다. 미리 넣지 않는다.
- 서버 시크릿은 클라이언트(Desktop, Website)에 넣지 않는다.
- 저장소는 public이다. 시크릿은 GitHub Secrets에만 둔다.

## Change policy

- 요청받은 범위만 수정한다. 인접 코드를 개선하거나 리팩터링하지 않는다.
- 커밋 금지: `application-dev.yml`, `application-prod.yml`, 키·인증서. `.env`, `*.example` 파일을 추가하지 않는다.
- 확장 후보(검색 고도화, 수익 모델·Pro 차별, 결제 연동, 이메일 알림, 로컬 전용 개인 밈, 컬렉션, 이미지·GIF 제한, 설정 예약 적용)는 이슈로 관리하며 요청 없이 구현하지 않는다.

# Project Overview

## Scope

- 이 저장소는 `backend/`, `desktop/`, `website/` 세 프로젝트로 구성된다. 제품의 목적·사용자·범위·우선순위는 기획 문서가 원천이며 이 규칙 문서에 적지 않는다. 규칙은 기획이 바뀌어도 유효한 구현 규칙만 담는다.
- 서비스 표시명은 중앙 설정으로 관리하고 코드·DB·API 경로는 서비스명에 의존하지 않는다.

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
- 기획 문서에 후속·확장 후보로 적힌 기능은 이슈로 관리하며 요청 없이 구현하지 않는다.

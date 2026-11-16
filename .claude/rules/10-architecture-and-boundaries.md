# Architecture And Boundaries

## High-level architecture

- 모노레포. `backend/`(단일 Gradle 모듈), `desktop/`, `website/`는 서로 독립된 프로젝트이며 공유 패키지를 두지 않는다. 계약의 원천은 문서가 아니라 코드다. 다른 쪽 코드를 고치는 일은 사용자 요청 범위 안에서만 한다.
- API 계약: 컨트롤러와 `XxxControllerDocs`가 원천이고 springdoc이 OpenAPI를 생성한다. API를 호출하는 클라이언트(`desktop/`, `website/`)는 그 OpenAPI에서 각자 클라이언트를 생성한다. 수작업 OpenAPI 파일을 따로 만들지 않고 생성 파일을 수동 수정하지 않는다.
- BE 패키지: `<domain>.{core,application,infrastructure}`, 공통은 `common/{core,application,infrastructure}`. 루트 패키지명은 확정 후 정한다.
  - core: 엔티티, enum, 리포지토리 인터페이스, 외부 연동 port
  - application: 서비스, DTO(`dto/{request,response}`), 컨트롤러와 `XxxControllerDocs`
  - infrastructure: adapter, 설정, `@ConfigurationProperties`, 보안 설정·필터
- Desktop: React UI는 화면과 상태를 맡고, OS 연동(전역 단축키, 클립보드, 포커스 복귀, 붙여넣기 입력, 로컬 캐시, 보안 저장소)은 Rust가 맡는다. UI는 좁게 노출된 Tauri command로만 OS 기능에 접근한다.
- 미디어: 파일은 R2에 직접 업로드하고 API 서버가 원본을 저장하거나 프록시하지 않는다. 공개 승인 전 파일과 비공개 파일은 공개 접근이 불가능한 영역에 둔다.

## Module boundaries

- 허용 의존: `application → core`, `infrastructure → core`, `core → common.core`, `infrastructure → common.application`.
- 금지: core가 application·infrastructure를 참조, application이 infrastructure를 직접 import(외부 연동은 core의 port로), 도메인 간 순환 의존. 도메인 간 의존 자체는 허용한다. ArchUnit 테스트(`LayerDependencyTest`)가 계층 규칙과 순환을 강제하며 완화·삭제하지 않는다.
- port·adapter는 교체·확장 가능성이 있는 외부 연동에만 둔다. 구현체가 하나뿐인 서비스에는 인터페이스를 만들지 않는다.
- 엔티티의 도메인 간 연관은 `@ManyToOne(LAZY)` 단방향으로 허용한다. 다른 도메인이 참조하는 쪽(의존의 종점에 가까운 도메인)을 가리키는 것을 기본으로 한다.
- common은 도메인에 의존하지 않고, 도메인은 common에 의존할 수 있다.

## Data flow

- 요청: 컨트롤러는 서비스만 호출하고 리포지토리를 직접 호출하지 않는다. 응답은 `ResponseEntity<Dto>`이며 envelope를 두지 않는다.
- 인증은 경로별로 보안 필터 체인을 나눈다.
  - `/api/**`: JWT(Bearer), stateless.
  - `/admin/**`: Thymeleaf 세션 + CSRF + `ROLE_ADMIN`. 사용자 JWT 체인과 분리한다.
  - 결제 웹훅(도입 시): 서명 검증 전용 체인.
- 토큰 보관: 액세스 토큰은 메모리에만 둔다. 리프레시 토큰은 클라이언트가 제공하는 보안 저장소(OS 보안 저장소, HttpOnly Secure 쿠키 등)에 둔다. `localStorage` 등 JS가 읽을 수 있는 저장소에 토큰을 두지 않는다. 리프레시는 회전하고 DB에는 해시만 저장한다.
- CORS는 필요한 출처만 명시적으로 허용하고(와일드카드 금지) 허용 출처는 코드의 한 곳에서 관리한다. 컨트롤러 인증 주체는 `@AuthenticationPrincipal`로 받는다.
- API 호환: URL에 버전을 넣지 않는다. 설치형 Desktop의 구버전이 공존하므로 변경은 추가만 허용하고, 호환을 깨야 할 때는 서버의 최소 클라이언트 버전(동적 설정)으로 업데이트를 강제한다.
- 동적 설정: 관리자가 운영 중 바꾸는 모든 값은 설정 종류마다 타입이 있는 현재값 테이블과 리비전 테이블로 관리한다. key-value 테이블을 쓰지 않는다. 리비전은 불변 이력(변경자, 시각, 선택 사유, 전체 값)이고, 되돌리기는 과거 값을 복사한 새 리비전을 만드는 전진형 롤백이다. 변경은 즉시 적용만 지원하고, 서버 상한으로 다시 검증하며, 낙관적 락으로 동시 수정을 막는다. 사유는 선택이다.
- 관리자 처리 행위(승인, 반려, 숨김, 제재, 태그 병합)는 리비전이 아니라 감사 로그로 남긴다.
- 새 기능을 구현할 때 동적 설정 후보를 검토해 추천한다.

## File / folder conventions

- 컨트롤러 경로는 `/api/<복수형-kebab-case>`(버전 접두사 없음), 경로 변수도 kebab-case.
- Swagger 어노테이션은 컨트롤러가 구현하는 `XxxControllerDocs` 인터페이스(같은 `controller` 패키지)에 둔다. 에러 코드와 발생 조건을 한국어로 적고, `@Valid` 같은 검증 어노테이션은 컨트롤러와 Docs에 똑같이 선언한다(다르면 HV000151로 500).
- 패키지·디렉터리 이름이 `.gitignore` 패턴과 겹치면 소스가 추적에서 빠진다. 새 패키지명을 정할 때 확인한다.
- 도메인 패키지는 구현할 때 만든다. 빈 패키지를 미리 만들지 않는다.
- Desktop·Website: 기능 단위로 `src/features/<feature>/…`, 공유는 `src/shared/…`에 둔다. Desktop의 Tauri 전용 API는 공유 영역이 아니라 Desktop 내부의 어댑터 계층에서만 호출한다.

## Extension points

- 새 도메인은 기존 도메인의 구조와 테스트를 먼저 참고한다.
- 외부 연동(R2, 결제 등)은 core의 port와 infrastructure의 adapter로 추가한다.
- 검색 로직은 교체·확장할 수 있도록 인터페이스 뒤에 두고, 첫 구현은 가벼운 검색으로 시작한다.

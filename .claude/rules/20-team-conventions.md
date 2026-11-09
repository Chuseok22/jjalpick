# Team Conventions

## Naming

- 약어를 쓰지 않는다(`id`, `url`, `dto` 제외). 이름은 의도를 드러내는 영어로 짓는다.
- 컴포넌트와 화면(페이지) 이름은 줄이거나 요약하지 않는다. 이름만 보고 그 컴포넌트·화면이 무엇이고 어떤 역할인지 알 수 있어야 하며, 비슷한 입력·버튼 등이 더 생겨도 구분되도록 형태와 역할을 이름에 드러낸다.
- 클래스는 PascalCase, DTO는 record, 테이블은 snake_case 단수형(`member`, `meme`), 클라이언트 모델 필드는 서버 응답 필드(camelCase)와 맞춘다.
- 주석과 Javadoc은 한국어, 식별자는 영어.

## Code style

- BE: 들여쓰기 4칸, 줄 길이 120. 생성자 주입(`@RequiredArgsConstructor`)만 쓰고 필드 주입은 쓰지 않는다. `@Data` 금지. `@Transactional`은 메서드 단위로 붙이고 조회는 `readOnly = true`.
- 유지보수: 같은 처리를 여러 곳에 반복하지 않는다. 직렬화·시간 변환·에러 변환·인증 주체 해석처럼 횡단 관심사는 중앙 한 곳(설정, 공통 컴포넌트)에서 처리하고, 각 컨트롤러·서비스·DTO에서 개별로 처리하지 않는다. 한 클래스·함수는 하나의 책임만 갖는다.
- 엔티티: 모든 엔티티는 `BaseEntity`(JPA Auditing 추상 클래스)를 상속한다. `BaseEntity`는 UUID v7 PK, `createdAt`/`updatedAt`, soft delete `deleted`/`deletedAt`을 가진다. `@Table` 명시, enum은 `EnumType.STRING`, 다른 엔티티를 가리키는 id를 UUID 컬럼으로 저장하지 않고 `@ManyToOne(LAZY)` 연관으로 선언한다(도메인 간 참조 포함). `@OneToMany`와 cascade는 금지하고 하위 목록은 리포지토리로 조회한다. 상태 변경은 도메인 메서드로 하며 허용 전이는 상태 enum의 `canTransitionTo`에 둔다(위반 시 `STATE_CONFLICT`).
- 결제·감사·리비전 기록은 `deleted` 컬럼이 있어도 삭제(soft delete 포함)하지 않는다. 구조가 아니라 코드 규칙으로 지킨다.
- 탈퇴는 `member`의 soft delete로 처리한다. 같은 Google 계정으로 재가입하면 새 `member`로 구분한다.
- soft delete 조회 필터(`deleted = false`)는 리포지토리 메서드에서 명시한다(`@SQLRestriction` 금지). UNIQUE는 활성 행에만 걸리게 partial unique index로 만든다.
- API 계약: 필드와 query parameter는 camelCase, path variable은 kebab-case, enum 값은 `UPPER_SNAKE_CASE` 문자열, ID는 UUID 문자열이다.
- 시간: 서버 코드 내부는 `Clock` 주입 + `Instant`만 쓴다(저장은 `timestamptz`). `Instant.now()`, `LocalDateTime.now()` 직접 호출 금지. API로 내려가는 시각은 직렬화 설정 한 곳에서 KST(`Asia/Seoul`) 오프셋이 붙은 ISO-8601 문자열(예: `2026-11-09T14:00:00+09:00`)로 자동 변환한다. 요청으로 받는 시각은 오프셋이 있는 ISO-8601만 받아 `Instant`로 변환한다. 서비스·도메인 코드에서 개별로 변환하지 않는다.
- 에러: 중앙 `ErrorCode` enum + `CustomException`, 응답은 `{ "errorCode", "errorMessage" }` 두 필드. 도메인별 예외 클래스를 만들지 않는다. 예외를 삼키지 않고 내부 정보(스택, SQL)를 노출하지 않는다.
- 로그: `@Slf4j`. 예상된 4xx는 WARN(스택 없음), 5xx는 ERROR와 스택. `System.out`·`printStackTrace` 금지. 컨트롤러 메서드는 `@LogMonitoring`을 붙이며 요청·응답 값(시크릿 포함)을 마스킹 없이 로깅한다.
- DB: 스키마는 Flyway로만 바꾼다(`ddl-auto: validate`). 파일명 `V{yyyyMMdd}_{HHmmss}__{snake_case}.sql`, 적용된 마이그레이션은 수정하지 않는다.
- 설정: `@Value`가 아니라 `@ConfigurationProperties`(도메인 `infrastructure/properties`)로 바인딩하고 필수 값이 비면 기동 시 실패시킨다. `application.yml`은 공통 설정만(값 없음), 실제 값은 `application-{dev,prod}.yml`(gitignore). JWT 수명 같은 보안 기본값은 이 설정에 두며 관리자 동적 설정 대상이 아니다.
- Spring Boot 4 주의: Web starter는 `spring-boot-starter-webmvc`, Flyway는 `spring-boot-starter-flyway`, JSON은 Jackson 3(`tools.jackson`).

## Responsibility separation

- controller는 요청·응답 변환과 서비스 호출만, 비즈니스 로직은 service·도메인 메서드에 둔다. 서비스는 도메인당 하나를 기본으로 하고 비대해지면 Query/Command로 나눈다.
- 서비스는 행위 주체·소유자 회원을 `Member`로 받는다. 컨트롤러가 `@AuthenticationPrincipal`에서 꺼내 넘기고, UUID만 있는 경로(관리자 경로 변수 등)는 서비스가 id로 조회한다. 컨트롤러가 넘긴 `Member`는 분리 상태라 연관 대상으로만 쓰고, 수정은 id로 다시 조회한 엔티티에서 한다.

## Client conventions (Desktop, Website)

- TypeScript strict. `any` 금지, `console.log` 금지, ESLint와 Prettier를 따른다.
- 서버가 내려주는 `errorMessage`는 개발자용 문자열이므로 사용자에게 노출하지 않는다. `errorCode`를 사용자 친화 문구로 매핑해서 보여 준다.
- 에러 처리는 중앙(HTTP 클라이언트 인터셉터와 에러 핸들러) 한 곳에서 한다. 화면이 에러 UI를 자체적으로 만들지 않는다.
- 색, 간격, 반경, 글자 스타일은 선언된 디자인 토큰으로만 참조한다. 컴포넌트에 리터럴 값을 쓰지 않는다. 토큰 이름은 의미를 드러내는 일반 이름으로 짓는다.
- 컴포넌트는 파일별로 분리해 화면에서 조립한다. 화면 파일 하나에 모든 코드를 쓰지 않는다. 비즈니스 로직은 컴포넌트가 아니라 훅·도메인에 둔다. 서버 상태는 TanStack Query로 다루고 별도 스토어에 복제하지 않는다.
- 비동기 작업이나 화면 이동을 일으키는 입력(버튼, 카드)은 처리 중 재진입을 막는다.
- 토큰은 메모리 밖(`localStorage` 등)에 저장하지 않는다.
- 아이콘은 아이콘 라이브러리를 최대한 활용한다. 라이브러리로 해결할 수 없으면 사용자에게 요청한다. 아이콘 SVG를 임의로 만들지 않는다.
- 디자이너가 없다. UI는 구현 전에 사용자와 mock UI를 함께 만들어 확정한 뒤 구현한다. 모션, 로딩 표현, 빈 상태처럼 확정되지 않은 요소는 임의로 정하지 않고 구현마다 사용자와 합의한다.
- 생성된 API 클라이언트 파일은 수동으로 수정하지 않는다.

## Desktop (Rust / Tauri) 불변 규칙

- 자동 메시지 전송을 하지 않는다(Enter 전송, 전송 버튼 클릭, 채팅 API 호출 금지).
- 클립보드 기록 성공을 확인한 뒤에만 붙여넣기를 시도한다. 이벤트 주입 성공을 "삽입 성공"으로 단정하지 않는다. 실패하거나 불확실하면 복사된 상태를 유지하고 수동 붙여넣기를 안내한다.
- OS 연동은 플랫폼별 어댑터(Windows, macOS)로 격리하고, 유스케이스는 trait 뒤의 fake로 테스트한다.
- Tauri command는 좁은 범위의 유스케이스만 노출한다. capability 권한을 최소화하고 WebView에서 임의 파일 접근과 셸 실행을 허용하지 않는다.
- 로그에 토큰, 사용자 텍스트, 클립보드 내용을 남기지 않는다.
- 리프레시 토큰은 OS 보안 저장소에만 저장한다. 평문 파일에 저장하지 않는다.
- 다른 앱의 입력을 감시하는 기능(키 후킹, 타이핑 감지)을 만들지 않는다.
- 검증하지 않은 대상 앱을 "지원됨"으로 표시하지 않는다. 지원 등급은 VERIFIED / FALLBACK_ONLY / UNSUPPORTED로 기록한다.

## Admin (Thymeleaf) 불변 규칙

- 모든 상태 변경은 POST/PATCH/DELETE + CSRF로 한다. GET으로 상태를 바꾸지 않는다.
- 공개 여부 같은 상태는 체크박스로 DB를 직접 바꾸지 않고 항상 전용 서비스(승인, 숨김 등)를 거친다.
- 사용자 입력은 Thymeleaf 이스케이프 기본(`th:text`)을 유지하고 `th:utext`를 쓰지 않는다.
- 설정 변경은 리비전을 남기고, 승인·반려·제재 등 처리 행위는 감사 로그를 남긴다.
- `ROLE_ADMIN` 검사는 컨트롤러가 아니라 보안 설정의 경로 단위로 건다.

## Secrets

- 클라이언트에 넣은 값은 추출할 수 있다고 가정한다. 클라이언트 설정에는 API base URL과 공개 클라이언트 ID만 둔다. Google client secret, R2 키 같은 서버 시크릿은 클라이언트에 넣지 않는다.
- 저장소는 public이다. 시크릿은 GitHub Secrets와 서버 환경에만 둔다.
- 시크릿 파일은 읽을 수 있어도 값을 커밋·로그·PR·대화에 노출하지 않는다. 값이 필요한 작업은 키 이름과 위치만 만들고 값은 사용자가 채운다. 노출되면 즉시 알리고 폐기(rotate)한다.
- 새 설정 키를 추가하면 `application.yml` 주석 또는 클라이언트 설정 정의와 README 안내를 함께 갱신한다.

## Review expectations

- 확인 항목: 계층·의존 방향, 트랜잭션 경계, 동시성(unique 위반·잠금), 시크릿 노출, 에러 코드와 Docs 일치, 중복된 횡단 처리, 테스트가 핵심 도메인 로직을 덮는지.
- block 기준: 시크릿 노출, force git, `LayerDependencyTest` 완화, 적용된 마이그레이션 수정, 보안 경로 누락.

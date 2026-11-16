# Changelog

**현재 버전:** 0.2.1  
**마지막 업데이트:** 2026-11-16T06:09:27Z  

---

## [0.2.1] - 2026-11-16

**📝 문서**
- Website_로그인_제거에_따른_인증_코드_단순화 — 규칙의 OpenAPI 클라이언트 생성 대상을 Desktop으로 한정
- Website_로그인_제거에_따른_인증_코드_단순화 — 규칙의 Website 로그인·쿠키·CORS 항목을 Desktop 전용으로 변경

**♻️ 리팩토링**
- Website_로그인_제거에_따른_인증_코드_단순화 — Website 출처 CORS 설정 제거
- Website_로그인_제거에_따른_인증_코드_단순화 — 클라이언트 종류 구분과 client_type 컬럼 제거
- Website_로그인_제거에_따른_인증_코드_단순화 — 리프레시 토큰을 요청·응답 본문으로만 주고받도록 쿠키 처리 제거

---

## [0.2.0] - 2026-11-16

**✨ 기능**
- Google_로그인과_JWT_발급_API_추가 — 인증 API 컨트롤러와 리프레시 쿠키 처리 추가
- Google_로그인과_JWT_발급_API_추가 — JWT 인증 필터와 보안 설정 추가
- Google_로그인과_JWT_발급_API_추가 — 로그인·갱신·로그아웃 서비스와 요청 DTO 추가
- Google_로그인과_JWT_발급_API_추가 — Firebase ID 토큰 신원 검증 추가
- Google_로그인과_JWT_발급_API_추가 — JWT 액세스·리프레시 토큰 발급과 검증 추가
- Google_로그인과_JWT_발급_API_추가 — 로그인 세션 엔티티와 회원·세션 마이그레이션 추가
- Google_로그인과_JWT_발급_API_추가 — 회원 엔티티와 상태 추가
- Google_로그인과_JWT_발급_API_추가 — 인증 에러 코드 7개 추가

**🐛 수정**
- Google_로그인과_JWT_발급_API_추가 — Bearer 인증 스킴을 대소문자 구분 없이 처리
- Google_로그인과_JWT_발급_API_추가 — 다른 기기의 현재 토큰으로 대상 기기 세션이 삭제되지 않도록 수정

**📝 문서**
- 서비스명을 짤픽(JjalPick)으로 확정
- README 서비스명을 짤픽(JjalPick)으로 변경

**♻️ 리팩토링**
- Google_로그인과_JWT_발급_API_추가 — 배포 프로젝트 이름과 NAS 경로를 jjalpick으로 변경
- Google_로그인과_JWT_발급_API_추가 — Website 제목을 짤픽으로 변경
- Google_로그인과_JWT_발급_API_추가 — Desktop 앱 이름과 식별자를 짤픽·jjalpick으로 변경
- Google_로그인과_JWT_발급_API_추가 — Gradle 프로젝트 이름과 Docker jar 이름을 jjalpick으로 변경
- Google_로그인과_JWT_발급_API_추가 — 백엔드 패키지와 애플리케이션 클래스를 jjalpick으로 변경

**🔧 변경사항**
- 인증 의존성과 설정 키 추가

---

## [0.1.0] - 2026-11-14

**✨ 기능**
- 시각을 KST 오프셋 ISO-8601로 직렬화하는 설정 추가
- BaseEntity와 Clock, JPA Auditing 설정 추가
- 공통 에러 코드와 전역 예외 처리 추가

**🐛 수정**
- desktop prettier·eslint 무시 경로와 Rust 포맷 정리

**📝 문서**
- 배포·버전 규칙을 규칙 문서에 반영
- 검증된 실행 명령을 규칙 문서에 반영

**✅ 테스트**
- 계층 의존 규칙과 도메인 순환 금지 테스트 추가
- 에포크 값 요청이 400으로 거절되는 HTTP 수준 테스트 추가

**🔧 변경사항**
- desktop 설치 파일 빌드·릴리스 파이프라인 추가
- desktop 검증 워크플로우 추가
- website CI/CD를 pnpm·Node 24 기준으로 수정
- website 정적 사이트 Docker 이미지 설정 추가
- 백엔드 배포 안정성 보강(재시작 정책, 헬스체크, 이미지 정리)
- PR 미리보기 워크플로우 삭제
- project-auto-wizard 워크플로우와 버전 설정 추가
- desktop tauri-plugin-log 및 log 의존성 제거
- desktop 프로젝트 골격 추가
- website 프로젝트 골격 추가
- backend 프로젝트 골격 추가
- 저장소 루트 파일과 규칙 문서 정리

---


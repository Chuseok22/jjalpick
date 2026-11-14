# Changelog

**현재 버전:** 0.1.0  
**마지막 업데이트:** 2026-11-14T08:55:28Z  

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


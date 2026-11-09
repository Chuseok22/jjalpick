# Delivery And Review

## Report format

- report 단계에서 반드시 포함할 섹션: 변경 목적, 변경 파일, 위험 요소, 검증 결과, 남은 이슈.
- 저장 위치와 이름: `.report/[YYYYMMDD]_[ISSUE#]_[간단한설명].md` (`.report/`는 gitignore, 없으면 생성). 예: `20261109_#12_즐겨찾기_한도_검사.md`. 날짜는 작업 완료 시점, 설명의 단어 구분은 언더스코어.
- 작성 원칙
  - 작성자/작성일 필드를 넣지 않는다(파일명에 날짜가 있다).
  - AI 도구명(Claude, GPT, Copilot, Cursor 등)을 언급하지 않는다.
  - 능동태, 키워드 기반 문장으로 쓴다. 예: "주문 저장 실패 시 에러 모달 노출 처리 누락 확인. onError 핸들러 추가로 수정"
  - 토큰·비밀번호·API Key 같은 민감 정보는 `{TOKEN}`, `{API_KEY}`, `{PASSWORD}`로 마스킹한다.
  - 실제로 검증한 것만 성공으로 적고, 못 한 검증은 명시한다. 위험 요소가 없으면 "없음"이라고 쓴다.
- 구조

  ```markdown
  ### 📌 작업 개요
  ### 🎯 구현 목표 (기능 구현) 또는 🔍 문제 분석 (버그 수정)
  ### ✅ 구현 내용   (변경사항마다 파일 / 변경 내용 / 이유)
  ### 🔧 주요 변경사항 상세
  ### 🧪 검증 결과   (lint, build, 테스트 결과. PASS/FAIL 명시)
  ### ⚠️ 위험 요소
  ### 📌 남은 이슈
  ```

- 분석 절차: `git status`를 한 번만 실행해 변경 파일을 확인하고, 이슈와 관련된 파일만 골라 직접 읽어 분석한다. 이후 추가 git 명령은 최소화한다.

## Branch and commit

- 전략: `main` 단일 브랜치(`develop` 없음). 작업 위치(이슈 브랜치, 수동 브랜치, `main` 직접 작업)는 매번 사용자 지시를 따르고, 지시가 없으면 현재 브랜치에서 작업한다. 새 브랜치 이름은 `YYYYMMDD_#이슈번호_제목` 또는 `<type>/<설명>` 중 자유.
- 커밋 형식: `<type>: <한국어 설명>` (type: feat, fix, refactor, docs, test, chore, perf, ci). 스테이징은 경로를 지정한다(`git add <경로>`, `git add -A`/`.` 금지).
- 권한: 하위 브랜치에서 commit, push, PR 생성은 요청 없이도 할 수 있다. `main`에는 사용자가 그 순간 명시적으로 지시했을 때만 push한다(직접 푸시, PR 병합 모두).
- 금지: `push --force`/`-f`/`--force-with-lease`, `git add -f`(`.gitignore` 우회 포함), `checkout -f`, `clean -f`, `branch -D`, `reset --hard`, 공유 이력의 `commit --amend`, `--no-verify`, `--no-gpg-sign`. 사용자가 그 순간 정확히 그 작업을 요청한 경우만 예외이고 서브에이전트에도 적용한다. `.gitignore`로 제외된 파일은 제외된 상태를 유지하고, 추적이 필요해 보이면 먼저 사용자에게 확인한다.

## Pull request expectations

- 제목: `[브랜치명] : [타입] : [설명] [이슈링크]` (타입: feat, fix, refactor, chore, docs).
- 본문: 변경 요약, 테스트/검증 결과, 확인이 필요한 사항. `.report/` 보고서가 있으면 그 내용을 바탕으로 쓴다. `Closes #N`으로 이슈를 연결한다.
- reviewer: 정의하지 않았다.
- PR 전에 해당 변경 범위의 검증 명령을 통과시킨다(`30-testing-and-verification.md`).

## Delivery constraints

- 배포·버전 규칙은 아직 정하지 않았다. GitHub Actions 워크플로우를 만든 뒤 실제 모습에 맞춰 추가한다.

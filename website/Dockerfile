# 빌드된 정적 파일(dist)을 nginx로 서빙한다. 컨테이너 내부 포트는 3000이다.
# dist는 이미지 안에서 빌드하지 않는다. CI/CD 워크플로우(PROJECT-REACT-CICD)가 먼저 `pnpm build`로 만든다.
# 로컬에서 직접 빌드할 때도 `pnpm build`를 먼저 실행해야 한다.
FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO /dev/null http://127.0.0.1:3000/ || exit 1

# 1. 경량 웹 서버인 Nginx의 최신 stable 버전을 사용합니다.
FROM nginx:stable-alpine

# 2. 컨테이너 내부의 기본 Nginx 정적 파일 경로에 로컬 frontend 폴더의 내용을 복사합니다.
# Docker Compose의 'context: ./frontend' 덕분에 이 경로는 로컬의 frontend 폴더를 가리킵니다.
COPY . /usr/share/nginx/html

# 3. Nginx는 기본적으로 80 포트를 사용합니다. (노출)
EXPOSE 80
# 1. 베이스 이미지: Node.js 20 버전의 경량(alpine) 이미지를 사용합니다.
FROM node:20-alpine

# 2. 작업 디렉토리 설정: 컨테이너 내부에서 코드가 위치할 경로를 지정합니다.
WORKDIR /usr/src/app

# 3. 코드 복사 전, 의존성 설치 파일을 먼저 복사하여 Docker 캐시를 활용합니다.
COPY package*.json ./

# 4. 의존성 설치: 운영 환경에서는 개발 의존성(Jest 등)을 제외하고 설치합니다.
RUN npm install --omit=dev

VOLUME /usr/src/app/node_modules

# 5. 나머지 모든 소스 코드 복사
# .dockerignore 파일을 사용하면 git, .env 등 불필요한 파일을 제외할 수 있습니다.
COPY . .

# 6. 애플리케이션 포트 노출 (정보 제공용)
# 실제 외부 접근을 위해서는 방화벽 설정이 필요합니다.
EXPOSE 8080

# 7. 컨테이너가 시작될 때 실행할 명령어 (서버 실행)
# Node.js 서버 파일 이름인 server.js를 실행하도록 설정합니다.
CMD [ "node", "server.js" ]
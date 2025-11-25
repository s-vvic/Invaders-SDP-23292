🐳 개발 환경 설정 가이드 (Docker Compose)

이 문서는 팀원 간의 환경 불일치 문제를 해결하고, CI/CD 파이프라인의 안정성을 보장하기 위해 Docker Compose를 사용하여 백엔드(Node.js) 개발 환경을 설정하는 방법을 안내합니다.

1. 전제 조건

개발을 시작하기 전에 다음 도구가 로컬 컴퓨터에 설치되어 있어야 합니다.

Git: 소스 코드 관리를 위해 필요합니다.

Docker Desktop: Docker 컨테이너를 관리하고 실행하기 위해 필요합니다. (설치 후 실행 상태 유지 필수)

2. 환경 변수 설정 (필수)

보안을 위해 민감한 정보는 Git에 커밋하지 않습니다. 각 팀원은 로컬 환경에서 .env 파일을 설정해야 합니다.

파일 복사: backend 폴더 내에 있는 .env.example 파일을 복사하여 .env 파일로 이름을 변경합니다.

값 입력: .env 파일을 열고 JWT_SECRET을 비롯한 모든 필수 환경 변수 항목에 로컬 개발에 사용할 비밀 값을 입력합니다.

# backend/.env (예시)
JWT_SECRET=your_local_secret_key_for_dev
DB_USER=dev_user
DB_PASSWORD=dev_password


3. Docker Compose를 사용한 환경 실행

프로젝트 루트 폴더에서 다음 명령어를 실행하여 백엔드 서버와 데이터베이스 컨테이너를 한 번에 빌드하고 실행합니다.

3.1. 최초 실행 및 이미지 빌드

최초 실행 시, 또는 package.json이나 Dockerfile을 수정한 후에는 반드시 이미지를 새로 빌드해야 합니다.

# 이미지를 빌드하고 백그라운드(-d)로 컨테이너 실행
docker compose up --build -d


3.2. 일반적인 컨테이너 실행

이미 빌드된 이미지를 사용하여 컨테이너만 실행할 때 사용합니다.

docker compose up -d


3.3. 컨테이너 상태 확인

컨테이너가 정상적으로 실행 중인지 확인합니다.

docker compose ps


3.4. 컨테이너 로그 확인

백엔드 서버의 출력 로그(오류 등)를 실시간으로 확인합니다.

docker compose logs -f backend


4. 환경 종료 및 정리

4.1. 컨테이너 정지 (Stop)

컨테이너를 정지합니다. (데이터 및 네트워크는 유지)

docker compose stop


4.2. 컨테이너 및 네트워크 삭제 (Clean Up)

컨테이너, 네트워크 및 볼륨(데이터)을 완전히 삭제하여 깨끗한 상태로 되돌립니다. 데이터베이스의 데이터도 삭제되므로 주의해서 사용하세요.

# 컨테이너, 네트워크, 익명 볼륨(-v) 모두 삭제
docker compose down -v


5. CI/CD 환경 변수 관리 (참고)

로컬 개발에 사용되는 .env와 달리, GitHub Actions에서 사용하는 JWT_SECRET과 같은 민감 정보는 GitHub Repository Settings > Secrets and variables > Actions에 안전하게 등록되어 있습니다.

CI 파이프라인은 이 Secret 값을 사용하여 컨테이너 테스트를 자동으로 진행합니다.
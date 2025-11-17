# Jira 스토리 및 태스크 정리

## 📋 Epic: 프론트엔드 UI/UX 개선 및 기능 추가

---

## Story 1: 토스트 알림 시스템 구현
**Story Type:** Feature  
**Priority:** High  
**Status:** ✅ Completed  
**Description:** alert() 대신 사용자 친화적인 토스트 알림 시스템 구현

### Tasks:
- [x] 토스트 알림 CSS 스타일 작성 (`toast.css`)
  - 4가지 타입 (success, error, info, warning) 스타일 정의
  - 슬라이드 인/아웃 애니메이션 구현
  - 모바일 반응형 디자인 적용
- [x] 토스트 알림 JavaScript 함수 구현
  - `showToast()` 기본 함수 구현
  - `toastSuccess()`, `toastError()`, `toastInfo()`, `toastWarning()` 편의 함수 구현
  - 자동 사라짐 기능 (에러: 5초, 일반: 3초)
  - 수동 닫기 버튼 기능
- [x] 기존 alert() 호출을 toast로 교체
  - 게임 종료 시뮬레이션 alert → toastSuccess/toastError
  - 세션 만료 alert → toastWarning
  - API 에러 처리에 toast 적용

### Acceptance Criteria:
- ✅ alert() 대신 토스트 알림이 표시됨
- ✅ 4가지 타입의 토스트가 올바르게 표시됨
- ✅ 토스트가 자동으로 사라지거나 수동으로 닫을 수 있음
- ✅ 모바일에서도 정상 작동함

---

## Story 2: 사용자 통계 대시보드 추가
**Story Type:** Feature  
**Priority:** High  
**Status:** ✅ Completed  
**Description:** 사용자의 게임 통계 및 최근 게임 기록을 표시하는 대시보드 기능 추가

### Tasks:
- [x] 통계 카드 UI 추가
  - Total Games 카드 추가
  - Average Score 카드 추가
  - Rank 카드 추가
- [x] Recent Games 테이블 구현
  - 최근 10게임 기록 표시
  - 점수 및 날짜 컬럼
  - 테이블 스타일링 (`stats-table`)
- [x] 사용자 통계 데이터 로딩 함수 구현
  - `loadUserStats()` 함수 구현
  - `renderRecentGames()` 함수 구현
  - 목업 데이터 연동 (`user_stats.json`)
- [x] 목업 데이터 생성
  - `frontend/mock_data/user_stats.json` 생성
  - 총 게임 수, 평균 점수, 순위, 최근 게임 기록 포함

### Acceptance Criteria:
- ✅ 대시보드에 통계 카드가 표시됨
- ✅ 최근 게임 기록이 테이블로 표시됨
- ✅ 목업 데이터가 올바르게 로드되어 표시됨
- ✅ 실제 API 연동 준비 완료

---

## Story 3: 리더보드 검색/필터 기능 추가
**Story Type:** Feature  
**Priority:** High  
**Status:** ✅ Completed  
**Description:** 리더보드에서 사용자명으로 검색 및 필터링 기능 추가

### Tasks:
- [x] 검색 입력 UI 추가
  - 리더보드 헤더에 검색 입력창 추가
  - 검색어 지우기 버튼 추가
  - 검색 입력 스타일링
- [x] 검색 필터 로직 구현
  - `filterLeaderboard()` 함수 구현
  - 대소문자 구분 없는 검색
  - 실시간 필터링 (input 이벤트)
- [x] 검색 상태 관리
  - `currentScores` 객체로 원본 데이터 저장
  - Overall/Weekly/Yearly 탭별 데이터 관리
  - 필터링된 결과만 표시
- [x] 검색 기능 통합
  - 리더보드 로딩 시 원본 데이터 저장
  - 검색어 입력 시 즉시 필터링 적용
  - 검색어 지우기 기능

### Acceptance Criteria:
- ✅ 리더보드에 검색 입력창이 표시됨
- ✅ 사용자명으로 실시간 검색이 가능함
- ✅ Overall/Weekly/Yearly 모든 탭에서 검색 작동
- ✅ 검색어를 지울 수 있음

---

## Story 4: UI/UX 개선 - 로딩 스켈레톤 및 애니메이션
**Story Type:** Enhancement  
**Priority:** High  
**Status:** ✅ Completed  
**Description:** 로딩 상태를 스켈레톤 UI로 표시하고, 숫자 카운트업 및 페이드인 애니메이션 추가

### Tasks:
- [x] 로딩 스켈레톤 UI 구현
  - `skeleton.css` 파일 생성
  - 스켈레톤 애니메이션 (shimmer 효과)
  - 테이블 스켈레톤 행 생성 함수 (`generateSkeletonTableRows()`)
  - 리더보드 로딩 시 스켈레톤 표시
- [x] 숫자 카운트업 애니메이션 구현
  - `animateNumber()` 함수 구현
  - `animateValue()` 함수 구현
  - Easing 함수 적용 (ease-out)
  - High Score, Total Games, Average Score에 적용
- [x] 카드 페이드인 애니메이션 추가
  - 대시보드 카드 fadeInUp 애니메이션
  - Stagger 효과 (순차적 등장)
  - 리더보드 테이블 행 페이드인
  - Recent Games 테이블 행 슬라이드 인

### Acceptance Criteria:
- ✅ 로딩 중 스켈레톤 UI가 표시됨
- ✅ 숫자 값이 부드럽게 카운트업됨
- ✅ 카드와 테이블 행이 순차적으로 나타남
- ✅ 모든 애니메이션이 부드럽게 작동함

---

## Story 5: 리더보드 표 형식 개선
**Story Type:** Enhancement  
**Priority:** Medium  
**Status:** ✅ Completed  
**Description:** 리더보드를 텍스트 리스트에서 표 형식으로 변경하여 가독성 향상

### Tasks:
- [x] HTML 구조 변경
  - `<ol>` 리스트를 `<table>`로 변경
  - 테이블 헤더 추가 (순위, 사용자명, 점수, 날짜)
- [x] 테이블 스타일링
  - 헤더 배경색 및 정렬 설정
  - 행 호버 효과
  - 컬럼별 너비 및 정렬
- [x] JavaScript 렌더링 로직 변경
  - `<li>` 대신 `<tr>`, `<td>` 생성
  - 각 데이터를 별도 셀로 분리
  - Overall, Weekly, Yearly 모두 적용

### Acceptance Criteria:
- ✅ 리더보드가 표 형식으로 표시됨
- ✅ 각 정보가 컬럼으로 구분되어 표시됨
- ✅ 모든 리더보드 탭에서 표 형식 적용됨

---

## Story 6: 프론트엔드 코드 리팩토링
**Story Type:** Technical Debt  
**Priority:** Medium  
**Status:** ✅ Completed  
**Description:** 중복 코드 제거 및 코드 구조 개선

### Tasks:
- [x] 리더보드 로딩 함수 통합
  - `loadLeaderboardData()` 공통 함수 생성
  - 3개 함수를 1개로 통합 (약 200줄 → 40줄)
- [x] 유틸리티 함수 생성
  - `formatDate()` - 날짜 포맷팅
  - `getRankEmoji()` - 순위 이모지
  - `createLeaderboardRow()` - 테이블 행 생성
  - `renderLeaderboardTable()` - 테이블 렌더링
- [x] 뷰 관리 개선
  - `showView()` 공통 함수로 통합
  - 중복된 classList 코드 제거
- [x] 사용하지 않는 코드 제거
  - `renderScores()` 함수 제거
- [x] 인라인 스타일 제거
  - CSS 클래스로 변경
  - `.empty-message`, `.loading-message`, `.error-message-cell` 추가

### Acceptance Criteria:
- ✅ 코드 중복이 제거됨
- ✅ 함수 재사용성이 향상됨
- ✅ 코드 가독성이 개선됨
- ✅ 유지보수성이 향상됨

---

## 📊 작업 통계

### 완료된 스토리: 6개
### 완료된 태스크: 25개
### 추가된 파일: 4개
- `frontend/toast.css`
- `frontend/skeleton.css`
- `frontend/mock_data/user_stats.json`
- `JIRA_STORIES.md` (이 파일)

### 개선된 파일: 5개
- `frontend/index.html`
- `frontend/main.js`
- `frontend/dashboard.css`
- `frontend/api.js`
- `frontend/base.css`

---

## 🎯 다음 우선순위 작업

### High Priority:
1. 반응형 디자인 개선 (모바일 최적화)
2. 리더보드 페이지네이션 구현
3. 접근성 개선 (키보드 네비게이션, ARIA)

### Medium Priority:
4. 게임 히스토리 상세 페이지
5. 사용자 프로필 페이지
6. 점수 추이 그래프

---

## 📝 참고사항

- 모든 기능이 목업 데이터로 동작하며, 실제 API 연동 준비 완료
- 코드는 재사용 가능하도록 모듈화되어 있음
- 애니메이션은 성능 최적화를 위해 `requestAnimationFrame` 사용
- 모든 스타일은 CSS 변수를 사용하여 일관성 유지


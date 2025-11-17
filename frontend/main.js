document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const registerView = document.getElementById('register-view');

    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const loginError = document.getElementById('login-error');
    const registerError = document.getElementById('register-error');

    const logoutBtn = document.getElementById('logout-btn');
    const viewLeaderboardBtn = document.getElementById('view-leaderboard-btn');
    const backToDashboardBtn = document.getElementById('back-to-dashboard-btn');
    const showRegisterBtn = document.getElementById('show-register-btn');
    const showLoginBtn = document.getElementById('show-login-btn');

    const welcomeMessage = document.getElementById('welcome-message');
    const highScoreEl = document.getElementById('high-score');
    const goldEl = document.getElementById('gold');
    const upgradesListEl = document.getElementById('upgrades-list');
    const achievementsListEl = document.getElementById('achievements-list');
    const leaderboardView = document.getElementById('leaderboard-view');
    const leaderboardListEl = document.getElementById('leaderboard-list');
    const simulateGameOverBtn = document.getElementById('simulate-game-over-btn');

    // --- API Configuration ---
    const API_BASE_URL = 'http://localhost:8080/api';
    const USE_MOCK_API = true; // Set to false to use real API

    // --- Loading & Error Handling Utilities ---
    let loadingOverlay = null;

    function createLoadingOverlay() {
        if (loadingOverlay) return loadingOverlay;
        
        loadingOverlay = document.createElement('div');
        loadingOverlay.className = 'loading-overlay';
        loadingOverlay.innerHTML = `
            <div class="loading-overlay-content">
                <div class="loading-spinner"></div>
                <p>Loading...</p>
            </div>
        `;
        document.body.appendChild(loadingOverlay);
        return loadingOverlay;
    }

    function showLoading(message = 'Loading...') {
        const overlay = createLoadingOverlay();
        const messageEl = overlay.querySelector('p');
        if (messageEl) messageEl.textContent = message;
        overlay.classList.add('active');
    }

    function hideLoading() {
        if (loadingOverlay) {
            loadingOverlay.classList.remove('active');
        }
    }

    function showError(element, message) {
        if (!element) return;
        element.textContent = message;
        element.classList.remove('success-message');
        element.classList.add('error-message');
        // Auto-hide after 5 seconds
        setTimeout(() => {
            if (element.textContent === message) {
                element.textContent = '';
            }
        }, 5000);
    }

    function showSuccess(element, message) {
        if (!element) return;
        element.textContent = message;
        element.classList.remove('error-message');
        element.classList.add('success-message');
        // Auto-hide after 3 seconds
        setTimeout(() => {
            if (element.textContent === message) {
                element.textContent = '';
            }
        }, 3000);
    }

    function clearMessage(element) {
        if (element) {
            element.textContent = '';
            element.classList.remove('error-message', 'success-message');
        }
    }

    function setButtonLoading(button, isLoading) {
        if (!button) return;
        button.disabled = isLoading;
        if (isLoading) {
            button.dataset.originalText = button.textContent;
            button.innerHTML = '<span class="loading-spinner"></span> ' + button.dataset.originalText;
        } else {
            button.textContent = button.dataset.originalText || button.textContent;
            delete button.dataset.originalText;
        }
    }

    function getErrorMessage(error) {
        if (typeof error === 'string') return error;
        if (error.error) return error.error;
        if (error.message) return error.message;
        if (error.status === 401) return '인증에 실패했습니다. 사용자 이름과 비밀번호를 확인해주세요.';
        if (error.status === 404) return '요청한 리소스를 찾을 수 없습니다.';
        if (error.status === 409) return '이미 사용 중인 사용자 이름입니다.';
        if (error.status === 500) return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        if (error.status >= 500) return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        if (error.status >= 400) return '잘못된 요청입니다. 입력 정보를 확인해주세요.';
        return '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    }

    // --- Mock API ---
    function mockLogin(username, password) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test" && password === "1234") {
                    resolve({
                        token: "fake-jwt-token-for-testing",
                        username: "test"
                    });
                }
                else reject({ error: "Invalid username or password" });
            }, 300);
        });
    }

    function mockRegister(username, password) {
        console.log(`[Mock API] Register attempt for: ${username}`);
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test") {
                    reject({ error: "Username 'test' is already taken." });
                } else {
                    resolve({ message: "Account created successfully!" });
                }
            }, 500);
        });
    }

    function mockGetDashboardData(token) {
        return fetch('./mock_data/dashboard.json').then(res => res.json());
    }

    function mockGetLeaderboard() {
        return fetch('./mock_data/leaderboard.json').then(res => res.json());
    }

    // --- App Logic & View Management ---
    async function loadDashboard() {
        try {
            const username = localStorage.getItem('invaders_username');
            const userId = localStorage.getItem('invaders_userId');

            if (username) {
                welcomeMessage.textContent = `Welcome, ${username}!`;
            } else {
                welcomeMessage.textContent = 'Welcome!';
            }

            // Fetch user's specific data (including max_score)
            const response = await fetch(`${API_BASE_URL}/users/${userId}`);
            if (!response.ok) {
                throw { status: response.status, error: `HTTP error! status: ${response.status}` };
            }
            const userData = await response.json();

            highScoreEl.textContent = userData.max_score || 0;
            // goldEl.textContent = data.gold; // We don't have gold in our user data yet
            // upgradesListEl // Not implemented yet
            // achievementsListEl // Not implemented yet

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            welcomeMessage.textContent = 'Welcome!'; // Fallback
            highScoreEl.textContent = 'Error';
            // Show error notification (optional - could add a notification system)
        }
    }
    async function loadLeaderboard() {
        leaderboardListEl.innerHTML = '<li>로딩 중...</li>'; // Show loading state
        try {
            showLoading('리더보드 불러오는 중...');
            
            let scores;
            
            if (USE_MOCK_API) {
                // Use mock data
                const mockData = await mockGetLeaderboard();
                // Convert mock data format to server format
                scores = mockData.map((item, index) => ({
                    username: item.username,
                    score: item.score,
                    created_at: new Date().toISOString() // Use current date for mock data
                }));
            } else {
                // Use real API
                const response = await fetch(`${API_BASE_URL}/scores`); 
                
                if (!response.ok) {
                    throw { status: response.status, error: `HTTP error! status: ${response.status}` };
                }
                
                scores = await response.json();
            }

            leaderboardListEl.innerHTML = ''; // Clear loading message

            if (scores.length === 0) {
                leaderboardListEl.innerHTML = '<li>아직 기록된 점수가 없습니다.</li>';
                hideLoading();
                return;
            }

            // 정렬이 필요 없습니다. (서버에서 이미 ORDER BY s.score DESC 로 정렬함)
            // 또는 목업 데이터는 이미 정렬되어 있음

            // 받아온 점수 기록(record)을 <li> 항목으로 만듭니다.
            scores.forEach((record, index) => {
                const listItem = document.createElement('li');
                
                // 날짜 포맷을 보기 좋게 변경합니다. (예: 2025. 11. 9. 오후 9:30:00)
                const gameDate = record.created_at 
                    ? new Date(record.created_at).toLocaleString('ko-KR')
                    : '날짜 없음';
                
                // Add ranking
                const rank = index + 1;
                const rankEmoji = rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : `${rank}.`;
                
                listItem.textContent = `${rankEmoji} ${record.username}: ${record.score.toLocaleString()} 점${record.created_at ? ` (${gameDate})` : ''}`;
                leaderboardListEl.appendChild(listItem);
            });

            hideLoading();
        } catch (error) {
            console.error('Error loading leaderboard:', error);
            leaderboardListEl.innerHTML = `<li>점수판을 불러오는 데 실패했습니다: ${getErrorMessage(error)}</li>`;
            hideLoading();
        }
    }

    function showLoginView() {
        loginView.classList.remove('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.add('hidden');
        registerView.classList.add('hidden');
    }

    function showRegisterView() {
        loginView.classList.add('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.add('hidden');
        registerView.classList.remove('hidden');
    }

    function showDashboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        leaderboardView.classList.add('hidden');
        registerView.classList.add('hidden');
        loadDashboard();
    }

    function showLeaderboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.remove('hidden');
        registerView.classList.add('hidden');
        const username = localStorage.getItem('invaders_username');
        if (username) {
            welcomeMessage.textContent = `Welcome, ${username}!`;
        } else {
            welcomeMessage.textContent = 'Welcome!';
        }
        loadLeaderboard();
    }

    function logout() {
        localStorage.removeItem('invaders_token');
        localStorage.removeItem('invaders_username');
        showLoginView();
    }

    // --- Event Listeners & Initial Execution ---

    showRegisterBtn.addEventListener('click', showRegisterView);
    showLoginBtn.addEventListener('click', showLoginView);
    logoutBtn.addEventListener('click', logout);
    viewLeaderboardBtn.addEventListener('click', showLeaderboardView);
    backToDashboardBtn.addEventListener('click', showDashboardView);

    simulateGameOverBtn.addEventListener('click', async (e) => {
        e.preventDefault(); // 기본 동작 방지

        const userId = localStorage.getItem('invaders_userId');
        const username = localStorage.getItem('invaders_username');

        if (!userId) {
            alert('로그인된 사용자 정보가 없습니다. 먼저 로그인해주세요.');
            return;
        }

        // 100에서 10000 사이의 랜덤 점수 생성
        const randomScore = Math.floor(Math.random() * (10000 - 100 + 1)) + 100;

        setButtonLoading(simulateGameOverBtn, true);
        showLoading('점수 업데이트 중...');

        try {
            const response = await fetch(`${API_BASE_URL}/users/${userId}/score`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ score: randomScore }),
            });

            const data = await response.json();

            if (!response.ok) {
                throw { status: response.status, error: data.error || '점수 업데이트 실패' };
            }

            hideLoading();
            alert(`${username}님, 게임 종료! 점수: ${randomScore.toLocaleString()}. ${data.message}`);

            // 대시보드 정보를 새로고침하여 최고 점수 업데이트 반영
            await loadDashboard();

        } catch (error) {
            console.error('게임 종료 시뮬레이션 중 오류 발생:', error);
            hideLoading();
            alert(`점수 업데이트 중 오류 발생: ${getErrorMessage(error)}`);
        } finally {
            setButtonLoading(simulateGameOverBtn, false);
        }
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitButton = loginForm.querySelector('button[type="submit"]');
        const { username, password } = loginForm;
        
        // Clear previous errors
        clearMessage(loginError);
        
        // Validate inputs
        if (!username.value.trim()) {
            showError(loginError, '사용자 이름을 입력해주세요.');
            return;
        }
        if (!password.value) {
            showError(loginError, '비밀번호를 입력해주세요.');
            return;
        }

        setButtonLoading(submitButton, true);
        showError(loginError, '로그인 중...');

        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: username.value, password: password.value })
            });

            const data = await response.json();
            
            if (!response.ok || data.error) {
                throw { status: response.status, error: data.error || '로그인에 실패했습니다.' };
            }

            localStorage.setItem('invaders_token', data.token);
            localStorage.setItem('invaders_username', data.user.username);
            localStorage.setItem('invaders_userId', data.user.id);
            
            clearMessage(loginError);
            showSuccess(loginError, '로그인 성공!');
            
            // Small delay to show success message
            setTimeout(() => {
                showDashboardView();
            }, 500);

        } catch (error) {
            console.error('Login error:', error);
            showError(loginError, getErrorMessage(error));
        } finally {
            setButtonLoading(submitButton, false);
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitButton = registerForm.querySelector('button[type="submit"]');
        const username = document.getElementById('reg-username').value.trim();
        const password = document.getElementById('reg-password').value;
        const confirmPassword = document.getElementById('reg-confirm-password').value;

        // Clear previous errors
        clearMessage(registerError);

        // Validate inputs
        if (!username) {
            showError(registerError, '사용자 이름을 입력해주세요.');
            return;
        }
        if (username.length < 3) {
            showError(registerError, '사용자 이름은 최소 3자 이상이어야 합니다.');
            return;
        }
        if (!password) {
            showError(registerError, '비밀번호를 입력해주세요.');
            return;
        }
        if (password.length < 4) {
            showError(registerError, '비밀번호는 최소 4자 이상이어야 합니다.');
            return;
        }
        if (password !== confirmPassword) {
            showError(registerError, '비밀번호가 일치하지 않습니다.');
            return;
        }

        setButtonLoading(submitButton, true);
        showError(registerError, '회원가입 중...');

        try {
            const response = await fetch(`${API_BASE_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (!response.ok || data.error) {
                throw { status: response.status, error: data.error || '회원가입에 실패했습니다.' };
            }

            clearMessage(registerError);
            showSuccess(registerError, '회원가입이 완료되었습니다! 로그인 페이지로 이동합니다...');
            
            // Go to login page after successful registration
            setTimeout(() => {
                showLoginView();
                // Pre-fill username in login form
                document.getElementById('username').value = username;
            }, 1500);

        } catch (error) {
            console.error('Registration error:', error);
            showError(registerError, getErrorMessage(error));
        } finally {
            setButtonLoading(submitButton, false);
        }
    });

    // Initial check on page load
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});
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
    const USE_MOCK_API = false; // Set to false to use real API

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
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const userData = await response.json();

            highScoreEl.textContent = userData.max_score;
            // goldEl.textContent = data.gold; // We don't have gold in our user data yet
            // upgradesListEl // Not implemented yet
            // achievementsListEl // Not implemented yet

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            welcomeMessage.textContent = 'Welcome!'; // Fallback
            highScoreEl.textContent = 'Error';
        }
    }
    async function loadLeaderboard() {
        leaderboardListEl.innerHTML = ''; // Clear previous list
        try {
            const response = await fetch(`${API_BASE_URL}/users`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const users = await response.json();

            if (users.length === 0) {
                leaderboardListEl.innerHTML = '<li>No users found.</li>';
                return;
            }

            users.sort((a, b) => b.max_score - a.max_score); // Sort by max_score descending

            users.forEach(user => {
                const listItem = document.createElement('li');
                listItem.textContent = `${user.username}: ${user.max_score}`;
                leaderboardListEl.appendChild(listItem);
            });
        } catch (error) {
            console.error('Error loading leaderboard:', error);
            leaderboardListEl.innerHTML = '<li>Failed to load leaderboard.</li>';
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
                throw new Error(data.error || '점수 업데이트 실패');
            }

            alert(`${username}님, 게임 종료! 점수: ${randomScore}. ${data.message}`);

            // 대시보드 정보를 새로고침하여 최고 점수 업데이트 반영
            loadDashboard();

        } catch (error) {
            console.error('게임 종료 시뮬레이션 중 오류 발생:', error);
            alert(`점수 업데이트 중 오류 발생: ${error.message}`);
        }
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = 'Logging in...';
        try {
            const { username, password } = loginForm;
            const data = USE_MOCK_API ? await mockLogin(username.value, password.value) : await (await fetch(`${API_BASE_URL}/login`, { method: 'POST', 
            headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: username.value, password: password.value }) })).json();
            
            if (data.error) {
                throw new Error(data.error);
            }

            localStorage.setItem('invaders_token', data.token);
            localStorage.setItem('invaders_username', data.user.username);
            localStorage.setItem('invaders_userId', data.user.id);
            loginError.textContent = '';
            showDashboardView();
        } catch (error) {
            loginError.textContent = error.error || 'Login failed!';
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        registerError.textContent = 'Registering...';
        const username = document.getElementById('reg-username').value;
        const password = document.getElementById('reg-password').value;
        const confirmPassword = document.getElementById('reg-confirm-password').value;

        if (password !== confirmPassword) {
            registerError.textContent = "Passwords do not match.";
            return;
        }

        try {
            const data = USE_MOCK_API ? await mockRegister(username, password) : await (await fetch(`${API_BASE_URL}/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) })).json();
            alert(data.message); // Show success message
            showLoginView(); // Go to login page after successful registration
        } catch (error) {
            registerError.textContent = error.error || 'Registration failed!';
        }
    });

    // Initial check on page load
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});
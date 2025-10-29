document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const loginForm = document.getElementById('login-form');
    const loginError = document.getElementById('login-error');
    const logoutBtn = document.getElementById('logout-btn');
    const welcomeMessage = document.getElementById('welcome-message');
    const highScoreEl = document.getElementById('high-score');
    const goldEl = document.getElementById('gold');
    const upgradesListEl = document.getElementById('upgrades-list');
    const achievementsListEl = document.getElementById('achievements-list');

    // --- API Configuration ---
    const API_BASE_URL = 'http://localhost:7070/api';
    const USE_MOCK_API = true; // Set to false to use real API

    // --- Mock API ---
    function mockLogin(username, password) {
        console.log(`[Mock API] Login attempt for: ${username}`);
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test" && password === "1234") {
                    resolve({ token: "fake-jwt-token-for-testing" });
                } else {
                    reject({ error: "Invalid username or password" });
                }
            }, 500);
        });
    }

    function mockGetDashboardData(token) {
        console.log(`[Mock API] Dashboard data requested with token: ${token}`);
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (token === "fake-jwt-token-for-testing") {
                    resolve({
                        username: "MockUser",
                        highScore: 99999,
                        gold: 1234,
                        upgrades: [
                            { itemId: "Item_MultiShot", level: 3 },
                            { itemId: "Item_Atkspeed", level: 5 },
                            { itemId: "Item_Penetration", level: 2 },
                        ],
                        achievements: [ "FIRST_GAME", "BOSS_DEFEATED", "NO_MISS_STAGE" ]
                    });
                } else {
                    reject({ error: "Invalid token" });
                }
            }, 800);
        });
    }

    // --- App Logic ---
    async function loadDashboard() {
        const token = localStorage.getItem('invaders_token');
        if (!token) {
            showLoginView();
            return;
        }

        try {
            welcomeMessage.textContent = "Loading...";
            upgradesListEl.innerHTML = '';
            achievementsListEl.innerHTML = '';

            const data = USE_MOCK_API
                ? await mockGetDashboardData(token)
                : await (await fetch(`${API_BASE_URL}/dashboard`, { headers: { 'Authorization': `Bearer ${token}` } })).json();

            // Populate dashboard with data
            welcomeMessage.textContent = `Welcome, ${data.username}!`;
            highScoreEl.textContent = data.highScore;
            goldEl.textContent = data.gold;

            data.upgrades.forEach(item => {
                const li = document.createElement('li');
                li.textContent = `${item.itemId.replace('Item_', '')}: Level ${item.level}`;
                upgradesListEl.appendChild(li);
            });

            data.achievements.forEach(ach => {
                const li = document.createElement('li');
                li.textContent = ach;
                achievementsListEl.appendChild(li);
            });

        } catch (error) {
            console.error('Failed to load dashboard:', error);
            logout(); // If token is invalid or something fails, log out
        }
    }

    function showLoginView() {
        loginView.classList.remove('hidden');
        dashboardView.classList.add('hidden');
    }

    function showDashboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        loadDashboard();
    }

    function logout() {
        localStorage.removeItem('invaders_token');
        showLoginView();
    }

    // --- Event Listeners & Initial Execution ---

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = 'Logging in...';
        const username = loginForm.username.value;
        const password = loginForm.password.value;

        try {
            const data = USE_MOCK_API
                ? await mockLogin(username, password)
                : await (await fetch(`${API_BASE_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) })).json();

            localStorage.setItem('invaders_token', data.token);
            showDashboardView();
            loginError.textContent = '';
        } catch (error) {
            console.error('Login error:', error);
            loginError.textContent = error.error || 'Login failed!';
        }
    });

    logoutBtn.addEventListener('click', logout);

    // Initial check on page load
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});

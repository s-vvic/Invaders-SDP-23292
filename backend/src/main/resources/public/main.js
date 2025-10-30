document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const leaderboardView = document.getElementById('leaderboard-view'); // New

    const loginForm = document.getElementById('login-form');
    const loginError = document.getElementById('login-error');
    const logoutBtn = document.getElementById('logout-btn');
    const viewLeaderboardBtn = document.getElementById('view-leaderboard-btn'); // New
    const backToDashboardBtn = document.getElementById('back-to-dashboard-btn'); // New

    const welcomeMessage = document.getElementById('welcome-message');
    const highScoreEl = document.getElementById('high-score');
    const goldEl = document.getElementById('gold');
    const upgradesListEl = document.getElementById('upgrades-list');
    const achievementsListEl = document.getElementById('achievements-list');
    const leaderboardListEl = document.getElementById('leaderboard-list'); // Relocated

    // --- API Configuration ---
    const API_BASE_URL = 'http://localhost:7070/api';
    const USE_MOCK_API = true; // Set to false to use real API

    // --- Mock API ---
    function mockLogin(username, password) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test" && password === "1234") resolve({ token: "fake-jwt-token-for-testing" });
                else reject({ error: "Invalid username or password" });
            }, 300);
        });
    }

    function mockGetDashboardData(token) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (token === "fake-jwt-token-for-testing") {
                    resolve({
                        username: "MockUser",
                        highScore: 99999,
                        gold: 1234,
                        upgrades: [{ itemId: "Item_MultiShot", level: 3 }, { itemId: "Item_Atkspeed", level: 5 }],
                        achievements: ["FIRST_GAME", "BOSS_DEFEATED"]
                    });
                } else reject({ error: "Invalid token" });
            }, 500);
        });
    }

    function mockGetLeaderboard() {
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    { rank: 1, username: "PlayerOne", score: 150000 },
                    { rank: 2, username: "PlayerTwo", score: 125000 },
                    { rank: 3, username: "MockUser", score: 99999 },
                    { rank: 4, username: "PlayerFour", score: 80000 },
                    { rank: 5, username: "PlayerFive", score: 50000 },
                ]);
            }, 700);
        });
    }

    // --- App Logic & View Management ---

    async function loadDashboard() {
        const token = localStorage.getItem('invaders_token');
        if (!token) { showLoginView(); return; }
        try {
            welcomeMessage.textContent = "Loading...";
            const data = USE_MOCK_API ? await mockGetDashboardData(token) : await (await fetch(`${API_BASE_URL}/dashboard`, { headers: { 'Authorization': `Bearer ${token}` } })).json();
            welcomeMessage.textContent = `Welcome, ${data.username}!`;
            highScoreEl.textContent = data.highScore;
            goldEl.textContent = data.gold;
            upgradesListEl.innerHTML = '';
            data.upgrades.forEach(item => {
                const li = document.createElement('li');
                li.textContent = `${item.itemId.replace('Item_', '')}: Level ${item.level}`;
                upgradesListEl.appendChild(li);
            });
            achievementsListEl.innerHTML = '';
            data.achievements.forEach(ach => {
                const li = document.createElement('li');
                li.textContent = ach;
                achievementsListEl.appendChild(li);
            });
        } catch (error) { console.error('Failed to load dashboard:', error); logout(); }
    }

    async function loadLeaderboard() {
        try {
            leaderboardListEl.innerHTML = '<li>Loading...</li>';
            const data = USE_MOCK_API ? await mockGetLeaderboard() : await (await fetch(`${API_BASE_URL}/leaderboard`)).json();
            leaderboardListEl.innerHTML = '';
            data.forEach(player => {
                const li = document.createElement('li');
                li.innerHTML = `<span>${player.rank}. ${player.username}</span><span>${player.score}</span>`;
                leaderboardListEl.appendChild(li);
            });
        } catch (error) { console.error('Failed to load leaderboard:', error); leaderboardListEl.innerHTML = '<li>Failed to load leaderboard.</li>'; }
    }

    function showLoginView() {
        loginView.classList.remove('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.add('hidden');
    }

    function showDashboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        leaderboardView.classList.add('hidden');
        loadDashboard();
    }

    function showLeaderboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.remove('hidden');
        loadLeaderboard();
    }

    function logout() {
        localStorage.removeItem('invaders_token');
        showLoginView();
    }

    // --- Event Listeners & Initial Execution ---

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = 'Logging in...';
        try {
            const { username, password } = loginForm;
            const data = USE_MOCK_API ? await mockLogin(username.value, password.value) : await (await fetch(`${API_BASE_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: username.value, password: password.value }) })).json();
            localStorage.setItem('invaders_token', data.token);
            loginError.textContent = '';
            showDashboardView();
        } catch (error) {
            loginError.textContent = error.error || 'Login failed!';
        }
    });

    logoutBtn.addEventListener('click', logout);
    viewLeaderboardBtn.addEventListener('click', showLeaderboardView);
    backToDashboardBtn.addEventListener('click', showDashboardView);

    // Initial check on page load
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});
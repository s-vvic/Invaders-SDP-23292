document.addEventListener('DOMContentLoaded', () => {
<<<<<<< HEAD:frontend/src/main/resources/public/main.js
    
    const canvas = document.getElementById('starfield-canvas');
=======
    // --- DOM Elements ---
>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
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

<<<<<<< HEAD:frontend/src/main/resources/public/main.js
    const API_BASE_URL = '/api';

    const ctx = canvas.getContext('2d');
    let stars = [];
    const numStars = 800;

    function resizeCanvas() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }
    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    class Star {
        constructor() { this.reset(); }
        reset() {
            this.x = (Math.random() - 0.5) * canvas.width;
            this.y = (Math.random() - 0.5) * canvas.height;
            this.z = Math.random() * canvas.width;
        }
        update() {
            this.z -= 2;
            if (this.z < 1) { this.reset(); }
        }
        draw() {
            const sx = (this.x / this.z) * canvas.width / 2 + canvas.width / 2;
            const sy = (this.y / this.z) * canvas.height / 2 + canvas.height / 2;
            const r = Math.max(0.1, 2.5 * (1 - this.z / canvas.width));
            ctx.beginPath();
            ctx.arc(sx, sy, r, 0, Math.PI * 2);
            ctx.fillStyle = 'rgba(200, 255, 200, 0.8)';
            ctx.fill();
        }
=======
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
>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
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

<<<<<<< HEAD:frontend/src/main/resources/public/main.js
=======
    function mockGetDashboardData(token) {
        return fetch('./mock_data/dashboard.json').then(res => res.json());
    }

    function mockGetLeaderboard() {
        return fetch('./mock_data/leaderboard.json').then(res => res.json());
    }

    // --- App Logic & View Management ---
    async function loadDashboard() { /* ... same as before ... */ }
    async function loadLeaderboard() { /* ... same as before ... */ }

>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
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
<<<<<<< HEAD:frontend/src/main/resources/public/main.js
    }

=======
        leaderboardView.classList.add('hidden');
        registerView.classList.add('hidden');
        loadDashboard();
    }

    function showLeaderboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.add('hidden');
        leaderboardView.classList.remove('hidden');
        registerView.classList.add('hidden');
        loadLeaderboard();
    }

    function logout() {
        localStorage.removeItem('invaders_token');
        showLoginView();
    }

    // --- Event Listeners & Initial Execution ---

    showRegisterBtn.addEventListener('click', showRegisterView);
    showLoginBtn.addEventListener('click', showLoginView);
    logoutBtn.addEventListener('click', logout);
    viewLeaderboardBtn.addEventListener('click', showLeaderboardView);
    backToDashboardBtn.addEventListener('click', showDashboardView);

>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = 'Logging in...';
        try {
<<<<<<< HEAD:frontend/src/main/resources/public/main.js
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            const data = await response.json();

            if (response.ok) {
                localStorage.setItem('invaders_token', data.token);
                showDashboardView();
            } else {
                loginError.textContent = data.error || 'Login failed!';
            }
=======
            const { username, password } = loginForm;
            const data = USE_MOCK_API ? await mockLogin(username.value, password.value) : await (await fetch(`${API_BASE_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: username.value, password: password.value }) })).json();
            localStorage.setItem('invaders_token', data.token);
            loginError.textContent = '';
            showDashboardView();
>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
        } catch (error) {
            loginError.textContent = error.error || 'Login failed!';
        }
    });

<<<<<<< HEAD:frontend/src/main/resources/public/main.js
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('invaders_token');
        showLoginView();
    });

=======
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
>>>>>>> 7928ddf97f53d2fe0ae6e27bf696778642475fe1:backend/src/main/resources/public/main.js
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const canvas = document.getElementById('starfield-canvas');
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const loginForm = document.getElementById('login-form');
    const loginError = document.getElementById('login-error');
    const logoutBtn = document.getElementById('logout-btn');
    const welcomeMessage = document.getElementById('welcome-message');
    const highScoreEl = document.getElementById('high-score');

    const API_BASE_URL = 'http://localhost:7070/api';

    // --- Starfield Animation ---
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
    }

    for (let i = 0; i < numStars; i++) { stars.push(new Star()); }

    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        stars.forEach(star => { star.update(); star.draw(); });
        requestAnimationFrame(animate);
    }
    animate();

    // --- App Logic ---

    function showLoginView() {
        loginView.classList.remove('hidden');
        dashboardView.classList.add('hidden');
    }

    function showDashboardView() {
        loginView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
        // TODO: Fetch and display dashboard data
    }

    // Login form submission
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = '';
        const username = loginForm.username.value;
        const password = loginForm.password.value;

        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            const data = await response.json();

            if (response.ok) {
                // Login successful
                localStorage.setItem('invaders_token', data.token);
                showDashboardView();
            } else {
                // Login failed
                loginError.textContent = data.error || 'Login failed!';
            }
        } catch (error) {
            console.error('Login error:', error);
            loginError.textContent = 'Cannot connect to the server.';
        }
    });

    // Logout button
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('invaders_token');
        showLoginView();
    });

    // Initial check: If token exists, show dashboard, otherwise show login
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});

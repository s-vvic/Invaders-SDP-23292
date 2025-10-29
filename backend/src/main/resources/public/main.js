document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('starfield-canvas');
    const ctx = canvas.getContext('2d');

    function resizeCanvas() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    const numStars = 800;
    const stars = [];

    class Star {
        constructor() {
            this.reset();
        }

        reset() {
            this.x = (Math.random() - 0.5) * canvas.width;
            this.y = (Math.random() - 0.5) * canvas.height;
            this.z = Math.random() * canvas.width;
            this.pz = this.z; // previous z
        }

        update() {
            this.z -= 2; // speed
            if (this.z < 1) {
                this.reset();
            }
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

    for (let i = 0; i < numStars; i++) {
        stars.push(new Star());
    }

    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        stars.forEach(star => {
            star.update();
            star.draw();
        });

        requestAnimationFrame(animate);
    }

    animate();

    // --- MVP Frontend Logic will go here ---
    // For now, just toggle views for demonstration
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const loginForm = document.getElementById('login-form');

    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        // In a real scenario, you'd call the API here.
        // For this demo, we'll just switch views.
        console.log('Login attempt');
        loginView.classList.add('hidden');
        dashboardView.classList.remove('hidden');
    });

    const logoutBtn = document.getElementById('logout-btn');
    logoutBtn.addEventListener('click', () => {
        console.log('Logout');
        dashboardView.classList.add('hidden');
        loginView.classList.remove('hidden');
    });
});
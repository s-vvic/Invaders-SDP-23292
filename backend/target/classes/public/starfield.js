document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('starfield-canvas');
    if (!canvas) {
        console.error("Starfield canvas not found!");
        return;
    }
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
});

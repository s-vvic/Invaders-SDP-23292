package screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import engine.Cooldown;
import engine.Core;

/**
 * Refactored TransitionScreen Class.
 * We used Strategy Pattern to isolate the complexity of the screen transition effect.
 */
public class TransitionScreen extends Screen {

    public enum TransitionType {
        STARFIELD,
        FADE_OUT
    }

    private interface TransitionEffect {
        void update(long remainingTime, int width, int height);
        void draw(Graphics2D g2d, int width, int height);
        long getDuration();
    }

    private final Cooldown transitionCooldown;
    private final int nextScreenCode;
    private final TransitionEffect effectStrategy; // 전략 객체 보유

    /**
     * Constructor for STARFIELD transition.
     */
    public TransitionScreen(final int width, final int height, final int fps, final int nextScreenCode, final TransitionType type) {
        this(width, height, fps, nextScreenCode, type, null);
    }

    /**
     * Constructor for FADE_OUT transition.
     */
    public TransitionScreen(final int width, final int height, final int fps, final int nextScreenCode, final TransitionType type, final BufferedImage sourceImage) {
        super(width, height, fps);
        this.nextScreenCode = nextScreenCode;

        switch (type) {
            case STARFIELD:
                this.effectStrategy = new StarfieldEffect(width, height);
                break;
            case FADE_OUT:
                this.effectStrategy = new FadeOutEffect(sourceImage);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transition type: " + type);
        }

        this.transitionCooldown = Core.getCooldown((int) this.effectStrategy.getDuration());
    }

    @Override
    public void initialize() {
        super.initialize();
        this.transitionCooldown.reset();
    }

    @Override
    public final int run() {
        this.isRunning = true;
        inputManager.clearKeyQueue();

        while (this.isRunning) {
            long time = System.currentTimeMillis();
            update();
            time = (1000 / this.fps) - (System.currentTimeMillis() - time);
            if (time > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(time);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
        }
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();

        this.effectStrategy.update(transitionCooldown.getRemainingMilliseconds(), this.width, this.height);

        draw();

        if (this.transitionCooldown.checkFinished()) {
            this.isRunning = false;
            this.returnCode = this.nextScreenCode;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        Graphics2D g2d = (Graphics2D) drawManager.getBackBufferGraphics();

        this.effectStrategy.draw(g2d, this.width, this.height);

        drawManager.completeDrawing(this);
    }

    // ========================================================
    // STRATEGY IMPLEMENTATIONS (Inner Classes)
    // ========================================================

    /**
     * Strategy 1: Starfield Effect
     */
    private static class StarfieldEffect implements TransitionEffect {
        private static final int DURATION = 2500;
        private static final int NUM_STARS = 1600;
        private static final float INITIAL_SPEED = 1.0f;
        private static final float MAX_SPEED = 80.0f;
        private static final float STROKE_MULTIPLIER = 4.0f;

        private static final Color[] STAR_COLORS = new Color[] {
                new Color(255, 255, 255), new Color(173, 216, 230),
                new Color(255, 105, 180), new Color(0, 255, 255),
                new Color(255, 255, 0),   new Color(138, 43, 226)
        };

        private final List<Star> stars;
        private final Random random;

        public StarfieldEffect(int width, int height) {
            this.random = new Random();
            this.stars = new ArrayList<>();
            for (int i = 0; i < NUM_STARS; i++) {
                this.stars.add(new Star(this.random, width, height));
            }
        }

        @Override
        public long getDuration() {
            return DURATION;
        }

        @Override
        public void update(long remainingTime, int width, int height) {
            float progress = 1.0f - (remainingTime / (float) DURATION);
            float easedProgress = progress * progress * progress;
            float currentSpeed = INITIAL_SPEED + easedProgress * MAX_SPEED;

            for (Star star : this.stars) {
                star.z -= currentSpeed;
                if (star.z <= 0) {
                    star.randomize(this.random, width, height, false);
                }
            }
        }

        @Override
        public void draw(Graphics2D g2d, int width, int height) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float lastStrokeWidth = -1.0f;

            for (Star star : this.stars) {
                if (star.z <= 0) continue;

                float k = width / star.z;
                float px = star.x * k + (width / 2f);
                float py = star.y * k + (height / 2f);

                if (isWithinBounds(px, py, width, height)) {
                    // Draw Star Logic
                    float brightness = 1 - star.z / width;
                    int alpha = (int) (Math.max(0, Math.min(1, brightness)) * 255);
                    Color base = star.starColor;
                    g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));

                    if (star.px_prev != -1) {
                        float newStrokeWidth = (1 - star.z / width) * STROKE_MULTIPLIER;
                        if (Math.abs(newStrokeWidth - lastStrokeWidth) > 0.001f) {
                            g2d.setStroke(new BasicStroke(newStrokeWidth));
                            lastStrokeWidth = newStrokeWidth;
                        }
                        g2d.drawLine((int) star.px_prev, (int) star.py_prev, (int) px, (int) py);
                    }

                    star.px_prev = px;
                    star.py_prev = py;
                } else {
                    star.resetPrevPosition();
                }
            }
        }

        private boolean isWithinBounds(float px, float py, int width, int height) {
            return px >= 0 && px < width && py >= 0 && py < height;
        }

        // Inner class for Star
        private static class Star {
            float x, y, z;
            float px_prev, py_prev;
            Color starColor;

            Star(Random random, int width, int height) {
                this.randomize(random, width, height, true);
            }

            void randomize(Random random, int width, int height, boolean isInitial) {
                this.x = (random.nextFloat() - 0.5f) * width;
                this.y = (random.nextFloat() - 0.5f) * height;
                this.z = isInitial ? random.nextFloat() * width : width;
                this.starColor = STAR_COLORS[random.nextInt(STAR_COLORS.length)];
                this.resetPrevPosition();
            }

            void resetPrevPosition() {
                this.px_prev = -1;
                this.py_prev = -1;
            }
        }
    }

    /**
     * Strategy 2: Fade Out Effect
     */
    private static class FadeOutEffect implements TransitionEffect {
        private static final int DURATION = 1000;
        private final BufferedImage sourceImage;
        private float currentEasedProgress = 0f;

        public FadeOutEffect(BufferedImage sourceImage) {
            this.sourceImage = sourceImage;
        }

        @Override
        public long getDuration() {
            return DURATION;
        }

        @Override
        public void update(long remainingTime, int width, int height) {
            float progress = 1.0f - (float) remainingTime / DURATION;
            this.currentEasedProgress = calculateEaseInOut(progress);
        }

        @Override
        public void draw(Graphics2D g2d, int width, int height) {
            if (sourceImage != null) {
                drawDarkenedImage(g2d, width, height);
            } else {
                drawSimpleFade(g2d, width, height);
            }
        }

        private float calculateEaseInOut(float progress) {
            return progress < 0.5f
                    ? 4.0f * progress * progress * progress
                    : 1.0f - (float) Math.pow(-2.0f * progress + 2.0f, 3) / 2.0f;
        }

        private void drawDarkenedImage(Graphics2D g2d, int width, int height) {
            float scaleFactor = Math.max(0.0f, Math.min(1.0f, 1.0f - currentEasedProgress));
            RescaleOp op = new RescaleOp(scaleFactor, 0, null);
            BufferedImage darkenedImage = op.filter(sourceImage, null);
            g2d.drawImage(darkenedImage, 0, 0, width, height, null);
        }

        private void drawSimpleFade(Graphics2D g2d, int width, int height) {
            float alpha = Math.max(0.0f, Math.min(1.0f, currentEasedProgress));
            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.fillRect(0, 0, width, height);
        }
    }
}
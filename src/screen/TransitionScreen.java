package screen;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import engine.Cooldown;
import engine.Core;

public class TransitionScreen extends Screen {

    public enum TransitionType {
        STARFIELD,
        FADE_OUT
    }

    private static final int STARFIELD_DURATION = 2500;
    private static final int FADE_OUT_DURATION = 1000;
    private static final int NUM_STARS = 1600;
    private static final float INITIAL_SPEED = 1.0f;
    private static final float MAX_SPEED = 80.0f;

    private static final Color[] STAR_COLORS = new Color[] {
        new Color(255, 255, 255),
        new Color(173, 216, 230),
        new Color(255, 105, 180),
        new Color(0, 255, 255),
        new Color(255, 255, 0),
        new Color(138, 43, 226)
    };

    private Cooldown transitionCooldown;
    private int nextScreenCode;
    private List<Star> stars;
    private Random random;
    private TransitionType type;
    private BufferedImage sourceImage;

    private static class Star {
        float x;
        float y;
        float z;
        float px_prev;
        float py_prev;
        Color starColor;

        Star(Random random, int width, int height) {
            this.randomize(random, width, height, true);
        }

        void randomize(Random random, int width, int height, boolean isInitial) {
            this.x = (random.nextFloat() - 0.5f) * width;
            this.y = (random.nextFloat() - 0.5f) * height;
            this.z = isInitial ? random.nextFloat() * width : width;
            this.starColor = STAR_COLORS[random.nextInt(STAR_COLORS.length)];
            this.px_prev = -1;
            this.py_prev = -1;
        }
    }

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
        this.type = type;
        this.sourceImage = sourceImage;

        switch (this.type) {
            case STARFIELD:
                this.transitionCooldown = Core.getCooldown(STARFIELD_DURATION);
                this.random = new Random();
                this.stars = new ArrayList<>();
                for (int i = 0; i < NUM_STARS; i++) {
                    this.stars.add(new Star(this.random, this.width, this.height));
                }
                break;
            case FADE_OUT:
                this.transitionCooldown = Core.getCooldown(FADE_OUT_DURATION);
                break;
            default:
                throw new IllegalArgumentException("Unsupported transition type: " + this.type);
        }
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
					java.util.concurrent.TimeUnit.MILLISECONDS.sleep(time);
				} catch (InterruptedException e) {
					return 0;
				}
			}
		}
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();

        switch (this.type) {
            case STARFIELD:
                float progress = 1.0f - (transitionCooldown.getRemainingMilliseconds() / (float) STARFIELD_DURATION);
                float easedProgress = progress * progress * progress;
                float currentSpeed = INITIAL_SPEED + easedProgress * MAX_SPEED;

                for (Star star : this.stars) {
                    star.z -= currentSpeed;
                    if (star.z <= 0) {
                        star.randomize(this.random, this.width, this.height, false);
                    }
                }
                break;
            case FADE_OUT:
                // Logic is mainly in draw()
                break;
        }

        draw();

        if (this.transitionCooldown.checkFinished()) {
            this.isRunning = false;
            this.returnCode = this.nextScreenCode;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        Graphics2D g2d = (Graphics2D) drawManager.getBackBufferGraphics();

        switch (this.type) {
            case STARFIELD:
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float lastStrokeWidth = -1.0f;

                for (Star star : this.stars) {
                    if (star.z > 0) {
                        float k = this.width / star.z;
                        float px = star.x * k + (this.width / 2f);
                        float py = star.y * k + (this.height / 2f);

                        if (px >= 0 && px < this.width && py >= 0 && py < this.height) {
                            float brightness = 1 - star.z / this.width;
                            int alpha = (int) (Math.max(0, Math.min(1, brightness)) * 255);
                            g2d.setColor(new Color(star.starColor.getRed(), star.starColor.getGreen(), star.starColor.getBlue(), alpha));

                            if (star.px_prev != -1) {
                                float newStrokeWidth = (1 - star.z / this.width) * 4;
                                if (newStrokeWidth != lastStrokeWidth) {
                                    g2d.setStroke(new BasicStroke(newStrokeWidth));
                                    lastStrokeWidth = newStrokeWidth;
                                }
                                g2d.drawLine((int) star.px_prev, (int) star.py_prev, (int) px, (int) py);
                            }
                            
                            star.px_prev = px;
                            star.py_prev = py;
                        } else {
                            star.px_prev = -1;
                            star.py_prev = -1;
                        }
                    }
                }
                break;
            case FADE_OUT:
                long remaining = this.transitionCooldown.getRemainingMilliseconds();
                float progress = 1.0f - (float) remaining / FADE_OUT_DURATION;

                // Apply ease-in-out cubic easing function for a smoother effect
                float easedProgress = progress < 0.5f 
                                    ? 4.0f * progress * progress * progress
                        : 1.0f - ((-2.0f * progress + 2.0f) * (-2.0f * progress + 2.0f) * (-2.0f * progress + 2.0f)) / 2.0f;

                if (sourceImage != null) {
                    // Create a RescaleOp to darken the image
                    float scaleFactor = 1.0f - easedProgress; // Goes from 1.0 (full brightness) to 0.0 (black)
                    if (scaleFactor < 0.0f) scaleFactor = 0.0f;
                    if (scaleFactor > 1.0f) scaleFactor = 1.0f;

                    RescaleOp op = new RescaleOp(scaleFactor, 0, null);
                    BufferedImage darkenedImage = op.filter(sourceImage, null);
                    g2d.drawImage(darkenedImage, 0, 0, this.width, this.height, null);
                } else {
                    // Fallback to simple black fade if no source image
                    float alpha = easedProgress;
                    if (alpha > 1.0f) {
                        alpha = 1.0f;
                    } else if (alpha < 0.0f) {
                        alpha = 0.0f;
                    }
                    g2d.setColor(new Color(0, 0, 0, alpha));
                    g2d.fillRect(0, 0, this.width, this.height);
                }
                break;
        }

        drawManager.completeDrawing(this);
    }
}

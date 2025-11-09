package screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import engine.Cooldown;
import engine.Core;

public class GameOverScreen extends Screen {

    private static final int DE_REZ_DURATION = 2000;
    private static final int RE_REZ_DURATION = 2000;
    private static final int BLINK_DURATION = 2000;
    private static final int TEXT_BLINK_INTERVAL = 400;
    private static final int MAX_PIXEL_SIZE = 42;

    private BufferedImage sourceImage;
    private BufferedImage gameOverImage;
    
    private Cooldown screenCooldown;
    private Cooldown textBlinkCooldown;
    private boolean showText;

    public GameOverScreen(final int width, final int height, final int fps, final BufferedImage sourceImage) {
        super(width, height, fps);
        this.sourceImage = sourceImage;
        this.returnCode = 1;
    }

    @Override
    public void initialize() {
        super.initialize();
        int totalDuration = DE_REZ_DURATION + RE_REZ_DURATION + BLINK_DURATION;
        this.screenCooldown = Core.getCooldown(totalDuration);
        this.textBlinkCooldown = Core.getCooldown(TEXT_BLINK_INTERVAL);
        this.screenCooldown.reset();
        this.textBlinkCooldown.reset();
        this.showText = true;

        this.gameOverImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = this.gameOverImage.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, this.width, this.height);
        g.dispose();
        drawManager.drawCenteredBigStringOnImage(this.gameOverImage, "GAME OVER", this.height / 2);
    }

    @Override
    public final int run() {
        super.run();
        return this.returnCode;
    }

    @Override
    protected void update() {
        super.update();
        draw();

        long elapsed = (DE_REZ_DURATION + RE_REZ_DURATION + BLINK_DURATION) - this.screenCooldown.getRemainingMilliseconds();
        if (elapsed > DE_REZ_DURATION + RE_REZ_DURATION) {
            if (this.textBlinkCooldown.checkFinished()) {
                this.showText = !this.showText;
                this.textBlinkCooldown.reset();
            }
        }

        if (this.screenCooldown.checkFinished()) {
            this.isRunning = false;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        Graphics2D g2d = (Graphics2D) drawManager.getBackBufferGraphics();

        long elapsed = (DE_REZ_DURATION + RE_REZ_DURATION + BLINK_DURATION) - screenCooldown.getRemainingMilliseconds();

        if (elapsed < DE_REZ_DURATION) {
            float progress = (float) elapsed / DE_REZ_DURATION;
            int pixelSize = 1 + (int) (progress * (MAX_PIXEL_SIZE - 1));
            pixelate(g2d, this.sourceImage, pixelSize);
        } else if (elapsed < DE_REZ_DURATION + RE_REZ_DURATION) {
            float progress = (float) (elapsed - DE_REZ_DURATION) / RE_REZ_DURATION;
            int pixelSize = 1 + (int) ((1.0f - progress) * (MAX_PIXEL_SIZE - 1));
            pixelate(g2d, this.gameOverImage, pixelSize);
        } else {
            if (this.showText) {
                g2d.drawImage(this.gameOverImage, 0, 0, null);
            }
        }

        drawManager.completeDrawing(this);
    }

    private void pixelate(Graphics2D g2d, BufferedImage image, int pixelSize) {
        if (pixelSize <= 1) {
            g2d.drawImage(image, 0, 0, this.width, this.height, null);
            return;
        }

        int smallWidth = this.width / pixelSize;
        int smallHeight = this.height / pixelSize;

        if (smallWidth > 0 && smallHeight > 0) {
            BufferedImage smallImage = new BufferedImage(smallWidth, smallHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D gSmall = smallImage.createGraphics();
            gSmall.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gSmall.drawImage(image, 0, 0, smallWidth, smallHeight, null);
            gSmall.dispose();

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.drawImage(smallImage, 0, 0, this.width, this.height, null);
        }
    }
}

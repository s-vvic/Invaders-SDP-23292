package screen;

import java.awt.Color;
import java.awt.Graphics;
import engine.DrawManager;

public class ConfirmSessionDrawer {

    private DrawManager drawManager;

    public ConfirmSessionDrawer(DrawManager drawManager) {
        this.drawManager = drawManager;
    }

    public void draw(Screen screen, String username, String instruction, String errorMessage) {
        Graphics g = drawManager.getBackBufferGraphics();
        if (g == null) return; // Safety check

        // Draw title
        drawManager.drawCenteredBigString(screen, "Session Confirmation", screen.getHeight() / 6);

        // Draw username
        if (username != null && !username.isEmpty()) {
            g.setColor(Color.WHITE);
            drawManager.drawCenteredRegularString(screen, "Logged in as: " + username, screen.getHeight() / 3 - 20);
        }

        // Draw instruction text
        g.setColor(Color.WHITE);
        drawManager.drawCenteredRegularString(screen, instruction, screen.getHeight() / 3 + 20);
        
        // Draw error message
        if (errorMessage != null && !errorMessage.isEmpty()) {
            g.setColor(Color.RED);
            drawManager.drawCenteredRegularString(screen, errorMessage, screen.getHeight() / 2 + 50);
        }
        
        drawManager.drawCenteredRegularString(screen, "(Press ESC to cancel)", screen.getHeight() - 50);
    }
}

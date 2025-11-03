package screen.drawers;

import java.awt.Color;
import java.awt.Graphics;
import engine.DrawManager;
import screen.Screen;

/**
 * Draws the elements for the RegisterScreen.
 */
public class RegisterDrawer {

    private DrawManager drawManager;

    public RegisterDrawer(DrawManager drawManager) {
        this.drawManager = drawManager;
    }

    public void draw(Screen screen, String username, String password, String confirmPassword, 
                       int activeField, String errorMessage) {
        
        Graphics g = drawManager.getBackBufferGraphics();
        if (g == null) return;

        // Draw title
        drawManager.drawCenteredBigString(screen, "Register", screen.getHeight() / 4);

        // UI layout calculations
        int fieldWidth = 250;
        int fieldHeight = 30;
        int fieldX = (screen.getWidth() - fieldWidth) / 2;
        int usernameY = screen.getHeight() / 2 - 50;
        int passwordY = usernameY + fieldHeight + 25;
        int confirmY = passwordY + fieldHeight + 25;
        int buttonY = confirmY + fieldHeight + 20;

        // Draw labels
        g.setColor(Color.WHITE);
        drawManager.drawCenteredRegularString(screen, "Username", usernameY - 15);
        drawManager.drawCenteredRegularString(screen, "Password", passwordY - 15);
        drawManager.drawCenteredRegularString(screen, "Confirm Password", confirmY - 15);

        // Draw input fields
        g.setColor(Color.GRAY);
        g.drawRect(fieldX, usernameY, fieldWidth, fieldHeight);
        g.drawRect(fieldX, passwordY, fieldWidth, fieldHeight);
        g.drawRect(fieldX, confirmY, fieldWidth, fieldHeight);

        // Highlight active UI element
        g.setColor(Color.GREEN);
        if (activeField == 0) { // Username
            g.drawRect(fieldX - 2, usernameY - 2, fieldWidth + 4, fieldHeight + 4);
        } else if (activeField == 1) { // Password
            g.drawRect(fieldX - 2, passwordY - 2, fieldWidth + 4, fieldHeight + 4);
        } else if (activeField == 2) { // Confirm Password
            g.drawRect(fieldX - 2, confirmY - 2, fieldWidth + 4, fieldHeight + 4);
        } else if (activeField == 3) { // Register Button
            g.drawRect(fieldX - 2, buttonY - 2, fieldWidth + 4, fieldHeight + 4);
        }

        // Draw typed text
        g.setColor(Color.WHITE);
        g.drawString(username, fieldX + 10, usernameY + 22);
        g.drawString("*".repeat(password.length()), fieldX + 10, passwordY + 22);
        g.drawString("*".repeat(confirmPassword.length()), fieldX + 10, confirmY + 22);

        // Draw "Register" button
        if (activeField == 3) {
            g.setColor(Color.GREEN);
        } else {
            g.setColor(Color.GRAY);
        }
        g.drawRect(fieldX, buttonY, fieldWidth, fieldHeight);
        drawManager.drawCenteredRegularString(screen, "Register", buttonY + 22);

        // Draw error message
        if (errorMessage != null && !errorMessage.isEmpty()) {
            g.setColor(Color.RED);
            drawManager.drawCenteredRegularString(screen, errorMessage, buttonY + fieldHeight + 30);
        }
        
        // Draw instructions
        drawManager.drawCenteredRegularString(screen, "(Press ESC to go back)", screen.getHeight() - 50);
    }
}

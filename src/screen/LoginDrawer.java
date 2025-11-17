package screen;

import java.awt.Color;
import java.awt.Graphics;
import engine.DrawManager;

public class LoginDrawer {

    private DrawManager drawManager;

    public LoginDrawer(DrawManager drawManager) {
        this.drawManager = drawManager;
    }

    public void draw(Screen screen, String username, String password, int activeField, String errorMessage) {
        Graphics g = drawManager.getBackBufferGraphics();
        if (g == null) return; // Safety check

        // Draw title
        drawManager.drawCenteredBigString(screen, "Login", screen.getHeight() / 4);

        int fieldWidth = 200;
        int fieldHeight = 30;
        int fieldX = (screen.getWidth() - fieldWidth) / 2;
        int usernameY = screen.getHeight() / 2 - 50; // Adjusted Y for more space
        int passwordY = usernameY + fieldHeight + 25;
        int loginButtonY = passwordY + fieldHeight + 20;
        int registerButtonY = loginButtonY + fieldHeight + 10;

        // Draw labels
        g.setColor(Color.WHITE);
        drawManager.drawCenteredRegularString(screen, "Username", usernameY - 15);
        drawManager.drawCenteredRegularString(screen, "Password", passwordY - 15);

        // Draw input fields
        g.setColor(Color.GRAY);
        g.drawRect(fieldX, usernameY, fieldWidth, fieldHeight);
        g.drawRect(fieldX, passwordY, fieldWidth, fieldHeight);

        // Highlight active field
        g.setColor(Color.GREEN);
        if (activeField == 0) { // Username
            g.drawRect(fieldX - 2, usernameY - 2, fieldWidth + 4, fieldHeight + 4);
        } else if (activeField == 1) { // Password
            g.drawRect(fieldX - 2, passwordY - 2, fieldWidth + 4, fieldHeight + 4);
        }

        // Draw typed text
        g.setColor(Color.WHITE);
        g.drawString(username, fieldX + 10, usernameY + 22);
        g.drawString("*".repeat(password.length()), fieldX + 10, passwordY + 22);

        // --- Draw Login Button ---
        if (activeField == 2) {
            g.setColor(Color.GREEN);
            g.drawRect(fieldX - 2, loginButtonY - 2, fieldWidth + 4, fieldHeight + 4);
            g.setColor(Color.BLACK); // Set text color for active button
        } else {
            g.setColor(Color.GRAY);
            g.drawRect(fieldX, loginButtonY, fieldWidth, fieldHeight);
            g.setColor(Color.WHITE); // Set text color for inactive button
        }
        drawManager.drawCenteredRegularString(screen, "Login", loginButtonY + 22);

        // --- Draw Register Button ---
        if (activeField == 3) {
            g.setColor(Color.GREEN);
            g.drawRect(fieldX - 2, registerButtonY - 2, fieldWidth + 4, fieldHeight + 4);
            g.setColor(Color.BLACK); // Set text color for active button
        } else {
            g.setColor(Color.GRAY);
            g.drawRect(fieldX, registerButtonY, fieldWidth, fieldHeight);
            g.setColor(Color.WHITE); // Set text color for inactive button
        }
        drawManager.drawCenteredRegularString(screen, "Register", registerButtonY + 22);

        // Draw error message
        if (errorMessage != null && !errorMessage.isEmpty()) {
            g.setColor(Color.RED);
            drawManager.drawCenteredRegularString(screen, errorMessage, registerButtonY + fieldHeight + 30);
        }
        
        drawManager.drawCenteredRegularString(screen, "(UP/DOWN to switch, SPACE to select)", screen.getHeight() - 50);
    }
}

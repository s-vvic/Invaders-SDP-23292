package screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import engine.DrawManager;

public class LoginDrawer {

    private DrawManager drawManager;

    public LoginDrawer(DrawManager drawManager) {
        this.drawManager = drawManager;
    }

    public void draw(Screen screen, String userCode, String instruction, String errorMessage, int activeField) {
        Graphics g = drawManager.getBackBufferGraphics();
        if (g == null) return; // Safety check

        // Draw title
        drawManager.drawCenteredBigString(screen, "Connect Your Device", screen.getHeight() / 6);

        // Draw instruction text
        drawManager.drawCenteredRegularString(screen, instruction, screen.getHeight() / 3);

        // Draw the user code in a large, clear font
        if (userCode != null && !userCode.isEmpty()) {
            g.setColor(Color.GREEN);
            // Use a larger, monospaced font for the code to make it clear
            Font originalFont = g.getFont();
            Font codeFont = new Font("Monospaced", Font.BOLD, 48);
            g.setFont(codeFont);
            drawManager.drawCenteredString(screen, userCode, screen.getHeight() / 2);
            g.setFont(originalFont); // Reset to original font
        }
        
        // Draw error message
        if (errorMessage != null && !errorMessage.isEmpty()) {
            g.setColor(Color.RED);
            drawManager.drawCenteredRegularString(screen, errorMessage, screen.getHeight() / 2 + 50);
        }

        // Draw selectable options
        int optionY = screen.getHeight() / 2 + 100;
        
        // "Copy Code to Clipboard" option
        if (activeField == 0) g.setColor(Color.GREEN);
        else g.setColor(Color.WHITE);
        drawManager.drawCenteredRegularString(screen, "[C]opy Code to Clipboard", optionY);

        // "Go Back" option
        if (activeField == 1) g.setColor(Color.GREEN);
        else g.setColor(Color.WHITE);
        drawManager.drawCenteredRegularString(screen, "[E]xit to Main Menu", optionY + 30);
    }
}

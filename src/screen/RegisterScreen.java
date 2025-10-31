package screen;

import java.awt.event.KeyEvent;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.ApiClient;
import screen.drawers.RegisterDrawer;

/**
 * Implements the register screen.
 */
public class RegisterScreen extends Screen {

    private static final int SELECTION_COOLDOWN = 200;

    private RegisterDrawer registerDrawer;
    private Cooldown selectionCooldown;

    private StringBuilder username;
    private StringBuilder password;
    private StringBuilder confirmPassword;
    
    /** 0: username, 1: password, 2: confirm, 3: register button. */
    private int activeField;
    private String errorMessage;

    public RegisterScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
        this.returnCode = 0;
        this.selectionCooldown = Core.getCooldown(SELECTION_COOLDOWN);
        this.selectionCooldown.reset();

        this.registerDrawer = new RegisterDrawer(this.drawManager);
        this.username = new StringBuilder();
        this.password = new StringBuilder();
        this.confirmPassword = new StringBuilder();
        this.activeField = 0;
        this.errorMessage = "";
    }

    public final int run() {
        super.run();
        return this.returnCode;
    }

    protected final void update() {
        super.update();
        handleInput();
        draw();
    }

    private void handleInput() {
        if (this.selectionCooldown.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
                this.activeField = (this.activeField == 0) ? 3 : this.activeField - 1;
                this.selectionCooldown.reset();
            } else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
                this.activeField = (this.activeField == 3) ? 0 : this.activeField + 1;
                this.selectionCooldown.reset();
            }
        }

        Character typedChar = inputManager.pollTypedKey();
        while (typedChar != null) {
            if (Character.isLetterOrDigit(typedChar)) {
                appendCharacter(typedChar);
            }
            typedChar = inputManager.pollTypedKey();
        }

        if (inputManager.isKeyDown(KeyEvent.VK_BACK_SPACE) && this.selectionCooldown.checkFinished()) {
            deleteCharacter();
            this.selectionCooldown.reset();
        }

        if (inputManager.isKeyDown(KeyEvent.VK_SPACE) && this.activeField == 3) {
            this.errorMessage = ""; // Clear previous error
            if (!password.toString().equals(confirmPassword.toString())) {
                this.errorMessage = "Passwords do not match.";
            } else {
                try {
                    ApiClient apiClient = ApiClient.getInstance();
                    apiClient.register(username.toString(), password.toString());
                    
                    // On success, go back to login screen to log in.
                    this.returnCode = 9; // 9 is LoginScreen
                    this.isRunning = false;

                } catch (Exception e) {
                    this.errorMessage = e.getMessage();
                }
            }
            this.selectionCooldown.reset();
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.returnCode = 9; // Go back to LoginScreen
            this.isRunning = false;
        }
    }

    private void appendCharacter(char c) {
        StringBuilder targetBuilder = null;
        if (activeField == 0) targetBuilder = username;
        else if (activeField == 1) targetBuilder = password;
        else if (activeField == 2) targetBuilder = confirmPassword;

        if (targetBuilder != null && targetBuilder.length() < 20) {
            targetBuilder.append(Character.toLowerCase(c));
        }
    }

    private void deleteCharacter() {
        StringBuilder targetBuilder = null;
        if (activeField == 0) targetBuilder = username;
        else if (activeField == 1) targetBuilder = password;
        else if (activeField == 2) targetBuilder = confirmPassword;

        if (targetBuilder != null && targetBuilder.length() > 0) {
            targetBuilder.deleteCharAt(targetBuilder.length() - 1);
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        registerDrawer.draw(this, username.toString(), password.toString(), confirmPassword.toString(), activeField, errorMessage);
        drawManager.completeDrawing(this);
    }
}
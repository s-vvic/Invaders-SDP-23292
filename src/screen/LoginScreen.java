package screen;

import java.awt.event.KeyEvent;
import engine.Cooldown;
import engine.Core;
import engine.AuthManager;
import engine.ApiClient;

/**
 * Implements the login screen.
 */
public class LoginScreen extends Screen {

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_COOLDOWN = 200;

    /** Logic to draw the login screen UI. */
    private LoginDrawer loginDrawer;

    /** Cooldown for menu navigation. */
    private Cooldown selectionCooldown;

    /** The username string being entered. */
    private StringBuilder username;
    /** The password string being entered. */
    private StringBuilder password;
    
    /** 0: username, 1: password, 2: login button. */
    private int activeField;

    /** An error message to display, if any. */
    private String errorMessage;

    public LoginScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = 0; // Default return code
        this.selectionCooldown = Core.getCooldown(SELECTION_COOLDOWN);
        this.selectionCooldown.reset();

        this.loginDrawer = new LoginDrawer(this.drawManager);
        this.username = new StringBuilder();
        this.password = new StringBuilder();
        this.activeField = 0; // Default to username field
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

        if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            if (this.activeField == 2) { // Login button
                this.errorMessage = ""; // Clear previous error
                try {
                    ApiClient apiClient = ApiClient.getInstance();
                    // login now returns a structured response
                    ApiClient.LoginResponse loginResponse = apiClient.login(username.toString(), password.toString());
                    
                    AuthManager authManager = AuthManager.getInstance();
                    // Call login with all three required arguments
                    authManager.login(loginResponse.token(), loginResponse.username(), loginResponse.userId());

                    this.returnCode = 1; // Go back to TitleScreen
                    this.isRunning = false;

                } catch (Exception e) {
                    this.errorMessage = e.getMessage();
                }
            } else if (this.activeField == 3) { // Register button
                this.returnCode = 12; // Use 12 for RegisterScreen
                this.isRunning = false;
            }
            this.selectionCooldown.reset();
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.returnCode = 1; // Go back to TitleScreen
            this.isRunning = false;
        }
    }

    private void appendCharacter(char c) {
        if (activeField == 0) { // Username
            if (username.length() < 20) { // Max length
                username.append(Character.toLowerCase(c));
            }
        } else if (activeField == 1) { // Password
            if (password.length() < 20) {
                password.append(c);
            }
        }
    }

    private void deleteCharacter() {
        if (activeField == 0) { // Username
            if (username.length() > 0) {
                username.deleteCharAt(username.length() - 1);
            }
        } else if (activeField == 1) { // Password
            if (password.length() > 0) {
                password.deleteCharAt(password.length() - 1);
            }
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        loginDrawer.draw(this, username.toString(), password.toString(), activeField, errorMessage);
        drawManager.completeDrawing(this);
    }
}
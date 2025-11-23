package screen;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.SwingWorker;

import engine.Cooldown;
import engine.Core;
import engine.AuthManager;
import engine.ApiClient;
import engine.ApiClient.DeviceAuthResponse;
import engine.ApiClient.PollResponse;
import engine.ApiClient.PollStatus;

/**
 * Implements the login screen for device authentication.
 */
public class LoginScreen extends Screen {

    /** Cooldown for menu navigation. */
    private Cooldown selectionCooldown;
    
    /** Logic to draw the login screen UI. */
    private LoginDrawer loginDrawer;

    /** The user code to display. */
    private String userCode;
    /** The instruction text to display. */
    private String instruction;
    /** An error message to display, if any. */
    private String errorMessage;
    /** Currently active selectable field (0: copy code, 1: cancel/go back). */
    private int activeField; 

    /** Worker for handling the auth flow in the background. */
    private SwingWorker<PollResponse, Void> authWorker;

    public LoginScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = 0; // Default return code
        this.selectionCooldown = Core.getCooldown(200);
        this.selectionCooldown.reset();

        this.loginDrawer = new LoginDrawer(this.drawManager);
        this.userCode = "......";
        this.instruction = "Please wait, generating code...";
        this.errorMessage = "";
        this.activeField = 0; // 0 for "Copy Code", 1 for "Go Back"
    }

    public final int run() {
        startDeviceAuthFlow();
        try {
            super.run();
        } finally {
            // Ensure the worker is cancelled if the screen exits for any reason
            if (this.authWorker != null && !this.authWorker.isDone()) {
                this.authWorker.cancel(true);
            }
        }
        return this.returnCode;
    }

    protected final void update() {
        super.update();
        handleInput();
        draw();
    }

    private void handleInput() {
        if (this.selectionCooldown.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_UP) || inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
                this.activeField = 1 - this.activeField; // Toggle between 0 and 1
                this.selectionCooldown.reset();
            }

            if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                if (this.activeField == 0) { // "Copy Code" selected
                    copyToClipboard(this.userCode);
                    this.errorMessage = "Code copied to clipboard!";
                } else if (this.activeField == 1) { // "Go Back" selected
                    if (this.authWorker != null && !this.authWorker.isDone()) {
                        this.authWorker.cancel(true); // Attempt to cancel the background task
                    }
                    this.returnCode = 1; // Go back to TitleScreen
                    this.isRunning = false;
                }
                this.selectionCooldown.reset();
            }
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            if (this.authWorker != null && !this.authWorker.isDone()) {
                this.authWorker.cancel(true); // Attempt to cancel the background task
            }
            this.returnCode = 1; // Go back to TitleScreen
            this.isRunning = false;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        loginDrawer.draw(this, this.userCode, this.instruction, this.errorMessage, this.activeField); 
        drawManager.completeDrawing(this);
    }

    /**
     * Initiates the device authentication flow.
     */
    private void startDeviceAuthFlow() {
        this.authWorker = new SwingWorker<PollResponse, Void>() {
            @Override
            protected PollResponse doInBackground() throws Exception {
                DeviceAuthResponse initResponse;
                try {
                    initResponse = ApiClient.getInstance().initiateDeviceAuth();
                    userCode = initResponse.userCode();
                    instruction = "Go to " + initResponse.verificationUri() + " and enter the code:";
                } catch (IOException | InterruptedException e) {
                    return new PollResponse(PollStatus.ERROR, null, "Connection to server failed.");
                }

                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(initResponse.verificationUri()));
                    } else {
                         instruction = "Please manually open a browser and go to the URL above.";
                    }
                } catch (IOException | URISyntaxException e) {
                    instruction = "Please manually open a browser and go to the URL above.";
                }

                long startTime = System.currentTimeMillis();
                long timeout = initResponse.expiresIn() * 1000;

                while (System.currentTimeMillis() - startTime < timeout) {
                    if (isCancelled()) return null;

                    PollResponse pollResponse = ApiClient.getInstance().pollForToken(initResponse.deviceCode());

                    if (pollResponse.status() == PollStatus.SUCCESS || pollResponse.status() == PollStatus.ERROR) {
                        return pollResponse;
                    }
                    
                    Thread.sleep(initResponse.interval());
                }

                return new PollResponse(PollStatus.ERROR, null, "Login timed out. Please try again.");
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    PollResponse result = get();
                    if (result == null) {
                        return;
                    }

                    if (result.status() == PollStatus.SUCCESS) {
                        AuthManager.getInstance().login(
                            result.loginResponse().token(),
                            result.loginResponse().username(),
                            result.loginResponse().userId()
                        );
                        returnCode = 1; 
                        isRunning = false;
                    } else {
                        errorMessage = result.errorMessage();
                        userCode = "ERROR";
                        instruction = "Please press ESC and try again.";
                    }
                } catch (java.util.concurrent.CancellationException e) {
                    // Worker was cancelled, do nothing.
                } catch (Exception e) {
                    errorMessage = "An unexpected error occurred.";
                    userCode = "ERROR";
                    instruction = "Please press ESC and try again.";
                }
            }
        };

        authWorker.execute();
    }

    /**
     * Copies the given text to the system clipboard.
     * @param text The text to copy.
     */
    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
}
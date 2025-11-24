package screen;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.SwingWorker;

import engine.Cooldown;
import engine.Core;
import engine.AuthManager;
import engine.ApiClient;
import engine.ApiClient.SessionConfirmationResponse;
import engine.ApiClient.SessionPollResponse;
import engine.ApiClient.PollStatus;
import engine.DrawManager; // For addSystemMessage

/**
 * Implements the screen for confirming an existing session via web browser.
 */
public class ConfirmSessionScreen extends Screen {

    /** Logic to draw the confirmation screen UI. */
    private ConfirmSessionDrawer confirmSessionDrawer;

    /** The username of the session being confirmed. */
    private String username;
    /** The instruction text to display. */
    private String instruction;
    /** An error message to display, if any. */
    private String errorMessage;

    /** Worker for handling the session confirmation flow in the background. */
    private SwingWorker<SessionPollResponse, Void> sessionAuthWorker;

    public ConfirmSessionScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = 0; // Default return code
        this.confirmSessionDrawer = new ConfirmSessionDrawer(this.drawManager);
        this.username = AuthManager.getInstance().getUsername(); // Get username from current session
        this.instruction = "Please wait, initiating session confirmation...";
        this.errorMessage = "";
    }

    public final int run() {
        startSessionConfirmationFlow();
        try {
            super.run();
        } finally {
            // Ensure the worker is cancelled if the screen exits for any reason
            if (this.sessionAuthWorker != null && !this.sessionAuthWorker.isDone()) {
                this.sessionAuthWorker.cancel(true);
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
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            if (this.sessionAuthWorker != null && !this.sessionAuthWorker.isDone()) {
                this.sessionAuthWorker.cancel(true); // Attempt to cancel the background task
            }
            // If user escapes, proceed to LoginScreen, as they might want to log out.
            this.returnCode = 9; 
            this.isRunning = false;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        confirmSessionDrawer.draw(this, this.username, this.instruction, this.errorMessage); 
        drawManager.completeDrawing(this);
    }

    /**
     * Initiates the session confirmation flow.
     */
    private void startSessionConfirmationFlow() {
        this.sessionAuthWorker = new SwingWorker<SessionPollResponse, Void>() {
            @Override
            protected SessionPollResponse doInBackground() throws Exception {
                AuthManager authManager = AuthManager.getInstance();
                if (!authManager.isLoggedIn()) {
                    return new SessionPollResponse(PollStatus.ERROR, null, "Not logged in.");
                }

                SessionConfirmationResponse initResponse;
                try {
                    initResponse = ApiClient.getInstance().initiateSessionConfirmation(authManager.getToken());
                    instruction = "Confirm session for " + username + " in your browser.";
                } catch (IOException | InterruptedException e) {
                    return new SessionPollResponse(PollStatus.ERROR, null, "Failed to connect to server.");
                }

                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(initResponse.confirmationUri() + "?code=" + initResponse.confirmationCode()));
                    } else {
                         instruction = "Please manually open a browser and go to " + initResponse.confirmationUri() + "?code=" + initResponse.confirmationCode();
                    }
                } catch (IOException | URISyntaxException e) {
                    instruction = "Please manually open a browser and go to " + initResponse.confirmationUri() + "?code=" + initResponse.confirmationCode();
                }

                long startTime = System.currentTimeMillis();
                long timeout = initResponse.expiresIn() * 1000L;

                while (System.currentTimeMillis() - startTime < timeout) {
                    if (isCancelled()) return null;

                    SessionPollResponse pollResponse = ApiClient.getInstance().pollSessionStatus(initResponse.confirmationCode());

                    if (pollResponse.status() == PollStatus.CONFIRMED || 
                        pollResponse.status() == PollStatus.CANCELLED ||
                        pollResponse.status() == PollStatus.ERROR) {
                        return pollResponse;
                    }
                    
                    Thread.sleep(initResponse.interval());
                }

                return new SessionPollResponse(PollStatus.ERROR, username, "Session confirmation timed out.");
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    SessionPollResponse result = get();
                    if (result == null) {
                        return;
                    }

                    if (result.status() == PollStatus.CONFIRMED) {
                        DrawManager.addSystemMessage("Session confirmed for " + result.username() + ".");
                        returnCode = 1; // Go to TitleScreen
                        isRunning = false;
                    } else { // CANCELLED, ERROR, or TIMEOUT
                        if (result.status() == PollStatus.CANCELLED) {
                            DrawManager.addSystemMessage("Session cancelled. Please log in again.");
                        }
                        errorMessage = result.errorMessage();
                        instruction = "Please press ESC and try again.";
                        returnCode = 9; // Go to LoginScreen
                        isRunning = false;
                    }
                } catch (java.util.concurrent.CancellationException e) {
                    // Worker was cancelled, do nothing.
                } catch (Exception e) {
                    errorMessage = "An unexpected error occurred.";
                    instruction = "Please press ESC and try again.";
                    returnCode = 9; // Go to LoginScreen
                    isRunning = false;
                }
            }
        };
        sessionAuthWorker.execute();
    }
}
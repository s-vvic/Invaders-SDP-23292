package engine;

import java.util.prefs.Preferences;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the user's authentication state (session), with persistence.
 * This class is a singleton to ensure a single source of truth for the auth state.
 */
public class AuthManager {

    private static final String PREF_NODE = "com.example.invaders";
    private static final String AUTH_TOKEN_KEY = "authToken";
    private static final String USERNAME_KEY = "username";
    private static final String USER_ID_KEY = "userId";

    /** Singleton instance of the class. */
    private static AuthManager instance;

    /** The authentication token received from the server. */
    private String authToken;

    /** The username of the logged-in user. */
    private String username;

    /** The ID of the logged-in user. */
    private int userId;

    /**
     * Private constructor to prevent instantiation and load session data.
     */
    private AuthManager() {
        loadSession();
    }

    /**
     * Returns the shared instance of the AuthManager.
     *
     * @return The singleton instance.
     */
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Validates the loaded session with the backend.
     * @return A CompletableFuture that resolves to true if the session is valid, false otherwise.
     */
    public CompletableFuture<Boolean> validateSessionOnStartup() {
        if (!isLoggedIn()) {
            return CompletableFuture.completedFuture(false);
        }
        // Token exists locally, let's validate it with the server.
        return ApiClient.getInstance().validateToken();
    }

    /**
     * Stores the user's token, username, and ID upon successful login.
     * Also persists the session to the Preferences API.
     *
     * @param token The JWT received from the server.
     * @param username The username of the logged-in user.
     * @param userId The ID of the logged-in user.
     */
    public void login(String token, String username, int userId) {
        this.authToken = token;
        this.username = username;
        this.userId = userId;
        saveSession();
    }

    /**
     * Clears the user's session data upon logout.
     * Also clears the session from the Preferences API.
     */
    public void logout() {
        this.authToken = null;
        this.username = null;
        this.userId = 0; // Reset userId
        clearSession();
    }

    /**
     * Invalidates the current session, typically called when the token has expired
     * or is invalid. Logs a specific message and clears all session data.
     */
    public void invalidateSession() {
        Core.getLogger().warning("Session invalidated due to token expiration or auth error. Logging out.");
        DrawManager.addSystemMessage("Session expired. You will be logged out.");
        this.authToken = null;
        this.username = null;
        this.userId = 0;
        clearSession();
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if there is an active auth token, false otherwise.
     */
    public boolean isLoggedIn() {
        return this.authToken != null && !this.authToken.isEmpty();
    }

    /**
     * Gets the current authentication token.
     *
     * @return The auth token, or null if not logged in.
     */
    public String getToken() {
        return this.authToken;
    }

    /**
     * Gets the current username.
     *
     * @return The username, or null if not logged in.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Gets the current user ID.
     *
     * @return The user ID, or 0 if not logged in.
     */
    public int getUserId() {
        return this.userId;
    }

    /**
     * Loads the session token and username from the Preferences API.
     */
    private void loadSession() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        this.authToken = prefs.get(AUTH_TOKEN_KEY, null);
        this.username = prefs.get(USERNAME_KEY, null);
        this.userId = prefs.getInt(USER_ID_KEY, 0);
        if (this.authToken != null) {
            Core.getLogger().info("Loaded session for user: " + this.username + " (ID: " + this.userId + ")");
        }
    }

    /**
     * Saves the current session token and username to the Preferences API.
     */
    private void saveSession() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        if (this.authToken != null && this.username != null) {
            prefs.put(AUTH_TOKEN_KEY, this.authToken);
            prefs.put(USERNAME_KEY, this.username);
            prefs.putInt(USER_ID_KEY, this.userId);
            Core.getLogger().info("Saved session for user: " + this.username + " (ID: " + this.userId + ")");
        }
    }

    /**
     * Clears the session data from the Preferences API.
     */
    private void clearSession() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.remove(AUTH_TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        prefs.remove(USER_ID_KEY);
        Core.getLogger().info("Cleared session data.");
    }
}
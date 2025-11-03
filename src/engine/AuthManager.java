package engine;

import java.util.prefs.Preferences;

/**
 * Manages the user's authentication state (session), with persistence.
 * This class is a singleton to ensure a single source of truth for the auth state.
 */
public class AuthManager {

    private static final String PREF_NODE = "com.example.invaders";
    private static final String AUTH_TOKEN_KEY = "authToken";
    private static final String USERNAME_KEY = "username";

    /** Singleton instance of the class. */
    private static AuthManager instance;

    /** The authentication token received from the server. */
    private String authToken;

    /** The username of the logged-in user. */
    private String username;

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
     * Stores the user's token and username upon successful login.
     * Also persists the session to the Preferences API.
     *
     * @param token The JWT received from the server.
     * @param username The username of the logged-in user.
     */
    public void login(String token, String username) {
        this.authToken = token;
        this.username = username;
        saveSession();
    }

    /**
     * Clears the user's session data upon logout.
     * Also clears the session from the Preferences API.
     */
    public void logout() {
        this.authToken = null;
        this.username = null;
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
     * Loads the session token and username from the Preferences API.
     */
    private void loadSession() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        this.authToken = prefs.get(AUTH_TOKEN_KEY, null);
        this.username = prefs.get(USERNAME_KEY, null);
        if (this.authToken != null) {
            Core.getLogger().info("Loaded session for user: " + this.username);
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
            Core.getLogger().info("Saved session for user: " + this.username);
        }
    }

    /**
     * Clears the session data from the Preferences API.
     */
    private void clearSession() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.remove(AUTH_TOKEN_KEY);
        prefs.remove(USERNAME_KEY);
        Core.getLogger().info("Cleared session data.");
    }
}
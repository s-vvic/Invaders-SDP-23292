package engine;

/**
 * Manages the user's authentication state (session).
 * This class is a singleton to ensure a single source of truth for the auth state.
 */
public class AuthManager {

    /** Singleton instance of the class. */
    private static AuthManager instance;

    /** The authentication token received from the server. */
    private String authToken;

    /** The username of the logged-in user. */
    private String username;

    /**
     * Private constructor to prevent instantiation.
     */
    private AuthManager() { }

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
     *
     * @param token The JWT received from the server.
     * @param username The username of the logged-in user.
     */
    public void login(String token, String username) {
        this.authToken = token;
        this.username = username;
        // In a real application, you might also want to save the token to preferences here.
    }

    /**
     * Clears the user's session data upon logout.
     */
    public void logout() {
        this.authToken = null;
        this.username = null;
        // In a real application, you would also clear the token from preferences here.
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
}

package engine;

import java.io.IOException;

/**
 * Handles all communication with the backend API.
 * This is a mock implementation for frontend development.
 */
public class ApiClient {

    /** Singleton instance of the class. */
    private static ApiClient instance;

    /**
     * Private constructor to prevent instantiation.
     */
    private ApiClient() { }

    /**
     * Returns the shared instance of the ApiClient.
     *
     * @return The singleton instance.
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Mock login method.
     * @param username The user's username.
     * @param password The user's password.
     * @return A fake JWT on success.
     * @throws IOException if login fails.
     */
    public String login(String username, String password) throws IOException {
        Core.getLogger().info("[Mock API] Attempting login for user: " + username);
        if ("test".equals(username) && "1234".equals(password)) {
            Core.getLogger().info("[Mock API] Login successful.");
            return "fake-jwt-token-for-testing";
        } else {
            throw new IOException("Invalid username or password");
        }
    }

    /**
     * Mock method to save the score.
     * @param score The score to save.
     */
    public void saveScore(int score) {
        Core.getLogger().info("[Mock API] Saving score: " + score);
        // In the real implementation, this would make an HTTP request.
    }

    /**
     * Mock method to save a purchased upgrade.
     * @param itemId The ID of the item.
     * @param level The new level of the item.
     */
    public void saveUpgrade(String itemId, int level) {
        Core.getLogger().info("[Mock API] Saving upgrade: " + itemId + ", Level: " + level);
        // In the real implementation, this would make an HTTP request.
    }
}

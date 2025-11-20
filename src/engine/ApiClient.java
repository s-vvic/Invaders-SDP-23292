package engine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all communication with the backend API.
 */
public class ApiClient {

    /** The base URL for the backend API. */
    private static final String API_BASE_URL = "http://localhost:8080/api";

    /** Helper record to hold structured login response data. */
    public record LoginResponse(String token, int userId, String username) {}

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
     * Performs a real login request to the backend API.
     * @param username The user's username.
     * @param password The user's password.
     * @return A LoginResponse object on success.
     * @throws IOException if the request fails.
     * @throws InterruptedException if the request is interrupted.
     */
    public LoginResponse login(String username, String password) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String jsonPayload = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        Core.getLogger().info("Attempting login for user: " + username);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            Core.getLogger().severe("Login failed with status code: " + response.statusCode());
            throw new IOException("Login failed: " + response.body());
        }

        Core.getLogger().info("Login successful. Parsing response...");
        String responseBody = response.body();

        // Manual JSON parsing
        String token = parseJsonField(responseBody, "token");
        String userJson = parseJsonObject(responseBody, "user");
        int userId = Integer.parseInt(parseJsonField(userJson, "id"));
        String parsedUsername = parseJsonField(userJson, "username");

        return new LoginResponse(token, userId, parsedUsername);
    }

    /**
     * Saves the score by making a PUT request to the backend.
     * @param score The score to save.
     */
    public void saveScore(int score) {
        AuthManager authManager = AuthManager.getInstance();
        if (!authManager.isLoggedIn()) {
            Core.getLogger().warning("Cannot save score: User is not logged in.");
            return;
        }
        int userId = authManager.getUserId();
        String token = authManager.getToken();

        if (token == null || token.isEmpty()) {
            Core.getLogger().severe("Cannot save score: Auth token is missing.");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"score\": " + score + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/users/" + userId + "/score"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            Core.getLogger().info("Sending score " + score + " for user " + userId + " to the backend.");
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Core.getLogger().info("Save score response status code: " + response.statusCode());
                        Core.getLogger().info("Save score response body: " + response.body());
                        if (response.statusCode() != 200) {
                            Core.getLogger().severe("Failed to save score. Status: " + response.statusCode() + ", Body: " + response.body());
                        } else {
                            Core.getLogger().info("Score " + score + " successfully saved to database for user " + userId);
                        }
                    }).exceptionally(e -> {
                        Core.getLogger().severe("Failed to save score: " + e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            Core.getLogger().severe("Exception while trying to save score: " + e.getMessage());
        }
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

    /**
     * Mock register method.
     * @param username The username to register.
     * @param password The password for the new account.
     * @throws IOException if the username is already taken.
     */
    public void register(String username, String password) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String jsonPayload = "{\"username\": \"" + username.replace("\"", "\\\"") + "\", " +
                             "\"password\": \"" + password.replace("\"", "\\\"") + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        Core.getLogger().info("Attempting to register user: " + username);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            Core.getLogger().severe("Registration failed with status code: " + response.statusCode() + ", Body: " + response.body());
            String errorMessage = parseJsonField(response.body(), "error");
            if (errorMessage != null && !errorMessage.isEmpty()) {
                throw new IOException(errorMessage);
            }
            throw new IOException("Registration failed with status: " + response.statusCode());
        }

        Core.getLogger().info("Registration successful for user: " + username);
    }

    /**
     * Fetches the global high scores from the backend.
     * @return A list of Score objects representing the leaderboard.
     * @throws IOException if the request fails.
     * @throws InterruptedException if the request is interrupted.
     */
    public List<Score> getHighScores() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/scores"))
                .GET()
                .build();

        Core.getLogger().info("Requesting high scores from backend.");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            Core.getLogger().severe("Failed to fetch high scores with status code: " + response.statusCode());
            throw new IOException("Failed to fetch high scores: " + response.body());
        }

        String responseBody = response.body();
        return parseScoreList(responseBody);
    }

    /**
     * A very basic and brittle parser for a list of score objects from a JSON array.
     * Assumes a format like: [{"username":"x","score":X},{"username":"y","score":Y}]
     * @param jsonArray The JSON string representing the array of scores.
     * @return A List of Score objects.
     */
    private List<Score> parseScoreList(String jsonArray) {
        List<Score> scores = new ArrayList<>();

        if (jsonArray == null || jsonArray.trim().isEmpty() || !jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
            Core.getLogger().warning("Invalid JSON array for scores: " + jsonArray);
            return scores;
        }

        String innerContent = jsonArray.substring(1, jsonArray.length() - 1); // Remove outer []
        if (innerContent.isEmpty()) { // Handle empty array
            return scores;
        }

        // Split by "},{" to get individual score objects as strings
        String[] scoreStrings = innerContent.split("\\},\\{");

        for (String scoreStr : scoreStrings) {
            // Reconstruct a valid JSON object string for parsing
            String fullScoreObject = "{" + scoreStr + "}";
            String username = parseJsonField(fullScoreObject, "username");
            String scoreValue = parseJsonField(fullScoreObject, "score");
            
            if (username != null && scoreValue != null) {
                try {
                    scores.add(new Score(username, Integer.parseInt(scoreValue.trim())));
                } catch (NumberFormatException e) {
                    Core.getLogger().warning("Failed to parse score value: " + scoreValue + " from JSON object: " + fullScoreObject);
                }
            }
        }
        return scores;
    }

    /**
     * A very basic and brittle parser for a string value from a JSON object.
     * Assumes the value is a string enclosed in double quotes or a number/boolean.
     */
    private String parseJsonField(String json, String fieldName) {
        try {
            String key = "\"" + fieldName + "\":";
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return null;

            int valueStartIndex = keyIndex + key.length();
            char firstChar = json.charAt(valueStartIndex);

            if (firstChar == '"') { // It's a string
                valueStartIndex++; // Move past the opening quote
                int valueEndIndex = json.indexOf('"', valueStartIndex);
                return json.substring(valueStartIndex, valueEndIndex);
            } else { // It's a number or boolean
                int valueEndIndex = json.indexOf(',', valueStartIndex);
                if (valueEndIndex == -1) { // It might be the last field
                    valueEndIndex = json.indexOf('}', valueStartIndex);
                }
                return json.substring(valueStartIndex, valueEndIndex).trim();
            }
        } catch (Exception e) {
            Core.getLogger().severe("Failed to parse field '" + fieldName + "' from JSON: " + json);
            return null;
        }
    }

    /**
     * A very basic and brittle parser for a nested JSON object.
     */
    private String parseJsonObject(String json, String fieldName) {
        try {
            String key = "\"" + fieldName + "\":{";
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return null;

            int objectStartIndex = keyIndex + key.length() - 1;
            int braceCount = 1;
            int objectEndIndex = -1;

            for (int i = objectStartIndex + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                if (braceCount == 0) {
                    objectEndIndex = i + 1;
                    break;
                }
            }
            return json.substring(objectStartIndex, objectEndIndex);
        } catch (Exception e) {
            Core.getLogger().severe("Failed to parse object '" + fieldName + "' from JSON: " + json);
            return null;
        }
    }
}
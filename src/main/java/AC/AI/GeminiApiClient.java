package AC.AI;

import AC.CLARA;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import lombok.Data;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Client for communicating with Google's Gemini AI API.
 * Handles API requests, response parsing, and error handling.
 */
public class GeminiApiClient {
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AIConfig config;
    private final String apiKey;
    private final String baseUrl;
    
    public GeminiApiClient(AIConfig config) {
        this.config = config;
        this.apiKey = config.getGeminiApiKey();
        this.baseUrl = config.getGeminiApiUrl();
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
            
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }
    
    /**
     * Sends a prompt to Gemini AI and gets a response asynchronously
     */
    public CompletableFuture<String> analyzeAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyze(prompt);
            } catch (Exception e) {
                CLARA.getInstance().getLogger().log(Level.WARNING, 
                    "Failed to analyze with Gemini AI", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Sends a prompt to Gemini AI and gets a response synchronously
     */
    public String analyze(String prompt) throws IOException {
        if (!config.isValidConfiguration()) {
            throw new IllegalStateException("Gemini API is not properly configured");
        }
        
        // Build request payload
        JsonObject requestBody = buildRequestBody(prompt);
        
        // Create HTTP request
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        String url = baseUrl + "?key=" + apiKey;
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();
        
        // Execute request
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                throw new IOException("Gemini API request failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            return parseResponse(responseBody);
        }
    }
    
    /**
     * Builds the JSON request body for Gemini API
     */
    private JsonObject buildRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", config.getTemperature());
        generationConfig.addProperty("maxOutputTokens", config.getMaxTokens());
        requestBody.add("generationConfig", generationConfig);
        
        // Add contents
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        
        content.add("parts", parts);
        contents.add(content);
        
        requestBody.add("contents", contents);
        
        return requestBody;
    }
    
    /**
     * Parses the response from Gemini API to extract the generated text
     */
    private String parseResponse(String responseBody) throws IOException {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            
            if (response.has("candidates")) {
                JsonArray candidates = response.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        
                        if (content.has("parts")) {
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                
                                if (part.has("text")) {
                                    return part.get("text").getAsString();
                                }
                            }
                        }
                    }
                }
            }
            
            // Check for errors in response
            if (response.has("error")) {
                JsonObject error = response.getAsJsonObject("error");
                String errorMessage = error.has("message") ? 
                    error.get("message").getAsString() : "Unknown error";
                throw new IOException("Gemini API error: " + errorMessage);
            }
            
            throw new IOException("Unexpected response format from Gemini API: " + responseBody);
            
        } catch (Exception e) {
            throw new IOException("Failed to parse Gemini API response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Tests the connection to Gemini API
     */
    public boolean testConnection() {
        try {
            String testPrompt = "Hello, this is a test message. Please respond with 'Connection successful'.";
            String response = analyze(testPrompt);
            return response != null && !response.trim().isEmpty();
        } catch (Exception e) {
            CLARA.getInstance().getLogger().log(Level.WARNING, 
                "Gemini API connection test failed", e);
            return false;
        }
    }
    
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
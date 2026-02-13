package com.ibm.csvviewer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for handling user authentication
 */
@ApplicationScoped
public class AuthenticationService {
    private static final Logger logger = Logger.getLogger(AuthenticationService.class.getName());
    
    // In-memory session storage (token -> username)
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    
    @Inject
    private Config config;
    
    private String getConfiguredUsername() {
        try {
            return config.getOptionalValue("app.auth.username", String.class).orElse("admin");
        } catch (Exception e) {
            logger.warning("Failed to read app.auth.username, using default: " + e.getMessage());
            return "admin";
        }
    }
    
    private String getConfiguredPassword() {
        try {
            return config.getOptionalValue("app.auth.password", String.class).orElse("admin");
        } catch (Exception e) {
            logger.warning("Failed to read app.auth.password, using default: " + e.getMessage());
            return "admin";
        }
    }

    /**
     * Authenticate user with username and password
     * @param username the username
     * @param password the password
     * @return authentication token if successful, null otherwise
     */
    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            logger.warning("Authentication failed: username or password is null");
            return null;
        }
        
        String configuredUsername = getConfiguredUsername();
        String configuredPassword = getConfiguredPassword();
        
        logger.info("Attempting authentication for user: " + username);
        logger.info("Configured username: '" + configuredUsername + "'");
        logger.info("Configured password: '" + configuredPassword + "'");
        logger.info("Provided username: '" + username + "'");
        logger.info("Provided password: '" + password + "'");
        logger.info("Username match: " + username.equals(configuredUsername));
        logger.info("Password match: " + password.equals(configuredPassword));
        
        // Check credentials against configured values
        if (username.equals(configuredUsername) && password.equals(configuredPassword)) {
            // Generate session token
            String token = UUID.randomUUID().toString();
            activeSessions.put(token, username);
            logger.info("User authenticated successfully: " + username);
            return token;
        }
        
        logger.warning("Authentication failed for user: " + username);
        return null;
    }

    /**
     * Validate a session token
     * @param token the session token
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        return activeSessions.containsKey(token);
    }

    /**
     * Get username associated with a token
     * @param token the session token
     * @return username if token is valid, null otherwise
     */
    public String getUsernameFromToken(String token) {
        return activeSessions.get(token);
    }

    /**
     * Logout user by invalidating their token
     * @param token the session token
     */
    public void logout(String token) {
        if (token != null) {
            String username = activeSessions.remove(token);
            if (username != null) {
                logger.info("User logged out: " + username);
            }
        }
    }

    /**
     * Get the number of active sessions
     * @return number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}

// Made with Bob
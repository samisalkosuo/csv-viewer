package com.ibm.csvviewer.api;

import com.ibm.csvviewer.model.LoginRequest;
import com.ibm.csvviewer.model.LoginResponse;
import com.ibm.csvviewer.service.AuthenticationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * REST API endpoint for authentication operations
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger logger = Logger.getLogger(AuthResource.class.getName());

    @Inject
    private AuthenticationService authService;

    /**
     * Login endpoint
     * @param loginRequest the login credentials
     * @return login response with token if successful
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for user: " + loginRequest.getUsername());
            
            String token = authService.authenticate(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            if (token != null) {
                LoginResponse response = new LoginResponse(
                    true, 
                    "Login successful", 
                    loginRequest.getUsername(), 
                    token
                );
                return Response.ok(response).build();
            } else {
                LoginResponse response = new LoginResponse(
                    false, 
                    "Invalid username or password"
                );
                return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
            }
        } catch (Exception e) {
            logger.severe("Login error: " + e.getMessage());
            LoginResponse response = new LoginResponse(false, "Login failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Logout endpoint
     * @param token the session token from Authorization header
     * @return logout response
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            
            if (token != null) {
                authService.logout(token);
                LoginResponse response = new LoginResponse(true, "Logout successful");
                return Response.ok(response).build();
            } else {
                LoginResponse response = new LoginResponse(false, "No valid token provided");
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } catch (Exception e) {
            logger.severe("Logout error: " + e.getMessage());
            LoginResponse response = new LoginResponse(false, "Logout failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Validate token endpoint
     * @param authHeader the Authorization header containing the token
     * @return validation response
     */
    @GET
    @Path("/validate")
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            
            if (token != null && authService.validateToken(token)) {
                String username = authService.getUsernameFromToken(token);
                LoginResponse response = new LoginResponse(true, "Token is valid", username, token);
                return Response.ok(response).build();
            } else {
                LoginResponse response = new LoginResponse(false, "Invalid or expired token");
                return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
            }
        } catch (Exception e) {
            logger.severe("Token validation error: " + e.getMessage());
            LoginResponse response = new LoginResponse(false, "Validation failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    /**
     * Extract token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

// Made with Bob
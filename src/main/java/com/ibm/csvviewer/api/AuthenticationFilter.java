package com.ibm.csvviewer.api;

import com.ibm.csvviewer.service.AuthenticationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filter to protect API endpoints with authentication
 * Excludes /auth endpoints from authentication requirement
 */
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final Logger logger = Logger.getLogger(AuthenticationFilter.class.getName());

    @Inject
    private AuthenticationService authService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        logger.info("Authentication filter checking path: " + path);
        
        // Skip authentication for login/validate endpoints
        if (path.contains("auth/login") || path.contains("auth/validate")) {
            logger.info("Skipping authentication for path: " + path);
            return;
        }
        
        // Get Authorization header
        String authHeader = requestContext.getHeaderString("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warning("Missing or invalid Authorization header for path: " + path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"success\": false, \"message\": \"Authentication required\"}")
                    .build()
            );
            return;
        }
        
        // Extract and validate token
        String token = authHeader.substring(7);
        if (!authService.validateToken(token)) {
            logger.warning("Invalid token for path: " + path);
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"success\": false, \"message\": \"Invalid or expired token\"}")
                    .build()
            );
        }
    }
}

// Made with Bob
# Multi-stage Dockerfile for OpenLiberty CSV Viewer Application

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM openliberty/open-liberty:full-java21-openj9-ubi-minimal

# Set labels
LABEL maintainer="IBM CSV Viewer Team"
LABEL description="OpenLiberty CSV Viewer Application"
LABEL version="1.0.0"

# Copy Liberty server configuration
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/

# Copy the WAR file from builder stage
COPY --chown=1001:0 --from=builder /app/target/csv-viewer.war /config/apps/

# Create data directory for file uploads with proper permissions
RUN mkdir -p /opt/ol/wlp/output/defaultServer/data/uploads && \
    chown -R 1001:0 /opt/ol/wlp/output/defaultServer/data && \
    chmod -R g+rw /opt/ol/wlp/output/defaultServer/data

# Set environment variables
ENV HTTP_PORT=9080
ENV HTTPS_PORT=9443

# Expose ports
EXPOSE 9080 9443

# Configure Liberty
RUN configure.sh

# Run the server
CMD ["/opt/ol/wlp/bin/server", "run", "defaultServer"]
# Docker Deployment Guide

This guide explains how to build and run the CSV Viewer application using Docker.

## Prerequisites

- Docker 20.10 or higher
- Docker Compose 2.0 or higher (optional, for easier deployment)
- 4GB RAM minimum
- 10GB disk space

## Quick Start

### Using Docker Compose (Recommended)

1. **Build and start the application:**
   ```bash
   docker-compose up -d
   ```

2. **Access the application:**
   - Open your browser to http://localhost:9080

3. **View logs:**
   ```bash
   docker-compose logs -f csv-viewer
   ```

4. **Stop the application:**
   ```bash
   docker-compose down
   ```

5. **Stop and remove data:**
   ```bash
   docker-compose down -v
   ```

### Using Docker CLI

1. **Build the image:**
   ```bash
   docker build -t csv-viewer:latest .
   ```

2. **Run the container:**
   ```bash
   docker run -d \
     --name csv-viewer-app \
     -p 9080:9080 \
     -p 9443:9443 \
     -v csv-data:/opt/ol/wlp/output/defaultServer/data \
     csv-viewer:latest
   ```

3. **Access the application:**
   - Open your browser to http://localhost:9080

4. **View logs:**
   ```bash
   docker logs -f csv-viewer-app
   ```

5. **Stop the container:**
   ```bash
   docker stop csv-viewer-app
   docker rm csv-viewer-app
   ```

## Dockerfile Details

The Dockerfile uses a **multi-stage build** approach:

### Stage 1: Builder
- Base image: `maven:3.9-eclipse-temurin-21`
- Compiles the Java application
- Packages the WAR file
- Dependencies are cached for faster rebuilds

### Stage 2: Runtime
- Base image: `icr.io/appcafe/open-liberty:full-java21-openj9-ubi`
- Official IBM OpenLiberty image with Java 21
- Optimized for production use
- Includes OpenJ9 JVM for better performance

## Configuration

### Environment Variables

You can customize the application using environment variables:

```bash
docker run -d \
  --name csv-viewer-app \
  -p 9080:9080 \
  -p 9443:9443 \
  -e HTTP_PORT=9080 \
  -e HTTPS_PORT=9443 \
  csv-viewer:latest
```

### Port Mapping

- **9080**: HTTP port (default)
- **9443**: HTTPS port (default)

To use different ports on the host:
```bash
docker run -d -p 8080:9080 -p 8443:9443 csv-viewer:latest
```

### Data Persistence

Uploaded CSV files are stored in `/opt/ol/wlp/output/defaultServer/data` inside the container.

**Using Docker volumes (recommended):**
```bash
docker run -d \
  -v csv-data:/opt/ol/wlp/output/defaultServer/data \
  csv-viewer:latest
```

**Using bind mounts:**
```bash
docker run -d \
  -v /path/on/host:/opt/ol/wlp/output/defaultServer/data \
  csv-viewer:latest
```

## Advanced Usage

### Custom Memory Limits

Limit container memory usage:
```bash
docker run -d \
  --memory="2g" \
  --memory-swap="2g" \
  csv-viewer:latest
```

### Custom JVM Options

Pass JVM options to OpenLiberty:
```bash
docker run -d \
  -e JVM_ARGS="-Xmx2g -Xms512m" \
  csv-viewer:latest
```

### Health Check

The container includes a health check that verifies the application is running:

```bash
# Check container health status
docker inspect --format='{{.State.Health.Status}}' csv-viewer-app
```

### Running in Production

For production deployments, consider:

1. **Use specific image tags:**
   ```bash
   docker build -t csv-viewer:1.0.0 .
   ```

2. **Enable HTTPS only:**
   - Modify server.xml to disable HTTP
   - Use proper SSL certificates

3. **Set resource limits:**
   ```yaml
   services:
     csv-viewer:
       deploy:
         resources:
           limits:
             cpus: '2'
             memory: 4G
           reservations:
             cpus: '1'
             memory: 2G
   ```

4. **Use secrets for sensitive data:**
   ```yaml
   services:
     csv-viewer:
       secrets:
         - db_password
   ```

## Building for Different Architectures

### Build for ARM64 (Apple Silicon, ARM servers)
```bash
docker buildx build --platform linux/arm64 -t csv-viewer:arm64 .
```

### Build for AMD64 (Intel/AMD)
```bash
docker buildx build --platform linux/amd64 -t csv-viewer:amd64 .
```

### Multi-platform build
```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t csv-viewer:latest \
  --push .
```

## Troubleshooting

### Container won't start

1. **Check logs:**
   ```bash
   docker logs csv-viewer-app
   ```

2. **Verify ports are available:**
   ```bash
   netstat -an | grep 9080
   ```

3. **Check container status:**
   ```bash
   docker ps -a
   ```

### Out of memory errors

Increase container memory:
```bash
docker run -d --memory="4g" csv-viewer:latest
```

### Permission issues with volumes

Ensure the volume has correct permissions:
```bash
docker run -d \
  --user 1001:0 \
  -v csv-data:/opt/ol/wlp/output/defaultServer/data \
  csv-viewer:latest
```

### Build fails

1. **Clear Docker cache:**
   ```bash
   docker builder prune -a
   ```

2. **Rebuild without cache:**
   ```bash
   docker build --no-cache -t csv-viewer:latest .
   ```

## Image Size Optimization

The current image uses a multi-stage build to minimize size:
- Builder stage: ~1.5GB (discarded)
- Final image: ~500MB

To further optimize:
1. Use `open-liberty:kernel-slim-java21` for smaller base image
2. Only include required Liberty features in server.xml
3. Remove unnecessary dependencies from pom.xml

## Security Considerations

1. **Run as non-root user:**
   - The container runs as user 1001 (non-root)

2. **Scan for vulnerabilities:**
   ```bash
   docker scan csv-viewer:latest
   ```

3. **Keep base images updated:**
   ```bash
   docker pull icr.io/appcafe/open-liberty:full-java21-openj9-ubi
   docker build -t csv-viewer:latest .
   ```

4. **Use read-only filesystem where possible:**
   ```bash
   docker run -d --read-only \
     --tmpfs /tmp \
     -v csv-data:/opt/ol/wlp/output/defaultServer/data \
     csv-viewer:latest
   ```

## Monitoring

### View resource usage
```bash
docker stats csv-viewer-app
```

### Export logs
```bash
docker logs csv-viewer-app > app.log 2>&1
```

### Access container shell
```bash
docker exec -it csv-viewer-app /bin/bash
```

## Cleanup

### Remove container and image
```bash
docker stop csv-viewer-app
docker rm csv-viewer-app
docker rmi csv-viewer:latest
```

### Remove volumes
```bash
docker volume rm csv-data
```

### Clean up everything
```bash
docker-compose down -v --rmi all
```

## Support

For issues related to:
- **Application**: See main README.md
- **Docker**: Check Docker logs and this guide
- **OpenLiberty**: Visit https://openliberty.io/docs/

---

**Built with ❤️ using OpenLiberty and Docker**
FROM python:3.12-slim

# Non-root user
RUN useradd -m appuser

WORKDIR /app

# Install dependencies first to maximise layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8501 \
    DATA_DIR=/tmp/csv-viewer \
    SESSION_ONLY=false \
    AUTH_ENABLED=false \
    APP_USERNAME=admin \
    APP_PASSWORD=admin \
    MAX_FILE_SIZE_MB=512 \
    APP_TITLE="CSV Viewer"

# Copy the application
COPY . /app

# Storage directory writable by appuser
RUN mkdir -p /tmp/csv-viewer \
    && chown -R appuser:appuser /app /tmp/csv-viewer \
    && chmod 777 /tmp/csv-viewer

USER appuser

EXPOSE 8501

ENTRYPOINT ["sh", "-c"]
CMD ["exec streamlit run app.py --server.port ${PORT} --server.address 0.0.0.0 --server.headless true --server.maxUploadSize ${MAX_FILE_SIZE_MB}"]

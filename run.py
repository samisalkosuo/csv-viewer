"""Launcher: reads env vars and starts Streamlit with matching server settings."""
import os
import subprocess
import sys

port = os.getenv("PORT", "8501")
max_upload = os.getenv("MAX_FILE_SIZE_MB", "200")

subprocess.run(
    [
        sys.executable, "-m", "streamlit", "run", "app.py",
        "--server.port", port,
        "--server.maxUploadSize", max_upload,
    ],
    check=True,
)

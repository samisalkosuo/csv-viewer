"""Configuration module for CSV Viewer.

Reads settings from environment variables with sensible defaults.
Loads .env files automatically at import time via python-dotenv.
"""

import os
from dotenv import load_dotenv

# Load .env file at import time if present
load_dotenv()


def _get_bool(key: str, default: bool) -> bool:
    val = os.getenv(key)
    if val is None:
        return default
    return val.strip().lower() in ("true", "1", "yes", "y", "t", "on")


def _get_int(key: str, default: int) -> int:
    val = os.getenv(key)
    if val is None:
        return default
    try:
        return int(val.strip())
    except ValueError:
        return default


# Typed configuration constants
AUTH_ENABLED: bool = _get_bool("AUTH_ENABLED", False)
APP_USERNAME: str = os.getenv("APP_USERNAME", "admin")
APP_PASSWORD: str = os.getenv("APP_PASSWORD", "admin")
SESSION_ONLY: bool = _get_bool("SESSION_ONLY", False)
DATA_DIR: str = os.getenv("DATA_DIR", "/tmp/csv-viewer")
MAX_FILE_SIZE_MB: int = _get_int("MAX_FILE_SIZE_MB", 200)
PORT: int = _get_int("PORT", 8501)
APP_TITLE: str = os.getenv("APP_TITLE", "CSV Viewer")

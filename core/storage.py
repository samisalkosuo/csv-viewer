"""File storage module for CSV Viewer.

Supports session-only in-memory storage (Streamlit session_state)
and persistent file storage on disk (DATA_DIR).
"""

from dataclasses import dataclass, asdict
from datetime import datetime, timezone
import io
import json
import os
from typing import Optional
import uuid

import pandas as pd
import streamlit as st

from core.config import DATA_DIR, SESSION_ONLY
from core.detect import detect_delimiter

# Global in-memory fallback for testing outside active Streamlit runtime
_FALLBACK_FILES: dict[str, pd.DataFrame] = {}
_FALLBACK_META: dict[str, "FileInfo"] = {}


@dataclass
class FileInfo:
    """Metadata for an uploaded CSV file."""

    file_id: str
    name: str
    upload_time: str
    row_count: int
    col_count: int
    size_bytes: int
    delimiter: str


def _get_session_store() -> tuple[dict[str, pd.DataFrame], dict[str, FileInfo]]:
    """Retrieve session storage dictionaries from Streamlit session_state with safe fallback."""
    try:
        if "_csv_files" not in st.session_state:
            st.session_state["_csv_files"] = {}
        if "_csv_meta" not in st.session_state:
            st.session_state["_csv_meta"] = {}
        return st.session_state["_csv_files"], st.session_state["_csv_meta"]
    except Exception:
        return _FALLBACK_FILES, _FALLBACK_META


def _get_metadata_path() -> str:
    return os.path.join(DATA_DIR, "metadata.json")


def _read_persistent_metadata() -> list[FileInfo]:
    metadata_path = _get_metadata_path()
    if not os.path.exists(metadata_path):
        return []
    try:
        with open(metadata_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return [FileInfo(**item) for item in data]
    except Exception:
        return []


def _write_persistent_metadata(meta_list: list[FileInfo]) -> None:
    os.makedirs(DATA_DIR, exist_ok=True)
    metadata_path = _get_metadata_path()
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump([asdict(item) for item in meta_list], f, indent=2)


def save_file(name: str, raw_bytes: bytes) -> str:
    """Save an uploaded CSV file and return its generated file_id UUID."""
    file_id = str(uuid.uuid4())
    delim = detect_delimiter(raw_bytes)

    # Parse dataframe to obtain row and column counts
    df = pd.read_csv(io.BytesIO(raw_bytes), sep=delim)
    row_count = int(len(df))
    col_count = int(len(df.columns))
    size_bytes = len(raw_bytes)
    upload_time = datetime.now(timezone.utc).isoformat()

    file_info = FileInfo(
        file_id=file_id,
        name=name,
        upload_time=upload_time,
        row_count=row_count,
        col_count=col_count,
        size_bytes=size_bytes,
        delimiter=delim,
    )

    if SESSION_ONLY:
        files, meta = _get_session_store()
        files[file_id] = df
        meta[file_id] = file_info
    else:
        os.makedirs(DATA_DIR, exist_ok=True)
        csv_path = os.path.join(DATA_DIR, f"{file_id}.csv")
        with open(csv_path, "wb") as f:
            f.write(raw_bytes)

        existing_meta = _read_persistent_metadata()
        existing_meta.append(file_info)
        _write_persistent_metadata(existing_meta)

    return file_id


def load_file(file_id: str) -> pd.DataFrame:
    """Load and return DataFrame for the given file_id."""
    if SESSION_ONLY:
        files, _ = _get_session_store()
        if file_id not in files:
            raise FileNotFoundError(f"File '{file_id}' not found in session storage.")
        return files[file_id]

    csv_path = os.path.join(DATA_DIR, f"{file_id}.csv")
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"File '{file_id}' not found on disk at {csv_path}.")

    # Find delimiter from metadata
    meta_list = _read_persistent_metadata()
    info = next((m for m in meta_list if m.file_id == file_id), None)
    delim = info.delimiter if info else ","

    return pd.read_csv(csv_path, sep=delim)


def list_files() -> list[FileInfo]:
    """Return metadata list for all stored files."""
    if SESSION_ONLY:
        _, meta = _get_session_store()
        return list(meta.values())

    return _read_persistent_metadata()


def delete_file(file_id: str) -> None:
    """Delete the stored file and its metadata by file_id."""
    if SESSION_ONLY:
        files, meta = _get_session_store()
        files.pop(file_id, None)
        meta.pop(file_id, None)
    else:
        csv_path = os.path.join(DATA_DIR, f"{file_id}.csv")
        if os.path.exists(csv_path):
            try:
                os.remove(csv_path)
            except OSError:
                pass

        existing_meta = _read_persistent_metadata()
        filtered_meta = [m for m in existing_meta if m.file_id != file_id]
        _write_persistent_metadata(filtered_meta)

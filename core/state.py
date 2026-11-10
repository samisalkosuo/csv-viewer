"""Persist UI state (active file, chart/table selections) across page reloads.

Streamlit's ``st.session_state`` is discarded when the browser page is reloaded.
To survive a reload, a random browser-session id (``sid``) is kept in the URL
query string and the state is saved server-side as JSON in ``DATA_DIR/state``.

Persisted keys are ``active_file_id`` plus every session_state key starting with
``PERSIST_PREFIX`` ("p_"); values must be JSON-serialisable.

Persistence is disabled when ``SESSION_ONLY`` is true, since the uploaded files
themselves would not survive a reload either.
"""

import copy
import json
import os
import re
import uuid

import streamlit as st

from core.config import DATA_DIR, SESSION_ONLY

PERSIST_PREFIX = "p_"
ACTIVE_FILE_KEY = "active_file_id"

_SID_RE = re.compile(r"^[0-9a-f]{32}$")


def _state_path(sid: str) -> str:
    return os.path.join(DATA_DIR, "state", f"{sid}.json")


def _read(sid: str) -> dict:
    try:
        with open(_state_path(sid), "r", encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, dict) else {}
    except (OSError, ValueError):
        return {}


def _write(sid: str, data: dict) -> None:
    path = _state_path(sid)
    try:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tmp_path = f"{path}.tmp"
        with open(tmp_path, "w", encoding="utf-8") as f:
            json.dump(data, f)
        os.replace(tmp_path, path)
    except OSError:
        pass  # persistence is best-effort; never break the page


def init_state() -> None:
    """Restore persisted state into st.session_state. Call at the top of every page
    (after authentication) and before any persisted widget is created."""
    if SESSION_ONLY:
        return

    first_run = "_sid" not in st.session_state
    if first_run:
        url_sid = st.query_params.get("sid")
        sid = url_sid if url_sid and _SID_RE.match(url_sid) else uuid.uuid4().hex
        persisted = _read(sid)
        st.session_state["_sid"] = sid
        st.session_state["_persisted"] = persisted
        st.session_state["_persisted_json"] = json.dumps(persisted, sort_keys=True)

    sid = st.session_state["_sid"]
    # Navigating between pages drops the query string; put the id back.
    if st.query_params.get("sid") != sid:
        st.query_params["sid"] = sid

    # Widget keys are discarded by Streamlit when their page is left, so restore
    # missing ones on every run. The active file is only restored on a fresh
    # session, otherwise a deliberate deselect/delete would be undone.
    for key, value in st.session_state["_persisted"].items():
        if key in st.session_state:
            continue
        if key.startswith(PERSIST_PREFIX) or (first_run and key == ACTIVE_FILE_KEY):
            st.session_state[key] = copy.deepcopy(value)


def save_state() -> None:
    """Write the current persisted keys to disk. Call after the page's widgets have
    been created (and before any ``st.stop()`` that follows them)."""
    if SESSION_ONLY or "_sid" not in st.session_state:
        return

    new = dict(st.session_state["_persisted"])
    for key, value in st.session_state.items():
        if isinstance(key, str) and key.startswith(PERSIST_PREFIX):
            new[key] = value

    active = st.session_state.get(ACTIVE_FILE_KEY)
    if active:
        new[ACTIVE_FILE_KEY] = active
    else:
        new.pop(ACTIVE_FILE_KEY, None)

    serialized = json.dumps(new, sort_keys=True)
    if serialized != st.session_state.get("_persisted_json"):
        _write(st.session_state["_sid"], json.loads(serialized))
        st.session_state["_persisted"] = json.loads(serialized)
        st.session_state["_persisted_json"] = serialized

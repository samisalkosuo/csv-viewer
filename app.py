"""CSV Viewer — Streamlit entry point (home / upload page)."""

import streamlit as st

from core.auth import auth_required, logout
from core.config import APP_TITLE, AUTH_ENABLED, MAX_FILE_SIZE_MB
from core.storage import delete_file, list_files, save_file

st.set_page_config(page_title=APP_TITLE, page_icon="📊", layout="wide")

auth_required()


def _select_file(file_id: str) -> None:
    st.session_state["active_file_id"] = file_id


def _delete_file(file_id: str) -> None:
    delete_file(file_id)
    if st.session_state.get("active_file_id") == file_id:
        st.session_state.pop("active_file_id", None)


st.title(f"📊 {APP_TITLE}")
st.write("Upload a CSV file to instantly explore column statistics, charts and a filterable table.")

uploaded = st.file_uploader(
    f"Drag & drop a CSV file here (max {MAX_FILE_SIZE_MB} MB)",
    type=["csv"],
)

if uploaded is not None:
    # The uploader keeps its file across reruns; only process each upload once.
    processed = st.session_state.setdefault("_processed_uploads", set())
    if uploaded.file_id not in processed:
        if uploaded.size > MAX_FILE_SIZE_MB * 1024 * 1024:
            st.error(f"File is too large. The maximum allowed size is {MAX_FILE_SIZE_MB} MB.")
        else:
            try:
                new_id = save_file(uploaded.name, uploaded.getvalue())
            except Exception as exc:  # unparseable / empty CSV
                st.error(f"Could not read '{uploaded.name}': {exc}")
            else:
                processed.add(uploaded.file_id)
                st.session_state["active_file_id"] = new_id
                st.success("File uploaded! Navigate to Summary to explore.")

files = list_files()

with st.sidebar:
    st.header("Loaded Files")
    active_id = st.session_state.get("active_file_id")
    if not files:
        st.caption("No files loaded yet.")
    for info in files:
        name_col, del_col = st.columns([5, 1])
        name_col.button(
            info.name,
            key=f"select_{info.file_id}",
            on_click=_select_file,
            args=(info.file_id,),
            type="primary" if info.file_id == active_id else "secondary",
            use_container_width=True,
        )
        del_col.button(
            "🗑️",
            key=f"delete_{info.file_id}",
            on_click=_delete_file,
            args=(info.file_id,),
            help=f"Delete {info.name}",
        )
        st.caption(f"{info.row_count:,} rows × {info.col_count:,} cols")

    if AUTH_ENABLED:
        st.divider()
        if st.button("Logout", use_container_width=True):
            logout()

if not files:
    st.info("Upload a CSV file to get started")

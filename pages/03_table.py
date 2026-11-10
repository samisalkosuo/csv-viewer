"""Table page: filterable and sortable table view with CSV download."""

import pandas as pd
import streamlit as st

from core.auth import auth_required
from core.config import APP_TITLE
from core.state import init_state, save_state
from core.storage import list_files, load_file

st.set_page_config(page_title=APP_TITLE, page_icon="📊", layout="wide")

auth_required()
init_state()

DELIM_MAP = {
    "Comma (,)": ",",
    "Semicolon (;)": ";",
    "Tab (\\t)": "\t",
}


@st.cache_data(show_spinner="Loading data...")
def _load_df(file_id: str) -> pd.DataFrame:
    return load_file(file_id)


file_id = st.session_state.get("active_file_id")
if not file_id:
    st.info("Please upload a CSV file on the Home page.")
    st.stop()

try:
    df = _load_df(file_id)
except FileNotFoundError:
    st.session_state.pop("active_file_id", None)
    save_state()

    st.warning("The selected file is no longer available. Please upload a CSV file on the Home page.")
    st.stop()

file_info = next((f for f in list_files() if f.file_id == file_id), None)

st.title("📄 Table View")
if file_info:
    st.caption(f"{file_info.name} · delimiter: `{file_info.delimiter!r}` · {file_info.size_bytes / 1024:,.1f} KB")

# Sidebar filtering controls
with st.sidebar:
    st.header("Filter Controls")
    # Keys start with "p_" so core.state persists them across page reloads.
    global_search = st.text_input("Global search", placeholder="Search all columns...", key="p_table_search")
    inverse_search = st.checkbox(
        "Inverse search", help="Exclude rows that match the filter criteria", key="p_table_inverse"
    )

    with st.expander("Column Filters", expanded=False):
        col_filters: dict[str, str] = {}
        for col in df.columns:
            val = st.text_input(f"Filter by {col}", key=f"p_col_filter_{file_id}_{col}")
            if val.strip():
                col_filters[col] = val.strip()

    st.subheader("Columns")
    # Per-column visibility stored as a dict {str(col): bool} keyed to the file
    # (string keys so it survives a JSON round trip)
    vis_state_key = f"p_col_vis_{file_id}"
    if vis_state_key not in st.session_state:
        st.session_state[vis_state_key] = {str(col): True for col in df.columns}
    vis = st.session_state[vis_state_key]
    for col in df.columns:
        vis.setdefault(str(col), True)

    # "Show all" / "Hide all" quick actions
    qa_col1, qa_col2 = st.columns(2)
    if qa_col1.button("Show all", use_container_width=True):
        for col in df.columns:
            vis[str(col)] = True
    if qa_col2.button("Hide all", use_container_width=True):
        for col in df.columns:
            vis[str(col)] = False

    # Per-column toggle rows: eye icon button + column name
    for col in df.columns:
        icon = "👁️" if vis[str(col)] else "🙈"
        if st.button(f"{icon}  {col}", key=f"vis_toggle_{file_id}_{col}", use_container_width=True):
            vis[str(col)] = not vis[str(col)]

    visible_cols = [col for col in df.columns if vis.get(str(col), True)]

save_state()

# Apply filters
clean_global = global_search.strip()
has_global = bool(clean_global)
has_col_filters = bool(col_filters)

if has_global or has_col_filters:
    matches = pd.Series(True, index=df.index)

    if has_global:
        global_match = pd.Series(False, index=df.index)
        for col in df.columns:
            col_series = df[col].fillna("").astype(str)
            global_match |= col_series.str.contains(clean_global, case=False, regex=False)
        matches &= global_match

    if has_col_filters:
        col_match = pd.Series(True, index=df.index)
        for col, term in col_filters.items():
            col_series = df[col].fillna("").astype(str)
            col_match &= col_series.str.contains(term, case=False, regex=False)
        matches &= col_match

    if inverse_search:
        filtered_df = df[~matches]
    else:
        filtered_df = df[matches]
else:
    filtered_df = df

if not visible_cols:
    st.warning("Please select at least one column to display.")
    st.stop()

# Table display
st.caption(f"Showing {len(filtered_df):,} of {len(df):,} rows")
st.dataframe(
    filtered_df[visible_cols],
    width="stretch",
    hide_index=True,
    column_order=visible_cols,
)

# Download section
st.subheader("Download filtered data")
delim_choice = st.radio("Delimiter", ["Comma (,)", "Semicolon (;)", "Tab (\\t)"], horizontal=True, key="p_download_delim")
delim_char = DELIM_MAP.get(delim_choice, ",")

orig_name = file_info.name if file_info else "data.csv"
base_name = orig_name[:-4] if orig_name.lower().endswith(".csv") else orig_name
download_name = f"{base_name}_filtered.csv"

csv_data = filtered_df[visible_cols].to_csv(sep=delim_char, index=False).encode("utf-8")

st.download_button(
    label="Download CSV",
    data=csv_data,
    file_name=download_name,
    mime="text/csv",
)

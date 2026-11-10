"""CSV Viewer — Streamlit entry point: sets up the app and routes between the top tabs.

Navigation is rendered as tabs at the top of the page (no left navigation). The
sidebar only appears on the pages that put controls in it (Charts, Table).
"""

import streamlit as st

from core.auth import auth_required, logout
from core.config import APP_TITLE, AUTH_ENABLED
from core.state import init_state

st.set_page_config(page_title=APP_TITLE, page_icon="📊", layout="wide")

auth_required()
init_state()

pg = st.navigation(
    [
        st.Page("pages/00_upload.py", title="App", icon="🏠", default=True),
        st.Page("pages/01_summary.py", title="Summary", icon="📋", url_path="summary"),
        st.Page("pages/02_charts.py", title="Charts", icon="📈", url_path="charts"),
        st.Page("pages/03_table.py", title="Table", icon="📄", url_path="table"),
    ],
    position="top",
)

# Keep the top tabs pinned top-left across the full width, and start the sidebar
# (Charts / Table controls) below them instead of beside them.
st.markdown(
    """
    <style>
    header[data-testid="stHeader"] {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        width: 100%;
        z-index: 999991;
    }
    [data-testid="stSidebarContent"] {
        padding-top: 3.75rem;
    }
    </style>
    """,
    unsafe_allow_html=True,
)

if AUTH_ENABLED:
    _, logout_col = st.columns([8, 1])
    if logout_col.button("Logout", use_container_width=True):
        logout()

pg.run()

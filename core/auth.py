"""Authentication module for CSV Viewer.

Provides optional single-user authentication controlled by the AUTH_ENABLED
environment variable. When enabled, displays a login form and halts further
page rendering until valid credentials are provided.
"""

import streamlit as st

from core.config import APP_PASSWORD, APP_USERNAME, AUTH_ENABLED


def is_authenticated() -> bool:
    """Check if the user is currently authenticated."""
    if not AUTH_ENABLED:
        return True
    return bool(st.session_state.get("authenticated", False))


def logout() -> None:
    """Clear authentication state and rerun."""
    st.session_state["authenticated"] = False
    st.session_state.pop("username", None)
    st.rerun()


def auth_required() -> None:
    """Enforce authentication if AUTH_ENABLED is True.

    If not authenticated, renders a centered login form and calls st.stop()
    to prevent the rest of the page from executing.
    """
    if not AUTH_ENABLED:
        return

    if st.session_state.get("authenticated", False):
        return

    # Render centered login form
    _, col, _ = st.columns([1, 2, 1])
    with col:
        with st.form("login_form"):
            st.subheader("Login to CSV Viewer")
            username = st.text_input("Username")
            password = st.text_input("Password", type="password")
            submitted = st.form_submit_button("Login", use_container_width=True)

            if submitted:
                if username == APP_USERNAME and password == APP_PASSWORD:
                    st.session_state["authenticated"] = True
                    st.session_state["username"] = username
                    st.rerun()
                else:
                    st.error("Invalid credentials")

    # Stop rendering the rest of the page until authenticated
    st.stop()

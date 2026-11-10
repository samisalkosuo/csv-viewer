"""Summary page: dataset overview, per-column statistics and auto-charts."""

import html

import pandas as pd
import plotly.express as px
import streamlit as st

from core.auth import auth_required
from core.config import APP_TITLE
from core.stats import ColumnStat, compute_stats, get_df_overview
from core.storage import list_files, load_file

st.set_page_config(page_title=APP_TITLE, page_icon="📊", layout="wide")

auth_required()

TYPE_COLORS = {
    "numeric": "#2563eb",
    "categorical": "#16a34a",
    "datetime": "#ea580c",
    "boolean": "#9333ea",
    "text": "#6b7280",
}


@st.cache_data(show_spinner="Loading data...")
def _load_df(file_id: str) -> pd.DataFrame:
    return load_file(file_id)


@st.cache_data(show_spinner="Computing statistics...")
def _compute_stats(file_id: str, _df: pd.DataFrame) -> list[ColumnStat]:
    return compute_stats(_df)


def _fmt(value) -> str:
    if value is None:
        return "—"
    if isinstance(value, float):
        return f"{value:,.4g}" if abs(value) < 1e6 else f"{value:,.0f}"
    return f"{value:,}" if isinstance(value, int) else str(value)


def _type_stat_lines(stat: ColumnStat) -> list[str]:
    """Return 'label: value' markdown lines for the type-specific statistics."""
    if stat.inferred_type == "numeric":
        return [
            f"**Min:** {_fmt(stat.min_val)} &nbsp; **Max:** {_fmt(stat.max_val)}",
            f"**Mean:** {_fmt(stat.mean)} &nbsp; **Median:** {_fmt(stat.median)}",
            f"**Std:** {_fmt(stat.std)}",
            f"**P25:** {_fmt(stat.p25)} &nbsp; **P75:** {_fmt(stat.p75)}",
        ]
    if stat.inferred_type == "datetime":
        return [
            f"**From:** {html.escape((stat.min_date or '—')[:10])}",
            f"**To:** {html.escape((stat.max_date or '—')[:10])}",
            f"**Range:** {_fmt(stat.range_days)} days",
        ]
    if stat.inferred_type == "categorical" and stat.top_values:
        lines = ["**Top values:**"]
        lines += [f"- `{html.escape(v)}` — {c:,}" for v, c in stat.top_values]
        return lines
    return []


def _render_stat_card(stat: ColumnStat) -> None:
    color = TYPE_COLORS.get(stat.inferred_type, TYPE_COLORS["text"])
    with st.container(border=True):
        st.markdown(
            f"**{html.escape(stat.name)}** &nbsp; "
            f"<span style='background:{color};color:#fff;padding:2px 8px;"
            f"border-radius:10px;font-size:0.75em'>{stat.inferred_type}</span>",
            unsafe_allow_html=True,
        )
        st.progress(
            min(max(stat.null_pct / 100, 0.0), 1.0),
            text=f"Nulls: {stat.null_count:,} ({stat.null_pct:.1f}%)",
        )
        st.markdown(f"**Unique:** {stat.unique_count:,}")
        for line in _type_stat_lines(stat):
            st.markdown(line, unsafe_allow_html=True)


def _build_charts(df: pd.DataFrame, stats: list[ColumnStat]) -> list:
    """Create one Plotly figure per eligible column (skipping all-null columns)."""
    figures = []
    for i, stat in enumerate(stats):
        if stat.null_count >= len(df):
            continue
        series = df.iloc[:, i]
        name = stat.name

        if stat.inferred_type == "numeric":
            data = pd.DataFrame({name: pd.to_numeric(series, errors="coerce")}).dropna()
            if not data.empty:
                figures.append(px.histogram(data, x=name, title=f"Distribution: {name}"))

        elif stat.inferred_type == "categorical":
            counts = series.dropna().astype(str).value_counts().head(10).reset_index()
            counts.columns = [name, "count"]
            figures.append(px.bar(counts, x=name, y="count", title=f"Top values: {name}"))

        elif stat.inferred_type == "datetime":
            dates = pd.to_datetime(series, errors="coerce").dropna()
            if dates.empty:
                continue
            monthly = (stat.range_days or 0) > 180
            counts = dates.dt.to_period("M" if monthly else "W").dt.start_time.value_counts().sort_index()
            data = counts.rename_axis(name).reset_index(name="count")
            figures.append(
                px.bar(data, x=name, y="count", title=f"Timeline ({'monthly' if monthly else 'weekly'}): {name}")
            )
    return figures


file_id = st.session_state.get("active_file_id")
if not file_id:
    st.info("Please upload a CSV file on the Home page.")
    st.stop()

try:
    df = _load_df(file_id)
except FileNotFoundError:
    st.session_state.pop("active_file_id", None)
    st.warning("The selected file is no longer available. Please upload a CSV file on the Home page.")
    st.stop()

file_info = next((f for f in list_files() if f.file_id == file_id), None)

st.title("📋 Summary")
if file_info:
    st.caption(f"{file_info.name} · delimiter: `{file_info.delimiter!r}` · {file_info.size_bytes / 1024:,.1f} KB")

overview = get_df_overview(df)
m1, m2, m3, m4 = st.columns(4)
m1.metric("Total Rows", f"{overview['total_rows']:,}")
m2.metric("Total Columns", f"{overview['total_cols']:,}")
m3.metric("Total Nulls", f"{overview['total_nulls']:,}", help=f"{overview['null_pct']}% of all cells")
m4.metric("Memory (KB)", f"{overview['memory_kb']:,}")

stats = _compute_stats(file_id, df)

st.subheader("Column Statistics")
for start in range(0, len(stats), 3):
    cols = st.columns(3)
    for col, stat in zip(cols, stats[start : start + 3]):
        with col:
            _render_stat_card(stat)


with st.expander("Auto Charts", expanded=True):
    figures = _build_charts(df, stats)
    if not figures:
        st.write("No charts available for this dataset.")
    for start in range(0, len(figures), 2):
        cols = st.columns(2)
        for col, fig in zip(cols, figures[start : start + 2]):
            with col:
                st.plotly_chart(fig, width="stretch")

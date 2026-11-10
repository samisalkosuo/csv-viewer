"""Chart builder page: choose a chart type and map columns to axes without coding."""

import pandas as pd
import plotly.express as px
import streamlit as st

from core.auth import auth_required
from core.config import APP_TITLE
from core.state import init_state, save_state
from core.storage import load_file

st.set_page_config(page_title=APP_TITLE, page_icon="📊", layout="wide")

auth_required()
init_state()

CHART_TYPES = ["Bar", "Line", "Scatter", "Histogram", "Box", "Pie"]
NO_Y = "None / Count"
NO_COLOR = "None"
RAW = "None (raw values)"
AGGREGATIONS = [RAW, "Count", "Sum", "Mean", "Median", "Min", "Max"]
PX_FUNCS = {
    "Bar": px.bar,
    "Line": px.line,
    "Scatter": px.scatter,
    "Histogram": px.histogram,
    "Box": px.box,
    "Pie": px.pie,
}


@st.cache_data(show_spinner="Loading data...")
def _load_df(file_id: str) -> pd.DataFrame:
    return load_file(file_id)


def _aggregate(df: pd.DataFrame, x_col: str, y_col: str | None, color_col: str | None, agg: str):
    """Group by X (and colour) and aggregate Y (or count rows when Y is None).

    Returns (aggregated DataFrame, name of the value column).
    """
    keys = [x_col] + ([color_col] if color_col and color_col != x_col else [])
    if y_col is None:
        value_col = "count" if "count" not in keys else "count (rows)"
        return df.groupby(keys, dropna=True).size().reset_index(name=value_col), value_col
    if y_col in keys:
        raise ValueError("Y axis must differ from the X axis and Color column when aggregating.")
    value_col = f"{agg.lower()}({y_col})"
    grouped = df.groupby(keys, dropna=True)[y_col].agg(agg.lower()).reset_index()
    return grouped.rename(columns={y_col: value_col}), value_col


def _build_figure(df, chart_type, x_col, y_col, color_col, agg, title):
    """Return (figure, plotted DataFrame, aggregated flag)."""
    color = None if chart_type == "Pie" else color_col
    plot_df, y_plot = df, y_col

    # Bar/Line without a Y column count rows per X; otherwise aggregate when requested.
    aggregated = chart_type != "Histogram" and (
        (y_col is None and chart_type in ("Bar", "Line")) or (y_col is not None and agg != RAW)
    )
    if aggregated:
        plot_df, y_plot = _aggregate(df, x_col, y_col, color, agg if y_col else "Count")

    kwargs = {"title": title}
    if color:
        kwargs["color"] = color

    if chart_type == "Histogram":
        fig = px.histogram(plot_df, x=x_col, **kwargs)
    elif chart_type == "Pie":
        if y_plot:
            fig = px.pie(plot_df, names=x_col, values=y_plot, **kwargs)
        else:
            fig = px.pie(plot_df, names=x_col, **kwargs)
    else:
        if chart_type == "Scatter" and y_plot is None:
            raise ValueError("Scatter charts need a Y axis column.")
        fig = PX_FUNCS[chart_type](plot_df, x=x_col, y=y_plot, **kwargs)
    return fig, plot_df, aggregated


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

st.title("📈 Chart Builder")

columns = [str(c) for c in df.columns]

with st.sidebar:
    st.header("Chart settings")
    # Keys start with "p_" so core.state persists them across page reloads;
    # column-dependent ones are scoped to the file so options always match.
    chart_type = st.selectbox("Chart type", CHART_TYPES, key="p_chart_type")
    x_col = st.selectbox("X axis", columns, key=f"p_chart_x_{file_id}")
    y_choice = st.selectbox(
        "Y axis",
        [NO_Y] + columns,
        index=0,
        disabled=chart_type == "Histogram",
        help="Not used for Histogram. Leave as 'None / Count' to count rows.",
        key=f"p_chart_y_{file_id}",
    )
    color_choice = st.selectbox(
        "Color / Group by",
        [NO_COLOR] + columns,
        disabled=chart_type == "Pie",
        help="Not used for Pie charts.",
        key=f"p_chart_color_{file_id}",
    )

    y_col = None if (y_choice == NO_Y or chart_type == "Histogram") else y_choice
    color_col = None if color_choice == NO_COLOR else color_choice

    agg = RAW
    if y_col is not None:
        agg = st.selectbox("Aggregation", AGGREGATIONS, key="p_chart_agg")

    title_key = f"p_chart_title_{file_id}_{chart_type}_{x_col}"
    st.session_state.setdefault(title_key, f"{chart_type}: {x_col}")
    title = st.text_input("Title", key=title_key)

save_state()

try:
    fig, plot_df, aggregated = _build_figure(df, chart_type, x_col, y_col, color_col, agg, title)
    st.plotly_chart(fig, width="stretch")
    if aggregated:
        st.caption(f"Chart based on {len(plot_df):,} aggregated rows (from {len(df):,} source rows)")
    else:
        st.caption(f"Chart based on {len(plot_df):,} rows")
except Exception as e:  # Plotly/pandas raise a variety of errors for incompatible column types
    st.error(str(e))
    st.stop()

# Export as self-contained HTML — no external renderer or browser dependency needed
html_str = fig.to_html(full_html=True, include_plotlyjs="cdn")
st.download_button(
    "Download chart as HTML",
    data=html_str,
    file_name="chart.html",
    mime="text/html",
)

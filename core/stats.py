"""Column statistics computation and type inference."""

from __future__ import annotations

import math
import warnings
from dataclasses import dataclass, field

import pandas as pd

PARSE_THRESHOLD = 0.8
BOOLEAN_VALUES = frozenset({"true", "false", "yes", "no", "1", "0", "y", "n"})
CATEGORICAL_MAX_RATIO = 0.5
CATEGORICAL_MAX_UNIQUE = 50
TOP_N = 5
ROUND_DIGITS = 4


@dataclass
class ColumnStat:
    """Statistics for a single column.

    ``null_pct`` is relative to the total row count; ``unique_pct`` is relative
    to the number of non-null values. Both are expressed as percentages (0-100).
    """

    name: str
    inferred_type: str  # numeric | categorical | datetime | boolean | text
    null_count: int = 0
    null_pct: float = 0.0
    unique_count: int = 0
    unique_pct: float = 0.0
    # numeric
    min_val: float | None = field(default=None)
    max_val: float | None = field(default=None)
    mean: float | None = field(default=None)
    median: float | None = field(default=None)
    std: float | None = field(default=None)
    p25: float | None = field(default=None)
    p75: float | None = field(default=None)
    # categorical
    top_values: list[tuple[str, int]] | None = field(default=None)
    # datetime
    min_date: str | None = field(default=None)
    max_date: str | None = field(default=None)
    range_days: int | None = field(default=None)


def _round(value) -> float | None:
    """Round to 4 decimals; return None for missing / NaN / inf values."""
    if value is None or pd.isna(value):
        return None
    value = float(value)
    if math.isinf(value):
        return None
    return round(value, ROUND_DIGITS)


def _to_numeric(series: pd.Series) -> pd.Series:
    return pd.to_numeric(series, errors="coerce")


def _to_datetime(series: pd.Series) -> pd.Series:
    # Format is inferred per column by pandas; the "could not infer format"
    # warning is expected for free-form text and is not actionable here.
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        try:
            return pd.to_datetime(series, errors="coerce")
        except (ValueError, TypeError, OverflowError):
            return pd.Series(pd.NaT, index=series.index, dtype="datetime64[ns]")


def _infer_type(non_null: pd.Series) -> str:
    """Infer the column type from its non-null values."""
    total = len(non_null)
    if total == 0:
        return "text"

    if pd.api.types.is_bool_dtype(non_null):
        return "boolean"

    if pd.api.types.is_numeric_dtype(non_null) or (
        _to_numeric(non_null).notna().sum() / total >= PARSE_THRESHOLD
    ):
        return "numeric"

    if pd.api.types.is_datetime64_any_dtype(non_null) or (
        _to_datetime(non_null).notna().sum() / total >= PARSE_THRESHOLD
    ):
        return "datetime"

    as_text = non_null.astype(str).str.strip().str.lower()
    if as_text.isin(BOOLEAN_VALUES).all():
        return "boolean"

    unique_count = non_null.nunique()
    if unique_count / total <= CATEGORICAL_MAX_RATIO and unique_count <= CATEGORICAL_MAX_UNIQUE:
        return "categorical"

    return "text"


def _numeric_stats(non_null: pd.Series) -> dict:
    values = _to_numeric(non_null).dropna()
    if values.empty:
        return {}
    return {
        "min_val": _round(values.min()),
        "max_val": _round(values.max()),
        "mean": _round(values.mean()),
        "median": _round(values.median()),
        "std": _round(values.std()),
        "p25": _round(values.quantile(0.25)),
        "p75": _round(values.quantile(0.75)),
    }


def _datetime_stats(non_null: pd.Series) -> dict:
    values = _to_datetime(non_null).dropna()
    if values.empty:
        return {}
    min_date, max_date = values.min(), values.max()
    return {
        "min_date": min_date.isoformat(),
        "max_date": max_date.isoformat(),
        "range_days": int((max_date - min_date).days),
    }


def _categorical_stats(non_null: pd.Series) -> dict:
    counts = non_null.value_counts().head(TOP_N)
    return {"top_values": [(str(value), int(count)) for value, count in counts.items()]}


def _compute_column_stat(name: str, series: pd.Series) -> ColumnStat:
    total_rows = len(series)
    non_null = series.dropna()
    non_null_count = len(non_null)
    null_count = total_rows - non_null_count
    unique_count = int(non_null.nunique())

    stat = ColumnStat(
        name=str(name),
        inferred_type=_infer_type(non_null),
        null_count=int(null_count),
        null_pct=round(null_count / total_rows * 100, ROUND_DIGITS) if total_rows else 0.0,
        unique_count=unique_count,
        unique_pct=round(unique_count / non_null_count * 100, ROUND_DIGITS) if non_null_count else 0.0,
    )

    if stat.inferred_type == "numeric":
        extra = _numeric_stats(non_null)
    elif stat.inferred_type == "datetime":
        extra = _datetime_stats(non_null)
    elif stat.inferred_type == "categorical":
        extra = _categorical_stats(non_null)
    else:
        extra = {}

    for key, value in extra.items():
        setattr(stat, key, value)
    return stat


def compute_stats(df: pd.DataFrame) -> list[ColumnStat]:
    """Compute a ColumnStat for every column of ``df``."""
    return [_compute_column_stat(name, df.iloc[:, i]) for i, name in enumerate(df.columns)]


def get_df_overview(df: pd.DataFrame) -> dict:
    """Return high-level dataset figures: rows, columns, nulls and memory usage."""
    total_rows, total_cols = df.shape
    total_nulls = int(df.isna().sum().sum())
    total_cells = total_rows * total_cols
    return {
        "total_rows": int(total_rows),
        "total_cols": int(total_cols),
        "total_nulls": total_nulls,
        "null_pct": round(total_nulls / total_cells * 100, ROUND_DIGITS) if total_cells else 0.0,
        "memory_kb": round(float(df.memory_usage(deep=True).sum()) / 1024, 1),
    }

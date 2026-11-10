# CSV Viewer — Python / Streamlit Refactor Plan

## Top-Level Overview

Replace the existing Java + OpenLiberty application with a Python + Streamlit application.
The goal is maximum ease-of-use: any user with a CSV file should be able to upload it and
immediately start exploring — summary stats, charts, table filtering — without any
configuration or technical knowledge required.

**Key decisions:**
- **Language / runtime**: Python 3.12+
- **UI framework**: Streamlit (data-science focused, minimal boilerplate, built-in state)
- **Charts**: Plotly Express (interactive, renders natively in Streamlit)
- **CSV processing**: pandas (industry-standard, handles large files well)
- **Auth**: Optional — toggled via env var `AUTH_ENABLED`, single user via `APP_USERNAME` / `APP_PASSWORD`
- **Storage**: Session-only by default (`SESSION_ONLY=true`); persistent when `DATA_DIR` is set (default `/tmp/csv-viewer`)
- **Deployment**: Standalone (`streamlit run app.py`) or Docker (single-stage, small image)
- **Config**: 100% via environment variables, sensible defaults for every setting

**Out of scope for this refactor:**
- Multi-user accounts / RBAC
- Excel / JSON / XML export
- AI / LLM natural language queries
- The existing Java source is removed; the Python app lives at the repo root

---

## Architecture Overview

```
csv-viewer/
├── app.py                  # Streamlit entry point — page router
├── pages/
│   ├── 01_summary.py       # Column stats + auto-charts
│   ├── 02_charts.py        # Manual chart builder
│   └── 03_table.py         # Filterable / sortable table + download
├── core/
│   ├── auth.py             # Optional authentication logic
│   ├── storage.py          # File load / persist / session helpers
│   ├── stats.py            # Column statistics computation
│   └── detect.py           # Delimiter + data-type detection
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── sample-data/            # (kept from existing repo)
└── README.md               # Rewritten for Python
```

---

## Environment Variables Reference

| Variable | Default | Description |
|---|---|---|
| `AUTH_ENABLED` | `false` | Set to `true` to require login |
| `APP_USERNAME` | `admin` | Login username (only used when AUTH_ENABLED=true) |
| `APP_PASSWORD` | `admin` | Login password (only used when AUTH_ENABLED=true) |
| `SESSION_ONLY` | `true` | When `true`, files live only in session memory |
| `DATA_DIR` | `/tmp/csv-viewer` | Directory for persistent file storage (used when SESSION_ONLY=false) |
| `MAX_FILE_SIZE_MB` | `200` | Upload size limit in MB |
| `PORT` | `8501` | Streamlit server port |
| `APP_TITLE` | `CSV Viewer` | Browser tab / header title |

---

## Sub-Tasks

---

### Sub-Task 1 — Scaffold Python project structure

**Status:** [x] done

**Intent:**
Create the new Python project skeleton and remove/archive the Java source so the repo is
clean. The scaffold sets up every file that subsequent sub-tasks will fill in, giving a
consistent layout from the start.

**Expected Outcomes:**
- `app.py`, `pages/`, `core/` directories and placeholder files exist
- `requirements.txt` lists all dependencies with pinned major versions
- `.env.example` documents every supported env variable with defaults
- `sample-data/` is preserved unchanged
- Java source tree (`src/`, `pom.xml`, `target/`) is deleted
- Existing `Dockerfile` and `docker-compose.yml` are replaced with Python equivalents (stubs, filled in Sub-Task 6)
- `README.md` is replaced with a Python-focused one (stub, expanded in Sub-Task 7)

**Todo List:**
1. Delete `src/`, `pom.xml`, `target/` (Java artefacts)
2. Delete old `Dockerfile`, `docker-compose.yml`, `DOCKER.md`, `QUICKSTART.md`
3. Create `app.py` (entry point stub)
4. Create `pages/01_summary.py`, `pages/02_charts.py`, `pages/03_table.py` (stubs)
5. Create `core/__init__.py`, `core/auth.py`, `core/storage.py`, `core/stats.py`, `core/detect.py` (stubs)
6. Write `requirements.txt` with: `streamlit`, `pandas`, `plotly`, `python-dotenv`
7. Write `.env.example` with all env variables and inline comments
8. Write stub `README.md`

**Relevant Context:**
- Existing `sample-data/` directory must be preserved
- Existing `.gitignore` should be updated to ignore `__pycache__`, `*.pyc`, `.env`, `data/`

---

### Sub-Task 2 — Core: config, auth, storage

**Status:** [x] done

**Intent:**
Implement the three foundational modules that every page depends on:
config reading from env vars, optional authentication, and file storage (session vs persistent).
These must be done before any UI work so pages can import them without circular dependencies.

**Expected Outcomes:**
- `core/auth.py` — exposes `auth_required()` which either no-ops (AUTH_ENABLED=false) or shows
  a Streamlit login form and blocks the page until valid credentials are entered; credentials
  stored in `st.session_state`
- `core/storage.py` — exposes `save_file(name, bytes) -> file_id`, `load_file(file_id) -> DataFrame`,
  `list_files() -> list[FileInfo]`, `delete_file(file_id)`; respects `SESSION_ONLY` vs `DATA_DIR`
- `core/detect.py` — exposes `detect_delimiter(raw_bytes) -> str`; replicates the Java
  `DelimiterDetector` logic (test first 10 lines, score by consistency + frequency)
- All config values read via a single `core/config.py` module using `os.getenv` with defaults

**Todo List:**
1. Create `core/config.py` — read all env vars, expose typed constants
2. Implement `core/detect.py` — delimiter detection (comma, semicolon, tab, pipe)
3. Implement `core/storage.py` — session-only path (in-memory dict in `st.session_state`) and persistent path (save to `DATA_DIR/{uuid}.csv` + `DATA_DIR/metadata.json`)
4. Implement `core/auth.py` — `auth_required()` function using Streamlit form + `st.session_state`

**Relevant Context:**
- Java equivalent: `DelimiterDetector.java`, `FileStorageService.java`, `AuthenticationService.java`
- Session state key conventions: `st.session_state["files"]` for session-only file dict,
  `st.session_state["authenticated"]` for auth state
- `python-dotenv` loaded at app startup so `.env` files work for local dev

---

### Sub-Task 3 — Core: column statistics

**Status:** [x] done

**Intent:**
Implement the column statistics engine that powers the Summary page and the stats sidebar
visible on every page. This is the primary "quick analysis" feature.

**Expected Outcomes:**
- `core/stats.py` — exposes `compute_stats(df: DataFrame) -> list[ColumnStat]`
- `ColumnStat` dataclass fields per column:
  - `name`, `inferred_type` (numeric / categorical / datetime / boolean / text)
  - `null_count`, `null_pct`
  - `unique_count`, `unique_pct`
  - For numeric: `min`, `max`, `mean`, `median`, `std`, `p25`, `p75`
  - For categorical: `top_values` (top 5 value + count pairs)
  - For datetime: `min_date`, `max_date`, `range_days`
- Type inference: try numeric parse → try datetime parse → check boolean keywords → check cardinality (≤20 unique = categorical) → else text

**Todo List:**
1. Define `ColumnStat` dataclass in `core/stats.py`
2. Implement type inference logic
3. Implement per-type stat computation using pandas
4. Expose `compute_stats(df)` function
5. Add `get_df_overview(df)` helper returning total rows, total columns, total nulls, memory usage

**Relevant Context:**
- No Java equivalent — this is new functionality
- pandas `describe()`, `nunique()`, `isna().sum()`, `infer_objects()` are the key methods
- Stats are computed once and cached in `st.session_state` per file id

---

### Sub-Task 4 — Page: Upload + Summary (01_summary.py + app.py)

**Status:** [x] done

**Intent:**
Build the home/landing page and the Summary page. The home page (`app.py`) is the first
thing a user sees — it must make uploading a CSV file trivially easy. The Summary page
delivers immediate analytical value the moment a file is loaded.

**Expected Outcomes:**
- `app.py` renders:
  - App title and short description
  - Large file uploader widget (drag & drop)
  - If `SESSION_ONLY=false`: previously uploaded files listed as clickable cards with delete button
  - After upload/select: redirect user to Summary page automatically
- `pages/01_summary.py` renders:
  - Dataset overview card (rows, columns, nulls %, memory)
  - Per-column stats cards in a responsive grid (3 columns desktop)
  - For each column: inferred type badge, null bar, unique count, type-specific stats
  - Auto-charts section: for numeric columns → histogram; for categorical columns → top-10 bar chart; for datetime columns → timeline count chart
  - Charts rendered with Plotly Express, interactive (hover, zoom, pan)

**Todo List:**
1. Implement file upload flow in `app.py` (uses `core/storage.py`, `core/detect.py`)
2. Implement file selector sidebar showing loaded/persistent files
3. Set active file in `st.session_state["active_file_id"]` on selection
4. Implement `pages/01_summary.py` — overview card using `core/stats.py`
5. Implement per-column stat cards (use `st.columns(3)` grid)
6. Implement auto-chart generation loop using Plotly Express
7. Call `core/auth.auth_required()` at the top of `app.py` and each page

**Relevant Context:**
- Java equivalent UI: `index.html` left sidebar file list + file metadata display
- Streamlit multipage: pages in `pages/` are auto-discovered; `app.py` is the home page
- Use `st.cache_data` to cache DataFrame loading and stats computation per file id

---

### Sub-Task 5 — Page: Chart Builder (02_charts.py)

**Status:** [x] done

**Intent:**
Build the manual chart builder page where users can choose chart type and map columns to
axes without any coding. This complements the auto-charts on the Summary page.

**Expected Outcomes:**
- Sidebar controls: chart type selector, X axis, Y axis (optional), colour/group-by column (optional), aggregation function (count, sum, mean, median)
- Supported chart types: Bar, Line, Scatter, Histogram, Box, Pie
- Chart rendered full-width with Plotly Express, interactive
- "Export chart as PNG" button (Plotly `write_image` or Streamlit download button with `fig.to_image()`)
- Graceful handling: if no file is loaded → prompt user to upload on home page

**Todo List:**
1. Implement column type–aware axis selectors (numeric columns for Y, any for X)
2. Implement chart type → Plotly Express function mapping
3. Implement aggregation logic (groupby + agg before charting)
4. Render chart with `st.plotly_chart(fig, use_container_width=True)`
5. Add PNG export download button
6. Add guard: redirect to home if no active file

**Relevant Context:**
- Plotly Express functions: `px.bar`, `px.line`, `px.scatter`, `px.histogram`, `px.box`, `px.pie`
- Aggregation: `df.groupby(x_col)[y_col].agg(func).reset_index()`

---

### Sub-Task 6 — Page: Table View (03_table.py)

**Status:** [x] done

**Intent:**
Rebuild the existing table view with filtering, sorting, column hiding, and CSV download
using Streamlit's native data components — preserving feature parity with the Java version
while being simpler and more interactive.

**Expected Outcomes:**
- Global search text input (filters all columns, case-insensitive substring)
- Per-column filter expander (one text input per column, collapsible)
- Inverse search toggle checkbox
- Column visibility multi-select (hide/show columns)
- Paginated table using `st.dataframe` with native sorting
- Row count display ("Showing X of Y rows")
- "Download filtered data as CSV" button
- Delimiter selector for download (comma, semicolon, tab)

**Todo List:**
1. Implement global search filter on the DataFrame
2. Implement per-column filters (AND logic, same as Java version)
3. Implement inverse search toggle
4. Implement column visibility selector using `st.multiselect`
5. Render table with `st.dataframe` (uses Streamlit's built-in sort)
6. Show filtered row count
7. Implement CSV download with selected delimiter via `st.download_button`
8. Add guard: redirect to home if no active file

**Relevant Context:**
- Java equivalent: `StreamingCsvReader.matchesFilter()`, `CsvResource` download endpoint, `app.js` table rendering
- pandas filter: `df[df.apply(lambda row: term in str(row).lower(), axis=1)]`
- `st.dataframe` supports `column_config` for type-aware rendering

---

### Sub-Task 7 — Docker, deployment, and documentation

**Status:** [x] done

**Intent:**
Write the Dockerfile, docker-compose.yml, and updated README so the application is
immediately runnable both standalone and in a container with zero additional setup.

**Expected Outcomes:**
- `Dockerfile`: single-stage, `python:3.12-slim` base, installs requirements, copies app, exposes `PORT`, runs `streamlit run app.py`
- `docker-compose.yml`: mounts `./data` to `/tmp/csv-viewer`, passes all env vars, health check on `/_stcore/health`
- `.env.example`: every variable with inline comment and sensible default
- `README.md`: Quick start (3 commands), env variable table, standalone run instructions, Docker run one-liner, feature overview with screenshots placeholder
- `QUICKSTART.md`: single-page cheat sheet for the impatient

**Todo List:**
1. Write `Dockerfile` (python:3.12-slim, non-root user, proper ENTRYPOINT)
2. Write `docker-compose.yml` with volume, env vars, health check
3. Write `.env.example`
4. Write full `README.md`
5. Write `QUICKSTART.md`
6. Update `.gitignore`: add `__pycache__/`, `*.pyc`, `.env`, `data/`, `.streamlit/secrets.toml`

**Relevant Context:**
- Streamlit health endpoint: `GET /_stcore/health` returns `"ok"`
- Streamlit config can be passed via CLI flags: `--server.port`, `--server.headless true`, `--server.address 0.0.0.0`
- Non-root Docker user pattern: `RUN useradd -m appuser && chown -R appuser /app`
- `/tmp/csv-viewer` is world-writable on Linux so no permission issues in any container runtime

---

## Implementation Order

```
Sub-Task 1  →  Sub-Task 2  →  Sub-Task 3
                                   ↓
Sub-Task 4  ←──────────────────────┘
     ↓
Sub-Task 5  →  Sub-Task 6  →  Sub-Task 7
```

Sub-tasks 1–3 are foundational (no UI). Sub-tasks 4–6 are pages (depend on core).
Sub-task 7 is packaging (depends on everything).

# CSV Viewer

CSV Viewer is a Python + Streamlit app for exploring CSV files. Upload a file and instantly get column statistics, automatic charts, a manual chart builder and a filterable table — no configuration required.

## Features

- **Upload any CSV** — drag & drop, up to `MAX_FILE_SIZE_MB` (default 200 MB)
- **Auto-delimiter detection** — comma, semicolon, tab and pipe
- **Column stats** — inferred type, nulls, unique counts, min/max/mean/median/quartiles, top values, date ranges
- **Auto-charts** — histograms, top-10 bar charts and timelines generated for you
- **Chart builder** — Bar, Line, Scatter, Histogram, Box and Pie charts with aggregation and PNG export
- **Filterable table** — global search, per-column filters, inverse search, column hiding, sorting and CSV download
- **Optional auth** — single-user login toggled with `AUTH_ENABLED`
- **Docker-ready** — single `Dockerfile` with all defaults baked in

<!-- Screenshots placeholder: add images of the Summary, Chart Builder and Table View pages here -->

## Quick Start

### Standalone (Python)

```bash
pip install -r requirements.txt
python run.py
```

Then open <http://localhost:8501>. Set `MAX_FILE_SIZE_MB` in `.env` or as an environment variable before running to change the upload limit.

### Docker

Build and run:

```bash
docker build -t csv-viewer .
docker run -p 8501:8501 csv-viewer
```

Persist uploads across restarts (mounts a named volume):

```bash
docker run -p 8501:8501 -v mydata:/tmp/csv-viewer csv-viewer
```

Session-only mode (no disk writes, files lost on browser close):

```bash
docker run -p 8501:8501 -e SESSION_ONLY=true csv-viewer
```

## Configuration

All configuration is done through environment variables; every setting has a default.

| Variable | Default | Description |
|---|---|---|
| `AUTH_ENABLED` | `false` | Set to `true` to require login |
| `APP_USERNAME` | `admin` | Login username (only used when `AUTH_ENABLED=true`) |
| `APP_PASSWORD` | `admin` | Login password (only used when `AUTH_ENABLED=true`) |
| `SESSION_ONLY` | `false` | When `true`, files live only in session memory (lost on browser close) |
| `DATA_DIR` | `/tmp/csv-viewer` | Directory for persistent file storage (used when `SESSION_ONLY=false`) |
| `MAX_FILE_SIZE_MB` | `200` | Upload size limit in MB |
| `PORT` | `8501` | Streamlit server port |
| `APP_TITLE` | `CSV Viewer` | Browser tab / header title |

> Change the default `APP_USERNAME` / `APP_PASSWORD` before enabling authentication on a shared host.

To keep uploads between sessions, set `SESSION_ONLY=false` and mount a volume at `DATA_DIR`.

## Pages

- **Home** (`app.py`) — upload a CSV, see and switch between loaded files, delete files.
- **Summary** — dataset overview (rows, columns, nulls, memory), per-column statistics cards and auto-generated charts.
- **Chart Builder** — choose a chart type, axes, colour/group-by and aggregation, then export the chart as PNG.
- **Table View** — search and filter rows, hide columns, sort, and download the filtered data as CSV with a delimiter of your choice.

## Development

Run locally:

```bash
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python run.py
```

Configuration can be loaded from a `.env` file. Copy the template and edit it; it is read automatically at startup via `python-dotenv`:

```bash
cp .env.example .env
```

Project layout:

```
app.py          # Home / upload page
pages/          # Summary, Chart Builder, Table View
core/           # config, auth, storage, stats, delimiter detection
```

## Sample Data

The [`sample-data/`](sample-data/) directory contains example files (`sample-comma.csv`, `sample-semicolon.csv`) you can upload to try the app.

## License

See [LICENSE](LICENSE).

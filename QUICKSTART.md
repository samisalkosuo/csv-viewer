# CSV Viewer — Quick Start

## Run it (pick one)

**1. pip**
```bash
pip install -r requirements.txt
python run.py
```

**2. Docker**
```bash
docker build -t csv-viewer .
docker run -it --rm -p 8501:8501 csv-viewer
```

## Analyse a CSV in 3 steps

1. Open <http://localhost:8501> in your browser.
2. Drag & drop your CSV file onto the uploader (try `sample-data/sample-comma.csv`).
3. Explore the pages in the sidebar: **Summary**, **Chart Builder**, **Table View**.

## Handy settings

Set as environment variables (or in a `.env` file):

- `AUTH_ENABLED=true` — require login (`APP_USERNAME` / `APP_PASSWORD`)
- `SESSION_ONLY=false` — keep uploaded files between sessions

Full documentation: [README.md](README.md)

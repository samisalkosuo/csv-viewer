# Quick Start Guide

Get the CSV Viewer application up and running in 5 minutes!

## Prerequisites

- Java 21 installed
- Maven 3.8+ installed
- 2GB RAM available

## Steps

### 1. Build the Application

```bash
mvn clean package
```

This will download dependencies and build the application.

### 2. Start the Server

```bash
mvn liberty:dev
```

Wait for the message: `The defaultServer server is ready to run a smarter planet.`

### 3. Open the Application

Open your browser and go to:
```
http://localhost:9080
```

### 4. Upload a CSV File

Try one of the sample files in the `sample-data/` directory:
- `sample-comma.csv` - Comma-delimited
- `sample-semicolon.csv` - Semicolon-delimited

Or use your own CSV file!

### 5. Explore Features

- **Sort**: Click column headers
- **Search**: Use the search boxes
- **Filter**: Try inverse search
- **Download**: Export filtered data
- **Page**: Navigate through large datasets

## Stopping the Server

Press `Ctrl+C` in the terminal where the server is running.

## Troubleshooting

### Port Already in Use
If port 9080 is already in use, edit `pom.xml` and change:
```xml
<liberty.var.http.port>9080</liberty.var.http.port>
```
to another port like `9081`.

### Out of Memory
Increase heap size:
```bash
export MAVEN_OPTS="-Xmx2g"
mvn liberty:dev
```

### Build Fails
Make sure you have Java 21:
```bash
java -version
```

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Try uploading larger CSV files
- Experiment with different delimiters
- Test the search and filter features

## Sample Data

The `sample-data/` directory contains example CSV files:

**sample-comma.csv** (10 rows)
- Employee data with comma delimiter
- Columns: ID, Name, Email, Age, City, Country, Salary, Department

**sample-semicolon.csv** (6 rows)
- Same structure with semicolon delimiter
- Tests automatic delimiter detection

## Common Commands

```bash
# Build only
mvn clean package

# Run in dev mode (hot reload)
mvn liberty:dev

# Run tests
mvn test

# Clean build artifacts
mvn clean

# Package for deployment
mvn clean package
# Output: target/csv-viewer.war
```

## Development Mode Features

When running with `mvn liberty:dev`:
- **Hot Reload**: Code changes automatically restart the server
- **Test on Save**: Tests run when you save files
- **Quick Iteration**: Fast development cycle

Press `Enter` in the terminal to run tests manually.

## API Testing

You can test the API directly:

```bash
# List uploaded files
curl http://localhost:9080/api/csv/list

# Get file metadata
curl http://localhost:9080/api/csv/{fileId}/metadata

# Get paginated data
curl "http://localhost:9080/api/csv/{fileId}/data?page=0&pageSize=10"
```

## File Locations

- **Uploaded Files**: `data/uploads/`
- **Metadata**: `data/metadata.json`
- **Logs**: Check console output

## Support

For issues or questions, see the main [README.md](README.md) or open an issue on GitHub.

---

**Happy CSV Viewing! 📊**
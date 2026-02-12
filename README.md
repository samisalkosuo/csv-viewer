# CSV Viewer

A powerful web application built with OpenLiberty (Java 21) for uploading, viewing, and analyzing CSV files of any size. Features automatic delimiter detection, pagination, sorting, searching, and data export capabilities.

## Features

### Core Functionality
- ✅ **File Upload**: Drag-and-drop or browse to upload CSV files (up to 500MB)
- ✅ **Automatic Delimiter Detection**: Supports comma (,), semicolon (;), tab (\t), and pipe (|)
- ✅ **Streaming Parser**: Memory-efficient processing for large CSV files
- ✅ **File Management**: View list of uploaded files with metadata
- ✅ **Persistent Storage**: Files and metadata stored on server

### Data Viewing
- ✅ **Paginated Display**: Configurable page sizes (10, 25, 50, 100 rows)
- ✅ **Column Sorting**: Click column headers to sort ascending/descending
- ✅ **Colored Rows**: Alternating row colors for better readability
- ✅ **Responsive Design**: Works on desktop and mobile devices

### Search & Filter
- ✅ **Global Search**: Search across all columns simultaneously
- ✅ **Column-Specific Search**: Search within individual columns
- ✅ **Inverse Search**: Exclude rows matching search criteria
- ✅ **Real-time Filtering**: Results update as you type

### Export
- ✅ **Download Filtered Data**: Export currently filtered/sorted data as CSV
- ✅ **Preserve Formatting**: Maintains proper CSV escaping

## Technology Stack

- **Backend**: Java 21, OpenLiberty 24.x
- **REST API**: JAX-RS 3.1
- **JSON Processing**: JSON-B 3.0
- **CSV Parsing**: Apache Commons CSV 1.11.0
- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **Build Tool**: Maven

## Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- 2GB RAM minimum (more for large CSV files)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/csv-viewer.git
cd csv-viewer
```

### 2. Build the Application

```bash
mvn clean package
```

This will:
- Compile the Java code
- Run tests
- Package the application as a WAR file
- Download and configure OpenLiberty

### 3. Run the Application

```bash
mvn liberty:dev
```

The application will start on:
- **HTTP**: http://localhost:9080
- **HTTPS**: https://localhost:9443

The `liberty:dev` mode enables hot reload - changes to code will automatically restart the server.

### 4. Access the Application

Open your browser and navigate to:
```
http://localhost:9080
```

## Usage Guide

### Uploading a CSV File

1. **Drag and Drop**: Drag a CSV file onto the upload area
2. **Browse**: Click "Browse Files" to select a file from your computer
3. **Wait**: The file will be uploaded and processed automatically
4. **View**: The file will open in the viewer once processing is complete

### Viewing CSV Data

- **Navigate Pages**: Use the pagination controls at the bottom
- **Change Page Size**: Select rows per page (10, 25, 50, 100)
- **Sort Columns**: Click any column header to sort
  - First click: Sort ascending (▲)
  - Second click: Sort descending (▼)
  - Third click: Remove sorting

### Searching Data

**Global Search:**
- Type in the "Search all columns" box
- Searches across all columns simultaneously
- Case-insensitive

**Column-Specific Search:**
- Use the individual search boxes below each column name
- Searches only within that specific column
- Multiple column searches are combined (AND logic)

**Inverse Search:**
- Check the "Inverse Search" checkbox
- Excludes rows that match your search criteria
- Useful for filtering out unwanted data

### Downloading Filtered Data

1. Apply any filters or sorting you want
2. Click the "⬇️ Download Filtered" button
3. A CSV file with the filtered/sorted data will be downloaded
4. The original file on the server remains unchanged

### Managing Files

- **View File List**: All uploaded files are shown in the "Uploaded Files" section
- **View File**: Click "👁️ View" to open a file in the viewer
- **Delete File**: Click "🗑️ Delete" to remove a file (requires confirmation)

## Project Structure

```
csv-viewer/
├── src/
│   ├── main/
│   │   ├── java/com/ibm/csvviewer/
│   │   │   ├── api/                    # REST API endpoints
│   │   │   │   ├── CsvResource.java
│   │   │   │   └── CsvViewerApplication.java
│   │   │   ├── model/                  # Data models
│   │   │   │   ├── CsvData.java
│   │   │   │   ├── CsvMetadata.java
│   │   │   │   └── SearchFilter.java
│   │   │   ├── service/                # Business logic
│   │   │   │   ├── CsvParserService.java
│   │   │   │   └── FileStorageService.java
│   │   │   └── util/                   # Utilities
│   │   │       ├── DelimiterDetector.java
│   │   │       └── StreamingCsvReader.java
│   │   ├── webapp/                     # Frontend
│   │   │   ├── css/
│   │   │   │   └── styles.css
│   │   │   ├── js/
│   │   │   │   └── app.js
│   │   │   └── index.html
│   │   └── liberty/config/
│   │       └── server.xml              # OpenLiberty configuration
│   └── test/                           # Unit tests
├── data/                               # Runtime data (created automatically)
│   ├── uploads/                        # Uploaded CSV files
│   └── metadata.json                   # File metadata
├── pom.xml                             # Maven configuration
└── README.md
```

## API Endpoints

### Upload File
```
POST /api/csv/upload
Content-Type: multipart/form-data
Body: file (CSV file)
Response: CsvMetadata object
```

### List Files
```
GET /api/csv/list
Response: Array of CsvMetadata objects
```

### Get File Metadata
```
GET /api/csv/{fileId}/metadata
Response: CsvMetadata object
```

### Get Paginated Data
```
GET /api/csv/{fileId}/data
Query Parameters:
  - page: Page number (0-based)
  - pageSize: Rows per page
  - sortColumn: Column name to sort by
  - sortOrder: 'asc' or 'desc'
  - globalSearch: Search term for all columns
  - inverseSearch: true/false
Response: CsvData object
```

### Download Filtered CSV
```
GET /api/csv/{fileId}/download
Query Parameters:
  - sortColumn: Column name to sort by
  - sortOrder: 'asc' or 'desc'
  - globalSearch: Search term
  - inverseSearch: true/false
Response: CSV file
```

### Delete File
```
DELETE /api/csv/{fileId}
Response: Success message
```

## Configuration

### File Size Limit
Default: 500MB. To change, edit `CsvResource.java`:
```java
private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
```

### Storage Directory
Default: `data/uploads`. To change, edit `FileStorageService.java`:
```java
private static final String STORAGE_DIR = "data/uploads";
```

### Server Ports
Default: HTTP 9080, HTTPS 9443. To change, edit `pom.xml`:
```xml
<liberty.var.http.port>9080</liberty.var.http.port>
<liberty.var.https.port>9443</liberty.var.https.port>
```

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
```

The WAR file will be in `target/csv-viewer.war`

### Deploying to OpenLiberty
1. Copy `target/csv-viewer.war` to your Liberty server's `dropins` directory
2. Start the server
3. Access the application at the configured URL

## Troubleshooting

### File Upload Fails
- Check file size (must be < 500MB)
- Ensure file is a valid CSV
- Check server logs for errors

### Delimiter Not Detected Correctly
- The auto-detection analyzes the first 10 lines
- For unusual formats, ensure consistent delimiter usage
- Check that quoted fields are properly escaped

### Out of Memory Errors
- Increase JVM heap size: `-Xmx4g`
- Reduce page size for very large files
- Consider splitting extremely large files

### Port Already in Use
- Change ports in `pom.xml`
- Or stop the process using the port

## Performance Considerations

- **Large Files**: The streaming parser handles files of any size efficiently
- **Memory Usage**: Only the current page is loaded into memory
- **Search Performance**: Filtering requires scanning the entire file
- **Concurrent Users**: Each user's operations are independent

## Security Notes

- Files are stored with UUID names to prevent conflicts
- Input validation prevents malicious file uploads
- CSV content is escaped to prevent XSS attacks
- Consider adding authentication for production use

## License

MIT License - see LICENSE file for details

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check existing issues for solutions
- Review the troubleshooting section

## Roadmap

Future enhancements:
- [ ] Excel file support (.xlsx)
- [ ] Data visualization (charts/graphs)
- [ ] Column filtering by data type
- [ ] Export to different formats (JSON, XML)
- [ ] User authentication and authorization
- [ ] File sharing capabilities
- [ ] Advanced analytics features

---

**Built with ❤️ using OpenLiberty and Java 21**

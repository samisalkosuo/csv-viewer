// Authentication check
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }
    
    // Validate token
    validateAuthToken(token);
    
    // Display username and logout button
    displayUserInfo();
});

async function validateAuthToken(token) {
    try {
        const response = await fetch('/api/auth/validate', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            // Token is invalid, redirect to login
            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
            window.location.href = '/login.html';
        }
    } catch (error) {
        console.error('Token validation error:', error);
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
        window.location.href = '/login.html';
    }
}

function displayUserInfo() {
    const username = localStorage.getItem('username');
    const headerRight = document.querySelector('.header-right');
    
    const userInfoDiv = document.createElement('div');
    userInfoDiv.className = 'user-info';
    userInfoDiv.innerHTML = `
        <span class="username">👤 ${username}</span>
        <button id="logout-btn" class="btn btn-secondary">Logout</button>
    `;
    
    headerRight.appendChild(userInfoDiv);
    
    // Add logout handler
    document.getElementById('logout-btn').addEventListener('click', logout);
}

async function logout() {
    const token = localStorage.getItem('authToken');
    
    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
    } catch (error) {
        console.error('Logout error:', error);
    }
    
    // Clear local storage and redirect
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    window.location.href = '/login.html';
}

// Helper function to get auth headers
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return {
        'Authorization': `Bearer ${token}`
    };
}

// CSV Viewer Application
class CsvViewerApp {
    constructor() {
        this.currentFileId = null;
        this.currentPage = 0;
        this.pageSize = 25;
        this.sortColumn = null;
        this.sortOrder = 'asc';
        this.globalSearch = '';
        this.columnSearch = {};
        this.inverseSearch = false;
        this.currentMetadata = null;
        
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadFileList();
    }

    setupEventListeners() {
        // Upload button
        const fileInput = document.getElementById('file-input');
        const browseButton = document.getElementById('browse-button');

        browseButton.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', (e) => this.handleFileSelect(e.target.files[0]));

        // Viewer controls
        document.getElementById('close-viewer-btn').addEventListener('click', () => {
            this.closeViewer();
        });

        document.getElementById('download-btn').addEventListener('click', () => {
            this.downloadFilteredCsv();
        });

        // Search controls
        document.getElementById('global-search').addEventListener('input', (e) => {
            this.globalSearch = e.target.value;
            this.currentPage = 0;
            this.loadCsvData();
        });

        document.getElementById('inverse-search').addEventListener('change', (e) => {
            this.inverseSearch = e.target.checked;
            this.currentPage = 0;
            this.loadCsvData();
        });

        // Pagination controls
        document.getElementById('page-size-select').addEventListener('change', (e) => {
            this.pageSize = parseInt(e.target.value);
            this.currentPage = 0;
            this.loadCsvData();
        });

        document.getElementById('first-page-btn').addEventListener('click', () => {
            this.currentPage = 0;
            this.loadCsvData();
        });

        document.getElementById('prev-page-btn').addEventListener('click', () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.loadCsvData();
            }
        });

        document.getElementById('next-page-btn').addEventListener('click', () => {
            this.currentPage++;
            this.loadCsvData();
        });

        document.getElementById('last-page-btn').addEventListener('click', () => {
            // Will be calculated based on total rows
            this.loadCsvData();
        });
    }

    async handleFileSelect(file) {
        if (!file) return;

        if (!file.name.toLowerCase().endsWith('.csv')) {
            this.showError('Please select a CSV file');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        this.showProgress(0);

        try {
            const response = await fetch('/api/csv/upload', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: formData
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Upload failed');
            }

            const metadata = await response.json();
            this.showProgress(100);
            
            setTimeout(() => {
                this.hideProgress();
                this.loadFileList();
                this.openViewer(metadata.id);
            }, 500);

        } catch (error) {
            this.hideProgress();
            this.showError('Upload failed: ' + error.message);
        }
    }

    async loadFileList() {
        try {
            const response = await fetch('/api/csv/list', {
                headers: getAuthHeaders()
            });
            const files = await response.json();

            const fileListSection = document.getElementById('file-list-section');
            const fileList = document.getElementById('file-list');

            if (files.length === 0) {
                fileListSection.style.display = 'none';
                return;
            }

            fileListSection.style.display = 'block';
            fileList.innerHTML = '';

            files.forEach(file => {
                const fileItem = this.createFileListItem(file);
                fileList.appendChild(fileItem);
            });

        } catch (error) {
            console.error('Failed to load file list:', error);
        }
    }

    createFileListItem(metadata) {
        const div = document.createElement('div');
        div.className = 'file-item';

        const uploadDate = new Date(metadata.uploadDate).toLocaleString();
        const fileSize = this.formatFileSize(metadata.size);

        div.innerHTML = `
            <div class="file-info-left">
                <div class="file-name">${this.escapeHtml(metadata.originalName)}</div>
                <div class="file-meta">
                    ${metadata.rowCount.toLocaleString()} rows × ${metadata.columnCount} columns | 
                    ${fileSize} | 
                    Uploaded: ${uploadDate} |
                    Delimiter: ${this.getDelimiterName(metadata.delimiter)}
                </div>
            </div>
            <div class="file-actions">
                <button class="btn btn-primary btn-small" onclick="app.openViewer('${metadata.id}')">
                    👁️ View
                </button>
                <button class="btn btn-danger btn-small" onclick="app.deleteFile('${metadata.id}')">
                    🗑️ Delete
                </button>
            </div>
        `;

        return div;
    }

    async openViewer(fileId) {
        this.currentFileId = fileId;
        this.currentPage = 0;
        this.sortColumn = null;
        this.sortOrder = 'asc';
        this.globalSearch = '';
        this.columnSearch = {};
        this.inverseSearch = false;

        // Reset search inputs
        document.getElementById('global-search').value = '';
        document.getElementById('inverse-search').checked = false;

        try {
            // Load metadata
            const metadataResponse = await fetch(`/api/csv/${fileId}/metadata`, {
                headers: getAuthHeaders()
            });
            this.currentMetadata = await metadataResponse.json();

            // Update viewer header
            document.getElementById('current-file-name').textContent = this.currentMetadata.originalName;
            document.getElementById('file-stats').textContent = 
                `${this.currentMetadata.rowCount.toLocaleString()} rows × ${this.currentMetadata.columnCount} columns | ${this.formatFileSize(this.currentMetadata.size)}`;

            // Create column search inputs
            this.createColumnSearchInputs();

            // Show viewer section
            document.getElementById('viewer-section').style.display = 'block';
            document.getElementById('viewer-section').scrollIntoView({ behavior: 'smooth' });

            // Load data
            await this.loadCsvData();

        } catch (error) {
            this.showError('Failed to open file: ' + error.message);
        }
    }

    createColumnSearchInputs() {
        const container = document.getElementById('column-search-container');
        container.innerHTML = '';

        this.currentMetadata.columns.forEach(column => {
            const div = document.createElement('div');
            div.className = 'column-search-item';
            
            const input = document.createElement('input');
            input.type = 'text';
            input.placeholder = `Search in ${column}...`;
            input.dataset.column = column;
            
            input.addEventListener('input', (e) => {
                const value = e.target.value.trim();
                if (value) {
                    this.columnSearch[column] = value;
                } else {
                    delete this.columnSearch[column];
                }
                this.currentPage = 0;
                this.loadCsvData();
            });

            const label = document.createElement('label');
            label.textContent = column;

            div.appendChild(label);
            div.appendChild(input);
            container.appendChild(div);
        });
    }

    async loadCsvData() {
        if (!this.currentFileId) return;

        try {
            const params = new URLSearchParams({
                page: this.currentPage,
                pageSize: this.pageSize
            });

            if (this.sortColumn) {
                params.append('sortColumn', this.sortColumn);
                params.append('sortOrder', this.sortOrder);
            }

            if (this.globalSearch) {
                params.append('globalSearch', this.globalSearch);
            }

            if (this.inverseSearch) {
                params.append('inverseSearch', 'true');
            }

            // Add column search parameters
            Object.entries(this.columnSearch).forEach(([column, value]) => {
                params.append(`col_${column}`, value);
            });

            const response = await fetch(`/api/csv/${this.currentFileId}/data?${params}`, {
                headers: getAuthHeaders()
            });
            const data = await response.json();

            this.renderTable(data);
            this.updatePaginationControls(data);

        } catch (error) {
            this.showError('Failed to load data: ' + error.message);
        }
    }

    renderTable(data) {
        const thead = document.getElementById('table-header');
        const tbody = document.getElementById('table-body');

        // Render header
        thead.innerHTML = '';
        const headerRow = document.createElement('tr');
        
        data.columns.forEach(column => {
            const th = document.createElement('th');
            th.textContent = column;
            th.className = 'sortable';
            
            if (this.sortColumn === column) {
                th.classList.add(this.sortOrder === 'asc' ? 'sort-asc' : 'sort-desc');
            }

            th.addEventListener('click', () => {
                if (this.sortColumn === column) {
                    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
                } else {
                    this.sortColumn = column;
                    this.sortOrder = 'asc';
                }
                this.currentPage = 0;
                this.loadCsvData();
            });

            headerRow.appendChild(th);
        });
        
        thead.appendChild(headerRow);

        // Render body
        tbody.innerHTML = '';
        
        if (data.rows.length === 0) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = data.columns.length;
            td.textContent = 'No data found';
            td.style.textAlign = 'center';
            td.style.padding = '40px';
            td.style.color = '#999';
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }

        data.rows.forEach(row => {
            const tr = document.createElement('tr');
            row.forEach(cell => {
                const td = document.createElement('td');
                td.textContent = cell;
                td.title = cell; // Show full text on hover
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
    }

    updatePaginationControls(data) {
        const totalPages = data.totalPages || 1;
        const startRow = data.totalRows === 0 ? 0 : (this.currentPage * this.pageSize) + 1;
        const endRow = Math.min((this.currentPage + 1) * this.pageSize, data.totalRows);

        document.getElementById('pagination-info-text').textContent = 
            `Showing ${startRow.toLocaleString()}-${endRow.toLocaleString()} of ${data.totalRows.toLocaleString()} rows`;
        
        document.getElementById('page-info').textContent = 
            `Page ${this.currentPage + 1} of ${totalPages}`;

        // Update button states
        document.getElementById('first-page-btn').disabled = this.currentPage === 0;
        document.getElementById('prev-page-btn').disabled = this.currentPage === 0;
        document.getElementById('next-page-btn').disabled = this.currentPage >= totalPages - 1;
        document.getElementById('last-page-btn').disabled = this.currentPage >= totalPages - 1;

        // Update last page button to go to actual last page
        document.getElementById('last-page-btn').onclick = () => {
            this.currentPage = totalPages - 1;
            this.loadCsvData();
        };
    }

    closeViewer() {
        document.getElementById('viewer-section').style.display = 'none';
        this.currentFileId = null;
        document.getElementById('upload-section').scrollIntoView({ behavior: 'smooth' });
    }

    async deleteFile(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) {
            return;
        }

        try {
            const response = await fetch(`/api/csv/${fileId}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Delete failed');
            }

            if (this.currentFileId === fileId) {
                this.closeViewer();
            }

            this.loadFileList();

        } catch (error) {
            this.showError('Failed to delete file: ' + error.message);
        }
    }

    async downloadFilteredCsv() {
        if (!this.currentFileId) return;

        try {
            const params = new URLSearchParams();

            if (this.sortColumn) {
                params.append('sortColumn', this.sortColumn);
                params.append('sortOrder', this.sortOrder);
            }

            if (this.globalSearch) {
                params.append('globalSearch', this.globalSearch);
            }

            if (this.inverseSearch) {
                params.append('inverseSearch', 'true');
            }

            const token = localStorage.getItem('authToken');
            const url = `/api/csv/${this.currentFileId}/download?${params}&token=${token}`;
            window.open(url, '_blank');

        } catch (error) {
            this.showError('Failed to download file: ' + error.message);
        }
    }

    showProgress(percent) {
        const progressSection = document.getElementById('upload-progress');
        const progressFill = document.getElementById('progress-fill');
        const progressText = document.getElementById('progress-text');

        progressSection.style.display = 'block';
        progressFill.style.width = percent + '%';
        progressText.textContent = percent === 100 ? 'Processing...' : `Uploading... ${percent}%`;
    }

    hideProgress() {
        document.getElementById('upload-progress').style.display = 'none';
        document.getElementById('progress-fill').style.width = '0%';
        document.getElementById('file-input').value = '';
    }

    showError(message) {
        const errorDiv = document.getElementById('error-message');
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        
        setTimeout(() => {
            errorDiv.style.display = 'none';
        }, 5000);
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }

    getDelimiterName(delimiter) {
        const names = {
            ',': 'Comma',
            ';': 'Semicolon',
            '\t': 'Tab',
            '|': 'Pipe'
        };
        return names[delimiter] || 'Unknown';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize app when DOM is ready
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new CsvViewerApp();
});

// Made with Bob

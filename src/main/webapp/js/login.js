/**
 * Login page functionality
 */

// Check if user is already logged in
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('authToken');
    if (token) {
        // Validate token
        validateToken(token);
    }
});

// Handle login form submission
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const loginButton = document.getElementById('login-button');
    const errorMessage = document.getElementById('error-message');
    
    // Clear previous error
    errorMessage.classList.remove('show');
    
    // Disable button and show loading
    loginButton.disabled = true;
    loginButton.innerHTML = '<span class="loading-spinner"></span>Signing in...';
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            // Store token and username
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('username', data.username);
            
            // Redirect to main page
            window.location.href = '/index.html';
        } else {
            // Show error message
            showError(data.message || 'Login failed. Please try again.');
            loginButton.disabled = false;
            loginButton.innerHTML = 'Sign In';
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('Unable to connect to server. Please try again.');
        loginButton.disabled = false;
        loginButton.innerHTML = 'Sign In';
    }
});

/**
 * Validate token with server
 */
async function validateToken(token) {
    try {
        const response = await fetch('/api/auth/validate', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            // Token is valid, redirect to main page
            window.location.href = '/index.html';
        } else {
            // Token is invalid, clear storage
            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
        }
    } catch (error) {
        console.error('Token validation error:', error);
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
    }
}

/**
 * Show error message
 */
function showError(message) {
    const errorMessage = document.getElementById('error-message');
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
}

// Made with Bob
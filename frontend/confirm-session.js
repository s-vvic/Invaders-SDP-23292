document.addEventListener('DOMContentLoaded', () => {
    // UI Elements
    const loadingState = document.getElementById('loading-state');
    const sessionStatusMessage = document.getElementById('session-status-message');
    const successState = document.getElementById('success-state');
    const errorState = document.getElementById('error-state');
    const errorMessageElement = document.getElementById('error-message');
    const loggedInInfo = document.getElementById('logged-in-info');
    const loggedInUsernameElement = document.getElementById('logged-in-username');
    const logoutButton = document.getElementById('logout-button');

    let confirmationCode = null;

    // Helper to get query parameter
    const getQueryParam = (name) => {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    };

    // --- State Management ---
    const showLoading = (message) => {
        loadingState.classList.remove('hidden');
        sessionStatusMessage.textContent = message;
        successState.classList.add('hidden');
        errorState.classList.add('hidden');
        loggedInInfo.classList.add('hidden'); // Hide username info during loading
    };

    const showSuccess = () => {
        loadingState.classList.add('hidden');
        errorState.classList.add('hidden');
        loggedInInfo.classList.add('hidden');
        successState.classList.remove('hidden');
    };

    const showError = (message, showLogout = true) => {
        loadingState.classList.add('hidden');
        successState.classList.add('hidden');
        errorState.classList.remove('hidden');
        errorMessageElement.textContent = message;
        if (showLogout) {
            loggedInInfo.classList.remove('hidden'); // Show logout option on error
        } else {
            loggedInInfo.classList.add('hidden');
        }
    };

    // --- Main Logic ---
    const initSessionConfirmation = async () => {
        showLoading('Connecting to game client...');

        confirmationCode = getQueryParam('code');
        if (!confirmationCode) {
            showError('Invalid or missing confirmation code in URL.', false);
            return;
        }

        const token = localStorage.getItem('invaders_token');
        const username = localStorage.getItem('invaders_username');

        if (!token || !username) {
            showError('You are not logged in. Please log in first.', false);
            // Optionally redirect to main login if not logged in
            // window.location.href = '/index.html';
            return;
        }

        loggedInUsernameElement.textContent = username; // Display username
        loggedInInfo.classList.remove('hidden'); // Show username and logout button

        try {
            // Automatically attempt to confirm the session
            const response = await confirmSession(confirmationCode);

            if (response.ok) {
                showSuccess();
            } else {
                const errorData = await response.json();
                const msg = errorData.error || 'Failed to confirm session.';
                showError(msg);
                // If the token itself was invalid, clear it. fetchWithAuth would usually handle this too.
                if (response.status === 401) {
                    localStorage.removeItem('invaders_token');
                    localStorage.removeItem('invaders_username');
                    localStorage.removeItem('invaders_userId');
                }
            }
        } catch (error) {
            console.error('Session confirmation failed:', error);
            showError('An unexpected error occurred during confirmation.');
        }
    };

    // --- Event Listeners ---
    logoutButton.addEventListener('click', async () => {
        if (!confirmationCode) {
            console.error('No confirmation code available for cancellation.');
            showError('Cannot cancel session: code missing.', false);
            return;
        }

        try {
            // Attempt to cancel the session on the backend
            await cancelSession(confirmationCode);
        } catch (error) {
            console.error('Failed to cancel session on backend:', error);
            // Even if backend fails, we proceed to log out locally.
        } finally {
            // Clear local storage regardless of backend cancellation success
            localStorage.removeItem('invaders_token');
            localStorage.removeItem('invaders_username');
            localStorage.removeItem('invaders_userId');
            // Redirect to the main login page
            window.location.href = '/index.html';
        }
    });

    // Initialize on load
    initSessionConfirmation();
});
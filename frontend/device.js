document.addEventListener('DOMContentLoaded', () => {
    // Views
    const connectView = document.getElementById('connect-view');
    const fullLoginView = document.getElementById('full-login-view');
    const successView = document.getElementById('success-view');

    // Forms
    const connectForm = document.getElementById('connect-form');
    const deviceLoginForm = document.getElementById('device-login-form');

    // Buttons
    const logoutAndSwitchBtn = document.getElementById('logout-and-switch-btn');

    // Messages
    const welcomeMessage = document.getElementById('device-welcome-message');
    const connectError = document.getElementById('connect-error');
    const deviceLoginError = document.getElementById('device-login-error');

    // Inputs
    const deviceCodeAuthedInput = document.getElementById('device-code-authed');
    const deviceCodeInput = document.getElementById('device-code');
    const deviceUsernameInput = document.getElementById('device-username');
    const devicePasswordInput = document.getElementById('device-password');

    const showConnectView = () => {
        const username = localStorage.getItem('invaders_username');
        if (username) {
            welcomeMessage.textContent = `Welcome, ${username}! Enter the code from your game.`;
            fullLoginView.classList.add('hidden');
            successView.classList.add('hidden');
            connectView.classList.remove('hidden');
        } else {
            // If username is missing, something is wrong with the stored session.
            showFullLoginView();
        }
    };

    const showFullLoginView = () => {
        connectView.classList.add('hidden');
        successView.classList.add('hidden');
        fullLoginView.classList.remove('hidden');
    };
    
    const showSuccessView = () => {
        connectView.classList.add('hidden');
        fullLoginView.classList.add('hidden');
        successView.classList.remove('hidden');
    };

    // --- Event Listeners ---

    // Form for already-authenticated users
    connectForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        connectError.textContent = '';
        const userCode = deviceCodeAuthedInput.value.trim().toUpperCase();

        if (!userCode) {
            connectError.textContent = 'Please enter a device code.';
            return;
        }

        try {
            const response = await connectDevice(userCode);
            if (response.ok) {
                showSuccessView();
            } else {
                 if (response.status === 401) {
                    // This case is handled by the fetchWithAuth wrapper, but we can add specific logic here.
                    // The wrapper will remove the token and reload, let's just clear our UI and switch views.
                    localStorage.removeItem('invaders_token');
                    localStorage.removeItem('invaders_username');
                    localStorage.removeItem('invaders_userId');
                    showFullLoginView();
                    deviceLoginError.textContent = "Your session expired. Please log in again.";
                 } else {
                    const data = await response.json();
                    connectError.textContent = data.error || 'Failed to connect device.';
                 }
            }
        } catch (error) {
            // The fetchWithAuth will throw an error for 401, which we can catch here.
             if (error.message === 'Unauthorized') {
                showFullLoginView();
                deviceLoginError.textContent = "Your session expired. Please log in again.";
            } else {
                connectError.textContent = 'An unexpected error occurred.';
            }
        }
    });

    // Form for unauthenticated users
    deviceLoginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        deviceLoginError.textContent = '';
        const userCode = deviceCodeInput.value.trim().toUpperCase();
        const username = deviceUsernameInput.value.trim();
        const password = devicePasswordInput.value;

        if (!userCode || !username || !password) {
            deviceLoginError.textContent = 'All fields are required.';
            return;
        }

        try {
            const response = await loginWithDevice(userCode, username, password);
            const data = await response.json();

            if (response.ok) {
                // Also store the new token so the main dashboard would work if they navigate there
                localStorage.setItem('invaders_token', data.token);
                localStorage.setItem('invaders_username', data.user.username);
                localStorage.setItem('invaders_userId', data.user.id);
                showSuccessView();
            } else {
                deviceLoginError.textContent = data.error || 'Login failed.';
            }
        } catch (error) {
            deviceLoginError.textContent = 'An unexpected error occurred.';
        }
    });
    
    // Button to switch from authenticated view to login form
    logoutAndSwitchBtn.addEventListener('click', () => {
        localStorage.removeItem('invaders_token');
        localStorage.removeItem('invaders_username');
        localStorage.removeItem('invaders_userId');
        showFullLoginView();
    });


    // --- Initial View Setup ---
    const token = localStorage.getItem('invaders_token');
    if (token) {
        showConnectView();
    } else {
        showFullLoginView();
    }
});

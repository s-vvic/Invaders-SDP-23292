document.addEventListener('DOMContentLoaded', () => {
    // --- DOM Elements ---
    const loginView = document.getElementById('login-view');
    const dashboardView = document.getElementById('dashboard-view');
    const registerView = document.getElementById('register-view');

    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const loginError = document.getElementById('login-error');
    const registerError = document.getElementById('register-error');

    const logoutBtn = document.getElementById('logout-btn');
    const viewLeaderboardBtn = document.getElementById('view-leaderboard-btn');
    const backToDashboardBtn = document.getElementById('back-to-dashboard-btn');
    const showRegisterBtn = document.getElementById('show-register-btn');
    const showLoginBtn = document.getElementById('show-login-btn');

    const welcomeMessage = document.getElementById('welcome-message');
    const highScoreEl = document.getElementById('high-score');
    const goldEl = document.getElementById('gold');
    const totalGamesEl = document.getElementById('total-games');
    const averageScoreEl = document.getElementById('average-score');
    const userRankEl = document.getElementById('user-rank');
    const recentGamesListEl = document.getElementById('recent-games-list');
    const upgradesListEl = document.getElementById('upgrades-list');
    const achievementsListEl = document.getElementById('achievements-list');
    const leaderboardView = document.getElementById('leaderboard-view');
    const leaderboardListEl = document.getElementById('leaderboard-list');
    const simulateGameOverBtn = document.getElementById('simulate-game-over-btn');
    const weeklyLeaderboardListEl = document.getElementById('weekly-leaderboard-list');
    const yearlyLeaderboardListEl = document.getElementById('yearly-leaderboard-list');

    // Leaderboard Tab Elements
    const btnLeaderboardOverall = document.getElementById('btn-leaderboard-overall');
    const btnLeaderboardWeekly = document.getElementById('btn-leaderboard-weekly');
    const btnLeaderboardYearly = document.getElementById('btn-leaderboard-yearly');
    const cardLeaderboardOverall = document.getElementById('card-leaderboard-overall');
    const cardLeaderboardWeekly = document.getElementById('card-leaderboard-weekly');
    const cardLeaderboardYearly = document.getElementById('card-leaderboard-yearly');

    const leaderboardNavBtns = [btnLeaderboardOverall, btnLeaderboardWeekly, btnLeaderboardYearly];
    const leaderboardCards = [cardLeaderboardOverall, cardLeaderboardWeekly, cardLeaderboardYearly];
    const leaderboardSearchInput = document.getElementById('leaderboard-search');
    const leaderboardSearchClear = document.getElementById('leaderboard-search-clear');
    
    // Store original scores for filtering
    let currentScores = {
        overall: [],
        weekly: [],
        yearly: []
    };


    // --- API Configuration ---
    const API_BASE_URL = 'http://localhost:8080/api';
    const USE_MOCK_API = true; // Set to false to use real API

    // --- Loading & Error Handling Utilities ---
    let loadingOverlay = null;

    function createLoadingOverlay() {
        if (loadingOverlay) return loadingOverlay;
        
        loadingOverlay = document.createElement('div');
        loadingOverlay.className = 'loading-overlay';
        loadingOverlay.innerHTML = `
            <div class="loading-overlay-content">
                <div class="loading-spinner"></div>
                <p>Loading...</p>
            </div>
        `;
        document.body.appendChild(loadingOverlay);
        return loadingOverlay;
    }

    function showLoading(message = 'Loading...') {
        const overlay = createLoadingOverlay();
        const messageEl = overlay.querySelector('p');
        if (messageEl) messageEl.textContent = message;
        overlay.classList.add('active');
    }

    function hideLoading() {
        if (loadingOverlay) {
            loadingOverlay.classList.remove('active');
        }
    }

    function showError(element, message) {
        if (!element) return;
        element.textContent = message;
        element.classList.remove('success-message');
        element.classList.add('error-message');
        // Auto-hide after 5 seconds
        setTimeout(() => {
            if (element.textContent === message) {
                element.textContent = '';
            }
        }, 5000);
    }

    function showSuccess(element, message) {
        if (!element) return;
        element.textContent = message;
        element.classList.remove('error-message');
        element.classList.add('success-message');
        // Auto-hide after 3 seconds
        setTimeout(() => {
            if (element.textContent === message) {
                element.textContent = '';
            }
        }, 3000);
    }

    function clearMessage(element) {
        if (element) {
            element.textContent = '';
            element.classList.remove('error-message', 'success-message');
        }
    }

    function setButtonLoading(button, isLoading) {
        if (!button) return;
        button.disabled = isLoading;
        if (isLoading) {
            button.dataset.originalText = button.textContent;
            button.innerHTML = '<span class="loading-spinner"></span> ' + button.dataset.originalText;
        } else {
            button.textContent = button.dataset.originalText || button.textContent;
            delete button.dataset.originalText;
        }
    }

    function getErrorMessage(error) {
        if (typeof error === 'string') return error;
        if (error.error) return error.error;
        if (error.message) return error.message;
        if (error.status === 401) return '인증에 실패했습니다. 사용자 이름과 비밀번호를 확인해주세요.';
        if (error.status === 404) return '요청한 리소스를 찾을 수 없습니다.';
        if (error.status === 409) return '이미 사용 중인 사용자 이름입니다.';
        if (error.status === 500) return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        if (error.status >= 500) return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        if (error.status >= 400) return '잘못된 요청입니다. 입력 정보를 확인해주세요.';
        return '알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
    }

    // --- Toast Notification System ---
    const toastContainer = document.getElementById('toast-container');

    function showToast(type, title, message, duration = 3000) {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icons = {
            success: '✓',
            error: '✕',
            info: 'ℹ',
            warning: '⚠'
        };
        
        toast.innerHTML = `
            <span class="toast-icon">${icons[type] || icons.info}</span>
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                <div class="toast-message">${message}</div>
            </div>
            <button class="toast-close" aria-label="Close">×</button>
        `;
        
        toastContainer.appendChild(toast);
        
        // Close button handler
        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => {
            removeToast(toast);
        });
        
        // Auto remove after duration
        if (duration > 0) {
            setTimeout(() => {
                removeToast(toast);
            }, duration);
        }
        
        return toast;
    }

    function removeToast(toast) {
        toast.style.animation = 'fadeOut 0.3s ease-in forwards';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    // Convenience functions
    function toastSuccess(message, title = '성공') {
        return showToast('success', title, message);
    }

    function toastError(message, title = '오류') {
        return showToast('error', title, message, 5000); // Errors stay longer
    }

    function toastInfo(message, title = '알림') {
        return showToast('info', title, message);
    }

    function toastWarning(message, title = '경고') {
        return showToast('warning', title, message);
    }

    // --- Mock API ---
    function mockLogin(username, password) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test" && password === "1234") {
                    resolve({
                        token: "fake-jwt-token-for-testing",
                        username: "test"
                    });
                }
                else reject({ error: "Invalid username or password" });
            }, 300);
        });
    }

    function mockRegister(username, password) {
        console.log(`[Mock API] Register attempt for: ${username}`);
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                if (username === "test") {
                    reject({ error: "Username 'test' is already taken." });
                } else {
                    resolve({ message: "Account created successfully!" });
                }
            }, 500);
        });
    }

    function mockGetDashboardData(token) {
        return fetch('./mock_data/dashboard.json').then(res => res.json());
    }

    function mockGetLeaderboard() {
        return fetch('./mock_data/leaderboard.json').then(res => res.json());
    }

    function mockGetWeeklyLeaderboard() {
        return fetch('./mock_data/weekly_scores.json').then(res => res.json());
    }

    function mockGetYearlyLeaderboard() {
        return fetch('./mock_data/yearly_scores.json').then(res => res.json());
    }

    function mockGetUserStats() {
        return fetch('./mock_data/user_stats.json').then(res => res.json());
    }

    // --- Utility Functions ---
    function formatDate(dateString) {
        if (!dateString) return '날짜 없음';
        return new Date(dateString).toLocaleString('ko-KR');
    }

    function getRankEmoji(rank) {
        if (rank === 1) return '🥇';
        if (rank === 2) return '🥈';
        if (rank === 3) return '🥉';
        return `${rank}`;
    }

    // --- Number Animation ---
    function animateNumber(element, start, end, duration = 1000, formatter = null) {
        if (!element) return;
        
        const startTime = performance.now();
        const difference = end - start;
        
        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function (ease-out)
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const current = Math.floor(start + difference * easeOut);
            
            if (formatter) {
                element.textContent = formatter(current);
            } else {
                element.textContent = current.toLocaleString();
            }
            
            if (progress < 1) {
                requestAnimationFrame(update);
            } else {
                // Ensure final value is set
                if (formatter) {
                    element.textContent = formatter(end);
                } else {
                    element.textContent = end.toLocaleString();
                }
            }
        }
        
        requestAnimationFrame(update);
    }

    function animateValue(element, targetValue, options = {}) {
        if (!element) return;
        
        const {
            duration = 1000,
            formatter = null,
            prefix = '',
            suffix = ''
        } = options;
        
        const currentText = element.textContent || '0';
        const currentValue = parseInt(currentText.replace(/[^0-9]/g, '')) || 0;
        const target = typeof targetValue === 'number' ? targetValue : parseInt(targetValue) || 0;
        
        if (currentValue === target) {
            if (formatter) {
                element.textContent = formatter(target);
            } else {
                element.textContent = prefix + target.toLocaleString() + suffix;
            }
            return;
        }
        
        animateNumber(element, currentValue, target, duration, (value) => {
            if (formatter) {
                return formatter(value);
            }
            return prefix + value.toLocaleString() + suffix;
        });
    }

    function createLeaderboardRow(record, rank) {
        const row = document.createElement('tr');
        
        const rankCell = document.createElement('td');
        rankCell.className = 'rank-emoji';
        rankCell.textContent = getRankEmoji(rank);
        
        const usernameCell = document.createElement('td');
        usernameCell.textContent = record.username;
        
        const scoreCell = document.createElement('td');
        scoreCell.textContent = record.score.toLocaleString();
        
        const dateCell = document.createElement('td');
        dateCell.textContent = formatDate(record.created_at);
        
        row.appendChild(rankCell);
        row.appendChild(usernameCell);
        row.appendChild(scoreCell);
        row.appendChild(dateCell);
        
        return row;
    }

    function renderLeaderboardTable(tbodyElement, scores, errorMessage) {
        tbodyElement.innerHTML = '';
        
        if (scores.length === 0) {
            const emptyRow = document.createElement('tr');
            emptyRow.innerHTML = '<td colspan="4" class="empty-message">아직 기록된 점수가 없습니다.</td>';
            tbodyElement.appendChild(emptyRow);
            return;
        }

        scores.forEach((record, index) => {
            const row = createLeaderboardRow(record, index + 1);
            // Add fade-in animation with stagger
            row.style.opacity = '0';
            row.style.transform = 'translateY(10px)';
            row.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
            row.style.transitionDelay = `${index * 0.05}s`;
            tbodyElement.appendChild(row);
            
            // Trigger animation
            requestAnimationFrame(() => {
                row.style.opacity = '1';
                row.style.transform = 'translateY(0)';
            });
        });
    }

    function filterLeaderboard(scores, searchTerm) {
        if (!searchTerm || searchTerm.trim() === '') {
            return scores;
        }
        
        const term = searchTerm.toLowerCase().trim();
        return scores.filter(record => 
            record.username.toLowerCase().includes(term)
        );
    }

    function applyLeaderboardFilter() {
        const searchTerm = leaderboardSearchInput.value;
        const activeTab = getActiveLeaderboardTab();
        
        if (!activeTab) return;
        
        const originalScores = currentScores[activeTab];
        if (!originalScores || originalScores.length === 0) return;
        
        const filteredScores = filterLeaderboard(originalScores, searchTerm);
        
        // Update clear button visibility
        if (searchTerm.trim() !== '') {
            leaderboardSearchClear.classList.remove('hidden');
        } else {
            leaderboardSearchClear.classList.add('hidden');
        }
        
        // Render filtered results
        const tbodyElement = getActiveLeaderboardTbody();
        if (tbodyElement) {
            renderLeaderboardTable(tbodyElement, filteredScores);
        }
    }

    function getActiveLeaderboardTab() {
        if (btnLeaderboardOverall.classList.contains('active')) return 'overall';
        if (btnLeaderboardWeekly.classList.contains('active')) return 'weekly';
        if (btnLeaderboardYearly.classList.contains('active')) return 'yearly';
        return null;
    }

    function getActiveLeaderboardTbody() {
        const activeTab = getActiveLeaderboardTab();
        if (activeTab === 'overall') return leaderboardListEl;
        if (activeTab === 'weekly') return weeklyLeaderboardListEl;
        if (activeTab === 'yearly') return yearlyLeaderboardListEl;
        return null;
    }

    function showLoadingMessage(tbodyElement) {
        tbodyElement.innerHTML = generateSkeletonTableRows(5);
    }

    function generateSkeletonTableRows(count = 5) {
        let html = '';
        for (let i = 0; i < count; i++) {
            html += `
                <tr class="skeleton-table-row">
                    <td><div class="skeleton skeleton-text short"></div></td>
                    <td><div class="skeleton skeleton-text medium"></div></td>
                    <td><div class="skeleton skeleton-text short"></div></td>
                    <td><div class="skeleton skeleton-text medium"></div></td>
                </tr>
            `;
        }
        return html;
    }

    function generateSkeletonCard() {
        return `
            <div class="skeleton-card">
                <div class="skeleton skeleton-title"></div>
                <div class="skeleton skeleton-number"></div>
            </div>
        `;
    }

    function showErrorMessage(tbodyElement, message) {
        tbodyElement.innerHTML = `<tr><td colspan="4" class="error-message-cell">${message}</td></tr>`;
    }

    // --- App Logic & View Management ---
    async function loadDashboard() {
        try {
            const username = localStorage.getItem('invaders_username');
            const userId = localStorage.getItem('invaders_userId');

            if (username) {
                welcomeMessage.textContent = `Welcome, ${username}!`;
            } else {
                welcomeMessage.textContent = 'Welcome!';
            }

            // Fetch user's specific data (including max_score)
            const response = await fetchWithAuth(`${API_BASE_URL}/users/${userId}`);
            if (!response.ok) {
                throw { status: response.status, error: `HTTP error! status: ${response.status}` };
            }
            const userData = await response.json();

            // Animate high score
            animateValue(highScoreEl, userData.max_score || 0, { duration: 1000 });
            // goldEl.textContent = data.gold; // We don't have gold in our user data yet
            // upgradesListEl // Not implemented yet
            // achievementsListEl // Not implemented yet

            // Load user statistics
            await loadUserStats(userId);

        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            welcomeMessage.textContent = 'Welcome!'; // Fallback
            highScoreEl.textContent = 'Error';
            toastError('대시보드 데이터를 불러오는 데 실패했습니다.');
        }
    }

    async function loadUserStats(userId) {
        try {
            let stats;
            
            if (USE_MOCK_API) {
                stats = await mockGetUserStats();
            } else {
                // Use real API (when implemented)
                // const response = await fetchWithAuth(`${API_BASE_URL}/users/${userId}/stats`);
                // stats = await response.json();
                stats = await mockGetUserStats(); // Fallback to mock for now
            }

            // Update statistics with animation
            animateValue(totalGamesEl, stats.totalGames || 0, { duration: 800 });
            animateValue(averageScoreEl, stats.averageScore || 0, { duration: 1000 });
            
            if (stats.rank && stats.rankOutOf) {
                userRankEl.textContent = `${stats.rank} / ${stats.rankOutOf}`;
            } else {
                userRankEl.textContent = '-';
            }

            // Render recent games
            renderRecentGames(stats.recentGames || []);

        } catch (error) {
            console.error('Failed to load user stats:', error);
            totalGamesEl.textContent = 'Error';
            averageScoreEl.textContent = 'Error';
            userRankEl.textContent = '-';
            recentGamesListEl.innerHTML = '<tr><td colspan="2" class="error-message-cell">통계를 불러오는 데 실패했습니다.</td></tr>';
        }
    }

    function renderRecentGames(games) {
        recentGamesListEl.innerHTML = '';

        if (games.length === 0) {
            recentGamesListEl.innerHTML = '<tr><td colspan="2" class="empty-message">아직 게임 기록이 없습니다.</td></tr>';
            return;
        }

        games.forEach((game, index) => {
            const row = document.createElement('tr');
            
            const scoreCell = document.createElement('td');
            scoreCell.textContent = game.score.toLocaleString();
            
            const dateCell = document.createElement('td');
            dateCell.textContent = formatDate(game.created_at);
            
            row.appendChild(scoreCell);
            row.appendChild(dateCell);
            
            // Add fade-in animation with stagger
            row.style.opacity = '0';
            row.style.transform = 'translateX(-10px)';
            row.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
            row.style.transitionDelay = `${index * 0.05}s`;
            recentGamesListEl.appendChild(row);
            
            // Trigger animation
            requestAnimationFrame(() => {
                row.style.opacity = '1';
                row.style.transform = 'translateX(0)';
            });
        });
    }
    // --- Leaderboard Loading Functions ---
    async function loadLeaderboardData(endpoint, mockDataFn, tbodyElement, loadingMessage, tabKey) {
        showLoadingMessage(tbodyElement);
        try {
            showLoading(loadingMessage);
            
            let scores;
            
            if (USE_MOCK_API) {
                if (endpoint === '/scores') {
                    // Overall leaderboard needs date conversion
                    const mockData = await mockDataFn();
                    scores = mockData.map((item) => ({
                        username: item.username,
                        score: item.score,
                        created_at: new Date().toISOString()
                    }));
                } else {
                    scores = await mockDataFn();
                }
            } else {
                const response = await fetch(`${API_BASE_URL}${endpoint}`);
                
                if (!response.ok) {
                    throw { status: response.status, error: `HTTP error! status: ${response.status}` };
                }
                
                scores = await response.json();
            }

            // Store original scores for filtering
            if (tabKey) {
                currentScores[tabKey] = scores;
            }

            // Apply current filter if any
            const searchTerm = leaderboardSearchInput.value;
            const filteredScores = filterLeaderboard(scores, searchTerm);
            
            renderLeaderboardTable(tbodyElement, filteredScores);
            hideLoading();
        } catch (error) {
            console.error(`Error loading leaderboard (${endpoint}):`, error);
            showErrorMessage(tbodyElement, `점수판을 불러오는 데 실패했습니다: ${getErrorMessage(error)}`);
            hideLoading();
        }
    }

    async function loadLeaderboard() {
        await loadLeaderboardData(
            '/scores',
            mockGetLeaderboard,
            leaderboardListEl,
            '리더보드 불러오는 중...',
            'overall'
        );
    }

    // --- View Management ---
    const views = {
        login: loginView,
        register: registerView,
        dashboard: dashboardView,
        leaderboard: leaderboardView
    };

    function showView(viewName) {
        // Hide all views
        Object.values(views).forEach(view => view.classList.add('hidden'));
        // Show selected view
        if (views[viewName]) {
            views[viewName].classList.remove('hidden');
        }
    }

    function showLoginView() {
        showView('login');
    }

    // Pass the view handler to the API module
    setLoginViewHandler(showLoginView);

    function showRegisterView() {
        showView('register');
    }

    function showDashboardView() {
        showView('dashboard');
        loadDashboard();
    }

    async function loadWeeklyLeaderboard() {
        await loadLeaderboardData(
            '/scores/weekly',
            mockGetWeeklyLeaderboard,
            weeklyLeaderboardListEl,
            '주간 리더보드 불러오는 중...',
            'weekly'
        );
    }

    async function loadYearlyLeaderboard() {
        await loadLeaderboardData(
            '/scores/yearly',
            mockGetYearlyLeaderboard,
            yearlyLeaderboardListEl,
            '연간 리더보드 불러오는 중...',
            'yearly'
        );
    }

    function showLeaderboardTab(tabName) {
        // Hide all cards and remove active class from all buttons
        leaderboardCards.forEach(card => card.classList.add('hidden'));
        leaderboardNavBtns.forEach(btn => btn.classList.remove('active'));

        // Show the selected card and set the corresponding button to active
        if (tabName === 'overall') {
            cardLeaderboardOverall.classList.remove('hidden');
            btnLeaderboardOverall.classList.add('active');
        } else if (tabName === 'weekly') {
            cardLeaderboardWeekly.classList.remove('hidden');
            btnLeaderboardWeekly.classList.add('active');
            loadWeeklyLeaderboard();
        } else if (tabName === 'yearly') {
            cardLeaderboardYearly.classList.remove('hidden');
            btnLeaderboardYearly.classList.add('active');
            loadYearlyLeaderboard();
        }
    }

    function showLeaderboardView() {
        showView('leaderboard');
        
        const username = localStorage.getItem('invaders_username');
        welcomeMessage.textContent = username ? `Welcome, ${username}!` : 'Welcome!';
        
        loadLeaderboard();
        showLeaderboardTab('overall'); // Show the overall tab by default
    }

    function logout() {
        localStorage.removeItem('invaders_token');
        localStorage.removeItem('invaders_username');
        showLoginView();
    }

    // --- Event Listeners & Initial Execution ---

    showRegisterBtn.addEventListener('click', showRegisterView);
    showLoginBtn.addEventListener('click', showLoginView);
    logoutBtn.addEventListener('click', logout);
    viewLeaderboardBtn.addEventListener('click', showLeaderboardView);
    backToDashboardBtn.addEventListener('click', showDashboardView);

    // Leaderboard Tab Listeners
    btnLeaderboardOverall.addEventListener('click', () => showLeaderboardTab('overall'));
    btnLeaderboardWeekly.addEventListener('click', () => showLeaderboardTab('weekly'));
    btnLeaderboardYearly.addEventListener('click', () => showLeaderboardTab('yearly'));

    // Leaderboard Search Listeners
    if (leaderboardSearchInput) {
        leaderboardSearchInput.addEventListener('input', () => {
            applyLeaderboardFilter();
        });
    }

    if (leaderboardSearchClear) {
        leaderboardSearchClear.addEventListener('click', () => {
            leaderboardSearchInput.value = '';
            leaderboardSearchClear.classList.add('hidden');
            applyLeaderboardFilter();
        });
    }

    simulateGameOverBtn.addEventListener('click', async (e) => {
        e.preventDefault(); // 기본 동작 방지

        const userId = localStorage.getItem('invaders_userId');
        const username = localStorage.getItem('invaders_username');

        if (!userId) {
            toastWarning('로그인된 사용자 정보가 없습니다. 먼저 로그인해주세요.');
            return;
        }

        // 100에서 10000 사이의 랜덤 점수 생성
        const randomScore = Math.floor(Math.random() * (10000 - 100 + 1)) + 100;

        setButtonLoading(simulateGameOverBtn, true);
        showLoading('점수 업데이트 중...');

        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/users/${userId}/score`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ score: randomScore }),
            });

            const data = await response.json();

            if (!response.ok) {
                throw { status: response.status, error: data.error || '점수 업데이트 실패' };
            }

            hideLoading();
            toastSuccess(
                `${username}님, 게임 종료! 점수: ${randomScore.toLocaleString()}. ${data.message}`,
                '게임 종료'
            );

            // 대시보드 정보를 새로고침하여 최고 점수 업데이트 반영
            await loadDashboard();

        } catch (error) {
            console.error('게임 종료 시뮬레이션 중 오류 발생:', error);
            hideLoading();
            toastError(`점수 업데이트 중 오류 발생: ${getErrorMessage(error)}`);
        } finally {
            setButtonLoading(simulateGameOverBtn, false);
        }
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitButton = loginForm.querySelector('button[type="submit"]');
        const { username, password } = loginForm;
        
        // Clear previous errors
        clearMessage(loginError);
        
        // Validate inputs
        if (!username.value.trim()) {
            showError(loginError, '사용자 이름을 입력해주세요.');
            return;
        }
        if (!password.value) {
            showError(loginError, '비밀번호를 입력해주세요.');
            return;
        }

        setButtonLoading(submitButton, true);
        showError(loginError, '로그인 중...');

        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: username.value, password: password.value })
            });

            const data = await response.json();
            
            if (!response.ok || data.error) {
                throw { status: response.status, error: data.error || '로그인에 실패했습니다.' };
            }

            localStorage.setItem('invaders_token', data.token);
            localStorage.setItem('invaders_username', data.user.username);
            localStorage.setItem('invaders_userId', data.user.id);
            
            clearMessage(loginError);
            showSuccess(loginError, '로그인 성공!');
            
            // Small delay to show success message
            setTimeout(() => {
                showDashboardView();
            }, 500);

        } catch (error) {
            console.error('Login error:', error);
            showError(loginError, getErrorMessage(error));
        } finally {
            setButtonLoading(submitButton, false);
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitButton = registerForm.querySelector('button[type="submit"]');
        const username = document.getElementById('reg-username').value.trim();
        const password = document.getElementById('reg-password').value;
        const confirmPassword = document.getElementById('reg-confirm-password').value;

        // Clear previous errors
        clearMessage(registerError);

        // Validate inputs
        if (!username) {
            showError(registerError, '사용자 이름을 입력해주세요.');
            return;
        }
        if (username.length < 3) {
            showError(registerError, '사용자 이름은 최소 3자 이상이어야 합니다.');
            return;
        }
        if (!password) {
            showError(registerError, '비밀번호를 입력해주세요.');
            return;
        }
        if (password.length < 4) {
            showError(registerError, '비밀번호는 최소 4자 이상이어야 합니다.');
            return;
        }
        if (password !== confirmPassword) {
            showError(registerError, '비밀번호가 일치하지 않습니다.');
            return;
        }

        setButtonLoading(submitButton, true);
        showError(registerError, '회원가입 중...');

        try {
            const response = await fetch(`${API_BASE_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (!response.ok || data.error) {
                throw { status: response.status, error: data.error || '회원가입에 실패했습니다.' };
            }

            clearMessage(registerError);
            showSuccess(registerError, '회원가입이 완료되었습니다! 로그인 페이지로 이동합니다...');
            
            // Go to login page after successful registration
            setTimeout(() => {
                showLoginView();
                // Pre-fill username in login form
                document.getElementById('username').value = username;
            }, 1500);

        } catch (error) {
            console.error('Registration error:', error);
            showError(registerError, getErrorMessage(error));
        } finally {
            setButtonLoading(submitButton, false);
        }
    });

    // Initial check on page load
    if (localStorage.getItem('invaders_token')) {
        showDashboardView();
    } else {
        showLoginView();
    }
});
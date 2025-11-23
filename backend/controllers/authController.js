const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { getDb } = require('../db');
const { validateCredentials } = require('../utils/validators');
const deviceStore = require('../utils/deviceStore'); // Import deviceStore

const login = async (req, res) => {
    const db = getDb();
    try {
        const { username, password } = req.body;

        const validationError = validateCredentials({ username, password });
        if (validationError) {
            return res.status(400).json({ error: validationError });
        }

        const user = await db.get('SELECT * FROM users WHERE username = ?', [username]);

        if (user) {
            const match = await bcrypt.compare(password, user.password);
            if (match) {
                const payload = { id: user.id, username: user.username };
                const token = jwt.sign(
                    payload,
                    process.env.JWT_SECRET,
                    { expiresIn: '1h' }
                );

                console.log('Login successful:', user.username);
                res.json({ token: token, user: { id: user.id, username: user.username } });
            } else {
                console.log('Login failed for:', username);
                res.status(401).json({ error: 'Invalid username or password' });
            }
        } else {
            console.log('Login failed for:', username);
            res.status(401).json({ error: 'Invalid username or password' });
        }

    } catch (error) {
        console.error('Database error during login:', error);
        res.status(500).json({ error: 'Server database error' });
    }
};

const register = async (req, res) => {
    const db = getDb();
    try {
        const { username, password } = req.body;

        const validationError = validateCredentials({ username, password });
        if (validationError) {
            return res.status(400).json({ error: validationError });
        }

        const existingUser = await db.get('SELECT id FROM users WHERE username = ?', [username]);
        
        if (existingUser) {
            return res.status(400).json({ error: 'Username already taken' });
        }

        const saltRounds = 10;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        await db.run(
            'INSERT INTO users (username, password) VALUES (?, ?)',
            [username, hashedPassword]
        );
        
        console.log(`New user registered: ${username}`);
        res.status(201).json({ message: 'Account created successfully!' });

    } catch (error) {
        console.error('Database error during registration:', error);
        res.status(500).json({ error: 'Server database error' });
    }
};

// 1. 디바이스 코드 생성 컨트롤러
const initiateDeviceAuth = (req, res) => {
    try {
        const codeData = deviceStore.generateDeviceCode();
        res.json({
            ...codeData,
            // TODO: Replace with your actual frontend URL for the device login page
            verificationUri: 'http://localhost:8080/device.html' 
        });
    } catch (error) {
        console.error('Error initiating device auth:', error);
        res.status(500).json({ error: 'Failed to initiate device login' });
    }
};

// 2. 토큰 폴링 컨트롤러
const getDeviceToken = (req, res) => {
    const { deviceCode } = req.body;
    const info = deviceStore.getCodeInfo(deviceCode);

    if (!info) {
        return res.status(404).json({ error: 'Device code not found or already used.' });
    }

    if (info.expiresAt < Date.now()) {
        deviceStore.completeCode(deviceCode, { token: null, user: null }); // Mark as completed then cleanup will remove it
        return res.status(410).json({ error: 'Device code expired.', status: 'expired' });
    }

    if (info.status === 'pending') {
        return res.status(202).json({ message: 'Authorization pending.', status: 'pending' });
    }

    if (info.status === 'completed') {
        res.json({ token: info.token, user: info.user, status: 'completed' });
        deviceStore.completeCode(deviceCode, { token: null, user: null }); 
    } else {
        res.status(500).json({ error: 'Unexpected device code status.', status: 'error' });
    }
};

// 3. 웹에서 코드로 로그인하는 컨트롤러 (비로그인 사용자)
const loginWithDevice = async (req, res) => {
    const db = getDb();
    try {
        const { userCode, username, password } = req.body;

        const validationError = validateCredentials({ username, password });
        if (validationError) {
            return res.status(400).json({ error: validationError });
        }

        const searchResult = deviceStore.findByUserCode(userCode);

        if (!searchResult || searchResult.info.status !== 'pending' || searchResult.info.expiresAt < Date.now()) {
            return res.status(400).json({ error: 'Invalid or expired device code.' });
        }
        
        const user = await db.get('SELECT * FROM users WHERE username = ?', [username]);

        if (user) {
            const match = await bcrypt.compare(password, user.password);
            if (match) {
                const payload = { id: user.id, username: user.username };
                const token = jwt.sign(
                    payload,
                    process.env.JWT_SECRET,
                    { expiresIn: '1h' }
                );

                deviceStore.completeCode(searchResult.deviceCode, { token, user: { id: user.id, username: user.username } });
                console.log('Device connected successfully via web login for:', user.username);
                res.json({ message: 'Device connected successfully!', token: token, user: { id: user.id, username: user.username } });
            } else {
                console.log('Login failed for device connection:', username);
                res.status(401).json({ error: 'Invalid username or password' });
            }
        } else {
            console.log('Login failed for device connection:', username);
            res.status(401).json({ error: 'Invalid username or password' });
        }

    } catch (error) {
        console.error('Database error during device login:', error);
        res.status(500).json({ error: 'Server database error' });
    }
};

// 4. 웹에서 코드로 연결하는 컨트롤러 (로그인 사용자)
const connectDevice = (req, res) => {
    try {
        const { userCode } = req.body;
        const { id, username } = req.user; 
        const token = req.headers.authorization.split(' ')[1];

        const searchResult = deviceStore.findByUserCode(userCode);
        
        if (!searchResult || searchResult.info.status !== 'pending' || searchResult.info.expiresAt < Date.now()) {
            return res.status(400).json({ error: 'Invalid or expired device code.' });
        }

        deviceStore.completeCode(searchResult.deviceCode, { token, user: { id, username } });
        console.log('Device connected successfully by logged-in user:', username);
        res.json({ message: 'Device connected successfully!' });

    } catch (error) {
        console.error('Error connecting device for logged-in user:', error);
        res.status(500).json({ error: 'Failed to connect device.' });
    }
};

// --- New Session Confirmation Flow Controllers ---

// 1. 세션 확인 시작 API
const initiateSessionConfirmation = (req, res) => {
    try {
        const userToken = req.headers.authorization.split(' ')[1];
        const user = req.user; // Set by authMiddleware
        const codeData = deviceStore.generateConfirmationCode(userToken, user);
        res.json({
            confirmationCode: codeData.confirmationCode,
            expiresIn: codeData.expiresIn,
            interval: codeData.interval,
            // TODO: Replace with your actual frontend URL for the session confirmation page
            confirmationUri: 'http://localhost:8080/confirm-session.html' 
        });
    } catch (error) {
        console.error('Error initiating session confirmation:', error);
        res.status(500).json({ error: 'Failed to initiate session confirmation.' });
    }
};

// 2. 세션 확인 완료 API
const confirmSession = (req, res) => {
    try {
        const { confirmationCode } = req.body;
        // The user should be authenticated for this call too (from web session)
        const user = req.user; 

        const codeInfo = deviceStore.getConfirmationCodeInfo(confirmationCode);

        if (!codeInfo || codeInfo.user.id !== user.id) { // Ensure the web user is the same as the game user
            return res.status(400).json({ error: 'Invalid or mismatched confirmation code.' });
        }

        if (codeInfo.expiresAt < Date.now()) {
            deviceStore.cancelCode(confirmationCode); // Mark as cancelled if expired
            return res.status(410).json({ error: 'Confirmation code expired.' });
        }
        
        deviceStore.confirmCode(confirmationCode);
        res.json({ message: 'Session confirmed successfully!' });
    } catch (error) {
        console.error('Error confirming session:', error);
        res.status(500).json({ error: 'Failed to confirm session.' });
    }
};

// 3. 세션 상태 폴링 API
const getSessionStatus = (req, res) => {
    const { confirmationCode } = req.body;
    const info = deviceStore.getConfirmationCodeInfo(confirmationCode);

    if (!info) {
        return res.status(404).json({ error: 'Confirmation code not found or already used.' });
    }

    if (info.expiresAt < Date.now()) {
        deviceStore.cancelCode(confirmationCode); // Mark as cancelled if expired
        return res.status(410).json({ error: 'Confirmation code expired.', status: 'expired' });
    }

    // Return the current status
    res.json({ status: info.status, username: info.user.username });
};

// 4. 세션 취소 API
const cancelSession = (req, res) => {
    try {
        const { confirmationCode } = req.body;
        const user = req.user; // Set by authMiddleware

        const codeInfo = deviceStore.getConfirmationCodeInfo(confirmationCode);

        if (!codeInfo || codeInfo.user.id !== user.id) {
            return res.status(400).json({ error: 'Invalid or mismatched confirmation code.' });
        }
        
        deviceStore.cancelCode(confirmationCode);
        res.json({ message: 'Session cancelled successfully!' });
    } catch (error) {
        console.error('Error cancelling session:', error);
        res.status(500).json({ error: 'Failed to cancel session.' });
    }
};

module.exports = {
    login,
    register,
    initiateDeviceAuth,
    getDeviceToken,
    loginWithDevice,
    connectDevice,
    initiateSessionConfirmation,
    confirmSession,
    getSessionStatus,
    cancelSession,
};

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
        // Remove expired code from store if it somehow wasn't cleaned up
        deviceStore.completeCode(deviceCode, { token: null, user: null }); // Mark as completed then cleanup will remove it
        return res.status(410).json({ error: 'Device code expired.' });
    }

    if (info.status === 'pending') {
        return res.status(202).json({ message: 'Authorization pending.' });
    }

    if (info.status === 'completed') {
        res.json({ token: info.token, user: info.user });
        // After returning the token, remove the code from the store to prevent reuse
        deviceStore.completeCode(deviceCode, { token: null, user: null }); // Mark as completed then cleanup will remove it
    } else {
        res.status(500).json({ error: 'Unexpected device code status.' });
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
        // req.user is set by the authentication middleware
        const { id, username } = req.user; 
        const token = req.headers.authorization.split(' ')[1]; // Get the token from the request header

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

module.exports = {
    login,
    register,
    initiateDeviceAuth,
    getDeviceToken,
    loginWithDevice,
    connectDevice,
};

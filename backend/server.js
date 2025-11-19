require('dotenv').config();
const fs = require('fs');
const express = require("express");
const path = require("path");
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');
const bcrypt = require('bcrypt');
const helmet = require('helmet');
const hpp = require('hpp');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
const jwt = require('jsonwebtoken');

const app = express();
app.use(helmet());
app.use(hpp());
app.use(cors());

// Rate limiting
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // Limit each IP to 100 requests per window
    standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
    legacyHeaders: false, // Disable the `X-RateLimit-*` headers
});
app.use(limiter);
app.use(morgan('dev'));

let db;

async function startServer() {
    // .data 디렉토리가 없으면 생성
    const dbDir = path.join(__dirname, '.data');
    if (!fs.existsSync(dbDir)) {
        fs.mkdirSync(dbDir, { recursive: true });
        console.log(`Created database directory: ${dbDir}`);
    }

    // DB를 열고, '.data/invaders.db' 파일에 모든 것을 저장합니다.
    db = await open({
        filename: path.join(dbDir, 'invaders.db'),
        driver: sqlite3.Database
    });

    // --- SQL 스키마(테이블) 생성 ---
    await db.exec(`
        CREATE TABLE IF NOT EXISTS users(
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            username TEXT NOT NULL,
            password TEXT NOT NULL,
            max_score int DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS scores(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            score INTEGER NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        );
    `);
    
    // 배포를 위해 테스트 사용자 자동 생성 로직 제거
    
    return db; // Return the database connection
}
app.use(express.json());

app.use((req, res, next) => {
    if (req.url.endsWith('.ttf')) {
        res.setHeader('Content-Type', 'font/ttf');
    }
    next();
});

const publicPath = path.join(__dirname, '../frontend');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});

/**
 * @swagger
 * tags:
 *   name: Users
 *   description: User management and login
 */

/**
 * @swagger
 * /api/login:
 *   post:
 *     summary: Authenticate a user and return a token
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               username:
 *                 type: string
 *               password:
 *                 type: string
 *     responses:
 *       200:
 *         description: Login successful
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 token:
 *                   type: string
 *       401:
 *         description: Invalid username or password
 *       500:
 *         description: Server database error
 */
app.post('/api/login', async function(req, res) {
    try {
        const { username, password } = req.body;

        // DB에서 사용자 조회
        const user = await db.get(
            'SELECT * FROM users WHERE username = ?',
            [username]
        );

        if (user) {
            // 사용자가 존재하면 비밀번호 비교
            const match = await bcrypt.compare(password, user.password);
            if (match) {
                // 로그인 성공 -> JWT 생성
                const payload = { id: user.id, username: user.username };
                const token = jwt.sign(
                    payload,
                    process.env.JWT_SECRET,
                    { expiresIn: '1h' } // 1시간 유효
                );

                console.log('Login successful:', user.username);
                res.json({ token: token, user: { id: user.id, username: user.username } });
            } else {
                // 비밀번호 불일치
                console.log('Login failed for:', username);
                res.status(401).json({ error: 'Invalid username or password' });
            }
        } else {
            // 사용자가 존재하지 않음
            console.log('Login failed for:', username);
            res.status(401).json({ error: 'Invalid username or password' });
        }

    } catch (error) {
        console.error('Database error during login:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/register:
 *   post:
 *     summary: Register a new user
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               username:
 *                 type: string
 *               password:
 *                 type: string
 *     responses:
 *       201:
 *         description: User registered successfully
 *       400:
 *         description: Username already taken or bad request
 *       500:
 *         description: Server database error
 */
app.post('/api/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // 1. 유저 이름 중복 확인
        const existingUser = await db.get('SELECT id FROM users WHERE username = ?', [username]);
        
        if (existingUser) {
            // 400 Bad Request
            return res.status(400).json({ error: 'Username already taken' });
        }

        // 2. 새 유저 추가
        const saltRounds = 10;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        await db.run(
            'INSERT INTO users (username, password) VALUES (?, ?)',
            [username, hashedPassword]
        );
        
        console.log(`New user registered: ${username}`);
        // 201 Created
        res.status(201).json({ message: 'Account created successfully!' });

    } catch (error) {
        console.error('Database error during registration:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/users:
 *   get:
 *     summary: Retrieve a list of all users
 *     tags: [Users]
 *     responses:
 *       200:
 *         description: A list of users
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: integer
 *                   username:
 *                     type: string
 *                   max_score:
 *                     type: integer
 *       500:
 *         description: Server database error
 */
app.get('/api/users', async function(req, res) {
    try {
        const users = await db.all('SELECT id, username, max_score FROM users');
        res.json(users);
    } catch (error) {
        console.error('Database error while fetching users:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/users/{id}:
 *   get:
 *     summary: Retrieve a single user by ID
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     responses:
 *       200:
 *         description: A single user object
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 id:
 *                   type: integer
 *                 username:
 *                   type: string
 *                 max_score:
 *                   type: integer
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
app.get('/api/users/:id', async function(req, res) {
    try {
        const userId = parseInt(req.params.id, 10); // Convert ID to integer

        if (isNaN(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const user = await db.get(
            'SELECT id, username, max_score FROM users WHERE id = ?',
            [userId]
        );

        if (user) {
            res.json(user); // User found
        } else {
            res.status(404).json({ error: 'User not found' }); // User not found
        }
    } catch (error) {
        console.error('Database error while fetching single user:', error);
        res.status(500).json({ error: 'Server database error' }); // Internal server error
    }
});

/**
 * @swagger
 * /api/scores:
 *   get:
 *     summary: Retrieve a list of all scores
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of scores with usernames, ordered by score descending
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   username:
 *                     type: string
 *                   score:
 *                     type: integer
 *                   created_at:
 *                     type: string
 *       500:
 *         description: Server database error
 */
app.get('/api/scores', async function(req, res) {
    try {
        // scores 테이블과 users 테이블을 JOIN 하여
        // 유저이름, 점수, 생성일자를 점수 내림차순으로 100개 가져옵니다.
        const scores = await db.all(`
            SELECT u.username, s.score, s.created_at 
            FROM scores s
            JOIN users u ON s.user_id = u.id
            ORDER BY s.score DESC
            LIMIT 100 
        `);
        res.json(scores);
    } catch (error) {
        console.error('Database error while fetching scores:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/users/{id}/score:
 *   put:
 *     summary: Update a user\'s high score
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               score:
 *                 type: integer
 *                 description: The new score to check against the high score
 *     responses:
 *       200:
 *         description: Score checked or updated successfully
 *       400:
 *         description: Invalid user ID or score
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
// Middleware to check for a valid token
const authMiddleware = (req, res, next) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Unauthorized: No token provided' });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded; // Add the payload to the request object
        next();
    } catch (error) {
        // This will catch errors like expired token, invalid signature etc.
        return res.status(403).json({ error: 'Forbidden: Invalid or expired token' });
    }
};

app.put('/api/users/:id/score', authMiddleware, async (req, res) => {
    try {
        const userIdFromParams = parseInt(req.params.id, 10);
        const userIdFromToken = req.user.id;

        // Authorization check: Ensure the user is updating their own score
        if (userIdFromParams !== userIdFromToken) {
            return res.status(403).json({ error: 'Forbidden: You can only update your own score.' });
        }

        const { score } = req.body;

        if (isNaN(userIdFromParams) || typeof score !== 'number') {
            return res.status(400).json({ error: 'Invalid user ID or score' });
        }

        const user = await db.get('SELECT max_score FROM users WHERE id = ?', [userIdFromParams]);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        let responseMessage = '';
        let newMaxScore = user.max_score;

        if (score > user.max_score) {
            await db.run('UPDATE users SET max_score = ? WHERE id = ?', [score, userIdFromParams]);
            responseMessage = 'High score updated successfully';
            newMaxScore = score;
        } else {
            responseMessage = 'Score is not higher than the current high score';
        }

        // scores 테이블에 현재 점수 기록
        await db.run(
            'INSERT INTO scores (user_id, score) VALUES (?, ?)',
            [userIdFromParams, score]
        );
        
        console.log(`Logged score ${score} for user ${userIdFromParams}`);

        res.json({ message: responseMessage, new_max_score: newMaxScore });

    } catch (error) {
        console.error('Error updating/logging score:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

const swaggerUi = require('swagger-ui-express');
const specs = require('./config/swagger.js');

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs));

// ==================================
// 전역 오류 처리 미들웨어 (가장 마지막에 위치해야 함)
// ==================================
app.use((err, req, res, next) => {
    // 1. 서버 콘솔에 상세한 오류를 로깅합니다 (개발자 확인용).
    console.error(err.stack);

    // 2. 이미 응답이 전송된 경우, Express의 기본 오류 처리기에 위임합니다.
    if (res.headersSent) {
        return next(err);
    }

    // 3. 사용자에게는 일관된 일반 오류 메시지를 보냅니다.
    res.status(500).json({ error: 'Internal Server Error' });
});

if (require.main === module) {
    startServer().then(() => {
                    const PORT = process.env.PORT || 8080;
                    app.listen(PORT, () => {
                        console.log(`Server listening on port ${PORT}`);
                    });    });
}

module.exports = { app, startServer, db };
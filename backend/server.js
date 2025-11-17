const express = require("express");
const path = require("path");
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');
const jwt = require('jsonwebtoken');

const app = express();

// --- 추가 ---
// JWT 비밀 키 (실제 운영에서는 .env 파일로 숨겨야 합니다)
const JWT_SECRET = 'your-very-strong-secret-key-12345!';

let db;

async function startServer() {
    // DB를 열고, 'invaders.db' 파일에 모든 것을 저장합니다.
    // 파일이 없으면 자동으로 생성됩니다.
    db = await open({
        filename: './invaders.db', // DB 파일 이름
        driver: sqlite3.Database
    });

    // --- 4. SQL 스키마(테이블) 생성 ---
    // database_setup.sql 대신, 서버가 시작될 때마다
    // "테이블이 없으면(IF NOT EXISTS)" 만들도록 코드를 실행합니다.
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
    
    // [테스트용 사용자 추가 (없을 경우에만)]
    await db.run(
        'INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)',
        'test',
        '1234'
    );
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

        // DB에서 사용자 조회 (SQL은 동일!)
        // db.get() : 1개의 결과만 가져옴
        const user = await db.get(
            'SELECT * FROM users WHERE username = ? AND password = ?',
            [username, password]
        );

        if (user) {
            // 로그인 성공
            // --- 수정 ---
            // 로그인 성공 시 JWT 생성
            const payload = {
                id: user.id,
                username: user.username
            };
            
            // 토큰 서명 (유효기간 1시간)
            const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '1h' });

            console.log('Login successful:', user.username);
            
            // 생성된 토큰과 사용자 정보를 반환
            res.json({ 
                token: token, 
                user: { id: user.id, username: user.username } 
            });
            // --- 수정 끝 ---
        } else {
            // 로그인 실패
            console.log('Login failed for:', username);
            res.status(401).json({ error: 'Invalid username or password' });
        }

    } catch (error) {
        console.error('Database error during login:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

// --- 추가 ---
// JWT 인증 미들웨어
// ===============================================
function authenticateToken(req, res, next) {
    // 요청 헤더(Authorization)에서 'Bearer [token]' 형식의 토큰을 가져옵니다.
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // 'Bearer' 다음의 토큰 값

    if (token == null) {
        // 토큰이 없으면 401 Unauthorized (권한 없음)
        return res.status(401).json({ error: 'Access token is required' });
    }

    // 토큰 검증
    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            // 토큰이 유효하지 않거나 만료된 경우
            console.log('JWT verification failed:', err.message);
            return res.status(403).json({ error: 'Invalid or expired token' });
        }
        // 토큰이 유효하면, req.user에 사용자 정보를 추가합니다.
        req.user = user;
        next(); // 다음 핸들러로 이동
    });
}
// ===============================================

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
        // (참고: 실제 서비스에서는 비밀번호를 해싱(bcrypt)해야 하지만,
        //  현재 프로젝트 구조에 맞춰 평문으로 저장합니다.)
        await db.run(
            'INSERT INTO users (username, password) VALUES (?, ?)',
            [username, password]
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
app.get('/api/users', authenticateToken, async function(req, res) {
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
app.get('/api/users/:id', authenticateToken, async function(req, res) {
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
app.get('/api/scores', authenticateToken, async function(req, res) {
    try {
        // score 테이블과 users 테이블을 JOIN 하여
        // 유저이름, 점수, 생성일자를 점수 내림차순으로 100개 가져옵니다.
        const scores = await db.all(`
            SELECT u.username, s.score, s.created_at 
            FROM scores s 
            JOIN users u ON s.user_id = u.id
            ORDER BY s.score DESC
            LIMIT 100 
        `); // --- 수정: 'score' -> 'scores'
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
app.put('/api/users/:id/score', authenticateToken, async (req, res) => {
    try {
        // --- 수정 ---
        // URL의 ID 대신, 인증된 토큰의 사용자 ID를 사용하는 것이 더 안전합니다.
        const userId = req.user.id; 
        // const userId = parseInt(req.params.id, 10); // (이전 코드)
        
        const { score } = req.body;

        if (isNaN(userId) || typeof score !== 'number') {
            return res.status(400).json({ error: 'Invalid user ID or score' });
        }

        const user = await db.get('SELECT max_score FROM users WHERE id = ?', [userId]);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        let responseMessage = 'Score checked.';
        let newMaxScore = user.max_score;

        if (score > user.max_score) {
            await db.run('UPDATE users SET max_score = ? WHERE id = ?', [score, userId]);
            responseMessage = 'High score updated successfully';
            newMaxScore = score;
        }

        // score 테이블에 현재 점수 기록
        await db.run(
            'INSERT INTO scores (user_id, score) VALUES (?, ?)', // --- 수정: 'score' -> 'scores'
            [userId, score]
        );
        
        console.log(`Logged score ${score} for user ${userId}`);

        // --- 수정 ---
        // (중첩되었던 불필요한 app.put 핸들러 제거)
        
        res.json({ message: responseMessage, new_max_score: newMaxScore });

    } catch (error) {
        console.error('Error updating/logging score:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

const swaggerUi = require('swagger-ui-express');
const specs = require('./config/swagger.js');

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs));

if (require.main === module) {
    startServer().then(() => {
        app.listen(8080, () => {
            console.log('Server listening on port 8080');
        });
    });
}

module.exports = { app, startServer, db };
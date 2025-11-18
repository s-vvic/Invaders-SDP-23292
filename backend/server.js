const express = require("express");
const path = require("path");
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');
const bcrypt = require('bcrypt');
const helmet = require('helmet');
const hpp = require('hpp');

const app = express();
app.use(helmet());
app.use(hpp());

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
    // 기존 'test' 사용자가 평문 비밀번호로 저장되어 있을 수 있으므로 삭제 후 다시 생성
    await db.run('DELETE FROM users WHERE username = ?', ['test']);
    
    const saltRounds = 10;
    const testUserPassword = '1234';
    const hashedTestPassword = await bcrypt.hash(testUserPassword, saltRounds);

    await db.run(
        'INSERT INTO users (username, password) VALUES (?, ?)',
        ['test', hashedTestPassword]
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

        // DB에서 사용자 조회
        const user = await db.get(
            'SELECT * FROM users WHERE username = ?',
            [username]
        );

        if (user) {
            // 사용자가 존재하면 비밀번호 비교
            const match = await bcrypt.compare(password, user.password);
            if (match) {
                // 로그인 성공
                console.log('Login successful:', user.username);
                res.json({ token: 'your-generated-token-xyz123', user: { id: user.id, username: user.username } });
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

    // In a real app, you'd verify a real JWT. Here, we'll just check our hardcoded token.
    if (token === 'your-generated-token-xyz123') {
        next(); // Token is valid, proceed to the route handler
    } else {
        return res.status(401).json({ error: 'Unauthorized: Invalid token' });
    }
};

app.put('/api/users/:id/score', authMiddleware, async (req, res) => {
    try {
        const userId = parseInt(req.params.id, 10);
        const { score } = req.body;

        if (isNaN(userId) || typeof score !== 'number') {
            return res.status(400).json({ error: 'Invalid user ID or score' });
        }

        const user = await db.get('SELECT max_score FROM users WHERE id = ?', [userId]);

        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        let responseMessage = '';
        let newMaxScore = user.max_score;

        if (score > user.max_score) {
            await db.run('UPDATE users SET max_score = ? WHERE id = ?', [score, userId]);
            responseMessage = 'High score updated successfully';
            newMaxScore = score;
        } else {
            responseMessage = 'Score is not higher than the current high score';
        }

        // scores 테이블에 현재 점수 기록
        await db.run(
            'INSERT INTO scores (user_id, score) VALUES (?, ?)',
            [userId, score]
        );
        
        console.log(`Logged score ${score} for user ${userId}`);

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
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

// .env file setup
const envPath = path.join(__dirname, '.env');
const envExamplePath = path.join(__dirname, '.env.example');

if (!fs.existsSync(envPath)) {
    if (fs.existsSync(envExamplePath)) {
        fs.copyFileSync(envExamplePath, envPath);
        console.log('NOTE: .env file not found. A new .env file has been created by copying .env.example. Please review it.');
    } else {
        console.warn('WARNING: .env file not found and .env.example is also missing. Cannot auto-create .env.');
    }
}

require('dotenv').config({ path: envPath });

const REQUIRED_ENV_VARS = ['JWT_SECRET'];
const missingEnv = REQUIRED_ENV_VARS.filter((key) => !process.env[key]);

if (missingEnv.length > 0) {
    const errorMsg = `
        Missing required environment variables: ${missingEnv.join(', ')}
        Please ensure you have a .env file in the /backend directory with all the required variables.
        You can use the .env.example file as a template.
    `;
    throw new Error(errorMsg);
}

const app = express();
app.use(helmet());
app.use(hpp());

const isTestEnv = process.env.NODE_ENV === 'test';

const defaultOrigins = [
    'http://localhost:3000',
    'http://localhost:4173',
    'http://localhost:5173',
    'http://localhost:8080',
];

const configuredOrigins = (process.env.ALLOWED_ORIGINS || '')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);

const allowedOrigins = configuredOrigins.length ? configuredOrigins : defaultOrigins;

app.use(cors({
    origin: (origin, callback) => {
        if (isTestEnv || !origin) {
            return callback(null, true);
        }

        if (allowedOrigins.includes(origin)) {
            return callback(null, true);
        }

        return callback(new Error('Not allowed by CORS'));
    },
    optionsSuccessStatus: 200,
}));

// Rate limiting
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // Limit each IP to 100 requests per window
    standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
    legacyHeaders: false, // Disable the `X-RateLimit-*` headers
});
app.use(limiter);
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many attempts, please try again later.' },
});

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

        CREATE TABLE IF NOT EXISTS achievements(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            description TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS user_achievements(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            achievement_id INTEGER NOT NULL,
            unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (achievement_id) REFERENCES achievements(id),
            UNIQUE(user_id, achievement_id)
        );
    `);
    
    // 기본 achievements 데이터 추가
    await seedAchievements();
    
    // 테스트 데이터 추가 (개발 환경에서만)
    await seedTestData();
    
    return db; // Return the database connection
}

async function seedTestData() {
    try {
        console.log('Seeding test data...');
        
        // 테스트 사용자들 생성 (이미 있으면 스킵)
        const testUsers = [
            { username: 'PlayerOne', password: 'password123' },
            { username: 'PlayerTwo', password: 'password123' },
            { username: 'SpaceInvader', password: 'password123' },
            { username: 'ProGamer', password: 'password123' },
            { username: 'Newbie', password: 'password123' },
            { username: 'Champion', password: 'password123' },
            { username: 'AcePlayer', password: 'password123' },
            { username: 'Master', password: 'password123' },
            { username: 'Elite', password: 'password123' },
            { username: 'Legend', password: 'password123' }
        ];

        const userIds = [];
        
        for (const user of testUsers) {
            // 사용자가 이미 있는지 확인
            let existingUser = await db.get('SELECT id FROM users WHERE username = ?', [user.username]);
            
            if (existingUser) {
                // 이미 존재하면 기존 ID 사용
                userIds.push({ id: existingUser.id, username: user.username });
            } else {
                // 없으면 새로 생성
                const hashedPassword = await bcrypt.hash(user.password, 10);
                const result = await db.run(
                    'INSERT INTO users (username, password, max_score) VALUES (?, ?, ?)',
                    [user.username, hashedPassword, 0]
                );
                userIds.push({ id: result.lastID, username: user.username });
            }
        }
        
        // 기존 테스트 사용자들의 점수 확인
        const testUserIds = userIds.map(u => u.id);
        let existingScoresCount = 0;
        if (testUserIds.length > 0) {
            const countResult = await db.get(`
                SELECT COUNT(*) as count 
                FROM scores 
                WHERE user_id IN (${testUserIds.map(() => '?').join(',')})
            `, testUserIds);
            existingScoresCount = countResult ? countResult.count : 0;
        }
        
        // 이미 충분한 테스트 점수가 있으면 스킵 (27개 이상)
        if (existingScoresCount >= 27) {
            console.log(`Test scores already exist (${existingScoresCount} scores), skipping seed...`);
            return;
        }
        
        // 테스트 점수가 부족하면 기존 점수 삭제 후 재생성
        if (testUserIds.length > 0 && existingScoresCount > 0) {
            await db.run(`
                DELETE FROM scores 
                WHERE user_id IN (${testUserIds.map(() => '?').join(',')})
            `, testUserIds);
            console.log(`Cleared existing scores for test users`);
        }

        // 다양한 시점의 점수 데이터 추가
        const now = new Date();
        const scores = [
            // 최근 점수들 (주간/연간에 포함됨)
            { userId: userIds[0].id, score: 150000, daysAgo: 1 },
            { userId: userIds[1].id, score: 125000, daysAgo: 2 },
            { userId: userIds[2].id, score: 110000, daysAgo: 3 },
            { userId: userIds[3].id, score: 95000, daysAgo: 4 },
            { userId: userIds[4].id, score: 85000, daysAgo: 5 },
            { userId: userIds[5].id, score: 75000, daysAgo: 6 },
            { userId: userIds[6].id, score: 65000, daysAgo: 1 },
            { userId: userIds[7].id, score: 55000, daysAgo: 2 },
            { userId: userIds[8].id, score: 45000, daysAgo: 3 },
            { userId: userIds[9].id, score: 35000, daysAgo: 4 },
            
            // 추가 점수들 (더 많은 기록)
            { userId: userIds[0].id, score: 140000, daysAgo: 10 },
            { userId: userIds[1].id, score: 120000, daysAgo: 15 },
            { userId: userIds[2].id, score: 105000, daysAgo: 20 },
            { userId: userIds[3].id, score: 90000, daysAgo: 30 },
            { userId: userIds[4].id, score: 80000, daysAgo: 45 },
            { userId: userIds[5].id, score: 70000, daysAgo: 60 },
            { userId: userIds[6].id, score: 60000, daysAgo: 90 },
            { userId: userIds[7].id, score: 50000, daysAgo: 120 },
            { userId: userIds[8].id, score: 40000, daysAgo: 180 },
            { userId: userIds[9].id, score: 30000, daysAgo: 200 },
            
            // 연간에는 포함되지만 주간에는 포함되지 않는 점수들
            { userId: userIds[0].id, score: 135000, daysAgo: 100 },
            { userId: userIds[1].id, score: 115000, daysAgo: 150 },
            { userId: userIds[2].id, score: 100000, daysAgo: 200 },
            { userId: userIds[3].id, score: 88000, daysAgo: 250 },
            { userId: userIds[4].id, score: 78000, daysAgo: 300 },
            
            // 오래된 점수들 (연간에도 포함되지 않음)
            { userId: userIds[0].id, score: 130000, daysAgo: 400 },
            { userId: userIds[1].id, score: 110000, daysAgo: 500 }
        ];

        for (const scoreData of scores) {
            const scoreDate = new Date(now);
            scoreDate.setDate(scoreDate.getDate() - scoreData.daysAgo);
            const dateString = scoreDate.toISOString().replace('T', ' ').substring(0, 19);
            
            await db.run(
                'INSERT INTO scores (user_id, score, created_at) VALUES (?, ?, ?)',
                [scoreData.userId, scoreData.score, dateString]
            );
        }

        // 각 사용자의 최고 점수 업데이트
        for (const user of userIds) {
            const maxScore = await db.get(
                'SELECT MAX(score) as max FROM scores WHERE user_id = ?',
                [user.id]
            );
            if (maxScore && maxScore.max) {
                await db.run(
                    'UPDATE users SET max_score = ? WHERE id = ?',
                    [maxScore.max, user.id]
                );
            }
        }

        console.log(`Test data seeded: ${testUsers.length} users, ${scores.length} scores`);
    } catch (error) {
        console.error('Error seeding test data:', error);
        // 에러가 발생해도 서버는 계속 실행되도록 함
    }
}

async function seedAchievements() {
    try {
        // 기본 achievements 목록
        const achievements = [
            { name: 'Beginner', description: 'Clear level 1' },
            { name: 'Intermediate', description: 'Clear level 3' },
            { name: 'Boss Slayer', description: 'Defeat a boss' },
            { name: 'Mr. Greedy', description: 'Have more than 2000 coins' },
            { name: 'First Blood', description: 'Defeat your first enemy' },
            { name: 'Bear Grylls', description: 'Survive for 60 seconds' },
            { name: 'Bad Sniper', description: 'Under 80% accuracy' },
            { name: 'Conqueror', description: 'Clear the final level' }
        ];

        for (const achievement of achievements) {
            // 이미 존재하는지 확인
            const existing = await db.get(
                'SELECT id FROM achievements WHERE name = ?',
                [achievement.name]
            );

            if (!existing) {
                await db.run(
                    'INSERT INTO achievements (name, description) VALUES (?, ?)',
                    [achievement.name, achievement.description]
                );
            }
        }

        console.log('Achievements seeded successfully');
    } catch (error) {
        console.error('Error seeding achievements:', error);
        // 에러가 발생해도 서버는 계속 실행되도록 함
    }
}

app.use(express.json({ limit: '10kb' }));
app.use(express.urlencoded({ extended: false, limit: '10kb' }));

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
const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,24}$/;
const isValidPassword = (password = '') =>
    typeof password === 'string' &&
    password.length >= 8 &&
    password.length <= 64;

const validateCredentials = ({ username, password }) => {
    if (!USERNAME_REGEX.test(username || '')) {
        return 'Username must be 3-24 characters (letters, numbers, underscore).';
    }

    if (!isValidPassword(password)) {
        return 'Password must be 8-64 characters long.';
    }

    return null;
};

app.post('/api/login', authLimiter, async function(req, res) {
    try {
        const { username, password } = req.body;

        const validationError = validateCredentials({ username, password });

        if (validationError) {
            return res.status(400).json({ error: validationError });
        }

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
    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) {
            // --- 수정 ---
            // 토큰이 유효하지 않거나 만료된 경우
            // 403 (Forbidden) 대신 401 (Unauthorized)을 보냅니다.
            // 401은 "인증 실패" (토큰 만료/없음)에 더 적합합니다.
            console.log('JWT verification failed:', err.message);

            let errorMessage = 'Invalid token. Please log in again.';
            // 만료 오류인지 명확히 확인
            if (err.name === 'TokenExpiredError') {
                errorMessage = 'Token expired. Please log in again.';
            }
            
            return res.status(401).json({ error: errorMessage });
            // --- 수정 끝 ---
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
app.post('/api/register', authLimiter, async (req, res) => {
    try {
        const { username, password } = req.body;

        const validationError = validateCredentials({ username, password });

        if (validationError) {
            return res.status(400).json({ error: validationError });
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
 * /api/users/{id}/stats:
 *   get:
 *     summary: Retrieve user statistics (total games, average score, rank, recent games)
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
 *         description: User statistics
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 totalGames:
 *                   type: integer
 *                 averageScore:
 *                   type: number
 *                 rank:
 *                   type: integer
 *                 rankOutOf:
 *                   type: integer
 *                 recentGames:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       score:
 *                         type: integer
 *                       created_at:
 *                         type: string
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
app.get('/api/users/:id/stats', async function(req, res) {
    try {
        const userId = parseInt(req.params.id, 10);

        if (isNaN(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        // 사용자 존재 확인
        const user = await db.get('SELECT id, max_score FROM users WHERE id = ?', [userId]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // 총 게임 수
        const totalGamesResult = await db.get(
            'SELECT COUNT(*) as count FROM scores WHERE user_id = ?',
            [userId]
        );
        const totalGames = totalGamesResult ? totalGamesResult.count : 0;

        // 평균 점수
        const avgScoreResult = await db.get(
            'SELECT AVG(score) as avg FROM scores WHERE user_id = ?',
            [userId]
        );
        const averageScore = avgScoreResult && avgScoreResult.avg ? Math.round(avgScoreResult.avg) : 0;

        // 순위 계산 (max_score 기준)
        const rankResult = await db.get(`
            SELECT COUNT(*) + 1 as rank
            FROM users
            WHERE max_score > ?
        `, [user.max_score]);
        const rank = rankResult ? rankResult.rank : 1;

        // 전체 사용자 수
        const totalUsersResult = await db.get('SELECT COUNT(*) as count FROM users');
        const rankOutOf = totalUsersResult ? totalUsersResult.count : 1;

        // 최근 게임 기록 (최근 10개)
        const recentGames = await db.all(`
            SELECT score, created_at
            FROM scores
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT 10
        `, [userId]);

        res.json({
            totalGames,
            averageScore,
            rank,
            rankOutOf,
            recentGames: recentGames.map(game => ({
                score: game.score,
                created_at: game.created_at
            }))
        });

    } catch (error) {
        console.error('Database error while fetching user stats:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/users/{id}/achievements:
 *   get:
 *     summary: Retrieve user's achievements
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
 *         description: User achievements
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: integer
 *                   name:
 *                     type: string
 *                   description:
 *                     type: string
 *                   unlocked:
 *                     type: boolean
 *                   unlocked_at:
 *                     type: string
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
app.get('/api/users/:id/achievements', async function(req, res) {
    try {
        const userId = parseInt(req.params.id, 10);

        if (isNaN(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        // 사용자 존재 확인
        const user = await db.get('SELECT id FROM users WHERE id = ?', [userId]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // 모든 achievements와 사용자의 해제 상태 조회
        const achievements = await db.all(`
            SELECT 
                a.id,
                a.name,
                a.description,
                CASE WHEN ua.id IS NOT NULL THEN 1 ELSE 0 END as unlocked,
                ua.unlocked_at
            FROM achievements a
            LEFT JOIN user_achievements ua ON a.id = ua.achievement_id AND ua.user_id = ?
            ORDER BY a.id
        `, [userId]);

        res.json(achievements.map(ach => ({
            id: ach.id,
            name: ach.name,
            description: ach.description,
            unlocked: ach.unlocked === 1,
            unlocked_at: ach.unlocked_at || null
        })));

    } catch (error) {
        console.error('Database error while fetching user achievements:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

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

/**
 * @swagger
 * /api/users/{id}/achievements:
 *   post:
 *     summary: Unlock an achievement for a user
 *     security:
 *       - bearerAuth: []
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
 *               achievement_name:
 *                 type: string
 *     responses:
 *       200:
 *         description: Achievement unlocked successfully
 *       400:
 *         description: Invalid user ID or achievement name
 *       401:
 *         description: Unauthorized - No token provided
 *       403:
 *         description: Forbidden - Invalid token or trying to update another user's achievements
 *       404:
 *         description: User or achievement not found
 *       500:
 *         description: Server database error
 */
app.post('/api/users/:id/achievements', authMiddleware, async function(req, res) {
    try {
        const userIdFromParams = parseInt(req.params.id, 10);
        const userIdFromToken = req.user.id;

        // Authorization check
        if (userIdFromParams !== userIdFromToken) {
            return res.status(403).json({ error: 'Forbidden: You can only update your own achievements.' });
        }

        if (isNaN(userIdFromParams)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const { achievement_name } = req.body;

        if (!achievement_name || typeof achievement_name !== 'string') {
            return res.status(400).json({ error: 'Achievement name is required' });
        }

        // 사용자 존재 확인
        const user = await db.get('SELECT id FROM users WHERE id = ?', [userIdFromParams]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Achievement 존재 확인
        const achievement = await db.get(
            'SELECT id FROM achievements WHERE name = ?',
            [achievement_name]
        );

        if (!achievement) {
            return res.status(404).json({ error: 'Achievement not found' });
        }

        // 이미 해제되었는지 확인
        const existing = await db.get(
            'SELECT id FROM user_achievements WHERE user_id = ? AND achievement_id = ?',
            [userIdFromParams, achievement.id]
        );

        if (existing) {
            return res.status(200).json({ message: 'Achievement already unlocked' });
        }

        // 한국 시간대(UTC+9)로 현재 시간 저장
        const dateString = new Date().toISOString().slice(0, 19).replace('T', ' ');

        // Achievement 해제
        await db.run(
            'INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) VALUES (?, ?, ?)',
            [userIdFromParams, achievement.id, dateString]
        );

        console.log(`Achievement "${achievement_name}" unlocked for user ${userIdFromParams}`);

        res.json({ message: 'Achievement unlocked successfully' });

    } catch (error) {
        console.error('Error unlocking achievement:', error);
        res.status(500).json({ error: 'Server database error' });
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
        // scores 테이블과 users 테이블을 JOIN 하여
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
 * /api/scores/weekly:
 *   get:
 *     summary: Retrieve weekly high scores (last 7 days)
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of weekly scores with usernames, ordered by score descending
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
app.get('/api/scores/weekly', async function(req, res) {
    try {
        // 지난 7일간의 점수를 가져옵니다
        const scores = await db.all(`
            SELECT u.username, s.score, s.created_at 
            FROM scores s
            JOIN users u ON s.user_id = u.id
            WHERE s.created_at >= datetime('now', '-7 days')
            ORDER BY s.score DESC
            LIMIT 100 
        `);
        res.json(scores);
    } catch (error) {
        console.error('Database error while fetching weekly scores:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/scores/yearly:
 *   get:
 *     summary: Retrieve yearly high scores (last 365 days)
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of yearly scores with usernames, ordered by score descending
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
app.get('/api/scores/yearly', async function(req, res) {
    try {
        // 지난 1년간의 점수를 가져옵니다
        const scores = await db.all(`
            SELECT u.username, s.score, s.created_at 
            FROM scores s
            JOIN users u ON s.user_id = u.id
            WHERE s.created_at >= datetime('now', '-365 days')
            ORDER BY s.score DESC
            LIMIT 100 
        `);
        res.json(scores);
    } catch (error) {
        console.error('Database error while fetching yearly scores:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

/**
 * @swagger
 * /api/users/{id}/score:
 *   put:
 *     summary: Update a user's high score
 *     security:
 *       - bearerAuth: []
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
 *       401:
 *         description: Unauthorized - No token provided
 *       403:
 *         description: Forbidden - Invalid token or trying to update another user's score
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
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
        const dateString = new Date().toISOString().slice(0, 19).replace('T', ' ');
        
        await db.run(
            'INSERT INTO scores (user_id, score, created_at) VALUES (?, ?, ?)',
            [userIdFromParams, score, dateString]
        );
        
        console.log(`Logged score ${score} for user ${userIdFromParams}`);

        res.json({ message: responseMessage, new_max_score: newMaxScore });

    } catch (error) {
        console.error('Error updating/logging score:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

// Load and use Swagger only if not in a test environment
if (process.env.NODE_ENV !== 'test') {
    const swaggerUi = require('swagger-ui-express');
    const specs = require('./config/swagger.js');
    app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs));
}

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

    if (err.message === 'Not allowed by CORS') {
        return res.status(403).json({ error: 'Origin not allowed' });
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
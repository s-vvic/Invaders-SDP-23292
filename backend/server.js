const express = require("express");
const path = require("path");
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');

const app = express();

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
    `);
    
    // [테스트용 사용자 추가 (없을 경우에만)]
    await db.run(
        'INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)',
        'test',
        '1234'
    );
}
app.use(express.json());

const publicPath = path.join(__dirname, '../frontend');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});

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
            console.log('Login successful:', user.username);
            res.json({ token: 'your-generated-token-xyz123' });
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

app.get('/api/users', async function(req, res) {
    try {
        const users = await db.all('SELECT id, username, max_score FROM users');
        res.json(users);
    } catch (error) {
        console.error('Database error while fetching users:', error);
        res.status(500).json({ error: 'Server database error' });
    }
});

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

if (require.main === module) {
    startServer().then(() => {
        app.listen(8080, () => {
            console.log('Server listening on port 8080');
        });
    });
}

module.exports = { app, startServer };
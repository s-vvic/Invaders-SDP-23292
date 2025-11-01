const express = require("express");
const path = require("path");
const mysql = require('mysql2/promise');
const app = express();

app.listen(8080, function() {
    console.log('listening on 8080 port')
});

const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '1234',
    database: 'invaders_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

app.use(express.json());

const publicPath = path.join(__dirname, '../frontend/src/main/resources/public');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});

app.post('/api/login', async function(req, res) {
    const { username, password } = req.body;

    // 🚨 보안 경고:
    // 실제 서비스에서는 비밀번호를 DB에 그대로 저장하지 않습니다.
    // 1. 회원가입 시: 'bcrypt' 라이브러리로 비밀번호를 해싱(암호화)해서 DB에 저장
    // 2. 로그인 시: 
    //    (1) DB에서 username으로 해싱된 비밀번호(user.password)를 가져옵니다.
    //    (2) bcrypt.compare(사용자가-입력한-pw, user.password) 함수로 일치하는지 비교합니다.
    //
    // 이 코드는 학습용으로, 비밀번호를 *그대로* 비교하는 **안전하지 않은** 방식입니다.

    try {
        // SQL 쿼리 작성 (SQL Injection 방지를 위해 '?' 사용)
        const sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        // 풀에서 연결을 가져와 쿼리 실행
        const [rows] = await pool.query(sql, [username, password]);

        if (rows.length > 0) {
            // 로그인 성공 (일치하는 사용자가 있음)
            const user = rows[0];
            console.log(`Login successful: ${user.username}`);
            res.json({ 
                message: 'Login successful!',
                token: 'your-generated-token-xyz123' // (나중에 JWT 토큰으로 대체)
            });
        } else {
            // 로그인 실패 (일치하는 사용자가 없음)
            console.log(`Login failed: ${username}`);
            res.status(401).json({ 
                error: 'Invalid username or password' 
            });
        }
    } catch (error) {
        // DB 연결 또는 쿼리 중 에러 발생
        console.error('Database error during login:', error);
        res.status(500).json({
            error: 'Server database error'
        });
    }
});
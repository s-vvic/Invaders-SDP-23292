const fs = require('fs');
const path = require('path');
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');
const bcrypt = require('bcrypt');

let db;

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

async function initDb() {
    // .data 디렉토리가 없으면 생성
    const dbDir = path.join(__dirname, '..', '.data');
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

const getDb = () => {
    if (!db) {
        throw new Error('Database not initialized. Call initDb first.');
    }
    return db;
};

module.exports = { initDb, getDb };

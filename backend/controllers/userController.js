const { getDb } = require('../db');

const getAllUsers = async (req, res) => {
    const db = getDb();
    try {
        const users = await db.all('SELECT id, username, max_score FROM users');
        res.json(users);
    } catch (error) {
        console.error('Database error while fetching users:', error);
        res.status(500).json({ error: 'Server database error' });
    }
};

const getUserById = async (req, res) => {
    const db = getDb();
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
};

const getUserStats = async (req, res) => {
    const db = getDb();
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
};

const getUserAchievements = async (req, res) => {
    const db = getDb();
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
};

const unlockAchievement = async (req, res) => {
    const db = getDb();
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
        const dateString = new Date().toLocaleString('sv-SE', { timeZone: 'Asia/Seoul' }).slice(0, 19).replace('T', ' ');

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
};

const updateScore = async (req, res) => {
    const db = getDb();
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
};


module.exports = {
    getAllUsers,
    getUserById,
    getUserStats,
    getUserAchievements,
    unlockAchievement,
    updateScore
};

const { getDb } = require('../db');
const userService = require('../services/userService');
const { handleServiceResponse } = require('../utils/responseHelper');

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
    try {
        const userId = parseInt(req.params.id, 10);

        if (isNaN(userId)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        const stats = await userService.getUserStats(userId);

        if (!stats) {
            return res.status(404).json({ error: 'User not found' });
        }

        res.json(stats);

    } catch (error) {
        console.error('Error fetching user stats:', error); // Log general error
        console.error('Error fetching user stats:', error); // Log general error
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

        // Check if user exists
        const user = await db.get('SELECT id FROM users WHERE id = ?', [userId]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        // Retrieve all achievements and the user's unlock status
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
    try {
        const userIdFromParams = parseInt(req.params.id, 10);
        const userIdFromToken = req.user.id;

        

        // Authorization check: Ensure the user is updating their own achievements

        if (isNaN(userIdFromParams)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        // 사용자 존재 확인은 서비스에서 한 번 더 하지만, 컨트롤러에서 기본적인 사용자 ID 유효성 검사 후 권한 체크를 위해 user 객체는 가져오지 않아도 됨.
        // 하지만 테스트 편의성을 위해 404를 먼저 보내야 하는 경우를 대비해 여기서 먼저 체크하는 것이 좋음.
        const user = await getDb().get('SELECT id FROM users WHERE id = ?', [userIdFromParams]);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }
        
        // Authorization check
        if (userIdFromParams !== userIdFromToken) {
            return res.status(403).json({ error: 'Forbidden: You can only update your own achievements.' });
        }

        const { achievement_name } = req.body;

        if (!achievement_name || typeof achievement_name !== 'string') {
            return res.status(400).json({ error: 'Achievement name is required' });
        }

        const result = await userService.unlockAchievement(userIdFromParams, achievement_name);

        if (result.status === 404) {
            return res.status(404).json({ error: result.message });
        } else if (result.status === 200) {
            res.status(200).json({ message: result.message });
        } else {
            // Default to 500 if the service returns an unexpected status
            res.status(result.status || 500).json({ error: result.message || 'Server error unlocking achievement' });
        }

    } catch (error) {
        console.error('Error unlocking achievement:', error);
        res.status(500).json({ error: 'Server database error' });
    }
};

const updateScore = async (req, res) => {
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

        

        
        const result = await userService.updateUserScore(userIdFromParams, score);

        if (result.status === 404) {
            return res.status(404).json({ error: result.message });
        } else if (result.status === 200) {
            res.json({ message: result.message, new_max_score: result.new_max_score });
        } else {
            res.status(500).json({ error: result.message || 'Server error updating score' });
        }

    } catch (error) {
        console.error('Error updating/logging score:', error);
        res.status(500).json({ error: 'Server database error' });
    }
    return handleServiceResponse(res, result, 'Server error updating score');
};


module.exports = {
    getAllUsers,
    getUserById,
    getUserStats,
    getUserAchievements,
    unlockAchievement,
    updateScore
};

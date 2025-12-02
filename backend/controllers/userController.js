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

        if (isNaN(userIdFromParams)) {
            return res.status(400).json({ error: 'Invalid user ID' });
        }

        // Authorization check: Ensure the user is updating their own achievements
        if (userIdFromParams !== userIdFromToken) {
            return res.status(403).json({ error: 'Forbidden: You can only update your own achievements.' });
        }

        const { achievement_name } = req.body;

        if (!achievement_name || typeof achievement_name !== 'string') {
            return res.status(400).json({ error: 'Achievement name is required' });
        }

        const result = await userService.unlockAchievement(userIdFromParams, achievement_name);

        return handleServiceResponse(res, result, 'Server error unlocking achievement');

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

        return handleServiceResponse(res, result, 'Server error updating score');

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

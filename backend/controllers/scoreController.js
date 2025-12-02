const { getDb } = require('../db');

const getAllScores = async (req, res) => {
    const db = getDb();
    try {
        // Join scores and users tables to get username, score, and created_at, ordered by score descending, limit 100.
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
};

const getWeeklyScores = async (req, res) => {
    const db = getDb();
    try {
        // Get scores from the last 7 days
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
};

const getYearlyScores = async (req, res) => {
    const db = getDb();
    try {
        // Get scores from the last 1 year
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
};

module.exports = {
    getAllScores,
    getWeeklyScores,
    getYearlyScores,
};

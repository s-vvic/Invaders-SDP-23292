const { getDb } = require('../db');

const getAllScores = async (req, res) => {
    const db = getDb();
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
};

const getWeeklyScores = async (req, res) => {
    const db = getDb();
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
};

const getYearlyScores = async (req, res) => {
    const db = getDb();
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
};

module.exports = {
    getAllScores,
    getWeeklyScores,
    getYearlyScores,
};

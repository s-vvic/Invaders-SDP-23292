const { getDb } = require('../db');

async function getUserStats(userId) {
    const db = getDb();

    // Check if user exists first
    const user = await db.get('SELECT id, max_score FROM users WHERE id = ?', [userId]);
    if (!user) {
        return null; // Indicate user not found
    }

    // Execute queries in parallel using Promise.all
    const [
        totalGamesResult,
        avgScoreResult,
        rankResult,
        totalUsersResult,
        recentGames
    ] = await Promise.all([
        db.get('SELECT COUNT(*) as count FROM scores WHERE user_id = ?', [userId]),
        db.get('SELECT AVG(score) as avg FROM scores WHERE user_id = ?', [userId]),
        db.get(`
            SELECT COUNT(*) + 1 as rank
            FROM users
            WHERE max_score > ?
        `, [user.max_score]),
        db.get('SELECT COUNT(*) as count FROM users'),
        db.all(`
            SELECT score, created_at
            FROM scores
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT 10
        `, [userId])
    ]);

    const totalGames = totalGamesResult ? totalGamesResult.count : 0;
    const averageScore = avgScoreResult && avgScoreResult.avg ? Math.round(avgScoreResult.avg) : 0;
    const rank = rankResult ? rankResult.rank : 1;
    const rankOutOf = totalUsersResult ? totalUsersResult.count : 1;

    return {
        totalGames,
        averageScore,
        rank,
        rankOutOf,
        recentGames: recentGames.map(game => ({
            score: game.score,
            created_at: game.created_at
        }))
    };
}

async function unlockAchievement(userId, achievementName) {
    const db = getDb();

    // Check if user exists (already handled in controller before calling service, but good to have safety)
    const user = await db.get('SELECT id FROM users WHERE id = ?', [userId]);
    if (!user) {
        return { status: 404, message: 'User not found' };
    }

    // Check if achievement exists
    const achievement = await db.get(
        'SELECT id FROM achievements WHERE name = ?',
        [achievementName]
    );

    if (!achievement) {
        return { status: 404, message: 'Achievement not found' };
    }

    // Check if already unlocked
    const existing = await db.get(
        'SELECT id FROM user_achievements WHERE user_id = ? AND achievement_id = ?',
        [userId, achievement.id]
    );

    if (existing) {
        return { status: 200, message: 'Achievement already unlocked' };
    }

    // Save current time in Korean timezone (UTC+9)
    const dateString = new Date().toISOString().slice(0, 19).replace('T', ' ');

    // Unlock achievement
    await db.run(
        'INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) VALUES (?, ?, ?)',
        [userId, achievement.id, dateString]
    );

    console.log(`Achievement "${achievementName}" unlocked for user ${userId}`);

    return { status: 200, message: 'Achievement unlocked successfully' };
}

async function updateUserScore(userId, newScore) {
    const db = getDb();

    const user = await db.get('SELECT max_score FROM users WHERE id = ?', [userId]);

    if (!user) {
        return { status: 404, message: 'User not found' };
    }

    let responseMessage = '';
    let newMaxScore = user.max_score;

    if (newScore > user.max_score) {
        await db.run('UPDATE users SET max_score = ? WHERE id = ?', [newScore, userId]);
        responseMessage = 'High score updated successfully';
        newMaxScore = newScore;
    } else {
        responseMessage = 'Score is not higher than the current high score';
    }

    // Record current score in scores table
    const dateString = new Date().toISOString().slice(0, 19).replace('T', ' ');
    
    await db.run(
        'INSERT INTO scores (user_id, score, created_at) VALUES (?, ?, ?)',
        [userId, newScore, dateString]
    );
    
    console.log(`Logged score ${newScore} for user ${userId}`);

    return { status: 200, message: responseMessage, new_max_score: newMaxScore };
}

module.exports = {
    getUserStats,
    unlockAchievement,
    updateUserScore,
};
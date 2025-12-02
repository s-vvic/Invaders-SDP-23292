process.env.ALLOWED_ORIGINS = 'https://example.com';

const request = require('supertest');
const bcrypt = require('bcrypt');
const { app } = require('../server');
const { initDb } = require('../db');
const { stopCleanup } = require('../utils/deviceStore');

const TEST_USERNAME = 'test_users'; // Unique username for this test suite
const TEST_PASSWORD = 'Password123!';

let db;
let testUser;
let userToken; // Declaring token here for wider scope

beforeAll(async () => {
    db = await initDb();

    let existingUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);

    if (!existingUser) {
        const hashedPassword = await bcrypt.hash(TEST_PASSWORD, 10);
        await db.run(
            'INSERT INTO users (username, password) VALUES (?, ?)',
            [TEST_USERNAME, hashedPassword]
        );
    }
    // Always fetch the latest testUser after potential creation/update
    testUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);

    // Get a token for user API endpoints that require authentication
    const loginResponse = await request(app)
        .post('/api/auth/login')
        .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
    userToken = loginResponse.body.token;
});

afterAll(async () => {
    if (db) {
        await db.run('DELETE FROM users WHERE username = ?', [TEST_USERNAME]); // Clean up created user
        await db.close();
    }
    stopCleanup();
});

describe('User API Endpoints', () => {

    test('GET /api/users - should fetch all users successfully', async () => {
        const response = await request(app)
            .get('/api/users')
            .set('Authorization', `Bearer ${userToken}`); // Added authorization
        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(Array.isArray(response.body)).toBe(true);
        expect(response.body.length).toBeGreaterThan(0);
        expect(response.body[0]).toHaveProperty('id');
        expect(response.body[0]).toHaveProperty('username');
        expect(response.body[0]).toHaveProperty('max_score');
        expect(response.body[0]).not.toHaveProperty('password');
    });

    test('GET /api/users/:id - should fetch a single user successfully', async () => {
        const response = await request(app)
            .get(`/api/users/${testUser.id}`)
            .set('Authorization', `Bearer ${userToken}`); // Added authorization
        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(response.body.id).toBe(testUser.id);
        expect(response.body.username).toBe(TEST_USERNAME);
        expect(response.body).not.toHaveProperty('password');
    });

    test('GET /api/users/:id - should return 404 for a non-existent user', async () => {
        const response = await request(app)
            .get('/api/users/9999')
            .set('Authorization', `Bearer ${userToken}`); // Added authorization
        expect(response.statusCode).toBe(404);
        expect(response.body.error).toBe('User not found');
    });

    test('GET /api/users/:id - should return 400 for an invalid user ID', async () => {
        const response = await request(app)
            .get('/api/users/abc')
            .set('Authorization', `Bearer ${userToken}`); // Added authorization
        expect(response.statusCode).toBe(400);
        expect(response.body.error).toBe('Invalid user ID');
    });
});

describe('PUT /api/users/:id/score', () => {
    let token;

    // Before each test in this block, log in and get a fresh token
    beforeEach(async () => {
        await db.run(`UPDATE users SET max_score = 0 WHERE id = ?`, [testUser.id]);
        const loginResponse = await request(app)
            .post('/api/auth/login') // Changed from /api/login to /api/auth/login
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        token = loginResponse.body.token;
    });

    test('should update max_score if new score is higher', async () => {
        const newScore = 100;
        const response = await request(app)
            .put(`/api/users/${testUser.id}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: newScore });

        expect(response.statusCode).toBe(200);
        expect(response.body.message).toBe('High score updated successfully');
        expect(response.body.new_max_score).toBe(newScore);

        // Verify the score was actually updated in the DB
        const userResponse = await request(app)
            .get(`/api/users/${testUser.id}`)
            .set('Authorization', `Bearer ${token}`); // Added authorization
        expect(userResponse.body.max_score).toBe(newScore);
    });

    test('should not update max_score if new score is lower or equal', async () => {
        // First, set a high score
        await db.run(`UPDATE users SET max_score = 150 WHERE id = ?`, [testUser.id]);

        const newScore = 100;
        const response = await request(app)
            .put(`/api/users/${testUser.id}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: newScore });

        expect(response.statusCode).toBe(200);
        expect(response.body.message).toBe('Score is not higher than the current high score');
        expect(response.body.new_max_score).toBe(150);

        // Verify the score was not changed
        const userResponse = await request(app)
            .get(`/api/users/${testUser.id}`)
            .set('Authorization', `Bearer ${token}`); // Added authorization
        expect(userResponse.body.max_score).toBe(150);
    });

    test('should return 403 for a non-existent user', async () => {
        const response = await request(app)
            .put('/api/users/9999/score')
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 100 });

        expect(response.statusCode).toBe(403);
    });

    test('should return 400 for an invalid score', async () => {
        const response = await request(app)
            .put(`/api/users/${testUser.id}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 'a-string-not-a-number' });

        expect(response.statusCode).toBe(400);
    });

    test('should return 403 if a user tries to update another user\'s score', async () => {
        // 1. Create another user for this test
        const otherUserPassword = 'password123';
        const hashedOtherUserPassword = await bcrypt.hash(otherUserPassword, 10);
        const result = await db.run('INSERT INTO users (username, password) VALUES (?, ?)', ['otherUser', hashedOtherUserPassword]);
        const otherUserId = result.lastID;

        // 2. Log in as the primary testUser to get a valid token
        const loginResponse = await request(app)
            .post('/api/auth/login') // Changed from /api/login to /api/auth/login
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        const token = loginResponse.body.token;

        // 3. Use testUser's token to try to update otherUser's score
        const response = await request(app)
            .put(`/api/users/${otherUserId}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 999 });

        // 4. Assert that the request is forbidden
        expect(response.statusCode).toBe(403);
        expect(response.body.error).toBe('Forbidden: You can only update your own score.');

        // 5. Clean up the created user
        // 5. Clean up the created user
        await db.run('DELETE FROM users WHERE id = ?', [otherUserId]);
    });
});

describe('GET /api/users/:id/stats', () => {
    let token;

    beforeEach(async () => {
        // Ensure the test user has some scores
        await db.run('DELETE FROM scores WHERE user_id = ?', [testUser.id]); // Clean up previous scores
        await db.run('INSERT INTO scores (user_id, score, created_at) VALUES (?, ?, ?)', [testUser.id, 50, new Date().toISOString().slice(0, 19).replace('T', ' ')]);
        await db.run('INSERT INTO scores (user_id, score, created_at) VALUES (?, ?, ?)', [testUser.id, 100, new Date().toISOString().slice(0, 19).replace('T', ' ')]);
        
        // Ensure testUser max_score is updated
        await db.run('UPDATE users SET max_score = 100 WHERE id = ?', [testUser.id]);

        const loginResponse = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        token = loginResponse.body.token;
    });

    test('should fetch user stats successfully', async () => {
        const response = await request(app)
            .get(`/api/users/${testUser.id}/stats`)
            .set('Authorization', `Bearer ${token}`);

        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(response.body).toHaveProperty('totalGames');
        expect(response.body).toHaveProperty('averageScore');
        expect(response.body).toHaveProperty('rank');
        expect(response.body).toHaveProperty('rankOutOf');
        expect(response.body).toHaveProperty('recentGames');
        expect(response.body.totalGames).toBe(2);
        expect(response.body.averageScore).toBe(75); // (50+100)/2
        expect(Array.isArray(response.body.recentGames)).toBe(true);
    });

    test('should return 404 for a non-existent user', async () => {
        const response = await request(app)
            .get('/api/users/9999/stats')
            .set('Authorization', `Bearer ${token}`);

        expect(response.statusCode).toBe(404);
        expect(response.body.error).toBe('User not found');
    });

    test('should return 400 for an invalid user ID', async () => {
        const response = await request(app)
            .get('/api/users/abc/stats')
            .set('Authorization', `Bearer ${token}`);

        expect(response.statusCode).toBe(400);
        expect(response.body.error).toBe('Invalid user ID');
    });
});

describe('POST /api/users/:id/achievements', () => {
    let token;
    const TEST_ACHIEVEMENT_NAME = 'First Blood';
    let testAchievementId;

    beforeAll(async () => {
        // Ensure a dummy achievement exists for testing
        let existingAchievement = await db.get('SELECT id FROM achievements WHERE name = ?', [TEST_ACHIEVEMENT_NAME]);
        if (!existingAchievement) {
            await db.run('INSERT INTO achievements (name, description) VALUES (?, ?)', [TEST_ACHIEVEMENT_NAME, 'Unlock your first achievement']);
            existingAchievement = await db.get('SELECT id FROM achievements WHERE name = ?', [TEST_ACHIEVEMENT_NAME]);
        }
        testAchievementId = existingAchievement.id;
    });

    beforeEach(async () => {
        // Clean up user achievements before each test
        await db.run('DELETE FROM user_achievements WHERE user_id = ? AND achievement_id = ?', [testUser.id, testAchievementId]);

        const loginResponse = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        token = loginResponse.body.token;
    });

    afterAll(async () => {
        // Clean up the dummy achievement
        await db.run('DELETE FROM achievements WHERE name = ?', [TEST_ACHIEVEMENT_NAME]);
    });

    test('should unlock achievement successfully', async () => {
        const response = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: TEST_ACHIEVEMENT_NAME });

        expect(response.statusCode).toBe(200);
        expect(response.body.message).toBe('Achievement unlocked successfully');

        // Verify achievement is recorded in DB
        const unlocked = await db.get('SELECT * FROM user_achievements WHERE user_id = ? AND achievement_id = ?', [testUser.id, testAchievementId]);
        expect(unlocked).toBeDefined();
    });

    test('should return 200 if achievement is already unlocked', async () => {
        // Unlock it first
        await db.run('INSERT INTO user_achievements (user_id, achievement_id, unlocked_at) VALUES (?, ?, ?)', [testUser.id, testAchievementId, new Date().toISOString().slice(0, 19).replace('T', ' ')]);

        const response = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: TEST_ACHIEVEMENT_NAME });

        expect(response.statusCode).toBe(200);
        expect(response.body.message).toBe('Achievement already unlocked');
    });

    test('should return 404 for a non-existent achievement', async () => {
        const response = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: 'NonExistentAchievement' });

        expect(response.statusCode).toBe(404);
        expect(response.body.error).toBe('Achievement not found');
    });

    test('should return 400 for missing achievement_name', async () => {
        const response = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({});

        expect(response.statusCode).toBe(400);
        expect(response.body.error).toBe('Achievement name is required');
    });

    test('should return 400 for invalid achievement_name type', async () => {
        const response = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: 123 });

        expect(response.statusCode).toBe(400);
        expect(response.body.error).toBe('Achievement name is required'); // The current controller returns this error
    });

    test('should return 403 if a user tries to unlock an achievement for another user', async () => {
        // 1. Create another user for this test
        const otherUserPassword = 'password123';
        const hashedOtherUserPassword = await bcrypt.hash(otherUserPassword, 10);
        const result = await db.run('INSERT INTO users (username, password) VALUES (?, ?)', ['otherUser2', hashedOtherUserPassword]);
        const otherUserId = result.lastID;

        // 2. Log in as the primary testUser to get a valid token
        const loginResponse = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        const primaryUserToken = loginResponse.body.token;

        // 3. Use testUser's token to try to unlock achievement for otherUser
        const response = await request(app)
            .post(`/api/users/${otherUserId}/achievements`)
            .set('Authorization', `Bearer ${primaryUserToken}`)
            .send({ achievement_name: TEST_ACHIEVEMENT_NAME });

        // 4. Assert that the request is forbidden
        expect(response.statusCode).toBe(403);
        expect(response.body.error).toBe('Forbidden: You can only update your own achievements.');

        // 5. Clean up the created user
        await db.run('DELETE FROM users WHERE id = ?', [otherUserId]);
    });

    test('should return 403 for a non-existent user when unlocking achievement (security: auth check first)', async () => {
        const response = await request(app)
            .post('/api/users/9999/achievements')
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: TEST_ACHIEVEMENT_NAME });

        expect(response.statusCode).toBe(403);
        expect(response.body.error).toBe('Forbidden: You can only update your own achievements.');
    });

    test('should return 400 for an invalid user ID when unlocking achievement', async () => {
        const response = await request(app)
            .post('/api/users/abc/achievements')
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: TEST_ACHIEVEMENT_NAME });

        expect(response.statusCode).toBe(400);
        expect(response.body.error).toBe('Invalid user ID');
    });
});




const request = require('supertest');
const bcrypt = require('bcrypt');
const { app } = require('../server');
const { initDb } = require('../db');
const { stopCleanup } = require('../utils/deviceStore');

const TEST_USERNAME = 'test_achievements'; // a unique user for this test suite
const TEST_PASSWORD = 'Password123!';

let db;
let testUser;
let token;

beforeAll(async () => {
    db = await initDb();

    // Ensure a test user exists
    let existingUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);
    if (!existingUser) {
        const hashedPassword = await bcrypt.hash(TEST_PASSWORD, 10);
        await db.run('INSERT INTO users (username, password) VALUES (?, ?)', [TEST_USERNAME, hashedPassword]);
    }
    testUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);
});

beforeEach(async () => {
    // Clean up achievements for the test user before each test
    await db.run('DELETE FROM user_achievements WHERE user_id = ?', [testUser.id]);

    // Log in to get a fresh token
    const loginResponse = await request(app)
        .post('/api/auth/login') // Use the new auth path
        .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
    token = loginResponse.body.token;
});

afterAll(async () => {
    if (db) {
        await db.run('DELETE FROM users WHERE username = ?', [TEST_USERNAME]);
        await db.close();
    }
    stopCleanup();
});

describe('Achievement Endpoints', () => {
    test('should handle the full achievement lifecycle', async () => {
        // 1. Initial State: Verify all achievements are locked
        const initialGetResponse = await request(app)
            .get(`/api/users/${testUser.id}/achievements`);
        
        expect(initialGetResponse.statusCode).toBe(200);
        initialGetResponse.body.forEach(achievement => {
            expect(achievement.unlocked).toBe(false);
        });

        // 2. Unlock a new achievement
        const achievementToUnlock = 'Boss Slayer';
        const unlockResponse = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: achievementToUnlock });
        
        expect(unlockResponse.statusCode).toBe(200);
        expect(unlockResponse.body.message).toBe('Achievement unlocked successfully');

        // 3. Verify Unlocked State
        const afterUnlockGetResponse = await request(app)
            .get(`/api/users/${testUser.id}/achievements`);
            
        expect(afterUnlockGetResponse.statusCode).toBe(200);
        const unlockedAchievement = afterUnlockGetResponse.body.find(a => a.name === achievementToUnlock);
        expect(unlockedAchievement.unlocked).toBe(true);
        expect(unlockedAchievement.unlocked_at).not.toBeNull();

        // 4. Attempt to re-unlock the same achievement
        const reUnlockResponse = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: achievementToUnlock });

        expect(reUnlockResponse.statusCode).toBe(200);
        expect(reUnlockResponse.body.message).toBe('Achievement already unlocked');

        // 5. Attempt to unlock a non-existent achievement
        const nonExistentResponse = await request(app)
            .post(`/api/users/${testUser.id}/achievements`)
            .set('Authorization', `Bearer ${token}`)
            .send({ achievement_name: 'This achievement does not exist' });
        
        expect(nonExistentResponse.statusCode).toBe(404);
        expect(nonExistentResponse.body.error).toBe('Achievement not found');
    });
});

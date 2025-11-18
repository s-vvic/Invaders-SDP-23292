const request = require('supertest');
const { app, startServer } = require('../server');

let db;
let testUser;

beforeAll(async () => {
    db = await startServer();
    testUser = await db.get("SELECT * FROM users WHERE username = 'test'");
});

afterAll(async () => {
    await db.close();
});

describe('User API Endpoints', () => {

    test('GET /api/users - should fetch all users successfully', async () => {
        const response = await request(app).get('/api/users');
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
        const response = await request(app).get(`/api/users/${testUser.id}`);
        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(response.body.id).toBe(testUser.id);
        expect(response.body.username).toBe('test');
        expect(response.body).not.toHaveProperty('password');
    });

    test('GET /api/users/:id - should return 404 for a non-existent user', async () => {
        const response = await request(app).get('/api/users/9999');
        expect(response.statusCode).toBe(404);
        expect(response.body.error).toBe('User not found');
    });

    test('GET /api/users/:id - should return 400 for an invalid user ID', async () => {
        const response = await request(app).get('/api/users/abc');
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
            .post('/api/login')
            .send({ username: 'test', password: '1234' });
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
        const userResponse = await request(app).get(`/api/users/${testUser.id}`);
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
        const userResponse = await request(app).get(`/api/users/${testUser.id}`);
        expect(userResponse.body.max_score).toBe(150);
    });

    test('should return 404 for a non-existent user', async () => {
        const response = await request(app)
            .put('/api/users/9999/score')
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 100 });

        expect(response.statusCode).toBe(404);
    });

    test('should return 400 for an invalid score', async () => {
        const response = await request(app)
            .put(`/api/users/${testUser.id}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 'a-string-not-a-number' });

        expect(response.statusCode).toBe(400);
    });
});

describe('Authentication Middleware', () => {

    test('should return 401 if no token is provided', async () => {
        const protectedUrl = `/api/users/${testUser.id}/score`;
        const response = await request(app)
            .put(protectedUrl)
            .send({ score: 50 });
        
        expect(response.statusCode).toBe(401);
        expect(response.body.error).toBe('Unauthorized: No token provided');
    });

    test('should return 401 if token is invalid', async () => {
        const protectedUrl = `/api/users/${testUser.id}/score`;
        const response = await request(app)
            .put(protectedUrl)
            .set('Authorization', 'Bearer FAKE_INVALID_TOKEN')
            .send({ score: 50 });

        expect(response.statusCode).toBe(401);
        expect(response.body.error).toBe('Unauthorized: Invalid token');
    });

    test('should return 200 if token is valid', async () => {
        const protectedUrl = `/api/users/${testUser.id}/score`;
        // 1. Log in to get a valid token
        const loginResponse = await request(app)
            .post('/api/login')
            .send({ username: 'test', password: '1234' });
        
        const token = loginResponse.body.token;
        expect(token).toBeDefined();

        // 2. Use the valid token to access the protected route
        const response = await request(app)
            .put(protectedUrl)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 50 });

        expect(response.statusCode).toBe(200);
        expect(response.body.message).toBeDefined();
    });
});
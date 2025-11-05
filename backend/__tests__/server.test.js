const request = require('supertest');
const { app, startServer } = require('../server');

beforeAll(async () => {
    await startServer();
});

describe('User API Endpoints', () => {

    test('GET /api/users - should fetch all users successfully', async () => {
        const response = await request(app).get('/api/users');
        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(Array.isArray(response.body)).toBe(true);
        // Check for at least one user, since the DB is seeded with a test user
        expect(response.body.length).toBeGreaterThan(0);
        expect(response.body[0]).toHaveProperty('id');
        expect(response.body[0]).toHaveProperty('username');
        expect(response.body[0]).toHaveProperty('max_score');
        expect(response.body[0]).not.toHaveProperty('password'); // Ensure password is not exposed
    });

    test('GET /api/users/:id - should fetch a single user successfully', async () => {
        const response = await request(app).get('/api/users/1');
        expect(response.statusCode).toBe(200);
        expect(response.headers['content-type']).toMatch(/json/);
        expect(response.body.id).toBe(1);
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

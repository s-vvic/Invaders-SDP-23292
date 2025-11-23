const request = require('supertest');
const bcrypt = require('bcrypt');
const { app } = require('../server');
const { initDb } = require('../db');

// These are needed for the tests
const TEST_USERNAME = 'test_auth'; // Use a unique username to avoid conflicts
const TEST_PASSWORD = 'Password123!';

let db;
let testUser;

beforeAll(async () => {
    db = await initDb();

    // This setup is from the original server.test.js
    let existingUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);

    if (!existingUser) {
        const hashedPassword = await bcrypt.hash(TEST_PASSWORD, 10);
        await db.run(
            'INSERT INTO users (username, password) VALUES (?, ?)',
            [TEST_USERNAME, hashedPassword]
        );
        existingUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);
    } else {
        const passwordMatches = await bcrypt.compare(TEST_PASSWORD, existingUser.password);
        if (!passwordMatches) {
            const hashedPassword = await bcrypt.hash(TEST_PASSWORD, 10);
            await db.run('UPDATE users SET password = ? WHERE id = ?', [hashedPassword, existingUser.id]);
        }
    }

    testUser = existingUser;
});

afterAll(async () => {
    // It's important to close the database connection after tests
    // to prevent jest from hanging.
    if (db) {
        await db.run('DELETE FROM users WHERE username = ?', [TEST_USERNAME]);
        await db.close();
    }
});


describe('Authentication API & Middleware', () => {

    test('POST /api/auth/login - should login successfully with correct credentials', async () => {
        const response = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });

        expect(response.statusCode).toBe(200);
        expect(response.body).toHaveProperty('token');
        expect(response.body.user.username).toBe(TEST_USERNAME);
    });
    
    test('POST /api/auth/login - should fail with incorrect credentials', async () => {
        const response = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: 'wrongpassword' });

        expect(response.statusCode).toBe(401);
        expect(response.body.error).toBe('Invalid username or password');
    });

    test('Middleware (authMiddleware) - should return 401 if no token is provided', async () => {
        const protectedUrl = `/api/users/${testUser.id}/score`;
        const response = await request(app)
            .put(protectedUrl)
            .send({ score: 50 });
        
        expect(response.statusCode).toBe(401);
        expect(response.body.error).toBe('Unauthorized: No token provided');
    });

    test('Middleware (authMiddleware) - should return 403 if token is invalid', async () => {
        const protectedUrl = `/api/users/${testUser.id}/score`;
        const response = await request(app)
            .put(protectedUrl)
            .set('Authorization', 'Bearer FAKE_INVALID_TOKEN')
            .send({ score: 50 });

        expect(response.statusCode).toBe(403);
        expect(response.body.error).toBe('Forbidden: Invalid or expired token');
    });
    
    test('Middleware (authenticateToken) - should return 401 if token is invalid', async () => {
        const protectedUrl = `/api/users`;
        const response = await request(app)
            .get(protectedUrl)
            .set('Authorization', 'Bearer FAKE_INVALID_TOKEN');

        expect(response.statusCode).toBe(401);
        expect(response.body.error).toContain('Invalid token');
    });

    test('Middleware - should grant access to a protected route if token is valid', async () => {
        // 1. Log in to get a valid token
        const loginResponse = await request(app)
            .post('/api/auth/login')
            .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
        
        const token = loginResponse.body.token;
        expect(token).toBeDefined();

        // 2. Use the valid token to access a route protected by 'authenticateToken'
        const getResponse = await request(app)
            .get('/api/users')
            .set('Authorization', `Bearer ${token}`);

        expect(getResponse.statusCode).toBe(200);

        // 3. Use the valid token to access a route protected by 'authMiddleware'
        const putResponse = await request(app)
            .put(`/api/users/${testUser.id}/score`)
            .set('Authorization', `Bearer ${token}`)
            .send({ score: 50 });

        expect(putResponse.statusCode).toBe(200);
        expect(putResponse.body.message).toBeDefined();
    });
});

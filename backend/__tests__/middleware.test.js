process.env.ALLOWED_ORIGINS = 'https://example.com';

const request = require('supertest');
const { app, startServer, db: serverDb } = require('../server');
const rateLimit = require('express-rate-limit');

describe('Security Middleware Tests', () => {
    let db;
    let testUser;

    beforeAll(async () => {
        db = await startServer();
        testUser = await db.get("SELECT * FROM users WHERE username = 'test'");
    });

    afterAll(async () => {
        await db.close();
    });

    describe('Helmet', () => {
        test('should set security headers', async () => {
            const response = await request(app).get('/api/users');
            // Helmet sets many headers, we'll just check for one common one.
            expect(response.headers['x-content-type-options']).toBe('nosniff');
        });
    });

    describe('CORS', () => {
        test('should echo back the allowed origin', async () => {
            const response = await request(app)
                .get('/api/users')
                .set('Origin', 'https://example.com');
            
            expect(response.headers['access-control-allow-origin']).toBe('https://example.com');
        });
    });

    describe('Rate Limiting', () => {
        // This test needs a higher timeout because it's making multiple requests
        jest.setTimeout(10000);

        // Create a separate, very strict limiter just for this test route
        const testLimiter = rateLimit({
            windowMs: 1 * 60 * 1000, // 1 minute
            max: 2, // limit to 2 requests per window
            standardHeaders: true,
            legacyHeaders: false,
        });

        // Apply this limiter to a test-only route
        app.get('/test-rate-limit', testLimiter, (req, res) => {
            res.status(200).send('OK');
        });

        test('should block requests after exceeding the rate limit', async () => {
            // Make 2 successful requests
            await request(app).get('/test-rate-limit').expect(200);
            await request(app).get('/test-rate-limit').expect(200);

            // The 3rd request should be blocked
            const response = await request(app).get('/test-rate-limit');
            expect(response.statusCode).toBe(429);
            expect(response.text).toContain('Too many requests, please try again later.');
        });
    });
});

const request = require('supertest');
const { app } = require('../server');
const { initDb } = require('../db');
const bcrypt = require('bcrypt');
const { stopCleanup } = require('../utils/deviceStore'); // Import the cleanup function

const TEST_USERNAME = 'test_flow_user';
const TEST_PASSWORD = 'Password123!';

let db;
let testUser;

beforeAll(async () => {
    db = await initDb();
    // Ensure the test user exists for login-related flow tests
    let existingUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);
    if (!existingUser) {
        const hashedPassword = await bcrypt.hash(TEST_PASSWORD, 10);
        await db.run('INSERT INTO users (username, password) VALUES (?, ?)', [TEST_USERNAME, hashedPassword]);
    }
    testUser = await db.get('SELECT * FROM users WHERE username = ?', [TEST_USERNAME]);
});

afterAll(async () => {
    if (db) {
        await db.run('DELETE FROM users WHERE username = ?', [TEST_USERNAME]);
        await db.close();
    }
    stopCleanup(); // Stop the interval timer to allow Jest to exit gracefully
});

describe('Authentication Flows API', () => {

    describe('Device Authentication Flow', () => {

        test('should complete the full device login flow successfully', async () => {
            // 1. Game client initiates the device login flow
            const initiateResponse = await request(app)
                .post('/api/auth/device/initiate')
                .send();

            expect(initiateResponse.statusCode).toBe(200);
            expect(initiateResponse.body).toHaveProperty('deviceCode');
            expect(initiateResponse.body).toHaveProperty('userCode');
            expect(initiateResponse.body).toHaveProperty('verificationUri');

            const { deviceCode, userCode } = initiateResponse.body;

            // 2. Game client polls before user has logged in on the web
            const pendingResponse = await request(app)
                .post('/api/auth/device/token')
                .send({ deviceCode });
            
            expect(pendingResponse.statusCode).toBe(202); // 202 Accepted indicates pending
            expect(pendingResponse.body.status).toBe('pending');

            // 3. User logs in on the web using the userCode
            const webLoginResponse = await request(app)
                .post('/api/auth/device/login')
                .send({
                    userCode: userCode,
                    username: TEST_USERNAME,
                    password: TEST_PASSWORD
                });

            expect(webLoginResponse.statusCode).toBe(200);
            expect(webLoginResponse.body.message).toContain('Device connected successfully');

            // 4. Game client polls again and should now receive the token
            const tokenResponse = await request(app)
                .post('/api/auth/device/token')
                .send({ deviceCode });

            expect(tokenResponse.statusCode).toBe(200);
            expect(tokenResponse.body).toHaveProperty('token');
            expect(tokenResponse.body.user.id).toBe(testUser.id);
            expect(tokenResponse.body.user.username).toBe(TEST_USERNAME);
            expect(tokenResponse.body.status).toBe('completed');
        });

        test('should connect a device for an already authenticated user', async () => {
            // 1. Log in on the web to get a valid token
            const loginRes = await request(app)
                .post('/api/auth/login')
                .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
            
            expect(loginRes.statusCode).toBe(200);
            const userToken = loginRes.body.token;

            // 2. Game client initiates the device login flow
            const initiateResponse = await request(app)
                .post('/api/auth/device/initiate')
                .send();
            
            expect(initiateResponse.statusCode).toBe(200);
            const { deviceCode, userCode } = initiateResponse.body;

            // 3. Authenticated user "connects" the device using the code
            const connectResponse = await request(app)
                .post('/api/auth/device/connect')
                .set('Authorization', `Bearer ${userToken}`)
                .send({ userCode });
            
            expect(connectResponse.statusCode).toBe(200);
            expect(connectResponse.body.message).toBe('Device connected successfully!');

            // 4. Game client polls and receives the token
            const tokenResponse = await request(app)
                .post('/api/auth/device/token')
                .send({ deviceCode });

            expect(tokenResponse.statusCode).toBe(200);
            expect(tokenResponse.body.token).toBe(userToken); // Should be the same token
            expect(tokenResponse.body.user.id).toBe(testUser.id);
        });

    });

    describe('Session Confirmation Flow', () => {

        test('should complete the full session confirmation flow successfully', async () => {
            // 1. Log in to get a valid token (simulating an existing game session)
            const loginRes = await request(app)
                .post('/api/auth/login')
                .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
            
            expect(loginRes.statusCode).toBe(200);
            const userToken = loginRes.body.token;

            // 2. Game client initiates session confirmation
            const initiateResponse = await request(app)
                .post('/api/auth/session/initiate')
                .set('Authorization', `Bearer ${userToken}`)
                .send();
            
            expect(initiateResponse.statusCode).toBe(200);
            expect(initiateResponse.body).toHaveProperty('confirmationCode');
            const { confirmationCode } = initiateResponse.body;

            // 3. Game client polls before confirmation, should be 'pending'
            const pendingResponse = await request(app)
                .post('/api/auth/session/status')
                .send({ confirmationCode });

            expect(pendingResponse.statusCode).toBe(200);
            expect(pendingResponse.body.status).toBe('pending');
            
            // 4. Web page, loaded with the same user session, confirms
            const confirmResponse = await request(app)
                .post('/api/auth/session/confirm')
                .set('Authorization', `Bearer ${userToken}`)
                .send({ confirmationCode });
            
            expect(confirmResponse.statusCode).toBe(200);
            expect(confirmResponse.body.message).toBe('Session confirmed successfully!');

            // 5. Game client polls again and should now get a 'confirmed' status
            const confirmedResponse = await request(app)
                .post('/api/auth/session/status')
                .send({ confirmationCode });

            expect(confirmedResponse.statusCode).toBe(200);
            expect(confirmedResponse.body.status).toBe('confirmed');
            expect(confirmedResponse.body.username).toBe(TEST_USERNAME);
        });

        test('should allow a user to cancel the session confirmation flow', async () => {
            // 1. Log in to get a valid token
            const loginRes = await request(app)
                .post('/api/auth/login')
                .send({ username: TEST_USERNAME, password: TEST_PASSWORD });
            const userToken = loginRes.body.token;

            // 2. Game client initiates session confirmation
            const initiateResponse = await request(app)
                .post('/api/auth/session/initiate')
                .set('Authorization', `Bearer ${userToken}`)
                .send();
            const { confirmationCode } = initiateResponse.body;

            // 3. User on the web decides to cancel/logout
            const cancelResponse = await request(app)
                .post('/api/auth/session/cancel')
                .set('Authorization', `Bearer ${userToken}`)
                .send({ confirmationCode });

            expect(cancelResponse.statusCode).toBe(200);
            expect(cancelResponse.body.message).toBe('Session cancelled successfully!');

            // 4. Game client polls and should get a 'cancelled' status
            const cancelledResponse = await request(app)
                .post('/api/auth/session/status')
                .send({ confirmationCode });
            
            expect(cancelledResponse.statusCode).toBe(200);
            expect(cancelledResponse.body.status).toBe('cancelled');
        });

    });

});

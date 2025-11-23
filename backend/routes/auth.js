const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { authLimiter } = require('../middleware/rateLimiter');
const { authMiddleware } = require('../middleware/auth'); // Import the auth middleware

/**
 * @swagger
 * tags:
 *   name: Auth
 *   description: User authentication and registration
 */

/**
 * @swagger
 * /api/auth/login:
 *   post:
 *     summary: Authenticate a user and return a token
 *     tags: [Auth]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               username:
 *                 type: string
 *               password:
 *                 type: string
 *     responses:
 *       200:
 *         description: Login successful
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 token:
 *                   type: string
 *       401:
 *         description: Invalid username or password
 *       500:
 *         description: Server database error
 */
router.post('/login', authLimiter, authController.login);

/**
 * @swagger
 * /api/auth/register:
 *   post:
 *     summary: Register a new user
 *     tags: [Auth]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               username:
 *                 type: string
 *               password:
 *                 type: string
 *     responses:
 *       201:
 *         description: User registered successfully
 *       400:
 *         description: Username already taken or bad request
 *       500:
 *         description: Server database error
 */
router.post('/register', authLimiter, authController.register);

/* --- New Device Flow Routes --- */

/**
 * @swagger
 * /api/auth/device/initiate:
 *   post:
 *     summary: Initiate device authentication flow
 *     tags: [Auth]
 *     responses:
 *       200:
 *         description: Device code generated successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 deviceCode:
 *                   type: string
 *                 userCode:
 *                   type: string
 *                 verificationUri:
 *                   type: string
 *                 expiresIn:
 *                   type: number
 *                 interval:
 *                   type: number
 *       500:
 *         description: Server error
 */
router.post('/device/initiate', authController.initiateDeviceAuth);

/**
 * @swagger
 * /api/auth/device/token:
 *   post:
 *     summary: Poll for device authentication token
 *     tags: [Auth]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               deviceCode:
 *                 type: string
 *     responses:
 *       200:
 *         description: Authentication successful, token returned
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 token:
 *                   type: string
 *                 user:
 *                   type: object
 *       202:
 *         description: Authorization pending
 *       404:
 *         description: Device code not found
 *       410:
 *         description: Device code expired
 *       500:
 *         description: Server error
 */
router.post('/device/token', authController.getDeviceToken);

/**
 * @swagger
 * /api/auth/device/login:
 *   post:
 *     summary: Login via device code (for unauthenticated web users)
 *     tags: [Auth]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               userCode:
 *                 type: string
 *               username:
 *                 type: string
 *               password:
 *                 type: string
 *     responses:
 *       200:
 *         description: Device connected successfully
 *       400:
 *         description: Invalid or expired device code, or bad credentials
 *       401:
 *         description: Invalid username or password
 *       500:
 *         description: Server error
 */
router.post('/device/login', authLimiter, authController.loginWithDevice);

/**
 * @swagger
 * /api/auth/device/connect:
 *   post:
 *     summary: Connect device via code (for authenticated web users)
 *     tags: [Auth]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               userCode:
 *                 type: string
 *     responses:
 *       200:
 *         description: Device connected successfully
 *       400:
 *         description: Invalid or expired device code
 *       401:
 *         description: Unauthorized (invalid/expired JWT)
 *       500:
 *         description: Server error
 */
router.post('/device/connect', authMiddleware, authController.connectDevice);

/* --- New Session Confirmation Flow Routes --- */

/**
 * @swagger
 * /api/auth/session/initiate:
 *   post:
 *     summary: Initiate session confirmation flow for an already logged-in game client.
 *     tags: [Auth]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Session confirmation initiated successfully.
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 confirmationCode:
 *                   type: string
 *                 expiresIn:
 *                   type: number
 *                 interval:
 *                   type: number
 *                 confirmationUri:
 *                   type: string
 *       401:
 *         description: Unauthorized (invalid/expired JWT from game client).
 *       500:
 *         description: Server error.
 */
router.post('/session/initiate', authMiddleware, authController.initiateSessionConfirmation);

/**
 * @swagger
 * /api/auth/session/confirm:
 *   post:
 *     summary: Confirm the session via a web browser.
 *     tags: [Auth]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               confirmationCode:
 *                 type: string
 *     responses:
 *       200:
 *         description: Session confirmed successfully.
 *       400:
 *         description: Invalid or mismatched confirmation code.
 *       401:
 *         description: Unauthorized (invalid/expired JWT from web browser).
 *       410:
 *         description: Confirmation code expired.
 *       500:
 *         description: Server error.
 */
router.post('/session/confirm', authMiddleware, authController.confirmSession);

/**
 * @swagger
 * /api/auth/session/status:
 *   post:
 *     summary: Poll for the status of a session confirmation.
 *     tags: [Auth]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               confirmationCode:
 *                 type: string
 *     responses:
 *       200:
 *         description: Returns the status of the confirmation code ('pending', 'confirmed', 'cancelled').
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                   enum: ['pending', 'confirmed', 'cancelled']
 *                 username:
 *                   type: string
 *       404:
 *         description: Confirmation code not found or already used.
 *       410:
 *         description: Confirmation code expired.
 *       500:
 *         description: Server error.
 */
router.post('/session/status', authController.getSessionStatus);

/**
 * @swagger
 * /api/auth/session/cancel:
 *   post:
 *     summary: Cancel a session confirmation.
 *     tags: [Auth]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               confirmationCode:
 *                 type: string
 *     responses:
 *       200:
 *         description: Session cancelled successfully.
 *       400:
 *         description: Invalid or mismatched confirmation code.
 *       401:
 *         description: Unauthorized (invalid/expired JWT from web browser).
 *       500:
 *         description: Server error.
 */
router.post('/session/cancel', authMiddleware, authController.cancelSession);

module.exports = router;


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

module.exports = router;


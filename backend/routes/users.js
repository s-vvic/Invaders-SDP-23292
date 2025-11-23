const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { authenticateToken, authMiddleware } = require('../middleware/auth');

/**
 * @swagger
 * tags:
 *   name: Users
 *   description: User management, stats, and scores
 */

/**
 * @swagger
 * /api/users:
 *   get:
 *     summary: Retrieve a list of all users
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: A list of users
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: integer
 *                   username:
 *                     type: string
 *                   max_score:
 *                     type: integer
 *       500:
 *         description: Server database error
 */
router.get('/', authenticateToken, userController.getAllUsers);

/**
 * @swagger
 * /api/users/{id}:
 *   get:
 *     summary: Retrieve a single user by ID
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     responses:
 *       200:
 *         description: A single user object
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 id:
 *                   type: integer
 *                 username:
 *                   type: string
 *                 max_score:
 *                     type: integer
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
router.get('/:id', authenticateToken, userController.getUserById);

/**
 * @swagger
 * /api/users/{id}/stats:
 *   get:
 *     summary: Retrieve user statistics (total games, average score, rank, recent games)
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     responses:
 *       200:
 *         description: User statistics
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
router.get('/:id/stats', userController.getUserStats);

/**
 * @swagger
 * /api/users/{id}/achievements:
 *   get:
 *     summary: Retrieve user's achievements
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     responses:
 *       200:
 *         description: User achievements
 *       400:
 *         description: Invalid user ID
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
router.get('/:id/achievements', userController.getUserAchievements);

/**
 * @swagger
 * /api/users/{id}/achievements:
 *   post:
 *     summary: Unlock an achievement for a user
 *     security:
 *       - bearerAuth: []
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               achievement_name:
 *                 type: string
 *     responses:
 *       200:
 *         description: Achievement unlocked successfully
 *       401:
 *         description: Unauthorized - No token provided
 *       403:
 *         description: Forbidden - Invalid token or trying to update another user's achievements
 *       404:
 *         description: User or achievement not found
 *       500:
 *         description: Server database error
 */
router.post('/:id/achievements', authMiddleware, userController.unlockAchievement);

/**
 * @swagger
 * /api/users/{id}/score:
 *   put:
 *     summary: Update a user's high score
 *     security:
 *       - bearerAuth: []
 *     tags: [Users]
 *     parameters:
 *       - in: path
 *         name: id
 *         schema:
 *           type: integer
 *         required: true
 *         description: The user ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               score:
 *                 type: integer
 *                 description: The new score to check against the high score
 *     responses:
 *       200:
 *         description: Score checked or updated successfully
 *       401:
 *         description: Unauthorized - No token provided
 *       403:
 *         description: Forbidden - Invalid token or trying to update another user's score
 *       404:
 *         description: User not found
 *       500:
 *         description: Server database error
 */
router.put('/:id/score', authMiddleware, userController.updateScore);

module.exports = router;

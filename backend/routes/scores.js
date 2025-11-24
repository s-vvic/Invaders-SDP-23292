const express = require('express');
const router = express.Router();
const scoreController = require('../controllers/scoreController');
const { requireAuth } = require('../middleware/auth');

/**
 * @swagger
 * tags:
 *   name: Scores
 *   description: High score leaderboards
 */

/**
 * @swagger
 * /api/scores:
 *   get:
 *     summary: Retrieve a list of all scores (all-time)
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of scores with usernames, ordered by score descending
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   username:
 *                     type: string
 *                   score:
 *                     type: integer
 *                   created_at:
 *                     type: string
 *       500:
 *         description: Server database error
 */
router.get('/', scoreController.getAllScores);

/**
 * @swagger
 * /api/scores/weekly:
 *   get:
 *     summary: Retrieve weekly high scores (last 7 days)
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of weekly scores with usernames, ordered by score descending
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   username:
 *                     type: string
 *                   score:
 *                     type: integer
 *                   created_at:
 *                     type: string
 *       500:
 *         description: Server database error
 */
router.get('/weekly', scoreController.getWeeklyScores);

/**
 * @swagger
 * /api/scores/yearly:
 *   get:
 *     summary: Retrieve yearly high scores (last 365 days)
 *     tags: [Scores]
 *     responses:
 *       200:
 *         description: A list of yearly scores with usernames, ordered by score descending
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   username:
 *                     type: string
 *                   score:
 *                     type: integer
 *                   created_at:
 *                     type: string
 *       500:
 *         description: Server database error
 */
router.get('/yearly', scoreController.getYearlyScores);

module.exports = router;


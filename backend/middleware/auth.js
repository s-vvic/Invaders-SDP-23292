const jwt = require('jsonwebtoken');

/**
 * Middleware to verify JWT token and attach user to the request.
 * This is the single, consistent authentication middleware for the application.
 */
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Unauthorized: No token provided' });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;
        next();
    } catch (error) {
        // More specific error message based on the type of JWT error
        let errorMessage = 'Unauthorized: Invalid token';
        if (error.name === 'TokenExpiredError') {
            errorMessage = 'Unauthorized: Token has expired';
        }
        // Use 401 for all authentication-related failures
        return res.status(401).json({ error: errorMessage });
    }
};

module.exports = {
    requireAuth,
};


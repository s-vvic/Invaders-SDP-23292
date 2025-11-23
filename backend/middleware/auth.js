const jwt = require('jsonwebtoken');

function authenticateToken(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (token == null) {
        return res.status(401).json({ error: 'Access token is required' });
    }

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) {
            console.log('JWT verification failed:', err.message);

            let errorMessage = 'Invalid token. Please log in again.';
            if (err.name === 'TokenExpiredError') {
                errorMessage = 'Token expired. Please log in again.';
            }
            
            return res.status(401).json({ error: errorMessage });
        }

        req.user = user;
        next();
    });
}

const authMiddleware = (req, res, next) => {
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
        return res.status(403).json({ error: 'Forbidden: Invalid or expired token' });
    }
};

module.exports = {
    authenticateToken,
    authMiddleware,
};

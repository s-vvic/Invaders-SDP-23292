const rateLimit = require('express-rate-limit');

// Conditional export for test environment
if (process.env.NODE_ENV === 'test') {
    module.exports = {
        limiter: (req, res, next) => next(), // No-op middleware for tests
        authLimiter: (req, res, next) => next(), // No-op middleware for tests
    };
} else {
    // General purpose rate limiter
    const limiter = rateLimit({
        windowMs: 15 * 60 * 1000, // 15 minutes
        max: 100, // Limit each IP to 100 requests per window
        standardHeaders: true,
        legacyHeaders: false,
    });

    // Stricter rate limiter for authentication routes
    const authLimiter = rateLimit({
        windowMs: 15 * 60 * 1000, // 15 minutes
        max: 10, // Limit each IP to 10 attempts per window
        standardHeaders: true,
        legacyHeaders: false,
        message: { error: 'Too many attempts, please try again later.' },
    });

    module.exports = {
        limiter,
        authLimiter,
    };
}

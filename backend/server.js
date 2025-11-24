const fs = require('fs');
const express = require("express");
const path = require("path");
const sqlite3 = require('sqlite3');
const { open } = require('sqlite');
const bcrypt = require('bcrypt');
const helmet = require('helmet');
const hpp = require('hpp');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
const jwt = require('jsonwebtoken');
const { validateCredentials } = require('./utils/validators');
const { authenticateToken, authMiddleware } = require('./middleware/auth');
const { limiter, authLimiter } = require('./middleware/rateLimiter');
const { initDb } = require('./db');
const authRouter = require('./routes/auth');
const userRouter = require('./routes/users');
const scoreRouter = require('./routes/scores');

// .env file setup
const envPath = path.join(__dirname, '.env');
const envExamplePath = path.join(__dirname, '.env.example');

if (!fs.existsSync(envPath)) {
    if (fs.existsSync(envExamplePath)) {
        fs.copyFileSync(envExamplePath, envPath);
        console.log('NOTE: .env file not found. A new .env file has been created by copying .env.example. Please review it.');
    } else {
        console.warn('WARNING: .env file not found and .env.example is also missing. Cannot auto-create .env.');
    }
}

require('dotenv').config({ path: envPath });

const REQUIRED_ENV_VARS = ['JWT_SECRET'];
const missingEnv = REQUIRED_ENV_VARS.filter((key) => !process.env[key]);

if (missingEnv.length > 0) {
    const errorMsg = `
        Missing required environment variables: ${missingEnv.join(', ')}
        Please ensure you have a .env file in the /backend directory with all the required variables.
        You can use the .env.example file as a template.
    `;
    throw new Error(errorMsg);
}

const app = express();
app.set('trust proxy', 1); // Add this line to trust the proxy
app.use(helmet({
    contentSecurityPolicy: {
        directives: {
            defaultSrc: ["'self'"],
            scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"], // Allow eval()
            styleSrc: ["'self'", "'unsafe-inline'"],  // Allow inline styles for now
            imgSrc: ["'self'", "data:"],
            connectSrc: ["'self'", "http://localhost:8080", "ws://localhost:8080"], // Allow connections to the backend API and websockets
        },
    },
}));
app.use(hpp());

const isTestEnv = process.env.NODE_ENV === 'test';

const defaultOrigins = [
    'http://localhost:3000',
    'http://localhost:4173',
    'http://localhost:5173',
    'http://localhost:8080',
    'http://localhost:8080/',
    'https://cleveland-unbrittle-pseudoaesthetically.ngrok-free.dev'
];

const configuredOrigins = (process.env.ALLOWED_ORIGINS || '')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);

const allowedOrigins = configuredOrigins.length ? configuredOrigins : defaultOrigins;

app.use(cors({
    origin: (origin, callback) => {
        if (isTestEnv || !origin) {
            return callback(null, true);
        }

        if (allowedOrigins.includes(origin)) {
            return callback(null, true);
        }
        console.log('요청 들어온 주소(Origin):', origin);
        return callback(new Error('Not allowed by CORS'));
        
    },
    optionsSuccessStatus: 200,
}));
app.use(limiter);





app.use(morgan('dev'));




app.use(express.json({ limit: '10kb' }));
app.use(express.urlencoded({ extended: false, limit: '10kb' }));

app.use((req, res, next) => {
    if (req.url.endsWith('.css')) {
        res.setHeader('Content-Type', 'text/css');
    }
    if (req.url.endsWith('.ttf')) {
        res.setHeader('Content-Type', 'font/ttf');
    }
    next();
});

const publicPath = path.join(__dirname, '../frontend');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});

app.use('/api/auth', authRouter);
app.use('/api/users', userRouter);
app.use('/api/scores', scoreRouter);


// Load and use Swagger only if not in a test environment
if (process.env.NODE_ENV !== 'test') {
    const swaggerUi = require('swagger-ui-express');
    const specs = require('./config/swagger.js');
    app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs));
}

// ==================================
// 전역 오류 처리 미들웨어 (가장 마지막에 위치해야 함)
// ==================================
app.use((err, req, res, next) => {
    // 1. 서버 콘솔에 상세한 오류를 로깅합니다 (개발자 확인용).
    console.error(err.stack);

    // 2. 이미 응답이 전송된 경우, Express의 기본 오류 처리기에 위임합니다.
    if (res.headersSent) {
        return next(err);
    }

    if (err.message === 'Not allowed by CORS') {
        return res.status(403).json({ error: 'Origin not allowed' });
    }

    // 3. 사용자에게는 일관된 일반 오류 메시지를 보냅니다.
    res.status(500).json({ error: 'Internal Server Error' });
});

if (require.main === module) {
    initDb().then(() => {
                    const PORT = process.env.PORT || 8080;
                    app.listen(PORT, () => {
                        console.log(`Server listening on port ${PORT}`);
                    });
    });
}

module.exports = { app };
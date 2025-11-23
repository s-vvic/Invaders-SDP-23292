const crypto = require('crypto');

/**
 * @typedef {Object} DeviceCodeInfo
 * @property {string} userCode - The short, user-friendly code for the user to type.
 * @property {number} expiresAt - Timestamp when the code expires.
 * @property {'pending' | 'completed'} status - Current status of the code.
 * @property {number} interval - Recommended client polling interval (ms).
 * @property {string} [token] - JWT stored after successful login.
 * @property {{ id: number, username: string }} [user] - User info stored after successful login.
 */

/**
 * In-memory store for device codes.
 * @type {Map<string, DeviceCodeInfo>}
 */
const deviceCodes = new Map();

// Default expiration time for a device code (5 minutes)
const EXPIRATION_MS = 5 * 60 * 1000; 
// Default recommended client polling interval (5 seconds)
const POLLING_INTERVAL_MS = 5000;

/**
 * Generates a new device code and adds it to the store.
 * @returns {{deviceCode: string, userCode: string, expiresIn: number, interval: number}}
 */
function generateDeviceCode() {
    // Generate a cryptographically secure random string for the internal deviceCode
    const deviceCode = crypto.randomBytes(16).toString('hex');
    // Generate a shorter, user-friendly 6-character alphanumeric code (e.g., "ABC-123")
    const userCode = crypto.randomBytes(3).toString('hex').toUpperCase().match(/.{1,3}/g).join('-'); // Format to ABC-123

    const codeInfo = {
        userCode: userCode,
        expiresAt: Date.now() + EXPIRATION_MS,
        status: 'pending',
        interval: POLLING_INTERVAL_MS,
    };

    deviceCodes.set(deviceCode, codeInfo);

    return {
        deviceCode: deviceCode, // The internal code for the game client to use for polling
        userCode: userCode,       // The code for the user to type on the web page
        expiresIn: Math.round(EXPIRATION_MS / 1000), // Expiration time in seconds
        interval: POLLING_INTERVAL_MS,             // Polling interval in milliseconds
    };
}

/**
 * Retrieves code information from the store using the internal deviceCode.
 * @param {string} deviceCode - The internal deviceCode.
 * @returns {DeviceCodeInfo | undefined}
 */
function getCodeInfo(deviceCode) {
    return deviceCodes.get(deviceCode);
}

/**
 * Finds a device code entry by its userCode.
 * @param {string} userCode - The user-friendly code.
 * @returns {{deviceCode: string, info: DeviceCodeInfo} | null} - Returns the internal deviceCode and its info, or null if not found/expired.
 */
function findByUserCode(userCode) {
    const now = Date.now();
    for (const [deviceCode, info] of deviceCodes.entries()) {
        if (info.userCode === userCode && info.expiresAt > now) {
            return { deviceCode, info };
        }
    }
    return null;
}

/**
 * Completes a device code entry with the user's authentication details.
 * @param {string} deviceCode - The internal deviceCode.
 * @param {{token: string, user: {id: number, username: string}}} data - The JWT and user object.
 */
function completeCode(deviceCode, data) {
    const info = deviceCodes.get(deviceCode);
    if (info && info.status === 'pending') {
        info.status = 'completed';
        info.token = data.token;
        info.user = data.user;
        deviceCodes.set(deviceCode, info); // Update the map
    }
}

/**
 * Cleans up expired or completed device codes from the store periodically.
 */
setInterval(() => {
    const now = Date.now();
    for (const [deviceCode, info] of deviceCodes.entries()) {
        // Remove if expired OR if completed and a short grace period has passed (optional, for safety)
        if (info.expiresAt < now || (info.status === 'completed' && info.expiresAt < now + 5000)) { // Give it 5s after expiry if completed
            deviceCodes.delete(deviceCode);
        }
    }
    console.log(`Device code store cleanup. Current active codes: ${deviceCodes.size}`);
}, 60 * 1000); // Run every minute

module.exports = {
    generateDeviceCode,
    getCodeInfo,
    findByUserCode,
    completeCode
};

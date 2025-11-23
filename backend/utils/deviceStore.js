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

/**
 * @typedef {Object} ConfirmationCodeInfo
 * @property {string} userToken - The JWT of the user initiating the confirmation.
 * @property {{ id: number, username: string }} user - User info from the token.
 * @property {number} expiresAt - Timestamp when the code expires.
 * @property {'pending' | 'confirmed' | 'cancelled'} status - Current status of the confirmation.
 * @property {number} interval - Recommended client polling interval (ms).
 */

/**
 * In-memory store for session confirmation codes.
 * @type {Map<string, ConfirmationCodeInfo>}
 */
const confirmationCodes = new Map();

// Default expiration time (5 minutes)
const EXPIRATION_MS = 5 * 60 * 1000; 
// Default recommended client polling interval (5 seconds)
const POLLING_INTERVAL_MS = 5000;

/**
 * Generates a new device code and adds it to the store.
 * @returns {{deviceCode: string, userCode: string, expiresIn: number, interval: number}}
 */
function generateDeviceCode() {
    const deviceCode = crypto.randomBytes(16).toString('hex');
    const userCode = crypto.randomBytes(3).toString('hex').toUpperCase().match(/.{1,3}/g).join('-');

    const codeInfo = {
        userCode: userCode,
        expiresAt: Date.now() + EXPIRATION_MS,
        status: 'pending',
        interval: POLLING_INTERVAL_MS,
    };

    deviceCodes.set(deviceCode, codeInfo);

    return {
        deviceCode: deviceCode,
        userCode: userCode,
        expiresIn: Math.round(EXPIRATION_MS / 1000),
        interval: POLLING_INTERVAL_MS,
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
        deviceCodes.set(deviceCode, info);
    }
}

/**
 * Generates a new session confirmation code.
 * @param {string} userToken - The JWT of the user initiating the confirmation.
 * @param {{ id: number, username: string }} user - User info from the token.
 * @returns {{confirmationCode: string, expiresIn: number, interval: number}}
 */
function generateConfirmationCode(userToken, user) {
    const confirmationCode = crypto.randomBytes(16).toString('hex');
    const codeInfo = {
        userToken: userToken,
        user: user,
        expiresAt: Date.now() + EXPIRATION_MS,
        status: 'pending',
        interval: POLLING_INTERVAL_MS,
    };
    confirmationCodes.set(confirmationCode, codeInfo);
    return {
        confirmationCode: confirmationCode,
        expiresIn: Math.round(EXPIRATION_MS / 1000),
        interval: POLLING_INTERVAL_MS,
    };
}

/**
 * Retrieves confirmation code information.
 * @param {string} confirmationCode - The code to retrieve.
 * @returns {ConfirmationCodeInfo | undefined}
 */
function getConfirmationCodeInfo(confirmationCode) {
    return confirmationCodes.get(confirmationCode);
}

/**
 * Marks a confirmation code as 'confirmed'.
 * @param {string} confirmationCode - The code to confirm.
 */
function confirmCode(confirmationCode) {
    const info = confirmationCodes.get(confirmationCode);
    if (info && info.status === 'pending') {
        info.status = 'confirmed';
        confirmationCodes.set(confirmationCode, info);
    }
}

/**
 * Marks a confirmation code as 'cancelled'.
 * @param {string} confirmationCode - The code to cancel.
 */
function cancelCode(confirmationCode) {
    const info = confirmationCodes.get(confirmationCode);
    if (info && info.status === 'pending') { // Only cancel if still pending
        info.status = 'cancelled';
        confirmationCodes.set(confirmationCode, info);
    }
}

/**
 * Cleans up expired or completed codes from the store periodically.
 */
setInterval(() => {
    const now = Date.now();
    // Cleanup device codes
    for (const [deviceCode, info] of deviceCodes.entries()) {
        if (info.expiresAt < now || (info.status === 'completed' && info.expiresAt < now + 5000)) {
            deviceCodes.delete(deviceCode);
        }
    }
    // Cleanup confirmation codes
    for (const [confirmationCode, info] of confirmationCodes.entries()) {
        // Remove if expired OR if confirmed/cancelled and a short grace period has passed
        if (info.expiresAt < now || ((info.status === 'confirmed' || info.status === 'cancelled') && info.expiresAt < now + 5000)) {
            confirmationCodes.delete(confirmationCode);
        }
    }
    console.log(`Store cleanup. Active device codes: ${deviceCodes.size}, Active confirmation codes: ${confirmationCodes.size}`);
}, 60 * 1000); // Run every minute

module.exports = {
    generateDeviceCode,
    getCodeInfo,
    findByUserCode,
    completeCode,
    generateConfirmationCode,
    getConfirmationCodeInfo,
    confirmCode,
    cancelCode
};

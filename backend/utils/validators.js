const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,24}$/;
const isValidPassword = (password = '') =>
    typeof password === 'string' &&
    password.length >= 8 &&
    password.length <= 64;

const validateCredentials = ({ username, password }) => {
    if (!USERNAME_REGEX.test(username || '')) {
        return 'Username must be 3-24 characters (letters, numbers, underscore).';
    }

    if (!isValidPassword(password)) {
        return 'Password must be 8-64 characters long.';
    }

    return null;
};

module.exports = {
    validateCredentials,
};

/**
 * Handles the response from a service layer and sends the appropriate HTTP response.
 * 
 * @param {object} res - The Express response object.
 * @param {object} result - The result object returned from the service.
 * @param {string} [defaultErrorMessage='Server error'] - The default error message to use if the result doesn't contain one.
 */
const handleServiceResponse = (res, result, defaultErrorMessage = 'Server error') => {
    if (result.status === 200) {
        // Destructure status out, send the rest as JSON
        const { status, ...data } = result;
        return res.status(200).json(data);
    } else if (result.status === 404) {
        return res.status(404).json({ error: result.message });
    } else {
        // Handle other statuses or default to 500
        const statusCode = result.status || 500;
        const errorMessage = result.message || defaultErrorMessage;
        return res.status(statusCode).json({ error: errorMessage });
    }
};

module.exports = { handleServiceResponse };

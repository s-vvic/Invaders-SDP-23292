const express = require("express");
const path = require("path");
const app = express();

app.listen(8080, function() {
    console.log('listening on 8080 port')
});

app.use(express.json());

const publicPath = path.join(__dirname, '../frontend/src/main/resources/public');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});

app.post('/api/login', function(req, res) {
    const { username, password } = req.body; 

    if (username === 'test' && password === '1234') {
        res.json({ token: 'your-generated-token-xyz123' });
    } else {
        res.status(401).json({ error: 'Invalid username or password' });
    }
});
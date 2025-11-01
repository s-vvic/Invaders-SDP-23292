const express = require("express");
const path = require("path");
const app = express();

app.listen(8080, function() {
    console.log('listening on 8080 port')
});

const publicPath = path.join(__dirname, '../frontend/src/main/resources/public');
app.use(express.static(publicPath));

app.get('/', function(req,res) {
    res.sendFile(path.join(publicPath, 'index.html'));
});
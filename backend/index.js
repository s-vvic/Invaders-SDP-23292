const express = require("express");
const app = express();

app.listen(8080, function() {
    console.log('listening on 8080 port')
});

app.get('/', function(req,res) {
    res.sendFile('index.html', {root: '../frontend/src/main/resources/public'});
});
// server/server.js
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
const port = 5000; // Port za backend

app.use(cors());
app.use(bodyParser.json());

// In-memory storage for games (in real app, use a database)
let games = {};

// Endpoint for creating a game
app.post('/create-game', (req, res) => {
    const { adminName, adminSurname, playerCount } = req.body;
    const gameCode = Math.random().toString(36).substring(2, 8); // Random game code

    games[gameCode] = {
        adminName,
        adminSurname,
        playerCount,
    };

    res.status(201).json({ gameCode });
});

// Endpoint for checking game code
app.get('/check-game/:code', (req, res) => {
    const { code } = req.params;
    if (games[code]) {
        res.status(200).json({ exists: true });
    } else {
        res.status(404).json({ exists: false });
    }
});

app.listen(port, () => {
    console.log(`Server listening at http://localhost:${port}`);
});

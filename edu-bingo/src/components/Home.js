// src/components/Home.js

import React, { useState } from 'react';
import { useSocket } from '../SocketContext';

const Home = ({ onCreateGame, onJoinGame }) => {
  const [adminUsername, setAdminUsername] = useState('');
  const [adminPass, setAdminPass] = useState('');
  const [numPlayers, setNumPlayers] = useState('');
  const [gameCode, setGameCode] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const socket = useSocket()

  const handleStartGame = () => {
    if (!adminUsername || !adminPass || !numPlayers) {
      setErrorMessage("Molimo ispunite sva polja za kreiranje igre.");
      return;
    }
    const newGameCode = generateGameCode();
    onCreateGame({ adminUsername, adminPass, numPlayers, gameCode: newGameCode })
      .then(() => setErrorMessage(''))
      .catch(() => setErrorMessage("Greška pri kreiranju igre."));
  };

  const handleJoinGame = () => {
    if (gameCode && playerName) {
      const exists = onJoinGame(gameCode, playerName);
      if (exists) {
        if (!socket) {
          console.error("Socket not initialized")
        } else {
          socket.emit('joinGame', { game_code: gameCode, player_name: playerName })
        }
        setErrorMessage('');
      } else {
        setErrorMessage("Šifra igre je neispravna.");
      }
      setPlayerName('');
    } else {
      setErrorMessage("Unesite šifru igre i vaše ime i prezime.");
    }
  };

  const generateGameCode = () => {
    return Math.random().toString(36).substring(2, 8).toUpperCase();
  };

  return (
    <div className="home">
      <h1>EduBingo</h1>
      {errorMessage && <p className="error">{errorMessage}</p>}
      <div className="admin-info">
        <h2>Kreiraj igru</h2>
        <input 
          type="text" 
          placeholder="Username admina" 
          value={adminUsername} 
          onChange={(e) => setAdminUsername(e.target.value)} 
        />
        <input 
          type="text" 
          placeholder="Password admina" 
          value={adminPass} 
          onChange={(e) => setAdminPass(e.target.value)} 
        />
        <input 
          type="number" 
          placeholder="Broj igrača" 
          value={numPlayers} 
          onChange={(e) => setNumPlayers(e.target.value)} 
        />
        <button onClick={handleStartGame}>Start Game</button>
      </div>
      <div className="join-game">
        <h2>Pridruži se igri</h2>
        <input 
          type="text" 
          placeholder="Šifra igre" 
          value={gameCode} 
          onChange={(e) => setGameCode(e.target.value)} 
        />
        <input 
          type="text" 
          placeholder="Ime igrača" 
          value={playerName} 
          onChange={(e) => setPlayerName(e.target.value)} 
        />
        <button onClick={handleJoinGame}>Start</button>
      </div>
    </div>
  );
};

export default Home;

// src/components/Home.js

import React, { useState } from 'react';

const Home = ({ onCreateGame, onJoinGame }) => {
  const [adminName, setAdminName] = useState('');
  const [adminSurname, setAdminSurname] = useState('');
  const [numPlayers, setNumPlayers] = useState('');
  const [gameCode, setGameCode] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [playerSurname, setPlayerSurname] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleStartGame = () => {
    if (adminName && adminSurname && numPlayers) {
      const newGameCode = generateGameCode();
      onCreateGame({ adminName, adminSurname, numPlayers, gameCode: newGameCode });
      setErrorMessage('');
    }
  };

  const handleJoinGame = () => {
    if (gameCode && playerName && playerSurname) {
      const exists = onJoinGame({ gameCode, playerName: `${playerName} ${playerSurname}` });
      if (exists) {
        setErrorMessage('');
      } else {
        setErrorMessage("Šifra igre je neispravna.");
      }
      setPlayerName('');
      setPlayerSurname('');
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
          placeholder="Ime admina" 
          value={adminName} 
          onChange={(e) => setAdminName(e.target.value)} 
        />
        <input 
          type="text" 
          placeholder="Prezime admina" 
          value={adminSurname} 
          onChange={(e) => setAdminSurname(e.target.value)} 
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
        <input 
          type="text" 
          placeholder="Prezime igrača" 
          value={playerSurname} 
          onChange={(e) => setPlayerSurname(e.target.value)} 
        />
        <button onClick={handleJoinGame}>Start</button>
      </div>
    </div>
  );
};

export default Home;

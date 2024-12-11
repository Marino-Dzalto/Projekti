// src/components/Home.js

import React, { useState } from 'react';
import '@fortawesome/fontawesome-free/css/all.min.css';


const Home = ({ onCreateGame, onJoinGame }) => {
  const [adminUsername, setAdminUsername] = useState('');
  const [adminPass, setAdminPass] = useState('');
  const [numPlayers, setNumPlayers] = useState('');
  const [gameCode, setGameCode] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

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
      const exists = onJoinGame({ gameCode, playerName: `${playerName}` });
      if (exists) {
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
      <div className="clouds">
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
      </div>
      <h1 className="title">
        <i className="fas fa-graduation-cap"></i> EduBingo
      </h1>
      {errorMessage && <p className="error">{errorMessage}</p>}
      <div className="card">
        <h2><i className="fas fa-cogs"></i> Kreiraj igru</h2>
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
        <button onClick={handleStartGame}>
          <i className="fas fa-play"></i> Start Game
        </button>
      </div>
      <div className="card">
        <h2><i className="fas fa-sign-in-alt"></i> Pridruži se igri</h2>
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
        <button onClick={handleJoinGame}>
          <i className="fas fa-sign-in-alt"></i> Join Game
        </button>
      </div>
    </div>
  );
};

export default Home;

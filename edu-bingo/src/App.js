// src/App.js

import React, { useState } from 'react';
import Home from './components/Home';
import AdminGame from './components/AdminGame';
import Lobby from './components/Lobby';
import './App.css';

const App = () => {
  const [adminData, setAdminData] = useState(null);
  const [gameCode, setGameCode] = useState(null);

  const handleCreateGame = (data) => {
    setAdminData(data);
  };

  const handleJoinGame = (code, playerData) => {
    setGameCode(code);
    // Ovdje možete dodati logiku za dodavanje igrača u lobby
  };

  const handleLeaveLobby = () => {
    setGameCode(null);
  };

  return (
    <div className="App">
      {gameCode ? (
        <Lobby gameCode={gameCode} onLeaveLobby={handleLeaveLobby} />
      ) : adminData ? (
        <AdminGame adminData={adminData} />
      ) : (
        <Home onCreateGame={handleCreateGame} onJoinGame={handleJoinGame} />
      )}
    </div>
  );
};

export default App;

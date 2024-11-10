// src/App.js

import React, { useState } from 'react';
import Home from './components/Home';
import AdminGame from './components/AdminGame';
import Lobby from './components/Lobby';
import './App.css';

const App = () => {
  const [adminData, setAdminData] = useState(null);
  const [gameCode, setGameCode] = useState(null);
  const [players, setPlayers] = useState([]);  // gdje ćemo storeat igrače
  const [isGameLocked, setIsGameLocked] = useState(false); // je li soba zaključana/otključana

  const handleCreateGame = async (data) => {
    try {
      const response = await fetch('/api/create-room', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      const result = await response.json();
      setAdminData(result); // room podaci potrebni za sobu
      setIsGameLocked(false); // inicijalno je soba otvorena...
    } catch (error) {
      console.error('Error creating game:', error);
    }
  };

  const handleJoinGame = async (code, playerData) => {
    try {
      const response = await fetch(`/api/join-room/${code}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(playerData),
      });
      const result = await response.json();
      setGameCode(code);
      setPlayers(result.players); // inicijalna lista za postavljanje igrača
    } catch (error) {
      console.error('Error joining game:', error);
    }
  };

  const handleLeaveLobby = () => {
    setGameCode(null);
  };

  const handleStartGame = async () => {
    try {
      await fetch(`/api/lock-room/${gameCode}`, { method: 'POST' });
      setIsGameLocked(true); // lockamo sobu, igra kreće, ne može se nitko više pridružiti
    } catch (error) {
      console.error('Error locking the game:', error);
    }
  };

  return (
    <div className="App">
      {gameCode ? (
        <Lobby
          gameCode={gameCode}
          players={players}
          onLeaveLobby={handleLeaveLobby}
          isGameLocked={isGameLocked}
        />
      ) : adminData ? (
        <AdminGame
          adminData={adminData}
          onStartGame={handleStartGame}
        />
      ) : (
        <Home onCreateGame={handleCreateGame} onJoinGame={handleJoinGame} />
      )}
    </div>
  );
};

export default App;

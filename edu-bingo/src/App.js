// src/App.js

import React, { useState } from 'react';
import './App.css';
import AdminGame from './components/AdminGame';
import GameBoard from './components/GameBoard';
import Home from './components/Home';
import Lobby from './components/Lobby';
import { SocketProvider } from './SocketContext';

const App = () => {
  const [adminData, setAdminData] = useState(null);
  const [adminName, setAdminName] = useState(null);
  const [gameCode, setGameCode] = useState(null);
  const [players, setPlayers] = useState(null);  // gdje ćemo storeat igrače
  const [isGameLocked, setIsGameLocked] = useState(false); // je li soba zaključana/otključana
  const [isGameStarted, setIsGameStarted] = useState(false); //je li igra započela ili smo još u lobbyu

  const handleCreateGame = async (data) => {
    try {
      const verifyResponse = await fetch('/api/verify-teacher', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      const verifyResult = await verifyResponse.json();

      if(!verifyResponse.ok) {
        alert(verifyResult.message);
        return;
      }

      const response = await fetch('/api/create-game', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ teacher_id: verifyResult.teacher_id, data }),
      });

      const result = await response.json();

      if (response.ok) {
        setAdminData(result); // room podaci potrebni za sobu
        setIsGameLocked(false); // inicijalno je soba otvorena...
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('Error creating game:', error);
    }
  };

  const handleLeaveLobby = () => {
    setGameCode(null);
    setIsGameStarted(false);
  };

  return (
    <SocketProvider>
      <div className="App">
        {isGameStarted ? (
          // Ako je igra krenula idemo na GameBoard
          <GameBoard players={players} gameCode={gameCode} />
        ) : adminData ? (
          // Ako adminData imamo, a Game code jos ne onda mi pokaži AdminGame(teacher dio)
          <AdminGame
            adminData={adminData}
          />
        ) : players ? (
          // Ako je izgeneriran code, ali igra još nije krenla onda smo još u Lobbyu
          <Lobby
            gameCode={gameCode}
            adminName={adminName}
            players={players}
            onLeaveLobby={handleLeaveLobby}
            isGameLocked={isGameLocked}
          />
        ) : (
          // Pokaži index po defaultu
          <Home onCreateGame={handleCreateGame} setPlayers={setPlayers} setGameCode={setGameCode} setAdminName={setAdminName}/>
        )}
      </div>
    </SocketProvider>
  );
};

export default App;

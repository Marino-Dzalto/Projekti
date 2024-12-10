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
  const [gameCode, setGameCode] = useState(null);
  const [playerData, setPlayerData] = useState(false)
  const [players, setPlayers] = useState([]);  // gdje ćemo storeat igrače
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
        setGameCode(adminData.game_code)
        setIsGameLocked(false); // inicijalno je soba otvorena...
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('Error creating game:', error);
    }
  };

  const handleJoinGame = async (code, playerName) => {
    try {
      const response = await fetch(`/api/join-game/${code}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(playerName),
      });
      const result = await response.json();
      setPlayerData(result.game_id);
      setPlayers(result.players); // inicijalna lista za postavljanje igrača

    } catch (error) {
      console.error('Error joining game:', error);
    }
  };

  const handleLeaveLobby = () => {
    setGameCode(null);
    setIsGameStarted(false);
  };

  const handleStartGame = async () => {
    try {
      await fetch(`/api/lock-room/${gameCode}`, { method: 'POST' });
      setIsGameLocked(true); // lockamo sobu, igra kreće, ne može se nitko više pridružiti
      setIsGameStarted(true);
    } catch (error) {
      console.error('Error locking the game:', error);
    }
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
            onStartGame={handleStartGame}
          />
        ) : playerData ? (
          // Ako je izgeneriran code, ali igra još nije krenla onda smo još u Lobbyu
          <Lobby
            gameCode={gameCode}
            gameId={playerData.game_id}
            players={players}
            onLeaveLobby={handleLeaveLobby}
            isGameLocked={isGameLocked}
          />
        ) : (
          // Pokaži index po defaultu
          <Home onCreateGame={handleCreateGame} onJoinGame={handleJoinGame} />
        )}
      </div>
    </SocketProvider>
  );
};

export default App;

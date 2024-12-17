// src/App.js

import React, { useState, useEffect } from 'react';
import './App.css';
import AdminGame from './components/AdminGame';
import GameBoard from './components/GameBoard';
import Home from './components/Home';
import Lobby from './components/Lobby';
import Leaderboard from './components/Leaderboard';
import { SocketProvider } from './SocketContext';

const App = () => {
  const [adminData, setAdminData] = useState(null);
  const [adminName, setAdminName] = useState(null);
  const [gameCode, setGameCode] = useState(null);
  const [players, setPlayers] = useState(null);  // gdje ćemo storeat igrače
  const [isGameLocked, setIsGameLocked] = useState(false); // je li soba zaključana/otključana
  const [isGameStarted, setIsGameStarted] = useState(false); //je li igra započela ili smo još u lobbyu
  const [showLeaderboard, setShowLeaderboard] = useState(false); //pokazivanje leaderboarda
  const [scores, setScores] = useState([]);

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

  // Fetchanje leaderboarda sa backenda
  const fetchLeaderboard = async (gameCode) => {
    try {
      const response = await fetch(`/api/leaderboard/${gameCode}`);
      if (!response.ok) {
        throw new Error(`Error fetching leaderboard: ${response.statusText}`);
      }
      const data = await response.json();

      setPlayers(data.map((player) => ({ name: player.name })));
      setScores(data.map((player) => player.score));
      setShowLeaderboard(true);
    } catch (error) {
      console.error("Failed to fetch leaderboard:", error);
    }
  };

  const handleEndGame = () => {
    setIsGameStarted(false);
    fetchLeaderboard(gameCode);
  };

  useEffect(() => {
    // Da bismo testirali pojedinu stranicu samo uncommentaj pojedini dio
    // AdminGame
    //setAdminData({ roomId: '123', roomName: 'Test Room' });

    // Lobby
    //setGameCode('123');
    //setPlayers([{ name: 'Player1' }, { name: 'Player2' }]);

    // GameBoard
    //setIsGameStarted(true);
    //setPlayers([{ name: 'Player1' }, { name: 'Player2' }]);
    //setGameCode('123');


    // Leaderboard
    //  setPlayers([
    //  { name: 'Player1' },
    //  { name: 'Player2' },
    //  { name: 'Player3' },]);
    //setShowLeaderboard(true);
    //setScores([100, 90, 80]);

    //Home
  }, []);

  return (
    <SocketProvider>
      <div className="App">
        {isGameStarted ? (
          // Ako je igra krenula idemo na GameBoard
          <GameBoard players={players} gameCode={gameCode} onEndGame={handleEndGame} />
        ) : adminData ? (
          // Ako adminData imamo, a Game code jos ne onda mi pokaži AdminGame(teacher dio)
          <AdminGame
            adminData={adminData}
          />
        ) : showLeaderboard ? (
          <Leaderboard players={players} scores={scores} />
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

// src/components/AdminGame.js

import React, { useState, useEffect } from 'react';
import './AdminGame.css';

const AdminGame = ({ adminData, players }) => {
  const [gameCode, setGameCode] = useState('');
  const [playerList, setPlayerList] = useState([]);
  const [isGameLocked, setIsGameLocked] = useState(false);

  useEffect(() => {
    const generatedCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    setGameCode(generatedCode);
  }, []);

  useEffect(() => {
    // Fetchamo igrače kakoo bi mogli popunit listu
    const fetchPlayers = async () => {
      try {
        const response = await fetch(`/api/players/${gameCode}`);
        const result = await response.json();
        setPlayerList(result.players);
      } catch (error) {
        console.error('Error fetching players:', error);
      }
    };

    const interval = setInterval(fetchPlayers, 5000); // Poll every 5 seconds
    return () => clearInterval(interval);
  }, [gameCode]);

  const handleLockGame = async () => {
    try {
      // Sendamo request da zatvorimo sobu
      await fetch(`/api/lock-room/${gameCode}`, { method: 'POST' });
      setIsGameLocked(true);
    } catch (error) {
      console.error('Error locking game room:', error);
    }
  };

  return (
    <div className="admin-game">
      <h1>EduBingo</h1>
      <div className="game-code">
        <h2>Šifra igre: {gameCode}</h2>
        <button onClick={handleLockGame} disabled={isGameLocked}>
          {isGameLocked ? 'Igra zaključana' : 'Zaključaj igru'}
        </button>
      </div>
      <div className="player-list">
        <h2>Igrači:</h2>
        <ul>
          {playerList.length > 0 ? (
            playerList.map((player, index) => (
              <li key={index}>{player.name || player}</li>
            ))
          ) : (
            <li>Nema igrača u igri.</li>
          )}
        </ul>
      </div>
    </div>
  );
};

export default AdminGame;

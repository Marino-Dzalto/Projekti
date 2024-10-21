// src/components/AdminGame.js

import React, { useState, useEffect } from 'react';
import './AdminGame.css';

const AdminGame = ({ adminData, players }) => {
  const [gameCode, setGameCode] = useState('');
  const [playerList, setPlayerList] = useState([]);

  useEffect(() => {
    const generatedCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    setGameCode(generatedCode);
  }, []);

  useEffect(() => {
    // Kada se adminData promijeni, resetiraj listu igrača
    if (adminData) {
      setPlayerList([]);
    }
  }, [adminData]);

  const addPlayer = (playerName) => {
    setPlayerList((prevPlayers) => [...prevPlayers, playerName]);
  };

  return (
    <div className="admin-game">
      <h1>EduBingo</h1>
      <div className="game-code">
        <h2>Šifra igre: {gameCode}</h2>
      </div>
      <div className="player-list">
        <h2>Igrači:</h2>
        <ul>
          {playerList.length > 0 ? (
            playerList.map((player, index) => (
              <li key={index}>{player}</li>
            ))
          ) : (
            <li>Nema igrača u igri.</li>
          )}
        </ul>
      </div>
      {/* Ovdje će se dodati logika za dodavanje igrača */}
    </div>
  );
};

export default AdminGame;

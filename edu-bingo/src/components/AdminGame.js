// src/components/AdminGame.js

import React, { useState, useEffect } from 'react';
import '../styles/AdminGame.css';

const AdminGame = ({ adminData, players }) => {
  const [gameCode, setGameCode] = useState('');
  const [playerList, setPlayerList] = useState([]);
  const [isGameLocked, setIsGameLocked] = useState(false);
  const [subject, setSubject] = useState('');
  const [topic, setTopic] = useState('');
  const [availableTopics, setAvailableTopics] = useState([]);

  // Definicija predmeta i tema
  const subjects = ['Matematika', 'Hrvatski', 'Priroda', 'Engleski'];
  const topics = {
    Matematika: ['Brojevi do 100', 'Zbrajanje/oduzimanje', 'Množenje/dijeljenje', 'Zadaci s riječima'],
    Priroda: ['Razvrstavanje otpada', 'primjer2', 'primjer3'],
    Hrvatski: ['primjer1', 'primjer2', 'primjer3'],
    Engleski: ['primjer1', 'primjer2', 'primjer3']
  };

  useEffect(() => {
    const generatedCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    setGameCode(generatedCode);
  }, []);

  // Update tema i predmeta
  useEffect(() => {
    setAvailableTopics(subject ? topics[subject] : []);
    setTopic(''); // kad se promjeni predmet resetaj temu
  }, [subject, topics]);

  useEffect(() => {
    // Fetchamo igrače kakoo bi mogli popunit listu
    const fetchPlayers = async () => {
      try {
        const response = await fetch(`/api/players/${gameCode}`);
        const result = await response.json();
        setPlayerList(result);
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

   // Handle game creation nakon sta izaberemo temu i predmet
   const handleCreateGame = async () => {
    if (!subject || !topic) {
      alert('Please select both a subject and a topic.');
      return;
    }

    try {
      const response = await fetch('/api/create-game', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ gameCode, subject, topic })
      });

      if (response.ok) {
        console.log('Igra je uspješno napravljena');
      } else {
        console.error('Igra nije napravljena');
      }
    } catch (error) {
      console.error('Greška pri pravljenju igre!', error);
    }
  };

  return (
    <div className="admin-game">
      <h1>EduBingo</h1>
      <div className="game-setup">
        <label>
          Predmet:
          <select value={subject} onChange={(e) => setSubject(e.target.value)}>
            <option value="">Izaberite predmet</option>
            {subjects.map((subj, index) => (
              <option key={index} value={subj}>{subj}</option>
            ))}
          </select>
        </label>

        <label>
          Tema:
          <select value={topic} onChange={(e) => setTopic(e.target.value)} disabled={!subject}>
            <option value="">Izaberite temu</option>
            {availableTopics.map((top, index) => (
              <option key={index} value={top}>{top}</option>
            ))}
          </select>
        </label>

        <button onClick={handleCreateGame}>Pokreni igru</button>
      </div>

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

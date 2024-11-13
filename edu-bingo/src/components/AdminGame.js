// src/components/AdminGame.js

import React, { useState, useEffect } from 'react';
import '../styles/AdminGame.css';

const AdminGame = ({ adminData, players }) => {
  const [gameCode, setGameCode] = useState('');
  const [playerList, setPlayerList] = useState([]);
  const [isGameLocked, setIsGameLocked] = useState(false);
  const [subjects, setSubjects] = useState([]);// predmeti s backenda
  const [selectedSubject, setSelectedSubject] = useState('');// trenutacni predmet
  const [topics, setTopics] = useState([]);// teme s backenda
  const [selectedTopic, setSelectedTopic] = useState('');//trenutacna tema


  useEffect(() => {
    const generatedCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    setGameCode(generatedCode);
  }, []);

  // Fetchamo subjects iz backenda
  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const response = await fetch('/api/subjects');  // treba namjestit enrpoin
        if (response.ok) {
          const data = await response.json();
          setSubjects(data.subjects);
        } else {
          console.error('Failed to fetch subjects');
        }
      } catch (error) {
        console.error('Error fetching subjects:', error);
      }
    };
    fetchSubjects();
  }, []);

  useEffect(() => {
    if (!selectedSubject) {
      setTopics([]); // ako predmet nije izabran brisemo i teme
      return;
    }

    const fetchTopics = async () => {
      try {
        const response = await fetch(`/api/topics?subject=${selectedSubject}`); // Treba namjestit endpoint
        if (response.ok) {
          const data = await response.json();
          setTopics(data.topics);
        } else {
          console.error('Failed to fetch topics');
        }
      } catch (error) {
        console.error('Error fetching topics:', error);
      }
    };
    fetchTopics();
  }, [selectedSubject]);

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
      await fetch(`/api/lock-room/${gameCode}`, { method: 'POST' });
      setIsGameLocked(true);
    } catch (error) {
      console.error('Error locking game room:', error);
    }
  };

   // Handle game creation nakon sta izaberemo temu i predmet
   const handleCreateGame = async () => {
    if (!selectedSubject || !selectedTopic) {
      alert('Please select both a subject and a topic.');
      return;
    }

    try {
      const response = await fetch('/api/create-game', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ gameCode, subject: selectedSubject, topic: selectedTopic })
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
          <select value={selectedSubject} onChange={(e) => setSelectedSubject(e.target.value)}>
            <option value="">Izaberite predmet</option>
            {subjects.map((subj, index) => (
              <option key={index} value={subj.name}>{subj.name}</option>
            ))}
          </select>
        </label>

        <label>
          Tema:
          <select
            value={selectedTopic}
            onChange={(e) => setSelectedTopic(e.target.value)}
            disabled={!selectedSubject}
          >
            <option value="">Izaberite temu</option>
            {topics
              .filter((t) => t.subject === selectedSubject)
              .map((top, index) => (
                <option key={index} value={top.name}>{top.name}</option>
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

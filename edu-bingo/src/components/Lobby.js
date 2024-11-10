import React, { useEffect, useState } from 'react';
import './Lobby.css'; // Uključite CSS datoteku za stilizaciju

const Lobby = ({ gameCode, adminName, players, isGameLocked }) => {
  const [timer, setTimer] = useState(0);
  const [chatMessage, setChatMessage] = useState('');
  const [chat, setChat] = useState([]);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [updatedPlayers, setUpdatedPlayers] = useState(players);

  useEffect(() => {
    const interval = setInterval(() => {
      setTimer((prevTimer) => prevTimer + 1);
      setCurrentTime(new Date()); // Ažurirajte vrijeme svake sekunde
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);

  // Funkcija kako bi se igrači dohvatili periodično
  useEffect(() => {
    const fetchPlayers = async () => {
      try {
        const response = await fetch(`/api/players/${gameCode}`);
        const result = await response.json();
        setUpdatedPlayers(result.players); // Update liste igrača sa podatcim sa servera
      } catch (error) {
        console.error('Error fetching players:', error);
      }
    };

    // Poll svakih par sekundi da možemo ažurno updateat listu
    const interval = setInterval(fetchPlayers, 5000);
    return () => clearInterval(interval);
  }, [gameCode]);

  const handleSendChat = (e) => {
    e.preventDefault();
    if (chatMessage) {
      setChat((prevChat) => [...prevChat, chatMessage]);
      setChatMessage('');
    }
  };

  return (
    <div className="lobby-container">
      <div className="header">
        <h1>Lobby</h1>
        <div className="current-time">
          <h2>{currentTime.toLocaleTimeString()}</h2>
        </div>
      </div>
      <div className="lobby-info">
        <h2>Šifra igre: {gameCode}</h2>
        <h3>Admin: {adminName}</h3>
        <div className="timer">Vrijeme u lobby-u: {timer} sekundi</div>
        {isGameLocked && <p className="locked-message">Igra je zaključana. Nema novih igrača.</p>}
      </div>
      <div className="main-content">
        <div className="player-list">
          <h3>Igrači:</h3>
          <ul>
            {updatedPlayers.length > 0 ? (
              updatedPlayers.map((player, index) => (
                <li key={index}>{player.name || player}</li>
              ))
            ) : (
              <li>Nema igrača u igri.</li>
            )}
          </ul>
        </div>
        <div className="chat">
          <h3>Chat</h3>
          <div className="chat-window">
            {chat.map((message, index) => (
              <div key={index}>{message}</div>
            ))}
          </div>
          <form onSubmit={handleSendChat}>
            <input
              type="text"
              value={chatMessage}
              onChange={(e) => setChatMessage(e.target.value)}
              placeholder="Unesite poruku..."
            />
            <button type="submit">Pošalji</button>
          </form>
        </div>
      </div>
    </div>
  );
  
};

export default Lobby;

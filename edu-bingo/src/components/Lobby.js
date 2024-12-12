import React, { useEffect, useState } from 'react';
import '../styles/Lobby.css'; // Uključite CSS datoteku za stilizaciju
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

// Import specific icons
import { faClock, faKey, faUser, faUsers, faLock, faComments, faPaperPlane } from '@fortawesome/free-solid-svg-icons';

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
        setUpdatedPlayers(result); // Update liste igrača sa podatcim sa servera
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
          <h2><FontAwesomeIcon icon={faClock} />{currentTime.toLocaleTimeString()}</h2>
        </div>
      </div>
      <div className="lobby-info">
        <h2> <FontAwesomeIcon icon={faKey} /> Šifra igre:  <span className="highlight">{gameCode}</span></h2>
        <h3><FontAwesomeIcon icon={faUser} /> Admin: {adminName}</h3>
        <div className="timer"> <FontAwesomeIcon icon={faClock} /> Vrijeme u lobby-u: {timer} sekundi</div>
        {isGameLocked && <p className="locked-message"><FontAwesomeIcon icon={faLock} /> Igra je zaključana. Nema novih igrača.</p>}
      </div>
      <div className="main-content">
        <div className="player-list">
          <h3> <FontAwesomeIcon icon={faUsers} /> Igrači:</h3>
          <ul>
            {updatedPlayers.length > 0 ? (
              updatedPlayers.map((player, index) => (
                <li key={index}> <FontAwesomeIcon icon={faUser} /> {player.name || player}</li>
              ))
            ) : (
              <li>Nema igrača u igri.</li>
            )}
          </ul>
        </div>
        <div className="chat">
          <h3><FontAwesomeIcon icon={faComments} /> Chat</h3>
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
            <button type="submit"> <FontAwesomeIcon icon={faPaperPlane} /> Pošalji</button>
          </form>
        </div>
      </div>
    </div>
  );
  
};

export default Lobby;

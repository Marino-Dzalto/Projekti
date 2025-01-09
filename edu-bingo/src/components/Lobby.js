import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useEffect, useRef, useState } from 'react';
import { useSocket } from '../SocketContext';
import '../styles/Lobby.css'; // Uključite CSS datoteku za stilizaciju

// Import specific icons
import { faClock, faComments, faKey, faLock, faPaperPlane, faUser, faUsers } from '@fortawesome/free-solid-svg-icons';

const Lobby = ({ gameCode, adminName, players, isGameLocked }) => {
  const [timer, setTimer] = useState(0);
  const [chatMessage, setChatMessage] = useState('');
  const [chat, setChat] = useState([]);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [updatedPlayers, setUpdatedPlayers] = useState(players);
  const chatWindowRef = useRef(null);
  const socket = useSocket();

  useEffect(() => {
    const interval = setInterval(() => {
      setTimer((prevTimer) => prevTimer + 1);
      setCurrentTime(new Date()); // Ažurirajte vrijeme svake sekunde
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (socket) {
      const handleUpdatePlayers = (data) => {
        if (data && data.players) {
          setUpdatedPlayers(data.players);
        }
      };

      const handleUpdateChat = (data) => {
        if (data && data.message && data.timestamp) {
          setChat((prevChat) => [...prevChat, { message: data.message, timestamp: data.timestamp }]);
        }
      }

      socket.on("updatePlayers", handleUpdatePlayers);
      socket.on("chatMessage", handleUpdateChat);

      return () => {
        socket.off("updatePlayers", handleUpdatePlayers);
        socket.off("chatMessage", handleUpdateChat);
      };
    }
  }, [socket]);

  useEffect(() => {
    if (chatWindowRef.current) {
      chatWindowRef.current.scrollTop = chatWindowRef.current.scrollHeight;
    }

  }, [chat]);

  const handleSendChat = (e) => {
    e.preventDefault();
    if (chatMessage) {
      socket.emit("chatMessage", { game_code: gameCode, message: chatMessage });
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
          <div className="chat-window" ref={chatWindowRef}>
            {chat.map((item, index) => (
              <div className="message-box" key={index}>
                <div>{item.message}</div>
                <small>{item.timestamp}</small>
              </div>
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

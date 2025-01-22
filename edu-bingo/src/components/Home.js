// src/components/Home.js

import '@fortawesome/fontawesome-free/css/all.min.css';
import React, { useState } from 'react';
import mickey from "../mickey.png";
import minnie from "../minnie.png";

import { useSocket } from '../SocketContext';

const SignUp = ({ onClose }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); // Prevent page reload

    setError(''); // Reset error message

    // Check if passwords match
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      const response = await fetch('/api/create-teacher', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: formData.username,
          email: formData.email,
          password: formData.password
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Failed to create account');
      }

      setSuccess(true); // Show success message
      setTimeout(() => {
        onClose(); // Close the modal after success
      }, 2000); // Close modal after 2 seconds

    } catch (err) {
      setError(err.message); // Show error message
    }
  };

  return (
    <div className="signup-overlay">
      <div className="signup-modal">
        <button className="close-btn" onClick={onClose}>
          <i className="fas fa-times"></i>
        </button>
        <h2><i className="fas fa-user-plus"></i> Create Account</h2>
        {success ? (
          <p className="success-message">Account created successfully!</p>
        ) : (
          <form onSubmit={handleSubmit}>  {/* Ensuring form submits properly */}
            {error && <p className="error-message">{error}</p>}
            <input
              type="text"
              name="username"
              placeholder="Username"
              value={formData.username}
              onChange={handleChange}
              required
            />
            <input
              type="email"
              name="email"
              placeholder="Email"
              value={formData.email}
              onChange={handleChange}
              required
            />
            <input
              type="password"
              name="password"
              placeholder="Password"
              value={formData.password}
              onChange={handleChange}
              required
            />
            <input
              type="password"
              name="confirmPassword"
              placeholder="Confirm Password"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
            />
            <button type="submit" className="signup-submit-btn">  {/* Ensure the button is type="submit" */}
              <i className="fas fa-user-plus"></i> Sign Up
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

const Home = ({ onCreateGame, setPlayers, setGameCode, setAdminName}) => {
  const [adminUsername, setAdminUsername] = useState('');
  const [adminPass, setAdminPass] = useState('');
  const [gameCode, setGameCodeLocal] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [showSignUp, setShowSignUp] = useState(false);
  const socket = useSocket()

  const handleStartGame = () => {
    if (!adminUsername || !adminPass) {
      setErrorMessage("Molimo ispunite sva polja za kreiranje igre.");
      return;
    }
    const newGameCode = generateGameCode();
    onCreateGame({ adminUsername, adminPass, gameCode: newGameCode })
      .then(() => setErrorMessage(''))
      .catch(() => setErrorMessage("Greška pri kreiranju igre."));
  };

  const handleJoinGame = async () => {
    if (gameCode && playerName) {
      try {
        const response = await fetch(`/api/join-game/${gameCode}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(playerName),
        });

        const result = await response.json();

        if (response.ok) {
          setPlayers(result.players);
          setGameCode(result.game_code);
          setAdminName(result.teacher_name);

          if (socket) {
            socket.emit('joinGame', { game_code: result.game_code, player_name: playerName });
          } else {
            console.error("Socket not initialized");
          }
        } else {
          alert(result.message);
        }
      } catch (error) {
        console.error('Error joining game:', error);
      }
    } else {
      setErrorMessage("Unesite šifru igre i vaše ime i prezime.");
    }
  };

  const generateGameCode = () => {
    return Math.random().toString(36).substring(2, 8).toUpperCase();
  };

  const handleSignUpClick = (e) => {
    e.preventDefault();
    setShowSignUp(true);
  };

  return (
    <div className="home">
      <div className="clouds">
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
        <div className="cloud"><i className="fas fa-cloud"></i></div>
      </div>
      <h1 className="title">
        <i className="fas fa-graduation-cap"></i> EduBingo
      </h1>
      <div className="signup-link">
        <button 
          className="signup-link-btn" 
          onClick={handleSignUpClick}
        >
          <i className="fas fa-user-plus"></i> Create an Account
        </button>
      </div>
      {showSignUp && <SignUp onClose={() => setShowSignUp(false)} />}
      {errorMessage && <p className="error">{errorMessage}</p>}
      <div className="field">
        <h2><i className="fas fa-cogs"></i> Kreiraj igru</h2>
        <input
          type="text"
          placeholder="Username admina"
          value={adminUsername}
          onChange={(e) => setAdminUsername(e.target.value)}
        />
        <input
          type="text"
          placeholder="Password admina"
          value={adminPass}
          onChange={(e) => setAdminPass(e.target.value)}
        />
        <button onClick={handleStartGame}>
          <i className="fas fa-play"></i> Start Game
        </button>
      </div>
      <div className="field">
        <h2><i className="fas fa-sign-in-alt"></i> Pridruži se igri</h2>
        <input
          type="text"
          placeholder="Šifra igre"
          value={gameCode}
          onChange={(e) => setGameCodeLocal(e.target.value)}
        />
        <input
          type="text"
          placeholder="Ime igrača"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
        />
        <button onClick={handleJoinGame}>
          <i className="fas fa-sign-in-alt"></i> Join Game
        </button>
      </div>
      {/* Mickey and Minnie */}
      <div className = "character">
        <div className="minnie">
            <img src={minnie} alt="Minnie pic"/>
        </div>
        <div className="mickey">
            <img src={mickey} alt="Mickey pic"/>
        </div>
        
      </div>
      
    </div>
  );
};

export default Home;

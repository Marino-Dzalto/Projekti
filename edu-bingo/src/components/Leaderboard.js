import React from 'react';
import '../styles/Leaderboard.css';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMedal } from '@fortawesome/free-solid-svg-icons';

const Leaderboard = ({ players, scores }) => {
  const playerData = players.map((player, index) => ({
    name: player.name || `Player ${index + 1}`,
    score: scores[index] !== undefined ? scores[index] : 0, // Default to 0
  }));

  // Sortiranje igrača po scoreu
  const sortedPlayers = [...playerData].sort((a, b) => b.score - a.score);

  const medalColors = ['#FFD700', '#C0C0C0', '#CD7F32']; // Gold, Silver, Bronze

  return (
    <div className="leaderboard-container">
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
      <h1 className="leaderboard-title">Leaderboard</h1>
      <table className="leaderboard-table">
        <thead>
          <tr>
            <th>Rank</th>
            <th>Player</th>
            <th>Score</th>
          </tr>
        </thead>
        <tbody>
          {sortedPlayers.map((player, index) => (
            <tr key={index} className="leaderboard-row">
              <td>
                {index < 3 ? (
                  <FontAwesomeIcon
                    icon={faMedal}
                    style={{ color: medalColors[index], marginRight: '8px' }}
                  />
                ) : (
                  index + 1
                )}
              </td>
              <td>{player.name}</td>
              <td>{player.score}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default Leaderboard;
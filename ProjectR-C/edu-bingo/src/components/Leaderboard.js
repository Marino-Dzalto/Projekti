import { faMedal } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useEffect, useState } from 'react';
import '../styles/Leaderboard.css';

const Leaderboard = ({ gameId }) => {
  const [playerData, setPlayerData] = useState([]);

  useEffect(() => {
      const fetchLeaderboard = async () => {
        try {
          const response = await fetch(`/api/leaderboard/${gameId}`);
          if (response.ok) {
            const data = await response.json();
            const playerData = data.leaderboard.map((player, index) => ({
              name: player.username || `Player ${index + 1}`,
              score: player.total_score
            }));
            setPlayerData(playerData);
          } else {
            console.error('Failed to fetch leaderboard');
          }
        } catch (error) {
          console.error('Error fetching leaderboard', error);
        }
      };
      fetchLeaderboard();
    }, [gameId]);

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
          {playerData.map((player, index) => (
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
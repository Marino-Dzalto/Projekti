import React, { useState, useEffect } from 'react';
import '../styles/GameBoard.css';

const GameBoard = () => {
  const [cards, setCards] = useState([]);
  const [randomNumber, setRandomNumber] = useState(null);
  const [selectedCardIndex, setSelectedCardIndex] = useState(null);
  const [answer, setAnswer] = useState('');
  const [score, setScore] = useState(0);

  // Fetch pitanja s backenda
  useEffect(() => {
    const fetchTasks = async () => {
      try {
        const response = await fetch('/api/tasks');
        if (!response.ok) throw new Error("Failed to fetch tasks");

        const data = await response.json();
        
        const initialCards = Array.from({ length: 9 }, (_, index) => ({
          id: index,
          number: Math.floor(Math.random() * 90) + 1,
          task: data[index],
          flipped: false,
          points: data[index]?.difficulty || 1
        }));
        
        console.log('Cards:', initialCards);
        setCards(initialCards);

      } catch (error) {
        console.error('Error fetching tasks:', error);

        // defaulte data dok se backend ne izgradi
        const defaultCards = Array.from({ length: 9 }, (_, index) => ({
          id: index,
          number: Math.floor(Math.random() * 90) + 1,
          task: { 
            question: "Placeholder Question", 
            answer: "Placeholder Answer", 
            type: "text" 
          },
          flipped: false,
          points: 1
        }));

        setCards(defaultCards);
      }
    };

    fetchTasks();
  }, []);
  const generateRandomNumber = () => {
    setRandomNumber(Math.floor(Math.random() * 90) + 1);
  };


  // flipamo kartu ako task bude pogođen
  const handleFlipCard = (index) => {
    if (cards[index].number === randomNumber && !cards[index].flipped) {
      setSelectedCardIndex(index);
      setCards((prevCards) =>
        prevCards.map((card, i) => i === index ? { ...card, flipped: true } : card)
      );
    }
  };

  // kad igrač odgovori
  const handleSubmitAnswer = () => {
    const card = cards[selectedCardIndex];
    if (!card) return;

    const isCorrect =
      (card.task.type === 'text' && answer.toLowerCase() === card.task.answer.toLowerCase()) ||
      (card.task.type === 'number' && answer === card.task.answer) ||
      (card.task.type === 'multiple' && answer === card.task.answer);

    if (isCorrect) {
      setScore((prevScore) => prevScore + card.points);
      setCards((prevCards) =>
        prevCards.map((c, i) => (i === selectedCardIndex ? { ...c, completed: true } : c))
      );
    }

    setAnswer('');
    setSelectedCardIndex(null);
  };

  const closeModal = () => {
    setSelectedCardIndex(null);
    setAnswer('');
  };

  return (
    <div className="game-board">
      <div className="game-info">
        <h2>Score: {score}</h2>
        <button onClick={generateRandomNumber}>Novi broj</button>
        {randomNumber && <p>Moj novi broj {randomNumber}</p>}
      </div>

      <div className="card-grid">
        {cards.map((card, index) => (
          <div
            key={card.id}
            className={`card ${card.flipped ? 'flipped' : ''}`}
            onClick={() => handleFlipCard(index)}
          >
            <p>Redni broj: {card.number}</p>
          </div>
        ))}
      </div>

      {/* Modal za otvaranje prozora pitanja */}
      {selectedCardIndex !== null && (
        <div className="modal-overlay active">
          <div className="modal-content">
            <span className="close-button" onClick={closeModal}>&times;</span>
            <p>{cards[selectedCardIndex].task.question}</p>
            {!cards[selectedCardIndex].completed && (
              <>
                {cards[selectedCardIndex].task.type === 'text' && (
                  <input
                    type="text"
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                  />
                )}
                {cards[selectedCardIndex].task.type === 'number' && (
                  <input
                    type="number"
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                  />
                )}
                {cards[selectedCardIndex].task.type === 'multiple' && (
                  <select
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                  >
                    <option value="">Izaberi odgovor</option>
                    {cards[selectedCardIndex].task.options.map((opt, i) => (
                      <option key={i} value={opt}>{opt}</option>
                    ))}
                  </select>
                )}
                <button onClick={handleSubmitAnswer}>Potvrdi odgovor</button>
              </>
            )}
            {cards[selectedCardIndex].completed && <p>Rješena kartica!</p>}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameBoard;

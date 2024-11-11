import React, { useState, useEffect } from 'react';
import '../styles/GameBoard.css';

const GameBoard = () => {
  const [cards, setCards] = useState([]);
  const [randomNumber, setRandomNumber] = useState(null);
  const [selectedCardIndex, setSelectedCardIndex] = useState(null);
  const [answer, setAnswer] = useState('');
  const [score, setScore] = useState(0);

  useEffect(() => {
    const initialCards = Array.from({ length: 9 }, (_, index) => ({
      id: index,
      number: Math.floor(Math.random() * 90) + 1,
      task: getTask(), // Mock task
      flipped: false,
      points: getTaskPoints() // Mock task tezina
    }));
    setCards(initialCards);
  }, []);

  const generateRandomNumber = () => {
    setRandomNumber(Math.floor(Math.random() * 90) + 1);
  };

  // Mock task generator
  const getTask = () => {
    const tasks = [
      { type: 'text', question: 'What is the capital of France?', answer: 'Paris' },
      { type: 'number', question: 'What is 5 + 3?', answer: '8' },
      { type: 'multiple', question: 'Which one is a mammal?', options: ['Shark', 'Elephant', 'Eagle'], answer: 'Elephant' }
    ];
    return tasks[Math.floor(Math.random() * tasks.length)];
  };

  // Mock task points
  const getTaskPoints = () => {
    const difficulties = [1, 2, 3]; // Easy, medium, hard
    return difficulties[Math.floor(Math.random() * difficulties.length)];
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
            {card.flipped ? (
              <div className="card-content">
                <p>{card.task.question}</p>
                {!card.completed && (
                  <>
                    {card.task.type === 'text' && (
                      <input
                        type="text"
                        value={answer}
                        onChange={(e) => setAnswer(e.target.value)}
                      />
                    )}
                    {card.task.type === 'number' && (
                      <input
                        type="number"
                        value={answer}
                        onChange={(e) => setAnswer(e.target.value)}
                      />
                    )}
                    {card.task.type === 'multiple' && (
                      <select
                        value={answer}
                        onChange={(e) => setAnswer(e.target.value)}
                      >
                        <option value="">Izaberi odgovor</option>
                        {card.task.options.map((opt, i) => (
                          <option key={i} value={opt}>{opt}</option>
                        ))}
                      </select>
                    )}
                    <button onClick={handleSubmitAnswer}>Potvrdi odgovor</button>
                  </>
                )}
                {card.completed && <p>Rješena kartica!</p>}
              </div>
            ) : (
              <p>Redni broj: {card.number}</p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default GameBoard;

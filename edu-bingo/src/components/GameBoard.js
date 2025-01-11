import React, { useEffect, useState } from 'react';
import mickey from "../mickey.png";
import minnie from "../minnie.png";
import '../styles/GameBoard.css';

const GameBoard = ({questions }) => {
  const [cards, setCards] = useState([]);
  const [randomNumber, setRandomNumber] = useState(null);
  const [selectedCardIndex, setSelectedCardIndex] = useState(null);
  const [answer, setAnswer] = useState('');
  const [score, setScore] = useState(0);

  //izgeneriramo 9 jedinstvenih brojeva
  const generateUniqueCardNumbers = (count) => {
    const uniqueNumbers = new Set();
    while (uniqueNumbers.size < count) {
      const randomNumber = Math.floor(Math.random() * 90) + 1;
      uniqueNumbers.add(randomNumber);
    }
    return Array.from(uniqueNumbers);
  };

  useEffect(() => {
    const uniqueCardNumbers = generateUniqueCardNumbers(9);

    const initialCards = questions.slice(0, 9).map((question, index) => ({
      id: index,
      number: uniqueCardNumbers[index],
      task: question,
      flipped: false,
      points: question.difficulty === 'high' ? 3 : 1,
    }));

    setCards(initialCards);
  }, [questions]);

  const generateRandomNumber = () => {
    setRandomNumber(Math.floor(Math.random() * 90) + 1);
  };

  useEffect(() => {
    console.log(questions);
  });

  // kad igrač odgovori
  const handleSubmitAnswer = () => {
    console.log(answer);
    console.log(selectedCardIndex)
    const card = cards[selectedCardIndex];
    if (!card) return;

    const isCorrect =
    (card.task.answer.type === 'written' && answer.toLowerCase() === card.task.answer.correct_answer.toLowerCase()) ||
    (card.task.answer.type === 'numerical' && Number(answer) === card.task.answer.correct_answer) ||
    (card.task.answer.type === 'multiple choice' && answer === card.task.answer.correct_answer);


    if (isCorrect) {
      // povecaj score prema difficultyu
      let points = 0;
      if (card.task.difficulty === 'high') {
        points = 3;
      } else if (card.task.difficulty === 'medium') {
        points = 2;
      } else if (card.task.difficulty === 'easy') {
        points = 1;
      }
  
      setScore((prevScore) => prevScore + points);
  
      // oznacimo karticu kao completed
      setCards((prevCards) =>
        prevCards.map((c, i) =>
          i === selectedCardIndex ? { ...c, completed: true } : c
        )
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
            onClick={() => setSelectedCardIndex(index)}
          >
            <p>{card.completed ? 'Rješena kartica' : `Redni broj: ${card.number}`}</p>
          </div>
        ))}
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

      {/* Modal za otvaranje prozora pitanja */}
      {selectedCardIndex !== null && (
        <div className="modal-overlay active">
          <div className="modal-content">
            <span className="close-button" onClick={closeModal}>&times;</span>
            <p>{cards[selectedCardIndex].task.question}</p>
            {!cards[selectedCardIndex].completed && (
              <>
                {cards[selectedCardIndex].task.answer.type === 'written' && (
                  <input
                    type="text"
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                  />
                )}
                {cards[selectedCardIndex].task.answer.type === 'numerical' && (
                  <input
                    type="number"
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                  />
                )}
                {cards[selectedCardIndex].task.answer.type === 'multiple choice' && (
                  <div className="options-container">
                    {['a', 'b', 'c'].map((letter) => (
                      <div
                        key={letter}
                        className={`option-box ${
                          answer === letter ? 'selected' : ''
                        }`}
                        onClick={() => setAnswer(letter)}
                      >
                        {cards[selectedCardIndex].task.answer[`option_${letter}`]}
                      </div>
                    ))}
                  </div>
                )}
                <button onClick={handleSubmitAnswer}>Potvrdi odgovor</button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameBoard;

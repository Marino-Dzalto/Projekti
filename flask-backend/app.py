import os
from random import randrange
from datetime import datetime
from dotenv import load_dotenv
from flask import Flask, request, jsonify
from flask_cors import CORS
from db_models import (
    db,
    Task,
    NumericalAnswer,
    WrittenAnswer,
    MultipleChoiceAnswer,
    Teacher,
    Game,
    Student,
    Score,
)


load_dotenv()

app = Flask(__name__)
CORS(app)
app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URI")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

db.init_app(app)


@app.post("/api/create-room")
def create_game():
    data = request.get_json()
    name = data["adminName"]
    password = data["adminSurname"]

    teacher = Teacher.query.filter_by(name=name, password=password).first()

    if not teacher:
        return {"message": "Teacher not found"}, 404

    new_game = Game(
        teacher_id=teacher.teacher_id,
        topic_selected=data["topic"],
        game_code=data["gameCode"],
        start_time=datetime.now(),
        end_time=datetime.now(),
        is_locked=False,
    )

    db.session.add(new_game)
    db.session.commit()

    return {"gameId": new_game.game_id}, 200


@app.get("/api/players/<string:game_id>")
def get_players(game_id):
    return jsonify(
        [
            {"name": "Player" + str(randrange(10))},
            {"name": "Player" + str(randrange(10))},
        ]
    )

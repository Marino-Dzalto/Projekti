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


@app.post("/api/verify-teacher")
def verify_teacher():
    data = request.get_json()
    name = data["adminUsername"]
    password = data["adminPass"]

    teacher = Teacher.query.filter_by(name=name, password=password).first()

    if not teacher:
        return {"message": "Teacher not found"}, 404
    
    return {"teacher_id": teacher.teacher_id}, 200


@app.post("/api/create-game")
def create_game():
    data = request.get_json()

    new_game = Game(
        teacher_id=data["teacher_id"],
        game_code=data["data"]["gameCode"],
        start_time=datetime.now(),
        is_locked=False,
    )

    db.session.add(new_game)
    db.session.commit()

    return {
        "game_id": new_game.game_id,
        "gameCode": new_game.game_code
    }, 200


@app.post("/api/set-topic/<string:game_id>")
def set_topic(game_id):
    data = request.get_json()

    game = Game.query.filter_by(game_id=game_id, game_code=data["gameCode"]).first()

    if not game:
        return {"message": "No such game in database"}, 404

    game.topic_selected = data["topic"]
    db.session.commit()

    return {
        "game_id": game.game_id,
        "message": "Game topic set"
    }, 200


@app.post("/api/lock-room/<string:game_id>")
def lock_room(game_id):
    game = Game.query.filter_by(game_id=game_id).first()

    if not game:
        return {"message": "No such game in database"}, 404

    game.is_locked = True
    db.session.commit()

    return {
        "game_id": game.game_id,
        "message": "Game locked"
    }, 200


@app.get("/api/players/<string:game_id>")
def get_players(game_id):
    players = Student.query.filter_by(game_id=game_id).all()

    players_arr = [
        {
            "student_id": p.temp_student_id,
            "name": p.username
        } for p in players
    ]

    return jsonify({"players": players_arr}), 200


@app.get("/api/subjects")
def get_subjects():
    # trenutno samo placeholderi da radi, inace treba iz baze povuc
    return jsonify(
        [
            {"name": "Math"},
            {"name": "Science"},
            {"name": "Chemistry"}
        ]
    )


@app.get("/api/topics")
def get_topics():
    subject = request.args.get('subject')

    # trenutno samo placeholderi da radi, inace treba iz baze povuc
    return jsonify(
        [
            {
                "name": "Multiplication",
                "subject": "Math"
            },
            {
                "name": "Division",
                "subject": "Math"
            },
            {
                "name": "Voltage",
                "subject": "Science"
            },
            {
                "name": "Atomic Numbers",
                "subject": "Chemistry"
            }
        ]
    )


@app.post("/api/[PH]create-player")
def create_player():
    data = request.get_json()

    new_student = Student(
        game_id=data["adminData"]["game_id"],
        username=data["name"],
        start_time=datetime.now(),
        end_time=datetime.now(),
        is_active=True
    )

    db.session.add(new_student)
    db.session.commit()

    return {
        "student_id": new_student.temp_student_id,
        "message": "student created"
    }, 200
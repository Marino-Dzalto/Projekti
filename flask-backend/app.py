import os
import bcrypt
import base64
from datetime import datetime
from dotenv import load_dotenv
from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_socketio import SocketIO, emit, join_room, leave_room
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
    Subject,
    Topic,
)


load_dotenv()

app = Flask(__name__)
CORS(app)
app.config["SQLALCHEMY_DATABASE_URI"] = os.getenv("DATABASE_URI")
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

db.init_app(app)

socketio = SocketIO(app, cors_allowed_origins="*")


@app.post("/api/verify-teacher")
def verify_teacher():
    data = request.get_json()
    username = data["adminUsername"]
    password = data["adminPass"]

    teacher = Teacher.query.filter_by(username=username).first()

    if not teacher:
        return {"message": "Teacher username not found"}, 404

    if verify_password(password, teacher.password):
        return {"teacher_id": str(teacher.teacher_id)}, 200
    else:
        return {"message": "Invalid password"}, 401


@app.post("/api/create-game")
def create_game():
    data = request.get_json()

    new_game = Game(
        teacher_id=data["teacher_id"],
        game_code=data["data"]["gameCode"],
        start_time=datetime.now(),
        is_locked=False,
        max_players=data["data"]["numPlayers"],
    )

    db.session.add(new_game)
    db.session.commit()

    return {
        "game_id": str(new_game.game_id),
        "game_code": new_game.game_code,
    }, 200


@app.post("/api/join-game/<string:code>")
def join_game(code):
    player_name = request.get_json()

    game = Game.query.filter(
        Game.game_code == code, Game.end_time.is_(None), Game.is_locked == False
    ).first()

    if not game:
        return {"message": "Game not found"}, 404

    if (
        len(
            Student.query.filter(
                Student.game_id == game.game_id,
                Student.is_active == True,
                Student.end_time.is_(None),
            ).all()
        )
        >= game.max_players
    ):
        return {"message": "Game is full"}, 404

    new_student = Student(
        game_id=game.game_id,
        username=player_name,
        start_time=datetime.now(),
        is_active=True,
    )

    db.session.add(new_student)
    db.session.commit()

    players = Student.query.filter(
        Student.game_id == game.game_id,
        Student.is_active == True,
        Student.end_time.is_(None),
    ).all()
    teacher = Teacher.query.filter_by(teacher_id=game.teacher_id).first()

    players_arr = [
        {"student_id": str(p.temp_student_id), "name": p.username} for p in players
    ]

    socketio.emit("updatePlayers", {"players": players_arr}, room=game.game_id)

    return (
        jsonify(
            {
                "players": players_arr,
                "game_id": str(game.game_id),
                "game_code": game.game_code,
                "teacher_name": teacher.username,
            }
        ),
        200,
    )


@app.post("/api/set-topic/<string:game_id>")
def set_topic(game_id):
    data = request.get_json()

    game = Game.query.filter_by(game_id=game_id, game_code=data["gameCode"]).first()

    if not game:
        return {"message": "No such game in database"}, 404

    game.topic_selected = data["topic"]["topic_id"]
    db.session.commit()

    return {"game_id": str(game.game_id), "message": "Game topic set"}, 200


@app.post("/api/lock-room/<string:game_id>")
def lock_room(game_id):
    game = Game.query.filter_by(game_id=game_id).first()

    if not game:
        return {"message": "No such game in database"}, 404

    game.is_locked = True
    db.session.commit()

    return {"game_id": str(game.game_id), "message": "Game locked"}, 200


@app.get("/api/players/<string:game_id>")
def get_players(game_id):
    players = Student.query.filter(
        Student.game_id == game_id,
        Student.end_time.is_(None),
        Student.is_active == True,
    ).all()

    players_arr = [
        {"student_id": str(p.temp_student_id), "name": p.username} for p in players
    ]

    return jsonify({"players": players_arr}), 200


@app.get("/api/subjects")
def get_subjects():
    subjects = Subject.query.all()

    subjects_arr = [{"subject_id": str(s.subject_id), "name": s.name} for s in subjects]

    return jsonify(subjects_arr), 200


@app.get("/api/topics")
def get_topics():
    subject_id = request.args.get("subject")

    topics = Topic.query.filter_by(subject_id=subject_id).all()

    topics_arr = [
        {"topic_id": str(t.topic_id), "subject_id": str(t.subject_id), "name": t.name}
        for t in topics
    ]

    return jsonify(topics_arr), 200


@socketio.on("adminJoin")
def handle_admin_join(data):
    game_id = data["game_id"]

    join_room(game_id)


@socketio.on("joinGame")
def handle_join_room(data):
    game_code = data["game_code"]
    player_name = data["player_name"]

    game = Game.query.filter(
        Game.game_code == game_code, Game.end_time.is_(None), Game.is_locked == False
    ).first()

    if not game:
        emit("error", {"message": "Game not found or locked"}, to=request.sid)
        return

    student = Student.query.filter_by(
        game_id=game.game_id, username=player_name
    ).first()

    if student:
        student.socket_id = request.sid
        db.session.commit()

    join_room(str(game.game_id))
    handle_update_players({"game_id": str(game.game_id)})


@socketio.on("leaveGame")
def handle_leave_game(data):
    # implementirati kad bude zatrebalo
    ...


@socketio.on("updatePlayers")
def handle_update_players(data):
    game_id = data["game_id"]

    game = Game.query.filter(
        Game.game_id == game_id, Game.end_time.is_(None), Game.is_locked == False
    ).first()

    if not game:
        emit("error", {"message": "Game not found or locked"}, to=request.sid)
        return

    players = [
        {"student_id": str(p.temp_student_id), "name": p.username}
        for p in Student.query.filter(
            Student.game_id == game.game_id,
            Student.is_active == True,
            Student.end_time.is_(None),
        ).all()
    ]

    emit("updatePlayers", {"players": players}, room=game_id)


@socketio.on("disconnect")
def handle_player_disconnect():
    student = Student.query.filter_by(socket_id=request.sid).first()

    if student:
        student.is_active = False
        student.end_time = datetime.now()

        db.session.commit()

        leave_room(str(student.game_id))
        handle_update_players({"game_id": str(student.game_id)})


@socketio.on("chatMessage")
def handle_chat_message(data):
    game_code = data["game_code"]
    message = data["message"]
    timestamp = datetime.now().strftime("%H:%M:%S")

    game = Game.query.filter(
        Game.game_code == game_code, Game.end_time.is_(None), Game.is_locked == False
    ).first()

    if game:
        emit(
            "chatMessage",
            {"message": message, "timestamp": timestamp},
            room=str(game.game_id),
        )


def hash_password(password):
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode("utf-8"), salt)

    return base64.b64encode(hashed).decode("utf-8")


def verify_password(provided, hashed):
    hashed_bytes = base64.b64decode(hashed)

    return bcrypt.checkpw(provided.encode("utf-8"), hashed_bytes)


if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", port=5000, debug=True)

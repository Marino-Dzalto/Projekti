import os
from dotenv import load_dotenv
from flask import Flask, request
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


@app.post("/create-game")
def create_game():
    data = request.get_json()
    name = data["adminName"]
    email = data["adminSurname"]

    teacher = Teacher.query.filter_by(name=name, email=email).first()

    if teacher:
        return {"message": "Teacher in database"}, 200
    else:
        return {"message": "Teacher not in database"}, 404

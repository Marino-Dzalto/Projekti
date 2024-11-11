import os
from random import randrange
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
    ...


@app.get("/api/players/<string:game_id>")
def get_players(game_id):
    return jsonify([{ 'name': 'Player' + str(randrange(10)) }, { 'name': 'Player' + str(randrange(10)) }])
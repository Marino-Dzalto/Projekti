import os
import psycopg2
from dotenv import load_dotenv
from flask import Flask, request
from flask_cors import CORS


load_dotenv()

app = Flask(__name__)
CORS(app)
url = os.getenv("DATABASE_URL")
connection = psycopg2.connect(url)


@app.get("/")
def home():
    return "Hello world!"

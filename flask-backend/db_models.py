import uuid
from datetime import datetime
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import (
    CHAR,
    CheckConstraint,
    Column,
    DateTime,
    ForeignKey,
    Integer,
    String,
    text,
    Boolean,
)
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import UUID


db = SQLAlchemy()


class Task(db.Model):
    __tablename__ = "task"
    __table_args__ = (
        CheckConstraint(
            "(difficulty)::text = ANY ((ARRAY['easy'::character varying, 'medium'::character varying, 'high'::character varying])::text[])"
        ),
    )

    task_id = Column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    question = Column(String(255), nullable=False)
    subject_id = Column(ForeignKey("subjects.subject_id", ondelete="SET NULL"))
    topic_id = Column(ForeignKey("topics.topic_id", ondelete="SET NULL"))
    difficulty = Column(String(10))

    multiple_choice_answers = relationship(
        "MultipleChoiceAnswer", back_populates="task", cascade="all, delete-orphan"
    )
    numerical_answers = relationship(
        "NumericalAnswer", back_populates="task", cascade="all, delete-orphan"
    )
    written_answers = relationship(
        "WrittenAnswer", back_populates="task", cascade="all, delete-orphan"
    )

    subject = relationship("Subject")
    topic = relationship("Topic")


class MultipleChoiceAnswer(db.Model):
    __tablename__ = "multiple_choice_answer"

    task_id = Column(ForeignKey("task.task_id", ondelete="CASCADE"), primary_key=True)
    option_a = Column(String(255))
    option_b = Column(String(255))
    option_c = Column(String(255))
    correct_answer = Column(CHAR(1), nullable=False)

    task = relationship("Task", back_populates="multiple_choice_answers")


class NumericalAnswer(db.Model):
    __tablename__ = "numerical_answer"

    task_id = Column(ForeignKey("task.task_id", ondelete="CASCADE"), primary_key=True)
    correct_answer = Column(Integer, nullable=False)

    task = relationship("Task", back_populates="numerical_answers")


class WrittenAnswer(db.Model):
    __tablename__ = "written_answer"

    task_id = Column(ForeignKey("task.task_id", ondelete="CASCADE"), primary_key=True)
    correct_answer = Column(String(255), nullable=False)

    task = relationship("Task", back_populates="written_answers")


class Teacher(db.Model):
    __tablename__ = "teacher"

    teacher_id = Column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    username = Column(String(25), nullable=False, unique=True)
    email = Column(String(50), nullable=False, unique=True)
    password = Column(String(100), nullable=False)
    date_registered = Column(DateTime, server_default=text("now()"))


class Game(db.Model):
    __tablename__ = "game"

    game_id = Column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    teacher_id = Column(ForeignKey("teacher.teacher_id", ondelete="CASCADE"))
    topic_selected = Column(
        ForeignKey("topics.topic_id", ondelete="SET NULL"), nullable=True
    )
    game_code = Column(String(100), nullable=False, unique=True)
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=True)
    is_locked = Column(Boolean, nullable=False)
    max_players = Column(Integer)

    teacher = relationship("Teacher")
    topic = relationship("Topic")


class Student(db.Model):
    __tablename__ = "student"

    temp_student_id = Column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    game_id = Column(ForeignKey("game.game_id", ondelete="CASCADE"))
    username = Column(String(100), nullable=False)
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=True)
    is_active = Column(Boolean, nullable=False)
    socket_id = Column(String(255), nullable=True)

    game = relationship("Game")


class Score(db.Model):
    __tablename__ = "scores"
    __table_args__ = (CheckConstraint("score >= 0"),)

    score_id = Column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    game_id = Column(ForeignKey("game.game_id", ondelete="CASCADE"))
    temp_user_id = Column(ForeignKey("student.temp_student_id", ondelete="CASCADE"))
    task_id = Column(ForeignKey("task.task_id", ondelete="CASCADE"))
    score = Column(Integer, nullable=False)

    game = relationship("Game")
    task = relationship("Task")
    temp_user = relationship("Student")


class Topic(db.Model):
    __tablename__ = "topics"

    topic_id = Column(UUID(as_uuid=True), primary_key=True)
    name = Column(String(100), nullable=False, unique=True)
    subject_id = Column(ForeignKey("subjects.subject_id", ondelete="CASCADE"))

    subject = relationship("Subject")


class Subject(db.Model):
    __tablename__ = "subjects"

    subject_id = Column(UUID(as_uuid=True), primary_key=True)
    name = Column(String(100), nullable=False, unique=True)

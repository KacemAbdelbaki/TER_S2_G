"""
db.py - MongoDB Atlas connection and maze persistence.

Provides save, rate, retrieve, and list operations on the ``mazes`` collection.
Connection string is read from the MONGO_URI environment variable.
If MongoDB is unreachable, save_maze returns None so the API still works.
"""

import os
import logging
from datetime import datetime, timezone

from pymongo import MongoClient
from pymongo.errors import PyMongoError
from bson import ObjectId
from bson.errors import InvalidId

log = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Connection  (module-level singleton — standard for Flask single-process)
# ---------------------------------------------------------------------------

MONGO_URI = os.environ.get(
    "MONGO_URI",
    "mongodb+srv://danielao:SqMZ0sl5cU7GAkCy@pacmanter.atzvzck.mongodb.net/pacman_ter?appName=pacmanTer",
)

try:
    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
    db = client["pacman_ter"]
    mazes_collection = db["mazes"]
except PyMongoError as exc:
    log.warning("MongoDB connection failed: %s — persistence disabled", exc)
    client = None
    db = None
    mazes_collection = None


# ---------------------------------------------------------------------------
# CRUD helpers
# ---------------------------------------------------------------------------

def save_maze(maze_dict, algorithm, height, width):
    """
    Persist a generated maze to MongoDB.

    Returns the inserted document's string ID, or *None* if the DB is
    unavailable (the API still returns the maze without an ID).
    """
    if mazes_collection is None:
        return None
    try:
        doc = {
            "algorithm": algorithm,
            "height": height,
            "width": width,
            "maze": maze_dict,
            "rating": None,
            "rating_count": 0,
            "rating_sum": 0,
            "created_at": datetime.now(timezone.utc),
        }
        result = mazes_collection.insert_one(doc)
        return str(result.inserted_id)
    except PyMongoError as exc:
        log.warning("save_maze failed: %s", exc)
        return None


def rate_maze(maze_id, rating):
    """
    Rate a maze (0-5).  Uses incremental average.

    Returns ``{maze_id, new_rating, total_votes}`` or *None* if not found.

    :raises ValueError: if *rating* is outside 0-5
    :raises InvalidId:  if *maze_id* is not a valid ObjectId string
    """
    if mazes_collection is None:
        return None

    if not (0 <= rating <= 5):
        raise ValueError("Rating must be between 0 and 5")

    oid = ObjectId(maze_id)  # may raise InvalidId

    doc = mazes_collection.find_one({"_id": oid})
    if doc is None:
        return None

    new_count = doc.get("rating_count", 0) + 1
    new_sum = doc.get("rating_sum", 0) + rating
    new_avg = round(new_sum / new_count, 2)

    mazes_collection.update_one(
        {"_id": oid},
        {"$set": {
            "rating": new_avg,
            "rating_count": new_count,
            "rating_sum": new_sum,
        }},
    )
    return {"maze_id": maze_id, "new_rating": new_avg, "total_votes": new_count}


def get_maze_by_id(maze_id):
    """Retrieve a full maze document by its string ID (returns None if missing)."""
    if mazes_collection is None:
        return None
    try:
        oid = ObjectId(maze_id)
    except InvalidId:
        return None
    doc = mazes_collection.find_one({"_id": oid})
    if doc:
        doc["_id"] = str(doc["_id"])
    return doc


def list_mazes(limit=50):
    """List recent mazes (metadata only — heavy ``maze`` field excluded)."""
    if mazes_collection is None:
        return []
    cursor = (
        mazes_collection.find({}, {"maze": 0})
        .sort("created_at", -1)
        .limit(limit)
    )
    results = []
    for doc in cursor:
        doc["_id"] = str(doc["_id"])
        results.append(doc)
    return results

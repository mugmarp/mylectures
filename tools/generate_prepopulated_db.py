#!/usr/bin/env python3
"""
Build app/src/main/assets/mustime_prepopulated.db — a Room-valid prepackaged
database for AppDatabase v9.

Schema authority: the JSON Room itself exports (app/schemas/<db>/9.json).
This script reads that file and executes Room's own createSql statements with
${TABLE_NAME} substituted, so the asset can never drift from the entities.

Usage:
    python3 tools/generate_prepopulated_db.py --schemas <dir> [--check]

Exit codes:
    0 success
    1 schema JSON missing / unusable
    2 generated asset failed self-validation
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_SCHEMA_DIR = REPO / "app" / "schemas"
JSON_ASSET = REPO / "app" / "src" / "main" / "assets" / "timetable_export.json"
METADATA_ASSET = REPO / "app" / "src" / "main" / "assets" / "timetable_metadata.json"
ROOM_DIRECTORY = (
    REPO / "app" / "src" / "main" / "java" / "com" / "mustime"
    / "features" / "rooms" / "RoomDirectory.kt"
)
OUTPUT_DB = REPO / "app" / "src" / "main" / "assets" / "mustime_prepopulated.db"

DB_VERSION = 10

# Mirrors BuildingLevel.floorNumber in RoomDirectory.kt
LEVEL_TO_FLOOR = {
    "GROUND": 0, "FIRST": 1, "SECOND": 2, "THIRD": 3, "FOURTH": 4,
}


# --------------------------------------------------------------------------
# Room directory parsing — replicates UniversityDirectory.resolveLevel()
# --------------------------------------------------------------------------

def _normalize(s: str) -> str:
    """Mirror of RoomItem.normalize()."""
    return re.sub(r"\s+", " ", s.lower().replace("-", " ")).strip()


def load_room_levels(kt_path: Path) -> list[tuple[set[str], int]]:
    """
    Parse ALL_ROOMS out of RoomDirectory.kt and return
    [(set_of_identifiers, floorNumber), ...] in declaration order.

    Identifiers are code, name, and every alias — matching the strings
    RoomItem.matchesRoomString() compares against.
    """
    text = kt_path.read_text(encoding="utf-8")
    start = text.index("val ALL_ROOMS: List<RoomItem> = listOf(")
    # Walk braces to find the end of the listOf( ... ) call.
    depth = 0
    i = text.index("listOf(", start) + len("listOf(")
    end = i
    while i < len(text):
        ch = text[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            if depth == 0:
                end = i
                break
            depth -= 1
        i += 1
    body = text[start:end]

    rooms: list[tuple[set[str], int]] = []
    for block in re.finditer(r"RoomItem\((.*?)\n\s{8}\)", body, re.S):
        b = block.group(1)
        code = re.search(r'\bcode\s*=\s*"([^"]*)"', b)
        name = re.search(r'\bname\s*=\s*"([^"]*)"', b)
        level = re.search(r"\blevel\s*=\s*BuildingLevel\.(\w+)", b)
        if not level:
            continue
        aliases: set[str] = set()
        am = re.search(r"aliases\s*=\s*listOf\(([^)]*)\)", b)
        if am:
            aliases |= {a for a in re.findall(r'"([^"]*)"', am.group(1))}
        ident = set(aliases)
        if code:
            ident.add(code.group(1))
        if name:
            ident.add(name.group(1))
        ident = {x for x in ident if x.strip()}
        rooms.append((ident, LEVEL_TO_FLOOR[level.group(1)]))
    return rooms


# Heuristic fallback — mirror of UniversityDirectory.resolveLevel()'s regexes.
_LEVEL_PATTERNS = [
    (0, [r".*\b(GROUND\s+FLOOR|GRND\s+FLOOR|GROUND|GRND|GF|LG)\b.*", r".*[A-Z]?G\d{1,3}\b.*"]),
    (4, [r".*\b(4TH|FOURTH|LEVEL\s+4|FLOOR\s+4|L4)\b.*", r".*[A-Z]?4\d{2}\b.*"]),
    (3, [r".*\b(3RD|THIRD|LEVEL\s+3|FLOOR\s+3|L3)\b.*", r".*[A-Z]?3\d{2}\b.*"]),
    (2, [r".*\b(2ND|SECOND|LEVEL\s+2|FLOOR\s+2|L2)\b.*", r".*[A-Z]?2\d{2}\b.*"]),
    (1, [r".*\b(1ST|FIRST|LEVEL\s+1|FLOOR\s+1|L1)\b.*", r".*[A-Z]?1\d{2}\b.*"]),
]
_NON_ROOMS = {"tba", "tbd", "none", "n/a"}


def resolve_floor(room: str | None, rooms: list[tuple[set[str], int]]) -> int | None:
    """Mirror of UniversityDirectory.resolveLevel(room)?.floorNumber."""
    if not room or not room.strip():
        return None
    clean = room.strip()
    if clean.lower() in _NON_ROOMS:
        return None

    # 1. Directory lookup (findRoom -> matchesRoomString)
    clean_norm = _normalize(clean)
    for ident, floor in rooms:
        for candidate in ident:
            if candidate.strip().lower() == clean.lower():
                return floor
        for candidate in ident:
            if _normalize(candidate) == clean_norm:
                return floor
        for candidate in ident:
            parts = [p.strip() for p in re.split(r"[/,;&]", clean_norm)]
            if _normalize(candidate) in parts:
                return floor

    # 2. Heuristics
    upper = clean.upper()
    for floor, pats in _LEVEL_PATTERNS:
        if any(re.fullmatch(p, upper) for p in pats):
            return floor
    return None


# --------------------------------------------------------------------------
# Schema-driven database construction
# --------------------------------------------------------------------------

def load_schema(schema_dir: Path) -> dict:
    path = schema_dir / "com.mustime.core.database.AppDatabase" / f"{DB_VERSION}.json"
    if not path.exists():
        sys.exit(
            f"ERROR: exported schema not found at {path}\n"
            "Run a build first (CI uploads it as the 'room-schemas' artifact), then\n"
            "pass --schemas <dir> pointing at the extracted schema root."
        )
    return json.loads(path.read_text(encoding="utf-8"))


def create_tables(conn: sqlite3.Connection, schema: dict) -> list[str]:
    """Execute Room's own createSql. Returns the table names created."""
    created = []
    cur = conn.cursor()
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        sql = entity["createSql"].replace("${TABLE_NAME}", table)
        cur.execute(sql)
        for idx in entity.get("indices", []):
            cur.execute(idx["createSql"].replace("${TABLE_NAME}", table))
        created.append(table)
    return created


def insert_rooms(conn: sqlite3.Connection, entries: list[dict],
                 rooms: list[tuple[set[str], int]]) -> tuple[int, int]:
    """Insert timetable rows, computing `floor` exactly as the app does."""
    cur = conn.cursor()
    sql = (
        "INSERT OR REPLACE INTO timetable ("
        "natural_key, program_group, day, time_slot, start_time, end_time,"
        "course_code, course_title, session_type, lecturer, room, shared_with, floor"
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
    )
    rows = []
    resolved = 0
    for e in entries:
        floor = resolve_floor(e.get("room"), rooms)
        if floor is not None:
            resolved += 1
        shared = e.get("shared_with") or []
        rows.append((
            e.get("natural_key", ""),
            e.get("program_group", ""),
            e.get("day", ""),
            e.get("time_slot", ""),
            e.get("start_time", ""),
            e.get("end_time"),
            e.get("course_code", ""),
            e.get("course_title", ""),
            e.get("session_type"),
            e.get("lecturer"),
            e.get("room"),
            ",".join(shared),
            floor,
        ))
    cur.executemany(sql, rows)
    return len(rows), resolved


def insert_metadata(conn: sqlite3.Connection) -> None:
    """Seed the single metadata row with a real timestamp."""
    data = {}
    if METADATA_ASSET.exists():
        data = json.loads(METADATA_ASSET.read_text(encoding="utf-8"))
    cur = conn.cursor()
    cur.execute(
        "INSERT OR REPLACE INTO timetable_metadata ("
        "id, versionLabel, status, isFinal, draftNumber, academicYear, semester,"
        "releaseNotes, lastUpdated, source, totalSessions"
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        (
            1,
            data.get("version_label", "Final Timetable"),
            data.get("status", "Official Final"),
            1 if data.get("is_final", True) else 0,
            data.get("draft_number"),
            data.get("academic_year", "2026/2027"),
            data.get("semester", "Semester 1"),
            data.get("release_notes"),
            int(data.get("last_updated") or time.time() * 1000),
            data.get("source", "MUST Academic Registrar"),
            data.get("total_sessions", 1304),
        ),
    )


def finalize(conn: sqlite3.Connection, identity_hash: str) -> None:
    """
    Room requires:
      - user_version = schema version
      - room_master_table(42, identityHash) for integrity validation
    """
    conn.execute(f"PRAGMA user_version = {DB_VERSION}")
    conn.execute(
        "CREATE TABLE IF NOT EXISTS room_master_table "
        "(id INTEGER PRIMARY KEY NOT NULL, identity_hash TEXT NOT NULL)"
    )
    conn.execute(
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
        (identity_hash,),
    )
    conn.commit()
    conn.execute("PRAGMA journal_mode = DELETE")
    conn.execute("VACUUM")


def self_validate(db_path: Path, schema: dict, expected_rows: int) -> list[str]:
    """Re-open the asset and assert Room's invariants hold."""
    problems: list[str] = []
    conn = sqlite3.connect(str(db_path))
    try:
        uv = conn.execute("PRAGMA user_version").fetchone()[0]
        if uv != DB_VERSION:
            problems.append(f"user_version is {uv}, expected {DB_VERSION}")

        tables = {r[0] for r in conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table'")}
        for entity in schema["database"]["entities"]:
            t = entity["tableName"]
            if t not in tables:
                problems.append(f"missing table {t}")
                continue
            # Column names, types, notnull, pk — compare to Room's createSql.
            want = _parse_columns(entity["createSql"].replace("${TABLE_NAME}", t))
            got = [(r[1], r[2], r[3], r[5]) for r in
                   conn.execute(f"PRAGMA table_info(`{t}`)")]
            if sorted(want) != sorted(got):
                problems.append(f"column mismatch in {t}:\n    want {sorted(want)}\n    got  {sorted(got)}")

        row = conn.execute(
            "SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()
        want_hash = schema["database"]["identityHash"]
        if not row or row[0] != want_hash:
            problems.append(
                f"identity_hash mismatch: {row[0] if row else None} != {want_hash}")

        n = conn.execute("SELECT COUNT(*) FROM timetable").fetchone()[0]
        if n != expected_rows:
            problems.append(f"timetable has {n} rows, expected {expected_rows}")

        nulls = conn.execute("SELECT COUNT(*) FROM timetable WHERE floor IS NULL").fetchone()[0]
        if nulls == expected_rows:
            problems.append(f"every floor is NULL ({nulls}/{expected_rows}) — resolution broken")

        side = [p for p in (db_path.with_name(db_path.name + s)
                            for s in ("-wal", "-shm")) if p.exists()]
        if side:
            problems.append(f"leftover journal files: {[p.name for p in side]}")
    finally:
        conn.close()
    return problems


def _parse_columns(create_sql: str) -> list[tuple[str, str, int, int]]:
    """
    Extract (name, type, notnull, pk) tuples from a CREATE TABLE statement.

    Handles both inline `x TEXT PRIMARY KEY` and the table-level
    `PRIMARY KEY(x)` / `PRIMARY KEY(x, y)` form that Room emits, so the result
    matches what `PRAGMA table_info` reports.
    """
    inner = create_sql[create_sql.index("(") + 1:create_sql.rindex(")")]
    parts = _split_top_level(inner)

    # Collect table-level PRIMARY KEY(col, ...) columns first.
    table_level_pk: set[str] = set()
    for part in parts:
        p = part.strip()
        m = re.match(r"PRIMARY\s+KEY\s*\(([^)]*)\)", p, re.I)
        if m:
            table_level_pk |= {
                c.strip().strip("`") for c in m.group(1).split(",") if c.strip()
            }

    cols = []
    for part in parts:
        p = part.strip()
        if not p or re.match(r"(PRIMARY\s+KEY|FOREIGN\s+KEY|UNIQUE|CHECK)\b", p, re.I):
            continue
        m = re.match(r"`([^`]+)`\s+([A-Z]+)", p)
        if not m:
            continue
        name, typ = m.group(1), m.group(2)
        upper = p.upper()
        notnull = 0 if "NOT NULL" not in upper else 1
        pk = 1 if ("PRIMARY KEY" in upper or name in table_level_pk) else 0
        cols.append((name, typ, notnull, pk))
    return cols


def _split_top_level(s: str) -> list[str]:
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--schemas", type=Path, default=DEFAULT_SCHEMA_DIR,
                    help="directory containing <db-class>/<version>.json")
    ap.add_argument("--check", action="store_true",
                    help="validate an existing asset without rebuilding")
    args = ap.parse_args()

    schema = load_schema(args.schemas)
    identity_hash = schema["database"]["identityHash"]

    if args.check:
        if not OUTPUT_DB.exists():
            print(f"ERROR: {OUTPUT_DB} does not exist")
            return 1
        entries = json.loads(JSON_ASSET.read_text(encoding="utf-8"))
        problems = self_validate(OUTPUT_DB, schema, len(entries))
        for p in problems:
            print(f"  FAIL: {p}")
        print("OK — asset is Room-valid" if not problems else f"{len(problems)} problem(s)")
        return 2 if problems else 0

    entries = json.loads(JSON_ASSET.read_text(encoding="utf-8"))
    rooms = load_room_levels(ROOM_DIRECTORY)
    print(f"Room directory: {len(rooms)} rooms parsed")

    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()
    for suffix in ("-wal", "-shm"):
        side = OUTPUT_DB.with_name(OUTPUT_DB.name + suffix)
        if side.exists():
            side.unlink()

    conn = sqlite3.connect(str(OUTPUT_DB))
    try:
        created = create_tables(conn, schema)
        print(f"Tables created from Room's own createSql: {', '.join(created)}")
        insert_metadata(conn)
        n, resolved = insert_rooms(conn, entries, rooms)
        print(f"Inserted {n} timetable rows; floor resolved for {resolved} "
              f"({n - resolved} NULL)")
        finalize(conn, identity_hash)
    finally:
        conn.close()

    problems = self_validate(OUTPUT_DB, schema, len(entries))
    size_kb = OUTPUT_DB.stat().st_size / 1024
    print(f"Asset: {OUTPUT_DB.relative_to(REPO)}  ({size_kb:.1f} KB)")
    print(f"identityHash: {identity_hash}")
    if problems:
        for p in problems:
            print(f"  FAIL: {p}")
        return 2
    print("Self-validation passed — asset is Room-valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())

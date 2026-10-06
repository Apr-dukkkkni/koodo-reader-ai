# docs/evaluation/run_eval.py
import csv
import json
import time
from pathlib import Path

import requests

BASE = "http://127.0.0.1:8080"
ASK_URL = f"{BASE}/api/v1/agent/ask"
SEARCH_URL = f"{BASE}/api/v1/agent/book/search-test"

EVAL_DIR = Path(__file__).parent
CSV_PATH = EVAL_DIR / "questions.csv"
ASK_OUT = EVAL_DIR / "ask_results.json"
RAG_OUT = EVAL_DIR / "rag_results.json"

SLEEP = 2.0
DEFAULT_CONTEXT = "用户正在阅读《窄门》。"


def load_rows():
    with open(CSV_PATH, encoding="utf-8") as f:
        return list(csv.DictReader(f))


def build_payload(row):
    return {
        "term": row["term"],
        "question": row["question"],
        "context": row["context"] or DEFAULT_CONTEXT,
        "bookId": row["bookId"] or None,
        "bookTitle": row["bookTitle"] or None,
        "cfi": row["cfi"] or None,
    }


def clean(payload):
    return {k: v for k, v in payload.items() if v is not None}


def post_json(url, payload, timeout=120):
    try:
        r = requests.post(url, json=payload, timeout=timeout)
        if r.status_code != 200:
            return {"_http_status": r.status_code, "_raw": r.text[:500]}
        return r.json()
    except Exception as e:
        return {"_error": str(e)}


def post_query(url, params, timeout=60):
    try:
        r = requests.post(url, params=params, timeout=timeout)
        if r.status_code != 200:
            return {"_http_status": r.status_code, "_raw": r.text[:500]}
        return r.json()
    except Exception as e:
        return {"_error": str(e)}


def seed_e_terms(rows):
    terms = sorted({r["seedRequired"] for r in rows if r.get("seedRequired")})
    for t in terms:
        print(f"[seed] {t}")
        post_json(ASK_URL, clean({
            "term": t,
            "question": f"{t}是什么？",
            "context": DEFAULT_CONTEXT,
            "bookId": "窄门",
            "bookTitle": "窄门",
        }))
        time.sleep(SLEEP)


def run_ask(rows):
    results = []
    for i, row in enumerate(rows, 1):
        print(f"[ask {i}/{len(rows)}] {row['id']} {row['question'][:40]}")
        resp = post_json(ASK_URL, clean(build_payload(row)))
        results.append({
            "id": row["id"],
            "category": row["category"],
            "expectedRoute": row["expectedRoute"],
            "expectedEvidenceType": row["expectedEvidenceType"],
            "expectedBookKeywords": row["expectedBookKeywords"],
            "question": row["question"],
            "term": row["term"],
            "response": resp,
        })
        time.sleep(SLEEP)
    return results


def run_rag(rows):
    rag_rows = [r for r in rows if r["category"] == "B"]
    results = []
    for i, row in enumerate(rag_rows, 1):
        print(f"[rag {i}/{len(rag_rows)}] {row['id']} {row['question'][:40]}")
        resp = post_json(SEARCH_URL, {
            "bookId": row["bookId"] or "窄门",
            "query": row["question"],
            "topK": 5,
        })
        results.append({
            "id": row["id"],
            "question": row["question"],
            "expectedBookKeywords": row["expectedBookKeywords"],
            "response": resp,
        })
        time.sleep(SLEEP)
    return results


def main():
    rows = load_rows()
    print(f"loaded {len(rows)} rows")
    seed_e_terms(rows)
    ask_results = run_ask(rows)
    json.dump(ask_results, open(ASK_OUT, "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    rag_results = run_rag(rows)
    json.dump(rag_results, open(RAG_OUT, "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)
    print("done")


if __name__ == "__main__":
    main()
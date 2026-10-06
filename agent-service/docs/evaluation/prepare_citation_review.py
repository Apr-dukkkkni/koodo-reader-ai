import json
import csv

with open("ask_results.json", encoding="utf-8") as f:
    data = json.load(f)

rows = []

for r in data:
    resp = r.get("response", {}) or {}
    sources = resp.get("sources", []) or []

    answer = " ".join([
        str(resp.get("oneLine", "") or ""),
        str(resp.get("explanation", "") or ""),
        str(resp.get("keyPoint", "") or "")
    ]).strip()

    for s in sources:
        rows.append({
            "id": r.get("id", ""),
            "category": (r.get("id", "")[:1]),
            "question": r.get("question", ""),
            "route": resp.get("route", ""),
            "answer": answer,
            "sourceId": s.get("sourceId", ""),
            "title": s.get("title", ""),
            "domain": s.get("domain", ""),
            "url": s.get("url", ""),
            "valid": "",
            "notes": ""
        })

with open(
    "citation_review.csv",
    "w",
    newline="",
    encoding="utf-8-sig"
) as f:
    writer = csv.DictWriter(f, fieldnames=rows[0].keys())
    writer.writeheader()
    writer.writerows(rows)

print(f"已生成 citation_review.csv，共 {len(rows)} 条引用待检查")
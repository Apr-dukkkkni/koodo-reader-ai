import csv
from collections import defaultdict

rows = list(csv.DictReader(
    open("citation_review.csv", encoding="utf-8-sig")
))

def evidence_type(r):
    domain = (r.get("domain") or "").strip()

    if domain == "memory":
        return "MEMORY"
    if domain.startswith("book:"):
        return "BOOK"
    return "WEB"


checked = [
    r for r in rows
    if r["valid"].strip() in ("0", "1")
    and evidence_type(r) != "MEMORY"
]

valid = [r for r in checked if r["valid"].strip() == "1"]

print("Citation Validity (BOOK + WEB)")
print("------------------------------")

print(
    f"Overall: {len(valid)}/{len(checked)}"
    f" = {len(valid) / len(checked) * 100:.1f}%"
)

type_stats = defaultdict(lambda: [0, 0])

for r in checked:
    t = evidence_type(r)
    type_stats[t][1] += 1

    if r["valid"].strip() == "1":
        type_stats[t][0] += 1

print()
print("By Evidence Type")

for t in ("BOOK", "WEB"):
    ok, total = type_stats[t]
    if total:
        print(f"{t}: {ok}/{total} = {ok / total * 100:.1f}%")

category_stats = defaultdict(lambda: [0, 0])

for r in checked:
    category_stats[r["category"]][1] += 1
    if r["valid"].strip() == "1":
        category_stats[r["category"]][0] += 1

print()
print("By Category")

for cat in sorted(category_stats):
    ok, total = category_stats[cat]
    print(f"{cat}: {ok}/{total} = {ok / total * 100:.1f}%")

print()
print("Invalid citations")

for r in checked:
    if r["valid"].strip() == "0":
        print(
            f"{r['id']} | {r['sourceId']} | "
            f"{r['domain']} | {r['notes']}"
        )
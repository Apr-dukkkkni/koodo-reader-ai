# docs/evaluation/summarize.py
import json
import statistics
from pathlib import Path

EVAL_DIR = Path(__file__).parent


def load(p):
    return json.load(open(EVAL_DIR / p, encoding="utf-8"))


def route_accuracy(results):
    ok = sum(1 for r in results
             if r["response"].get("route") == r["expectedRoute"])
    return ok, len(results), ok / len(results) if results else 0


def structured_success(results):
    ok = sum(1 for r in results
             if "qaId" in r["response"] and "route" in r["response"])
    return ok, len(results), ok / len(results) if results else 0


def latency_stats(results):
    lat = sorted(
        r["response"].get("usage", {}).get("latencyMs", 0)
        for r in results if "usage" in r["response"]
    )
    if not lat:
        return 0, 0
    p50 = lat[len(lat) // 2]
    p95 = lat[min(int(len(lat) * 0.95), len(lat) - 1)]
    return p50, p95


def token_stats(results):
    inp = [r["response"].get("usage", {}).get("inputTokens", 0)
           for r in results if "usage" in r["response"]]
    out = [r["response"].get("usage", {}).get("outputTokens", 0)
           for r in results if "usage" in r["response"]]
    return (statistics.mean(inp) if inp else 0,
            statistics.mean(out) if out else 0)


def rag_hit5(rag_results):
    hits = 0
    for r in rag_results:
        keywords = [k.strip() for k in (r["expectedBookKeywords"] or "").split(";")
                    if k.strip()]
        hits_list = r["response"].get("hits") or []
        content = " ".join(str(h.get("contentPreview", "")) for h in hits_list)
        if keywords and any(k in content for k in keywords):
            hits += 1
    return hits, len(rag_results), (hits / len(rag_results) if rag_results else 0)


def main():
    ask_results = load("ask_results.json")
    rag_results = load("rag_results.json")

    ok, total, acc = route_accuracy(ask_results)
    s_ok, s_total, s_rate = structured_success(ask_results)
    p50, p95 = latency_stats(ask_results)
    avg_in, avg_out = token_stats(ask_results)
    h, h_total, h_rate = rag_hit5(rag_results)

    lines = [
        "# Week 7 评估结果（Day 2 第一版）",
        "",
        f"- 样本数：{total}",
        f"- Route Accuracy：{ok}/{total} = {acc:.1%}",
        f"- Structured Success Rate：{s_ok}/{s_total} = {s_rate:.1%}",
        f"- RAG Hit@5：{h}/{h_total} = {h_rate:.1%}",
        f"- P50 时延：{p50} ms",
        f"- P95 时延：{p95} ms",
        f"- 平均 input token：{avg_in:.0f}",
        f"- 平均 output token：{avg_out:.0f}",
        "",
        "## 分类别 Route Accuracy",
        "",
    ]
    for cat in "ABCDE":
        sub = [r for r in ask_results if r["category"] == cat]
        if not sub:
            continue
        c_ok = sum(1 for r in sub
                   if r["response"].get("route") == r["expectedRoute"])
        lines.append(f"- {cat} 类：{c_ok}/{len(sub)} = {c_ok/len(sub):.1%}")

    lines += [
        "",
        "## Citation Validity",
        "",
        "需要人工检查，见 ask_results.json 中每条 sources。",
        "",
        "## 失败明细",
        "",
    ]
    for r in ask_results:
        got = r["response"].get("route", "?")
        exp = r["expectedRoute"]
        if got != exp:
            lines.append(f"- {r['id']} expected={exp} got={got} | {r['question'][:30]}")

    open(EVAL_DIR / "summary.md", "w", encoding="utf-8").write("\n".join(lines))
    print("\n".join(lines))


if __name__ == "__main__":
    main()
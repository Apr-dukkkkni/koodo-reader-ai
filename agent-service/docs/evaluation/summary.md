# Week 7 评估结果（Day 2 第一版）

- 样本数：30
- Route Accuracy：29/30 = 96.7%
- Structured Success Rate：29/30 = 96.7%
- RAG Hit@5：1/6 = 16.7%
- P50 时延：4699 ms
- P95 时延：18288 ms
- 平均 input token：5469
- 平均 output token：173

## 分类别 Route Accuracy

- A 类：6/6 = 100.0%
- B 类：6/6 = 100.0%
- C 类：5/6 = 83.3%
- D 类：6/6 = 100.0%
- E 类：6/6 = 100.0%

## Citation Validity

需要人工检查，见 ask_results.json 中每条 sources。

## 失败明细

- C03 expected=WEB got=? | 安德烈·纪德的代表作有哪些？
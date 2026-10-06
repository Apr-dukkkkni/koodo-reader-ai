# 评估集说明

## 目标
从“能跑”变成“能证明”。所有简历数字必须来自这里。

## 类别
- A 当前段落解释 → DIRECT
- B 书内前文 → BOOK_RAG
- C 外部事实 → WEB
- D 书内外对比 → HYBRID
- E 记忆深度 → MEMORY

## 标注原则
1. expectedRoute 按理想应然标注，不看系统输出。
2. B/D 类必须填 bookId 和 expectedBookKeywords。
3. E 类跑之前必须先 seed 历史问答。
4. 评估集一旦开跑，不允许修改 expected 字段。

## 指标
- Route Accuracy
- RAG Hit@5
- Citation Validity
- Structured Output Success Rate
- P50/P95
- 平均 input/output token
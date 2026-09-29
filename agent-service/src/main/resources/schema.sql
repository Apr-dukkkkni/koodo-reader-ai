CREATE TABLE IF NOT EXISTS qa_history (
                                          id TEXT PRIMARY KEY,
                                          term TEXT NOT NULL,
                                          question TEXT,
                                          context_text TEXT,
                                          book_id TEXT,
                                          book_title TEXT,
                                          cfi TEXT,
                                          route TEXT,
                                          answer_json TEXT NOT NULL,
                                          evidence_level TEXT,
                                          input_tokens INTEGER DEFAULT 0,
                                          output_tokens INTEGER DEFAULT 0,
                                          latency_ms INTEGER DEFAULT 0,
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
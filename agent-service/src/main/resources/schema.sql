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
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS concept_profile (
                                               id SERIAL PRIMARY KEY,
                                               concept_name TEXT NOT NULL UNIQUE,
                                               familiarity_level INTEGER NOT NULL DEFAULT 0,
                                               ask_count INTEGER NOT NULL DEFAULT 0,
                                               too_shallow_count INTEGER NOT NULL DEFAULT 0,
                                               just_right_count INTEGER NOT NULL DEFAULT 0,
                                               too_deep_count INTEGER NOT NULL DEFAULT 0,
                                               last_seen TIMESTAMP
);

CREATE TABLE IF NOT EXISTS feedback (
                                        id SERIAL PRIMARY KEY,
                                        qa_id TEXT NOT NULL,
                                        rating TEXT NOT NULL,
                                        comment TEXT,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Knowledge Graph Database Schema
-- Creates tables for storing knowledge nodes and their relationships.
--
-- This migration sets up the foundation for the knowledge graph feature,
-- supporting WikiLink extraction ([[link]]), semantic similarity, and
-- graph visualization via D3.js force-directed layout.
--
-- Reference: Obsidian Graph, Logseq, D3.js

-- Knowledge nodes: each node represents a document or concept
CREATE TABLE IF NOT EXISTS knowledge_nodes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT,
    file_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Node relations: directed edges between nodes
-- relation_type can be: 'references', 'mentions', 'related'
CREATE TABLE IF NOT EXISTS node_relations (
    source_id TEXT REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    target_id TEXT REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL,  -- 'references', 'mentions', 'related'
    weight REAL DEFAULT 1.0,
    PRIMARY KEY (source_id, target_id, relation_type)
);

-- Indexes for efficient graph traversal
CREATE INDEX IF NOT EXISTS idx_node_relations_source ON node_relations(source_id);
CREATE INDEX IF NOT EXISTS idx_node_relations_target ON node_relations(target_id);
CREATE INDEX IF NOT EXISTS idx_node_relations_type ON node_relations(relation_type);
CREATE INDEX IF NOT EXISTS idx_nodes_title ON knowledge_nodes(title);
CREATE INDEX IF NOT EXISTS idx_nodes_updated ON knowledge_nodes(updated_at);

-- Full-text search index for node content (SQLite FTS5)
CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_nodes_fts USING fts5(
    title,
    content,
    content='knowledge_nodes',
    content_rowid='rowid'
);

-- Triggers to keep FTS index in sync
CREATE TRIGGER IF NOT EXISTS knowledge_nodes_ai AFTER INSERT ON knowledge_nodes BEGIN
    INSERT INTO knowledge_nodes_fts(rowid, title, content)
    VALUES (new.rowid, new.title, new.content);
END;

CREATE TRIGGER IF NOT EXISTS knowledge_nodes_ad AFTER DELETE ON knowledge_nodes BEGIN
    INSERT INTO knowledge_nodes_fts(knowledge_nodes_fts, rowid, title, content)
    VALUES ('delete', old.rowid, old.title, old.content);
END;

CREATE TRIGGER IF NOT EXISTS knowledge_nodes_au AFTER UPDATE ON knowledge_nodes BEGIN
    INSERT INTO knowledge_nodes_fts(knowledge_nodes_fts, rowid, title, content)
    VALUES ('delete', old.rowid, old.title, old.content);
    INSERT INTO knowledge_nodes_fts(rowid, title, content)
    VALUES (new.rowid, new.title, new.content);
END;

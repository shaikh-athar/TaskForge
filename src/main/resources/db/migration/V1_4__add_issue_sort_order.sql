ALTER TABLE issues ADD COLUMN sort_order DOUBLE PRECISION;

-- Initialize sort_order based on creation time to preserve existing order
UPDATE issues SET sort_order = EXTRACT(EPOCH FROM created_at) WHERE sort_order IS NULL;

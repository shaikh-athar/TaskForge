-- Add parent_id to issues table for hierarchical linking (Epic -> Story -> Task)
ALTER TABLE issues ADD COLUMN parent_id UUID;

-- Index for finding children of an issue efficiently
CREATE INDEX idx_issues_parent
    ON issues (tenant_id, parent_id)
    WHERE deleted = false;

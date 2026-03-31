-- Add position column for custom ordering within a list
ALTER TABLE items ADD COLUMN position INTEGER;

-- Initialize position based on creation order within each list
UPDATE items i
SET position = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY item_list_id ORDER BY created_at ASC) - 1 AS rn
    FROM items
) sub
WHERE i.id = sub.id;

-- Make position non-nullable now that it's been populated
ALTER TABLE items ALTER COLUMN position SET NOT NULL;

-- Index for efficient ordering queries
CREATE INDEX idx_items_list_position ON items (item_list_id, position);

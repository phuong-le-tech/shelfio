DROP INDEX IF EXISTS idx_items_legacy_images;
ALTER TABLE items DROP COLUMN IF EXISTS image_data;
ALTER TABLE items DROP COLUMN IF EXISTS content_type;

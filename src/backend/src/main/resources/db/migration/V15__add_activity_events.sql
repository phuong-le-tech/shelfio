CREATE TABLE activity_events (
    id           UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    actor_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(20) NOT NULL,
    entity_id    UUID,
    entity_name  VARCHAR(255),
    occurred_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_activity_events_workspace_occurred
    ON activity_events(workspace_id, occurred_at DESC);

CREATE INDEX idx_activity_events_actor
    ON activity_events(actor_id);

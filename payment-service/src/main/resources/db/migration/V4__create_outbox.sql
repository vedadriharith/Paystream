-- Transactional outbox: events are written here in the same transaction as the business change,
-- then relayed to Kafka by a background publisher. Row id equals the event's eventId.
CREATE TABLE outbox (
                        id              UUID PRIMARY KEY,
                        aggregate_type  VARCHAR(60)  NOT NULL,
                        aggregate_id    UUID         NOT NULL,
                        event_type      VARCHAR(80)  NOT NULL,
                        payload         JSONB        NOT NULL,
                        created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        published_at    TIMESTAMPTZ,
                        attempts        INT          NOT NULL DEFAULT 0,
                        last_error      VARCHAR(500)
);

-- Partial index: only unpublished rows are indexed, so the publisher's poll stays fast
-- even after millions of rows have been published
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
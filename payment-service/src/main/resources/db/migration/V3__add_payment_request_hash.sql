-- SHA-256 (hex) of the business fields of the original create request.
-- Used to detect an Idempotency-Key being reused with a different request body.
-- Nullable because rows created before this migration have no recorded hash;
-- a replay against such a row is treated as a mismatch (409).
ALTER TABLE payments ADD COLUMN request_hash CHAR(64);
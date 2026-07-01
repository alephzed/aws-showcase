-- Seed one provider and a few AVAILABLE slots so a booking can succeed
-- immediately after deploy (walking skeleton). Idempotent on re-run.

INSERT INTO availability_slots (id, provider_id, start_ts, end_ts, status, version) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111',
     '2026-07-01T09:00:00+00', '2026-07-01T09:30:00+00', 'AVAILABLE', 0),
    ('aaaaaaaa-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111',
     '2026-07-01T10:00:00+00', '2026-07-01T10:30:00+00', 'AVAILABLE', 0),
    ('aaaaaaaa-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111',
     '2026-07-01T11:00:00+00', '2026-07-01T11:30:00+00', 'AVAILABLE', 0)
ON CONFLICT (id) DO NOTHING;

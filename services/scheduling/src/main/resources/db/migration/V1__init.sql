-- Scheduling schema (ADR-0007): availability modeled as discrete slot rows,
-- double-booking prevented by an optimistic version + a unique partial index.

CREATE TABLE availability_slots (
    id             uuid        PRIMARY KEY,
    provider_id    uuid        NOT NULL,
    start_ts       timestamptz NOT NULL,
    end_ts         timestamptz NOT NULL,
    status         varchar(20) NOT NULL,
    version        bigint      NOT NULL DEFAULT 0,
    appointment_id uuid
);

-- Hard safety net: at most one BOOKED slot per (provider, start time).
CREATE UNIQUE INDEX uq_slot_booked
    ON availability_slots (provider_id, start_ts)
    WHERE status = 'BOOKED';

CREATE TABLE appointments (
    id             uuid        PRIMARY KEY,
    patient_id     uuid        NOT NULL,
    provider_id    uuid        NOT NULL,
    slot_id        uuid        NOT NULL REFERENCES availability_slots (id),
    status         varchar(20) NOT NULL,
    correlation_id varchar(64),
    created_at     timestamptz NOT NULL DEFAULT now()
);

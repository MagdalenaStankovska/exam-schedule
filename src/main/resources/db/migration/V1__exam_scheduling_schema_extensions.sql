-- Adds schema pieces required by the new scheduling workflow/time-slot features.

CREATE TABLE IF NOT EXISTS time_slot (
    id BIGSERIAL PRIMARY KEY,
    from_time TIMESTAMP NOT NULL,
    to_time TIMESTAMP NOT NULL
);

ALTER TABLE subject_exam
    ADD COLUMN IF NOT EXISTS time_slot_id BIGINT,
    ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(255),
    ADD COLUMN IF NOT EXISTS expected_students_submitted_at TIMESTAMP;

ALTER TABLE subject_exam
    DROP CONSTRAINT IF EXISTS fk_subject_exam_time_slot;

ALTER TABLE subject_exam
    ADD CONSTRAINT fk_subject_exam_time_slot
        FOREIGN KEY (time_slot_id) REFERENCES time_slot(id);

ALTER TABLE year_exam_session
    ADD COLUMN IF NOT EXISTS submission_deadline DATE,
    ADD COLUMN IF NOT EXISTS scheduling_triggered_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS finalized_at TIMESTAMP;


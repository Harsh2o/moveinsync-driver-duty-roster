CREATE TABLE assignment_audits (
    id BIGSERIAL PRIMARY KEY,
    duty_id BIGINT NOT NULL REFERENCES duties(id),
    previous_driver_id BIGINT REFERENCES drivers(id),
    previous_vehicle_id BIGINT REFERENCES vehicles(id),
    new_driver_id BIGINT REFERENCES drivers(id),
    new_vehicle_id BIGINT REFERENCES vehicles(id),
    action VARCHAR(30) NOT NULL,
    reason TEXT,
    acting_user VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_duty ON assignment_audits(duty_id);
CREATE INDEX idx_audit_time ON assignment_audits(created_at);

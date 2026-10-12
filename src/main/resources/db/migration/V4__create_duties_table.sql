CREATE TABLE duties (
    id BIGSERIAL PRIMARY KEY,
    required_vehicle_type VARCHAR(10) NOT NULL CHECK (required_vehicle_type IN ('CAB', 'BUS', 'SHUTTLE')),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    assigned_driver_id BIGINT REFERENCES drivers(id),
    assigned_vehicle_id BIGINT REFERENCES vehicles(id),
    duty_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (duty_status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    assignment_state VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED' CHECK (assignment_state IN ('ASSIGNED', 'UNASSIGNED')),
    unfilled_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_duty_time CHECK (end_time > start_time)
);

CREATE INDEX idx_duties_status ON duties(duty_status);
CREATE INDEX idx_duties_assignment ON duties(assignment_state);
CREATE INDEX idx_duties_driver ON duties(assigned_driver_id);
CREATE INDEX idx_duties_vehicle ON duties(assigned_vehicle_id);
CREATE INDEX idx_duties_time_range ON duties(start_time, end_time);
CREATE INDEX idx_duties_status_time ON duties(duty_status, start_time, end_time);

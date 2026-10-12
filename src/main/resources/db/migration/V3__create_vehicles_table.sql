CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(10) NOT NULL CHECK (vehicle_type IN ('CAB', 'BUS', 'SHUTTLE')),
    required_licence_class VARCHAR(10) NOT NULL CHECK (required_licence_class IN ('LMV', 'HMV')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_vehicles_active ON vehicles(active);
CREATE INDEX idx_vehicles_type ON vehicles(vehicle_type);
CREATE INDEX idx_vehicles_registration ON vehicles(registration_number);

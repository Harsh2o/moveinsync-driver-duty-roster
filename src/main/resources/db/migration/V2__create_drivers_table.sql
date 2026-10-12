CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    licence_class VARCHAR(10) NOT NULL CHECK (licence_class IN ('LMV', 'HMV')),
    licence_expiry_date DATE NOT NULL,
    medical_validity_date DATE NOT NULL,
    training_validity_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_drivers_active ON drivers(active);
CREATE INDEX idx_drivers_licence_class ON drivers(licence_class);

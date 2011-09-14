-- Station information ---------------------------
CREATE TABLE IF NOT EXISTS Station (
    id INTEGER NOT NULL AUTO INCREMENT,
    network TEXT,
    name TEXT NOT NULL,
    UNIQUE (network, name),
    PRIMARY KEY (id)
);

-- Station information ---------------------------
CREATE TABLE IF NOT EXISTS Sensor (
    id INTEGER NOT NULL AUTO INCREMENT,
    station_id INTEGER NOT NULL REFERENCES Station(id),
    location TEXT,
    UNIQUE (station_id, location),
    PRIMARY KEY (id)
);

-- Channel information ---------------------------
CREATE TABLE IF NOT EXISTS Channel (
    id INTEGER NOT NULL AUTO INCREMENT,
    sensor_id INTEGER NOT NULL REFERENCES Sensor(id),
    name TEXT NOT NULL,
    UNIQUE (sensor_id, name),
    PRIMARY KEY (id)
);

-- DerivedChannel information ---------------------------
CREATE TABLE IF NOT EXISTS DerivedChannel (
    id INTEGER NOT NULL AUTO INCREMENT,
    sensor_id INTEGER NOT NULL REFERENCES Sensor(id),
    name TEXT NOT NULL,
    UNIQUE (sensor_id, name),
    PRIMARY KEY (id)
);

-- Metadata --------------------------------------
CREATE TABLE IF NOT EXISTS Metadata (
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    epoch DATETIME NOT NULL,
    sensor_info TEXT,
    UNIQUE (channel_id, epoch)
);

-- Measurements ----------------------------------
CREATE TABLE IF NOT EXISTS PerformanceMetrics (
    id INTEGER NOT NULL AUTO INCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    availability INTEGER NOT NULL,
    clock_quality INTEGER NOT NULL,
    gap_count INTEGER NOT NULL,
    UNIQUE (channel_id, year, month, day),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS CalibrationMetrics (
    id INTEGER NOT NULL AUTO INCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    cal_time DATETIME NOT NULL,
    UNIQUE (channel_id, year, month, day),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS NoiseMetrics (
    id INTEGER NOT NULL AUTO INCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    noise INTEGER NOT NULL,
    UNIQUE (channel_id, year, month, day),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SensorComparison (
    id INTEGER NOT NULL AUTO INCREMENT,
    derived_channel_id INTEGER NOT NULL REFERENCES DerivedChannel(id),
    reference_channel_id INTEGER NOT NULL REFERENCES DerivedChannel(id),
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    power_level INTEGER NOT NULL,
    UNIQUE (derived_channel_id, reference_channel_id, year, month, day),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SensorComparisonDeviations (
    id INTEGER NOT NULL AUTO INCREMENT,
    sensor_comparison_id INTEGER NOT NULL REFERENCES SensorComparison(id),
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS SensorComparisonCoherence (
    id INTEGER NOT NULL AUTO INCREMENT,
    sensor_comparison_id INTEGER NOT NULL REFERENCES SensorComparison(id),
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (id)
);


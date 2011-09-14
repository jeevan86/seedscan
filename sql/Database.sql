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
    location TEXT NOT NULL,
    UNIQUE (station_id, location),
    PRIMARY KEY (id)
);

-- Channel information ---------------------------
CREATE TABLE IF NOT EXISTS Channel (
    id INTEGER NOT NULL AUTO INCREMENT,
    sensor_id INTEGER NOT NULL REFERENCES Sensor(id),
    name TEXT NOT NULL,
    derived INTEGER NOT NULL
    UNIQUE (sensor_id, name),
    PRIMARY KEY (id)
);

-- Metadata --------------------------------------
CREATE TABLE IF NOT EXISTS Metadata (
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    epoch DATETIME NOT NULL,
    sensor_info TEXT,
    raw_metadata BLOB,
    UNIQUE (channel_id, epoch)
);

-- Measurements ----------------------------------
CREATE TABLE IF NOT EXISTS Metrics (
    id INTEGER NOT NULL AUTO INCREMENT,
    channel_id INTEGER NOT NULL REFERENCES Channel(id),
    year INTEGER NOT NULL,
    month INTEGER,
    day INTEGER,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    UNIQUE (channel_id, year, month, day, key),
    PRIMARY KEY (id)
);


#!/bin/bash
set -e

# This script runs when the PostgreSQL container first starts
# It ensures the database exists

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Database is already created by POSTGRES_DB environment variable
    -- Just grant permissions
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO $POSTGRES_USER;
EOSQL

echo "Database initialization completed for $POSTGRES_DB"

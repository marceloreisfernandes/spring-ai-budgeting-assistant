#!/bin/bash
# PostgreSQL refuses to let the bootstrap superuser strip its own SUPERUSER attribute
# ("permission denied to alter role: the bootstrap superuser must have the SUPERUSER
# attribute" - confirmed by actually running this against postgres:18-alpine). So instead of
# trying to demote POSTGRES_USER after the fact, this script creates a SEPARATE, genuinely
# non-superuser role (APP_DB_USER, from the application's own POSTGRES_USER/POSTGRES_PASSWORD
# in .env) and makes it the owner of the application database. The application only ever
# connects as that role, never as the bootstrap superuser, so Flyway/Hibernate DDL keeps
# working (the role owns the database) with a strictly smaller privilege set than superuser.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${APP_DB_USER}') THEN
            CREATE ROLE "${APP_DB_USER}" LOGIN PASSWORD '${APP_DB_PASSWORD}'
                NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
        END IF;
    END
    \$\$;

    ALTER DATABASE "${POSTGRES_DB}" OWNER TO "${APP_DB_USER}";
    ALTER SCHEMA public OWNER TO "${APP_DB_USER}";
EOSQL

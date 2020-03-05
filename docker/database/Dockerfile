FROM postgres:alpine

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d letter_tracking -U letterservice

EXPOSE 5432

-- Demo data, only loaded by the 'dev' profile (see application-dev.yml)
INSERT INTO accounts (id, owner_id, currency, balance, reserved, status, version) VALUES
    ('11111111-1111-1111-1111-111111111111', 'alice', 'USD', 5000.00, 0, 'ACTIVE', 0),
    ('22222222-2222-2222-2222-222222222222', 'bob',   'USD', 1000.00, 0, 'ACTIVE', 0),
    ('33333333-3333-3333-3333-333333333333', 'carol', 'USD',    0.00, 0, 'FROZEN', 0);

-- Demo accounts for local/dev environments. Password for all: Passw0rd!123
-- NOT for production use - see docs/SECURITY.md for production hardening guidance.
INSERT INTO users (email, password_hash, full_name, role, business_unit, is_active) VALUES
('admin@erip.com',    '$2b$12$/pwXbhpGfKmX7.QQf/BqeeuT6x/PRXnyf1vg/AnPnpYL7.qanb3aK', 'Ava Administrator', 'ADMIN',            'Corporate IT',        TRUE),
('analyst@erip.com',  '$2b$12$F33JaJtyyOc7IExLd.14ZOABguZysgV2WTSOS13IY./Cqslow0sh6', 'Noah Analyst',      'SECURITY_ANALYST',  'Security Operations', TRUE),
('exec@erip.com',     '$2b$12$lc0e6iRSCyIo.mX43rDGzuK3jW80/y4lKttVDRH8EBWza.qqIsRES', 'Elena Executive',   'EXECUTIVE',         'Corporate IT',        TRUE),
('viewer@erip.com',   '$2b$12$lA2H.fZWV/EPpnJO2cV76eY.MfoyUy/NApdeWLUtWfU1BBNsZSiBO', 'Victor Viewer',     'VIEWER',            'Compliance',          TRUE);

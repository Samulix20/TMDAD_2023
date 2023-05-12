INSERT INTO users (id, username, password, role)
VALUES (1, 'Admin', '$2a$10$UD3YYsA6Bhl0.YDXgTsb7OVeJD3ibfhtlUXB4vW0GKudBhHN8CiTy', 'ADMIN')
ON CONFLICT DO NOTHING;
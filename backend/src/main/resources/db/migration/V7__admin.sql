-- Slice 8b. Q22: platform administrators are separate accounts with their own role.
ALTER TABLE user_role DROP CONSTRAINT ck_user_role_roles;
ALTER TABLE user_role
    ADD CONSTRAINT ck_user_role_roles CHECK (roles IN ('ROLE_VOLUNTEER', 'ROLE_ORGANIZATION', 'ROLE_ADMIN'));

-- Q21, Q28: the first admin. The credentials are Flyway placeholders fed by environment variables
-- (ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD_HASH), so none is in git. Runs once; later changes are manual.
INSERT INTO app_users (username, email, password, is_banned, created_at)
VALUES ('${admin_username}', '${admin_email}', '${admin_password_hash}', FALSE, now());

INSERT INTO user_role (user_id, roles)
SELECT id, 'ROLE_ADMIN'
FROM app_users
WHERE username = '${admin_username}';

-- Allow the admin role. The admin account itself is created at startup by
-- AdminAccountInitializer from ADMIN_USERNAME / ADMIN_PASSWORD, so no
-- credentials live in version control.
ALTER TABLE login DROP CONSTRAINT IF EXISTS login_role_check;
ALTER TABLE login ADD CONSTRAINT login_role_check CHECK (role IN ('doctor', 'secretaire', 'admin'));

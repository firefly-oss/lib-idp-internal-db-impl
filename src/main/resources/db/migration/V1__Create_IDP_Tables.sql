-- Create IDP Users Table
CREATE TABLE IF NOT EXISTS idp_users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Create index on username for faster lookups
CREATE INDEX idx_idp_users_username ON idp_users(username);

-- Create index on email for faster lookups
CREATE INDEX idx_idp_users_email ON idp_users(email);

-- Create IDP Roles Table
CREATE TABLE IF NOT EXISTS idp_roles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on role name
CREATE INDEX idx_idp_roles_name ON idp_roles(name);

-- Create IDP User-Roles Junction Table
CREATE TABLE IF NOT EXISTS idp_user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES idp_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES idp_roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Create indexes on user_roles for faster joins
CREATE INDEX idx_idp_user_roles_user_id ON idp_user_roles(user_id);
CREATE INDEX idx_idp_user_roles_role_id ON idp_user_roles(role_id);

-- Create IDP Sessions Table
CREATE TABLE IF NOT EXISTS idp_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    access_token_jti VARCHAR(255) UNIQUE NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES idp_users(id) ON DELETE CASCADE
);

-- Create indexes on sessions
CREATE INDEX idx_idp_sessions_user_id ON idp_sessions(user_id);
CREATE INDEX idx_idp_sessions_access_token_jti ON idp_sessions(access_token_jti);
CREATE INDEX idx_idp_sessions_expires_at ON idp_sessions(expires_at);
CREATE INDEX idx_idp_sessions_revoked ON idp_sessions(revoked);

-- Create IDP Refresh Tokens Table
CREATE TABLE IF NOT EXISTS idp_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_jti VARCHAR(255) UNIQUE NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    session_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES idp_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES idp_sessions(id) ON DELETE CASCADE
);

-- Create indexes on refresh tokens
CREATE INDEX idx_idp_refresh_tokens_user_id ON idp_refresh_tokens(user_id);
CREATE INDEX idx_idp_refresh_tokens_session_id ON idp_refresh_tokens(session_id);
CREATE INDEX idx_idp_refresh_tokens_token_jti ON idp_refresh_tokens(token_jti);
CREATE INDEX idx_idp_refresh_tokens_expires_at ON idp_refresh_tokens(expires_at);
CREATE INDEX idx_idp_refresh_tokens_revoked ON idp_refresh_tokens(revoked);

-- Insert default admin user (password: admin123 - CHANGE IN PRODUCTION!)
-- Password hash is BCrypt encoded 'admin123' with strength 12
-- Generated using BCryptPasswordEncoder with strength 12
INSERT INTO idp_users (id, username, email, password_hash, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, mfa_enabled, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@firefly.local',
    '$2a$12$XRIMvQrfjMsLEWzPgy5mfufYUkNdkB5a45eDCBE3qeqzzxTwGhEL2',
    'Admin',
    'User',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO idp_roles (id, name, description, created_at, updated_at)
VALUES 
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrator role with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'USER', 'Standard user role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'MANAGER', 'Manager role with elevated permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign ADMIN role to admin user
INSERT INTO idp_user_roles (id, user_id, role_id, assigned_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
);

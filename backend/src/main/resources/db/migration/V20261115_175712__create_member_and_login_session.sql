create table member (
    id uuid primary key,
    firebase_uid text not null,
    email text not null,
    name text,
    status text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted boolean not null default false,
    deleted_at timestamptz,
    constraint ck_member_status check (status in ('ACTIVE', 'SUSPENDED'))
);

create unique index uq_member_firebase_uid_active on member (firebase_uid) where deleted = false;

create table login_session (
    id uuid primary key,
    member_id uuid not null,
    device_identifier text not null,
    device_name text not null,
    refresh_token_hash text not null,
    expires_at timestamptz not null,
    last_used_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted boolean not null default false,
    deleted_at timestamptz,
    constraint uq_login_session_member_device unique (member_id, device_identifier),
    constraint uq_login_session_refresh_token_hash unique (refresh_token_hash),
    constraint fk_login_session_member foreign key (member_id) references member (id)
);

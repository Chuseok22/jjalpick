create table member (
    id uuid primary key,
    firebase_uid text not null,
    email text not null,
    name text,
    status text not null,
    refresh_token_hash text,
    refresh_token_expires_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted boolean not null default false,
    deleted_at timestamptz,
    constraint ck_member_status check (status in ('ACTIVE', 'SUSPENDED'))
);

create unique index uq_member_firebase_uid_active on member (firebase_uid) where deleted = false;

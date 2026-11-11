create table app_user (
    id uuid primary key,
    provider varchar(20) not null,
    provider_user_id varchar(255) not null,
    email varchar(320),
    display_name varchar(100) not null,
    profile_image_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_user_provider unique (provider, provider_user_id)
);

create table payout_account (
    id uuid primary key,
    user_id uuid not null references app_user(id),
    institution_code varchar(20) not null,
    institution_name varchar(100) not null,
    account_number_encrypted text not null,
    account_number_masked varchar(100) not null,
    account_holder varchar(100) not null,
    alias varchar(100),
    is_primary boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_payout_account_user on payout_account(user_id);

create table settlement_room (
    id uuid primary key,
    invite_token varchar(100) not null unique,
    title varchar(120) not null,
    host_user_id uuid not null references app_user(id),
    selected_account_id uuid references payout_account(id),
    account_institution_code varchar(20) not null,
    account_institution_name varchar(100) not null,
    account_number_encrypted text not null,
    account_number_masked varchar(100) not null,
    account_holder varchar(100) not null,
    total_amount bigint not null check (total_amount >= 0),
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    settled_at timestamptz
);
create index idx_settlement_room_host on settlement_room(host_user_id, created_at desc);

create table room_participant (
    id uuid primary key,
    room_id uuid not null references settlement_room(id),
    user_id uuid not null references app_user(id),
    role varchar(20) not null,
    payment_status varchar(30) not null,
    joined_at timestamptz not null default now(),
    paid_at timestamptz,
    constraint uk_room_participant unique (room_id, user_id)
);
create index idx_participant_user on room_participant(user_id, joined_at desc);

create table expense_item (
    id uuid primary key,
    room_id uuid not null references settlement_room(id),
    participant_id uuid not null references room_participant(id),
    menu_name varchar(150) not null,
    amount bigint not null check (amount > 0),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_expense_item_room on expense_item(room_id, created_at);

create table settlement_audit_log (
    id uuid primary key,
    room_id uuid not null references settlement_room(id),
    actor_user_id uuid not null references app_user(id),
    event_type varchar(50) not null,
    entity_type varchar(50) not null,
    entity_id uuid,
    before_data jsonb,
    after_data jsonb,
    created_at timestamptz not null default now()
);
create index idx_audit_room on settlement_audit_log(room_id, created_at);


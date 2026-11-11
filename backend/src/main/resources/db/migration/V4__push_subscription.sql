create table push_subscription (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    endpoint text not null unique,
    p256dh text not null,
    auth text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_push_subscription_user on push_subscription(user_id);

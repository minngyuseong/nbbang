create table participant_amount (
    participant_id uuid primary key references room_participant(id) on delete cascade,
    room_id uuid not null references settlement_room(id) on delete cascade,
    amount bigint not null check (amount >= 0),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_participant_amount_room on participant_amount(room_id);

insert into participant_amount (participant_id, room_id, amount, created_at, updated_at)
select participant_id, room_id, sum(amount), min(created_at), max(updated_at)
from expense_item
group by participant_id, room_id;

drop table expense_item;

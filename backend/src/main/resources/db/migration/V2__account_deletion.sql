alter table settlement_room drop constraint settlement_room_selected_account_id_fkey;
alter table settlement_room add constraint settlement_room_selected_account_id_fkey
    foreign key (selected_account_id) references payout_account(id) on delete set null;

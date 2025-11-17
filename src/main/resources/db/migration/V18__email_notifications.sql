create table if not exists email_notifications (
    id uuid primary key default gen_random_uuid(),
    recipient text not null,
    subject text not null,
    body text not null,
    body_hash varchar(64) not null,
    status varchar(16) not null,
    attempts integer not null default 0,
    last_error text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    sent_at timestamptz
);

create index if not exists ix_email_notifications_status on email_notifications(status);
create index if not exists ix_email_notifications_created_at on email_notifications(created_at);

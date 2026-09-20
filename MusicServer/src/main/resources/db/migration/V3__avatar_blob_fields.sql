alter table app_users add column if not exists avatar_data bytea;
alter table app_users add column if not exists avatar_content_type varchar(120);
alter table app_users add column if not exists avatar_filename varchar(255);
alter table app_users add column if not exists avatar_size bigint;
alter table app_users add column if not exists avatar_updated_at timestamp(6) with time zone;

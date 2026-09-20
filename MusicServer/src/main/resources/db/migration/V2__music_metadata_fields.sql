alter table music_files add column if not exists cover_path varchar(2000);
alter table music_files add column if not exists cover_content_type varchar(120);
alter table music_files add column if not exists duration bigint;

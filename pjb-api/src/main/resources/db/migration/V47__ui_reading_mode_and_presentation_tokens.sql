alter table if exists tb_usuario_accessibility_pref
  add column if not exists reading_mode_enabled boolean not null default true;

alter table if exists tb_usuario_accessibility_pref
  add column if not exists reading_intensity varchar(20) not null default 'SOFT';

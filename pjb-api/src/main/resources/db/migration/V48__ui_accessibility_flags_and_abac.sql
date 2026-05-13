alter table if exists tb_usuario_accessibility_pref
  add column if not exists accessibility_flags bigint not null default 0;

update tb_usuario_accessibility_pref
set accessibility_flags = case preset
  when 'HIGH_CONTRAST' then (1)
  when 'LARGE_TEXT' then (2)
  when 'REDUCED_MOTION' then (4)
  when 'SCREEN_READER_OPTIMIZED' then (8)
  when 'KEYBOARD_ONLY' then (16)
  else 0
end
where accessibility_flags = 0;

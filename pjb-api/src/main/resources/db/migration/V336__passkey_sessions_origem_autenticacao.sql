alter table passkey_sessions
    add column if not exists origem_autenticacao varchar(30);

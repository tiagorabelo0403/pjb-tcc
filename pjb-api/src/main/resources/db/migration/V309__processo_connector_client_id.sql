alter table tb_processo
    add column if not exists connector_client_id varchar(120);

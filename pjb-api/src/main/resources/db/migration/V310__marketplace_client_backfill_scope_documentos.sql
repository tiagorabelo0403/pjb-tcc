update tb_marketplace_client_app
   set allowed_scopes = allowed_scopes || ' processos:documentos'
 where allowed_scopes not like '%processos:documentos%';

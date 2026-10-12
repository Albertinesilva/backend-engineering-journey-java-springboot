alter table if exists tb_token 
  add constraint FKgvxl8jsfjnhlyu6oufm0rb5hx 
  foreign key (user_id) 
  references tb_user;
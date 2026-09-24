-- =========================
-- USERS
-- =========================
INSERT INTO tb_user (first_name, last_name, email, password, active) VALUES ('Albert', 'Silva', 'albert@gmail.com', '$2a$10$eDIzRoyjJ4Rw7RbsBBfqVuzxU8lABGMlgKAMqqLtnpu9iN6b1w7ve', true);
INSERT INTO tb_user (first_name, last_name, email, password, active) VALUES ('Maria', 'Green', 'maria@gmail.com', '$2a$10$eDIzRoyjJ4Rw7RbsBBfqVuzxU8lABGMlgKAMqqLtnpu9iN6b1w7ve', true);

-- =========================
-- ROLES
-- =========================
INSERT INTO tb_role (authority) VALUES ('ROLE_OPERATOR');
INSERT INTO tb_role (authority) VALUES ('ROLE_ADMIN');

-- =========================
-- USER-ROLE RELATIONSHIP
-- =========================
INSERT INTO tb_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO tb_user_role (user_id, role_id) VALUES (2, 1);
INSERT INTO tb_user_role (user_id, role_id) VALUES (2, 2);

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Electronics', 'Electronic devices and gadgets', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Books', 'Books and literature', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Computers', 'Computers, laptops and accessories', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Home Appliances', 'Appliances for home use', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Furniture', 'Home and office furniture', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Toys', 'Toys and games for children', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Clothing', 'Men and women clothing', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Shoes', 'Footwear and sneakers', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Sports', 'Sports equipment and accessories', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Health', 'Health and personal care products', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Beauty', 'Beauty and cosmetics products', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Automotive', 'Car parts and automotive accessories', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Garden', 'Garden and outdoor products', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Pet Supplies', 'Products for pets and animals', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Office Supplies', 'Office materials and supplies', true, NOW(), NOW());
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Notebooks', 'Laptops for work, study, gaming and software development', true, TIMESTAMP '2026-06-10 12:07:00', TIMESTAMP '2026-06-12 15:22:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Monitors', 'Computer monitors for office, gaming and content creation', true, TIMESTAMP '2024-03-20 08:46:00', TIMESTAMP '2024-03-30 15:14:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Smartphones', 'Smartphones and mobile phones', true, TIMESTAMP '2024-12-23 11:12:00', TIMESTAMP '2025-01-13 16:46:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Tablets', 'Tablets for reading, drawing, study and entertainment', true, TIMESTAMP '2024-03-13 14:06:00', TIMESTAMP '2024-03-31 15:17:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Keyboards', 'Mechanical, wireless and office keyboards', true, TIMESTAMP '2025-02-19 17:38:00', TIMESTAMP '2025-03-03 18:06:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Mice', 'Gaming, ergonomic and wireless mice', true, TIMESTAMP '2025-07-15 16:34:00', TIMESTAMP '2025-07-27 23:48:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Headsets', 'Headsets for gaming, calls and remote work', true, TIMESTAMP '2026-04-16 12:52:00', TIMESTAMP '2026-05-15 22:01:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Graphics Cards', 'Video cards for gaming, design and rendering', true, TIMESTAMP '2026-01-03 17:51:00', TIMESTAMP '2026-02-04 19:59:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Processors', 'Desktop processors and CPUs', true, TIMESTAMP '2024-02-05 15:24:00', TIMESTAMP '2024-02-08 19:56:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Storage', 'SSDs, hard drives and external storage', true, TIMESTAMP '2024-07-11 17:57:00', TIMESTAMP '2024-08-08 03:29:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Computer Accessories', 'Webcams, hubs, stands and other computer accessories', true, TIMESTAMP '2025-08-06 18:00:00', TIMESTAMP '2025-09-06 21:28:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Networking', 'Routers, switches and Wi-Fi equipment', true, TIMESTAMP '2024-07-06 10:54:00', TIMESTAMP '2024-07-09 13:15:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Televisions', 'TVs, projectors and home cinema', true, TIMESTAMP '2025-06-27 13:34:00', TIMESTAMP '2025-07-22 20:05:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Cameras', 'Digital, action and security cameras', true, TIMESTAMP '2024-02-16 11:17:00', TIMESTAMP '2024-02-28 11:58:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Audio', 'Headphones, speakers and soundbars', true, TIMESTAMP '2025-03-30 10:06:00', TIMESTAMP '2025-04-30 10:52:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Wearables', 'Smartwatches and fitness bands', true, TIMESTAMP '2025-09-22 16:03:00', TIMESTAMP '2025-10-10 19:15:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Game Consoles', 'Video game consoles and controllers', true, TIMESTAMP '2025-02-24 10:16:00', TIMESTAMP '2025-02-26 13:25:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Smart Home', 'Connected devices for home automation', true, TIMESTAMP '2024-11-22 14:31:00', TIMESTAMP '2024-11-22 14:34:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Bicycles', 'Mountain, road, urban and electric bicycles', true, TIMESTAMP '2024-05-14 17:30:00', TIMESTAMP '2024-06-02 02:54:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Cycling Accessories', 'Helmets, gloves, GPS and accessories for cyclists', true, TIMESTAMP '2024-10-18 11:16:00', TIMESTAMP '2024-11-27 12:22:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Running', 'Running shoes, apparel and accessories', true, TIMESTAMP '2024-09-23 08:55:00', TIMESTAMP '2024-10-21 11:49:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Fitness Equipment', 'Equipment for home workouts and training', true, TIMESTAMP '2025-06-03 12:51:00', TIMESTAMP '2025-07-10 21:00:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Cars', 'New cars, SUVs, pickups and electric vehicles', true, TIMESTAMP '2025-10-19 13:31:00', TIMESTAMP '2025-10-25 23:31:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Motorcycles', 'Motorcycles, scooters and rider gear', true, TIMESTAMP '2026-02-27 08:55:00', TIMESTAMP '2026-03-06 09:47:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Automotive Tools', 'Tools and diagnostics for vehicle maintenance', true, TIMESTAMP '2026-05-23 10:20:00', TIMESTAMP '2026-06-27 19:31:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Watches', 'Analog, automatic and digital watches', true, TIMESTAMP '2024-08-31 11:37:00', TIMESTAMP '2024-09-17 19:42:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Lighting', 'Lamps, pendants and LED lighting', true, TIMESTAMP '2025-10-22 08:54:00', TIMESTAMP '2025-11-15 13:57:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Home Organization', 'Organizers, boxes and storage solutions', true, TIMESTAMP '2024-11-12 15:41:00', TIMESTAMP '2024-11-20 15:41:00');
INSERT INTO tb_category(name, description, active, created_at, updated_at) VALUES ('Legacy Hardware', 'Discontinued hardware kept for reference', false, TIMESTAMP '2026-02-01 12:14:00', TIMESTAMP '2026-02-02 18:38:00');


-- =========================
-- PRODUCTS
-- =========================
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Eloquent JavaScript', 90.5, NOW(), NOW(), 'Introdução moderna à linguagem JavaScript, de Marijn Haverbeke, com foco em programação, estruturas de dados e projetos práticos.', true, 'https://covers.openlibrary.org/b/id/7082166-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smart TV', 2190.0, NOW(), NOW(), 'Smart TV com alta resolução, acesso a streaming e conectividade Wi-Fi.', true, 'https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/2-big.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Macbook Pro', 1250.0, NOW(), NOW(), 'Notebook de alto desempenho ideal para desenvolvimento e produtividade.', true, 'https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/3-big.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer', 1200.0, NOW(), NOW(), 'Computador gamer com bom desempenho para jogos atuais.', true, 'https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/4-big.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Ruby on Rails For Dummies', 100.99, NOW(), NOW(), 'Livro introdutório sobre Ruby on Rails para iniciantes, de Barry Burd.', true, 'https://covers.openlibrary.org/b/id/299713-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Ex', 1350.0, NOW(), NOW(), 'PC gamer com desempenho aprimorado e melhor capacidade gráfica.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2025_Wn%C4%99trze_komputera_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer X', 1350.0, NOW(), NOW(), 'PC gamer equilibrado com bom custo-benefício.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2026_Wn%C4%99trze_komputera_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Alfa', 1850.0, NOW(), NOW(), 'PC gamer potente com foco em desempenho gráfico.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Ricer_gaming_PC_with_CPU_watercooler.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Tera', 1950.0, NOW(), NOW(), 'Computador gamer com alta capacidade de processamento.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaming_PC_(Unsplash).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Y', 1700.0, NOW(), NOW(), 'PC gamer intermediário ideal para jogos online.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Water_Cooled_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Nitro', 1450.0, NOW(), NOW(), 'PC gamer com boa refrigeração e desempenho estável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaming_computer_with_bat_decals.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Card', 1850.0, NOW(), NOW(), 'PC gamer com placa de vídeo dedicada para alto desempenho.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/PC-Geh%C3%A4use_Kolink_Observatory_RGB_Midi-Tower_20201120_DSC6134.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Plus', 1350.0, NOW(), NOW(), 'PC gamer com melhorias em memória e processamento.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Custom_built_computer_from_2016.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Hera', 2250.0, NOW(), NOW(), 'PC gamer premium para jogos pesados e streaming.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaming_computer_with_bat_decals.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Weed', 2200.0, NOW(), NOW(), 'PC gamer de alto desempenho com foco em velocidade.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Asus_Strix_RTX_4090_operational.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Max', 2340.0, NOW(), NOW(), 'PC gamer topo de linha com máximo desempenho.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Asus_Strix_RTX_4090_operational_with_aftermarket_12VHPWR_cable.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Turbo', 1280.0, NOW(), NOW(), 'PC gamer focado em velocidade com bom custo-benefício.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Asus_Strix_RTX_4090_operational_corner_view.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Hot', 1450.0, NOW(), NOW(), 'PC gamer com sistema de refrigeração eficiente.', true, 'https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/4-big.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Ez', 1750.0, NOW(), NOW(), 'PC gamer fácil de usar, ideal para iniciantes.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2025_Wn%C4%99trze_komputera_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Tr', 1650.0, NOW(), NOW(), 'PC gamer intermediário com desempenho equilibrado.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2026_Wn%C4%99trze_komputera_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Tx', 1680.0, NOW(), NOW(), 'PC gamer versátil para jogos e produtividade.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Ricer_gaming_PC_with_CPU_watercooler.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Er', 1850.0, NOW(), NOW(), 'PC gamer robusto para jogos exigentes.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaming_PC_(Unsplash).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Min', 2250.0, NOW(), NOW(), 'PC gamer compacto e potente.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Water_Cooled_PC.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Boo', 2350.0, NOW(), NOW(), 'PC gamer premium com alto desempenho gráfico.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaming_computer_with_bat_decals.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('PC Gamer Foo', 4170.0, NOW(), NOW(), 'PC gamer de altíssimo desempenho para jogos em 4K.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/PC-Geh%C3%A4use_Kolink_Observatory_RGB_Midi-Tower_20201120_DSC6134.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Aurora Pro 14', 7499.9, TIMESTAMP '2024-03-03 09:06:00', TIMESTAMP '2024-03-31 12:28:00', 'Notebook de 14 polegadas com processador de 12 núcleos, 32 GB de memória e SSD de 1 TB, ideal para desenvolvimento e multitarefa.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Computer_Running_Raptor_Linux.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Aurora Ultra 15', 9899.0, TIMESTAMP '2026-05-03 12:37:00', TIMESTAMP '2026-06-08 20:08:00', 'Notebook premium de 15 polegadas com tela OLED, bateria de longa duração e carcaça de alumínio.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Asus_x21_ultrabook.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Business Vertex 14', 5290.0, TIMESTAMP '2024-11-14 14:37:00', TIMESTAMP '2024-11-25 23:06:00', 'Notebook corporativo leve, com teclado resistente a derramamentos, leitor de digitais e 16 GB de memória.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/IBM_Thinkpad_R51.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Gamer Titan 16', 12499.0, TIMESTAMP '2025-11-09 14:45:00', TIMESTAMP '2025-11-30 21:18:00', 'Notebook gamer de 16 polegadas com placa de vídeo dedicada, tela de 240 Hz e sistema de refrigeração reforçado.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/MSI_Gaming_Laptop_on_wood_floor.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Developer Nimbus 15', 8350.5, TIMESTAMP '2025-03-14 17:26:00', TIMESTAMP '2025-03-20 03:10:00', 'Notebook voltado a programadores, com 32 GB de memória, SSD rápido e teclado confortável para longas jornadas de código.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Arch_Linux_system_update_via_pacman_on_an_Acer_laptop.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Notebook Essential Lite 14', 2799.99, TIMESTAMP '2024-06-20 14:54:00', TIMESTAMP '2024-07-14 23:32:00', 'Notebook de entrada para estudos e tarefas do dia a dia, com 8 GB de memória e SSD de 256 GB.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Toshiba_Satellite_Pro_(white_background).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Monitor UltraWide 34 Curvo', 3299.0, TIMESTAMP '2024-03-14 14:03:00', TIMESTAMP '2024-04-20 18:51:00', 'Monitor ultrawide curvo de 34 polegadas com resolução QHD, ideal para produtividade e edição.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/ASUS_curved_monitor_20170603.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Monitor 4K Creator 27', 2899.9, TIMESTAMP '2025-12-22 10:03:00', TIMESTAMP '2026-01-17 17:49:00', 'Monitor 4K de 27 polegadas com cobertura ampla de cores para criação de conteúdo e design.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Computer_monitor.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Monitor Gamer 24 144Hz', 1149.0, TIMESTAMP '2024-12-15 11:50:00', TIMESTAMP '2024-12-30 20:51:00', 'Monitor gamer de 24 polegadas com taxa de atualização de 144 Hz e tempo de resposta de 1 ms.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Computer_monitor_samsung.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Monitor Office 22 Full HD', 649.0, TIMESTAMP '2025-07-01 17:07:00', TIMESTAMP '2025-08-02 01:48:00', 'Monitor Full HD de 22 polegadas com painel antirreflexo para uso em escritório.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/LG_L194WT-SF_LCD_monitor.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Monitor Portátil 15.6 USB-C', 1390.0, TIMESTAMP '2025-06-27 10:00:00', TIMESTAMP '2025-07-29 10:46:00', 'Monitor portátil fino de 15,6 polegadas, alimentado por USB-C, para ampliar a tela do notebook em qualquer lugar.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/ASUS_ZenScreen_MB16AC_20170603.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartphone Orion X 256GB', 4599.0, TIMESTAMP '2024-03-09 08:15:00', TIMESTAMP '2024-03-15 08:19:00', 'Smartphone com tela AMOLED de 6,6 polegadas, câmera tripla de 50 MP e 256 GB de armazenamento.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Smartphone_Use.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartphone Orion Lite 128GB', 1899.9, TIMESTAMP '2026-05-26 13:06:00', TIMESTAMP '2026-06-04 21:30:00', 'Smartphone intermediário com boa bateria, câmera dupla e 128 GB de armazenamento.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Samsung_galaxy_young.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartphone Pulse Pro 512GB', 6999.0, TIMESTAMP '2025-06-08 11:00:00', TIMESTAMP '2025-07-06 13:42:00', 'Smartphone topo de linha com carregamento rápido, câmera com estabilização óptica e 512 GB.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Blackview_A60_Smartphone_Android_mobile_phone_back_face.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartphone Essential 64GB', 999.0, TIMESTAMP '2024-10-08 10:41:00', TIMESTAMP '2024-10-29 11:20:00', 'Smartphone básico para mensagens, redes sociais e chamadas, com 64 GB de armazenamento.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Blackview_A60_Smartphone_Android_mobile_phone_and_folio_case.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tablet Slate 11 128GB', 2999.0, TIMESTAMP '2025-12-21 11:02:00', TIMESTAMP '2025-12-21 16:15:00', 'Tablet de 11 polegadas com 128 GB, ótimo para leitura, estudos e streaming.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/IPad_Wiki.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tablet Slate Pro 12.9', 5799.0, TIMESTAMP '2024-11-17 14:33:00', TIMESTAMP '2024-12-12 19:54:00', 'Tablet profissional de 12,9 polegadas com tela de alta taxa de atualização e suporte a caneta digital.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/IPad_and_iPad_Pro.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tablet Kids Explorer 8', 899.0, TIMESTAMP '2025-09-11 17:58:00', TIMESTAMP '2025-10-11 01:06:00', 'Tablet infantil de 8 polegadas com capa emborrachada e controle parental.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Nabi_SE_and_American_Girl_tablets.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Teclado Mecânico Forge TKL', 429.9, TIMESTAMP '2025-12-06 15:54:00', TIMESTAMP '2026-01-10 16:15:00', 'Teclado mecânico compacto sem teclado numérico, com switches táteis e iluminação personalizável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Lingbao_JIGUANSHI_Mechanical_Gaming_Keyboard.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Teclado Sem Fio Slim Office', 189.0, TIMESTAMP '2024-10-16 11:22:00', TIMESTAMP '2024-11-23 15:55:00', 'Teclado sem fio de perfil baixo, silencioso e com bateria de longa duração.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Turkish_Q_computer_keyboard.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Teclado Gamer RGB Vortex', 349.0, TIMESTAMP '2025-02-10 10:02:00', TIMESTAMP '2025-03-08 11:11:00', 'Teclado gamer com iluminação RGB, teclas antighosting e apoio de pulso destacável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Mechanical_Keyboard.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Mouse Gamer Viper 16000 DPI', 259.9, TIMESTAMP '2024-07-23 17:43:00', TIMESTAMP '2024-07-24 20:44:00', 'Mouse gamer com sensor óptico de 16000 DPI, oito botões programáveis e cabo trançado.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Mysz_komputerowa_Logitech_G903_Lightspeed.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Mouse Ergonômico Vertical Flow', 219.0, TIMESTAMP '2024-08-12 16:03:00', TIMESTAMP '2024-09-04 17:12:00', 'Mouse vertical que reduz a tensão no pulso, indicado para uso prolongado.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/A_wireless_computer_mouse.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Mouse Sem Fio Compact', 89.9, TIMESTAMP '2026-06-16 15:54:00', TIMESTAMP '2026-07-23 01:36:00', 'Mouse sem fio compacto com receptor USB, ideal para notebooks e viagens.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Wireless_Logilink_Computer_mouse_with_USB_Micro_Dongle.JPG?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Headset Gamer Surround 7.1', 399.0, TIMESTAMP '2024-04-17 17:10:00', TIMESTAMP '2024-04-28 20:32:00', 'Headset gamer com som surround virtual 7.1, microfone removível e almofadas macias.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Xbox-360-Headset-White.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Headset Office Bluetooth NC', 549.0, TIMESTAMP '2024-08-13 14:57:00', TIMESTAMP '2024-09-17 16:35:00', 'Headset Bluetooth com cancelamento de ruído e microfone para reuniões e trabalho remoto.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Xbox-360-Headset-Mk2-Black.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Fone de Ouvido Studio Pro', 898.0, TIMESTAMP '2024-05-12 15:03:00', TIMESTAMP '2024-05-25 21:17:00', 'Fone de ouvido over-ear de estúdio com resposta plana de frequência para produção e edição de áudio.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Bose_QuietComfort_25_Acoustic_Noise_Cancelling_Headphones_with_Carry_Case.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Placa de Vídeo Nova RX 8GB', 2899.0, TIMESTAMP '2026-06-09 10:30:00', TIMESTAMP '2026-07-19 11:42:00', 'Placa de vídeo com 8 GB de memória, boa para jogos em Full HD e QHD.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Sapphire-Radeon-HD-5570-Video-Card.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Placa de Vídeo Nova RX Ti 16GB', 5490.0, TIMESTAMP '2026-06-16 17:50:00', TIMESTAMP '2026-06-24 21:59:00', 'Placa de vídeo de alto desempenho com 16 GB, ray tracing e suporte a jogos em 4K.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_MSI_GeForce_RTX_2080_Gaming_X_Trio_(3).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Placa de Vídeo Spectra 12GB', 3990.0, TIMESTAMP '2024-05-28 08:34:00', TIMESTAMP '2024-05-29 10:05:00', 'Placa de vídeo com 12 GB de memória, ótima relação entre desempenho e consumo de energia.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Zotac_GeForce_RTX_3060_12GB_VRAM_LHR_video_card.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Processador Quantum 8 Núcleos', 1399.0, TIMESTAMP '2024-02-25 10:43:00', TIMESTAMP '2024-03-22 11:41:00', 'Processador de 8 núcleos e 16 threads com frequência de até 4,9 GHz.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Intel_Core_i7_12700KF_(5).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Processador Quantum 16 Núcleos', 3298.0, TIMESTAMP '2025-12-27 12:29:00', TIMESTAMP '2026-01-12 14:13:00', 'Processador de 16 núcleos para renderização, compilação e cargas de trabalho pesadas.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Intel_Core_i7_12700KF_(2).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Processador Helix 6 Núcleos', 749.0, TIMESTAMP '2026-07-05 17:23:00', TIMESTAMP '2026-08-05 00:33:00', 'Processador de 6 núcleos com gráficos integrados, ideal para PCs de escritório e entrada.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Intel_Core_i7_8700_(3).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('SSD NVMe 1TB Velocity', 549.9, TIMESTAMP '2025-06-06 14:58:00', TIMESTAMP '2025-06-19 17:37:00', 'SSD NVMe de 1 TB com leitura sequencial de até 7000 MB/s.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Nap%C4%99d_SSD_Hikvision_G4000_2TB_(3).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('SSD SATA 500GB Core', 279.0, TIMESTAMP '2024-04-10 13:59:00', TIMESTAMP '2024-05-03 21:00:00', 'SSD SATA de 500 GB para atualizar computadores mais antigos com custo acessível.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/2023_Nap%C4%99d_SSD_GoodRam_CX100_120GB.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('HD Externo 2TB Portátil', 389.0, TIMESTAMP '2024-05-03 08:40:00', TIMESTAMP '2024-06-05 15:19:00', 'HD externo portátil de 2 TB com conexão USB 3.0 e proteção contra impactos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Toshiba_1_TB_External_USB_Hard_Drive.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('HD Interno 12TB NAS', 2290.0, TIMESTAMP '2024-11-14 12:52:00', TIMESTAMP '2024-11-20 18:04:00', 'HD de 12 TB projetado para operação contínua em servidores NAS.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Seagate_ST12000NM0117_2GY101-111_20190718.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Webcam Full HD Stream', 278.0, TIMESTAMP '2026-08-11 08:55:00', TIMESTAMP '2026-08-28 16:22:00', 'Webcam Full HD com foco automático e microfone duplo para videochamadas e transmissões.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Webcam_01.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Hub USB-C 7 em 1', 199.0, TIMESTAMP '2024-02-11 11:45:00', TIMESTAMP '2024-03-14 19:30:00', 'Hub USB-C com HDMI, leitor de cartões, portas USB 3.0 e carregamento passante.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/USB_HUB_2.0.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Suporte Ergonômico para Notebook', 129.0, TIMESTAMP '2024-07-22 17:49:00', TIMESTAMP '2024-08-21 02:47:00', 'Suporte de alumínio ajustável em altura que melhora a postura durante o trabalho.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Laptop_stand.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Roteador Wi-Fi 6 AX3000', 599.0, TIMESTAMP '2025-01-13 11:47:00', TIMESTAMP '2025-01-24 19:08:00', 'Roteador Wi-Fi 6 dual band com cobertura ampla e múltiplas portas Gigabit.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/ASUS_Wi-Fi_Router_(48767215803).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Switch Gigabit 8 Portas', 159.0, TIMESTAMP '2026-05-29 11:00:00', TIMESTAMP '2026-06-05 15:00:00', 'Switch de mesa com 8 portas Gigabit, sem configuração, para redes domésticas e pequenos escritórios.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Raspberry_Pi_4_on_a_network_switch.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Kit Mesh Wi-Fi 3 Unidades', 1199.0, TIMESTAMP '2024-09-06 17:35:00', TIMESTAMP '2024-10-07 18:17:00', 'Sistema mesh com três unidades para cobrir toda a casa com um único nome de rede.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Eero_6%2B_Image.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('TV OLED 65 4K Lumen', 8999.0, TIMESTAMP '2025-04-11 13:37:00', TIMESTAMP '2025-04-26 17:52:00', 'TV OLED de 65 polegadas 4K com pretos perfeitos, HDR dinâmico e taxa de 120 Hz.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/LG_%EB%A1%9C%EB%B4%87,_%EC%97%85%EA%B3%84_%EC%B2%AB_%EB%94%94%EC%9E%90%EC%9D%B8_%27%EB%8C%80%ED%86%B5%EB%A0%B9%EC%83%81%27_%EC%88%98%EC%83%81_(24107621107).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('TV QLED 55 4K Prisma', 4299.0, TIMESTAMP '2024-10-10 08:23:00', TIMESTAMP '2024-10-16 17:13:00', 'TV QLED de 55 polegadas 4K com cores vibrantes e sistema smart integrado.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Taipei_IT_Month_LG_55EA980T_20131202.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smart TV LED 43 Full HD', 1899.0, TIMESTAMP '2024-02-15 10:07:00', TIMESTAMP '2024-03-02 17:05:00', 'Smart TV LED de 43 polegadas Full HD com aplicativos de streaming pré-instalados.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Techwood_flat_screen_television_from_2010.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smart TV 32 HD Essencial', 1099.0, TIMESTAMP '2025-01-22 16:13:00', TIMESTAMP '2025-02-01 22:03:00', 'Smart TV de 32 polegadas HD, compacta, ideal para quarto ou cozinha.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/A_flat-screen_television.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Projetor Full HD Cine Home', 2790.0, TIMESTAMP '2026-05-16 17:52:00', TIMESTAMP '2026-05-18 21:16:00', 'Projetor Full HD com brilho de 3000 lúmens para cinema em casa e apresentações.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/InFocus_IN34.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Câmera Mirrorless Zeta 24MP', 6890.0, TIMESTAMP '2025-01-13 15:51:00', TIMESTAMP '2025-02-16 17:27:00', 'Câmera mirrorless com sensor de 24 MP, gravação 4K e lente intercambiável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Sony_Alpha_7R_IV_elevated_front_view_of_camera_body_with_exposed_sensor.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Câmera Compacta Travel Zoom', 2189.0, TIMESTAMP '2025-03-18 11:54:00', TIMESTAMP '2025-04-18 17:15:00', 'Câmera compacta com zoom óptico de 30x, leve para viagens.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/12_MP_digital_camera_-_red.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Câmera de Ação 4K Trail', 1290.0, TIMESTAMP '2026-07-15 10:51:00', TIMESTAMP '2026-08-13 13:08:00', 'Câmera de ação à prova d''água com gravação 4K e estabilização eletrônica.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/GoPro_Hero_9_Black_-_Front_2.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Fone de Ouvido Bluetooth Air', 348.0, TIMESTAMP '2025-05-16 11:41:00', TIMESTAMP '2025-05-17 19:26:00', 'Fones sem fio intra-auriculares com estojo carregador e até 24 horas de bateria.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/ActiveSound_wireless_earbuds_by_Hykker_(POJM200483).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Caixa de Som Portátil Boom', 299.0, TIMESTAMP '2025-05-09 15:44:00', TIMESTAMP '2025-05-27 22:56:00', 'Caixa de som Bluetooth resistente à água com graves potentes.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/JBL_Flip_4.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Soundbar 2.1 Cinema', 1198.0, TIMESTAMP '2026-02-18 09:25:00', TIMESTAMP '2026-03-20 11:58:00', 'Soundbar 2.1 com subwoofer sem fio e modo dedicado a filmes e diálogos.', true, 'https://placehold.co/640x480/37474f/ffffff/png?text=Soundbar%202.1%0ACinema');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartwatch Pulse Fit', 1299.0, TIMESTAMP '2025-08-30 11:46:00', TIMESTAMP '2025-08-31 18:43:00', 'Smartwatch com GPS, monitor cardíaco, oxímetro e mais de 100 modos esportivos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Android_Wear_Smartwatch-_LG_G_Watch_(15051774155).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Smartband Active 5', 249.0, TIMESTAMP '2025-02-14 16:04:00', TIMESTAMP '2025-03-02 22:55:00', 'Pulseira inteligente com monitoramento de sono, passos e frequência cardíaca.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Samsung_Galaxy_Fit2.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Console Nexus Edição Digital', 3499.0, TIMESTAMP '2024-07-25 11:18:00', TIMESTAMP '2024-07-27 21:05:00', 'Console de nova geração sem leitor de disco, com SSD ultrarrápido e jogos em 4K.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Microsoft-Xbox-One-Console-Set-wKinect.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Console Portátil Handy Play', 2599.0, TIMESTAMP '2026-05-20 09:09:00', TIMESTAMP '2026-05-21 18:43:00', 'Console portátil com tela OLED de 7 polegadas e biblioteca de jogos para levar para qualquer lugar.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Psp-1000.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Controle Sem Fio Nexus Pro', 449.0, TIMESTAMP '2026-03-19 09:49:00', TIMESTAMP '2026-04-06 16:15:00', 'Controle sem fio com gatilhos adaptáveis e vibração de alta precisão.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/M-tech_Gamepad_Joystick_(52711424910).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Lâmpada Inteligente Wi-Fi RGB', 59.9, TIMESTAMP '2025-07-13 16:11:00', TIMESTAMP '2025-07-17 21:40:00', 'Lâmpada LED Wi-Fi com 16 milhões de cores, controlada por aplicativo e assistente de voz.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/LED_bulb_800_Lm_Soft_white_2024.agr.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tomada Inteligente Wi-Fi 10A', 49.9, TIMESTAMP '2024-10-07 12:19:00', TIMESTAMP '2024-11-16 20:24:00', 'Tomada inteligente com agendamento e monitoramento de consumo de energia.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Wemo_Mini_Smart_Plug_(3750).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Câmera de Segurança Wi-Fi 360', 229.0, TIMESTAMP '2024-08-10 09:18:00', TIMESTAMP '2024-09-10 16:22:00', 'Câmera de segurança giratória 360 graus com visão noturna e detecção de movimento.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Security_camera_around_Ebisu_Garden_Place.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Head First Java', 119.9, TIMESTAMP '2025-02-22 12:50:00', TIMESTAMP '2025-03-20 14:46:00', 'Introdução visual e descontraída à linguagem Java, de Kathy Sierra e Bert Bates, com exercícios e ilustrações sobre orientação a objetos e fundamentos da linguagem.', true, 'https://covers.openlibrary.org/b/id/389102-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Java Concurrency in Practice', 149.9, TIMESTAMP '2024-05-28 16:55:00', TIMESTAMP '2024-06-07 19:51:00', 'Referência de Brian Goetz e coautores sobre programação concorrente em Java: threads, sincronização, executores e projeto de classes thread-safe.', true, 'https://covers.openlibrary.org/b/id/7069893-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Spring Boot in Action', 139.9, TIMESTAMP '2024-01-21 16:09:00', TIMESTAMP '2024-02-16 00:39:00', 'Guia de Craig Walls para criar aplicações com Spring Boot, cobrindo autoconfiguração, starters, testes e monitoramento com Actuator.', true, 'https://covers.openlibrary.org/b/id/8511672-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Spring Security in Action', 128.0, TIMESTAMP '2025-05-13 13:00:00', TIMESTAMP '2025-06-09 21:56:00', 'Livro de Laurentiu Spilca sobre autenticação, autorização e proteção de aplicações com Spring Security.', true, 'https://covers.openlibrary.org/b/id/13276048-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Spring in Action', 134.9, TIMESTAMP '2024-06-18 16:16:00', TIMESTAMP '2024-06-25 21:50:00', 'Obra de Craig Walls que apresenta o framework Spring: injeção de dependências, web MVC, persistência de dados e segurança.', true, 'https://covers.openlibrary.org/b/id/8513317-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('HTML and CSS: Design and Build Websites', 124.9, TIMESTAMP '2024-10-27 16:13:00', TIMESTAMP '2024-11-23 01:33:00', 'Jon Duckett apresenta HTML e CSS de forma visual para projetar e construir sites, com layouts, tipografia e imagens.', true, 'https://covers.openlibrary.org/b/id/12398906-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Patterns of Enterprise Application Architecture', 158.0, TIMESTAMP '2025-07-13 13:07:00', TIMESTAMP '2025-07-25 14:29:00', 'Catálogo de padrões de arquitetura de aplicações corporativas de Martin Fowler, como Domain Model, Repository e Unit of Work, para organizar sistemas de negócio em camadas.', true, 'https://covers.openlibrary.org/b/id/192501-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('JavaScript: The Good Parts', 109.9, TIMESTAMP '2025-04-19 11:01:00', TIMESTAMP '2025-05-28 16:37:00', 'Douglas Crockford destaca os recursos mais confiáveis do JavaScript e recomenda evitar os problemáticos para escrever código mais claro e robusto.', true, 'https://covers.openlibrary.org/b/id/9245523-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Clean Code', 99.9, TIMESTAMP '2025-03-24 15:54:00', TIMESTAMP '2025-04-18 21:15:00', 'Clássico de Robert C. Martin sobre como escrever código limpo, legível e fácil de manter, com foco em nomes, funções, comentários e tratamento de erros.', true, 'https://covers.openlibrary.org/b/id/8065615-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Design Patterns: Elements of Reusable Object-Oriented Software', 114.9, TIMESTAMP '2024-06-13 08:35:00', TIMESTAMP '2024-06-18 18:02:00', 'Obra clássica de Gamma, Helm, Johnson e Vlissides que cataloga 23 padrões de projeto orientados a objetos reutilizáveis.', true, 'https://covers.openlibrary.org/b/id/13750918-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Introduction to Algorithms', 129.9, TIMESTAMP '2026-01-19 14:36:00', TIMESTAMP '2026-02-24 19:03:00', 'Livro-texto de referência sobre algoritmos e estruturas de dados, com análise de complexidade e demonstrações.', true, 'https://covers.openlibrary.org/b/id/15111140-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Effective Java', 119.0, TIMESTAMP '2025-03-25 09:05:00', TIMESTAMP '2025-05-04 16:20:00', 'Guia clássico de Joshua Bloch com boas práticas e idiomas para escrever programas Java mais seguros, claros e reutilizáveis.', true, 'https://covers.openlibrary.org/b/id/12603105-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Designing Data-Intensive Applications', 104.9, TIMESTAMP '2024-11-04 12:22:00', TIMESTAMP '2024-11-18 19:55:00', 'Martin Kleppmann explica os princípios dos sistemas de dados modernos: armazenamento, replicação, particionamento, transações e processamento distribuído.', true, 'https://covers.openlibrary.org/b/id/8434671-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Growing Object-Oriented Software, Guided by Tests', 118.9, TIMESTAMP '2025-10-21 15:12:00', TIMESTAMP '2025-12-01 00:43:00', 'Steve Freeman e Nat Pryce mostram como desenvolver software orientado a objetos guiado por testes, com objetos simulados e testes de aceitação.', true, 'https://covers.openlibrary.org/b/id/14949057-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Clean Architecture', 169.9, TIMESTAMP '2026-02-22 09:59:00', TIMESTAMP '2026-02-22 14:12:00', 'Robert C. Martin apresenta princípios de arquitetura de software, como limites entre camadas e a regra de dependência, para construir sistemas fáceis de evoluir.', true, 'https://covers.openlibrary.org/b/id/8605114-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Enterprise Integration Patterns', 179.9, TIMESTAMP '2025-08-08 15:29:00', TIMESTAMP '2025-08-13 15:52:00', 'Gregor Hohpe e Bobby Woolf catalogam padrões de mensageria para integrar sistemas corporativos de forma assíncrona.', true, 'https://covers.openlibrary.org/b/id/14857293-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('xUnit Test Patterns', 94.9, TIMESTAMP '2025-06-14 08:46:00', TIMESTAMP '2025-06-30 10:33:00', 'Gerard Meszaros descreve padrões e antipadrões para escrever testes automatizados legíveis e de fácil manutenção com a família xUnit.', true, 'https://covers.openlibrary.org/b/id/87720-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Test-Driven Development: By Example', 88.9, TIMESTAMP '2025-02-03 16:39:00', TIMESTAMP '2025-03-14 17:58:00', 'Kent Beck ensina o desenvolvimento guiado por testes por meio de exemplos práticos e do ciclo vermelho, verde e refatorar.', true, 'https://covers.openlibrary.org/b/id/192577-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('The Clean Coder', 149.0, TIMESTAMP '2025-06-26 12:04:00', TIMESTAMP '2025-07-26 20:24:00', 'Robert C. Martin discute a conduta profissional do programador: responsabilidade, estimativas, prazos, testes e trabalho em equipe.', true, 'https://covers.openlibrary.org/b/id/7318893-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Continuous Delivery', 139.0, TIMESTAMP '2025-03-04 09:48:00', TIMESTAMP '2025-04-07 18:31:00', 'Jez Humble e David Farley detalham como automatizar build, testes e implantação para entregar software de forma rápida e confiável.', true, 'https://covers.openlibrary.org/b/id/6998977-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('The DevOps Handbook', 128.9, TIMESTAMP '2024-06-17 10:52:00', TIMESTAMP '2024-06-22 14:55:00', 'Guia de Gene Kim, Jez Humble, Patrick Debois e John Willis para adotar práticas DevOps que aumentam agilidade, confiabilidade e segurança na entrega de software.', true, 'https://covers.openlibrary.org/b/id/10860557-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Pro Git', 144.9, TIMESTAMP '2025-06-03 13:47:00', TIMESTAMP '2025-06-10 18:51:00', 'Livro de Scott Chacon e Ben Straub sobre o Git, do uso básico a ramificações, fluxos de trabalho e funcionamento interno.', true, 'https://covers.openlibrary.org/b/id/7892827-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Domain-Driven Design: Tackling Complexity in the Heart of Software', 164.9, TIMESTAMP '2025-06-12 17:49:00', TIMESTAMP '2025-07-08 22:14:00', 'Eric Evans propõe modelar o domínio do negócio com linguagem ubíqua, agregados e contextos delimitados para lidar com a complexidade do software.', true, 'https://covers.openlibrary.org/b/id/7375231-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Refactoring: Improving the Design of Existing Code', 117.9, TIMESTAMP '2025-08-26 17:09:00', TIMESTAMP '2025-09-27 01:22:00', 'Martin Fowler apresenta um catálogo de refatorações para melhorar o projeto de código existente sem alterar seu comportamento.', true, 'https://covers.openlibrary.org/b/id/8507565-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Head First Design Patterns', 189.9, TIMESTAMP '2026-02-26 16:14:00', TIMESTAMP '2026-03-01 00:11:00', 'Introdução visual aos padrões de projeto orientados a objetos, de Eric Freeman e Elisabeth Robson, com exemplos em Java.', true, 'https://covers.openlibrary.org/b/id/388950-L.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Bicicleta Mountain Bike Aro 29 Trail', 3297.0, TIMESTAMP '2025-12-02 15:00:00', TIMESTAMP '2025-12-31 16:02:00', 'Mountain bike aro 29 com quadro de alumínio, suspensão dianteira e 21 marchas.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Cube,_Cyclingworld_Europe_2024,_Meerbusch_(P1180003).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Bicicleta Speed Carbon Road Pro', 12990.0, TIMESTAMP '2024-11-15 13:29:00', TIMESTAMP '2024-12-04 15:42:00', 'Bicicleta de estrada com quadro de carbono, grupo de 22 marchas e freios a disco.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Guerciotti_with_Rolf_Elan_Wheelset.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Bicicleta Urbana Aro 26 Cidade', 1490.0, TIMESTAMP '2024-04-27 16:22:00', TIMESTAMP '2024-05-01 23:17:00', 'Bicicleta urbana confortável com cestinha, paralamas e selim ergonômico.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Azor_Oma_Oklahoma_R3D_Violet_Matt.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Bicicleta Elétrica City Motion', 7990.0, TIMESTAMP '2025-02-20 13:04:00', TIMESTAMP '2025-03-16 18:04:00', 'Bicicleta elétrica com motor de 350 W, bateria removível e autonomia de até 60 km.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Jump_Electric_Bicycle_by_Uber_in_Munich.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Capacete de Ciclismo Aero', 388.0, TIMESTAMP '2026-06-28 12:02:00', TIMESTAMP '2026-06-30 13:36:00', 'Capacete leve e ventilado com regulagem de tamanho e certificação de segurança.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Met_trenta_3k_carbon_mips_bicycle_helmet.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Ciclocomputador GPS Route', 799.0, TIMESTAMP '2024-05-24 16:24:00', TIMESTAMP '2024-06-07 19:54:00', 'Ciclocomputador com GPS, mapas de rotas, sensores de cadência e velocidade.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Mile_High_Ride.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Luvas de Ciclismo Gel', 87.9, TIMESTAMP '2024-01-20 13:20:00', TIMESTAMP '2024-02-19 15:27:00', 'Luvas com acolchoamento em gel que reduzem a pressão nas mãos em pedais longos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Rechterwielerhandschoen_voor_wereldkampioen,_Mitchelton-Scott,_Annemiek_Van_Vleuten,_2020_-_bovenzijde_op_zwarte_achtergrond_(WU3365_-_collectie_KOERS._Museum_van_de_Wielersport).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tênis de Corrida Aero Boost', 699.9, TIMESTAMP '2024-08-15 09:20:00', TIMESTAMP '2024-08-17 18:47:00', 'Tênis de corrida leve com entressola de alta absorção de impacto para longas distâncias.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Brooks_Ghost_14_GTX.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tênis de Corrida Trail Grip', 559.0, TIMESTAMP '2024-01-10 12:24:00', TIMESTAMP '2024-02-19 18:04:00', 'Tênis para trilhas com solado de tração agressiva e proteção frontal.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/New_Balance_-_Trail_More_running_shoes.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Esteira Elétrica Dobrável 12 km/h', 2789.0, TIMESTAMP '2025-08-10 13:35:00', TIMESTAMP '2025-09-18 18:54:00', 'Esteira elétrica dobrável com inclinação manual, painel digital e velocidade de até 12 km/h.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Treadmill_from_Viking_Sport,_side.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Kit Halteres Ajustáveis 20 kg', 598.0, TIMESTAMP '2026-07-09 14:18:00', TIMESTAMP '2026-07-18 15:26:00', 'Par de halteres com carga ajustável de até 20 kg para treinos em casa.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Kurzhanteln_2_x_15_kg_2v2.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Tapete de Yoga Antiderrapante', 118.0, TIMESTAMP '2025-10-28 17:22:00', TIMESTAMP '2025-11-22 21:53:00', 'Tapete de yoga de 6 mm com superfície antiderrapante e bolsa para transporte.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Gaim_Yoga_Mat_1_2019-05-15.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Squeeze Térmica 900ml', 79.9, TIMESTAMP '2024-02-08 17:54:00', TIMESTAMP '2024-02-27 21:04:00', 'Garrafa térmica de aço inox que mantém a bebida gelada por até 24 horas.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Waterbottle.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Camiseta Dry Fit Running', 86.9, TIMESTAMP '2026-05-01 14:46:00', TIMESTAMP '2026-05-07 00:05:00', 'Camiseta leve com tecido de secagem rápida e proteção UV para corrida.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Ambigram_Ideal,_polysymmetrical_logo_printed_on_a_green_T-shirt.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Carro Hatch Compacto 1.0 Flex', 78900.0, TIMESTAMP '2025-06-05 15:31:00', TIMESTAMP '2025-07-08 16:34:00', 'Hatch compacto econômico com motor 1.0 flex, ar-condicionado e central multimídia.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2007_Holden_Astra_(AH_MY07.5)_CD_3-door_hatchback_(2018-10-30)_02.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Carro Sedan Executivo 2.0', 142900.0, TIMESTAMP '2025-08-16 08:03:00', TIMESTAMP '2025-08-26 15:06:00', 'Sedan executivo com motor 2.0, câmbio automático e pacote completo de assistências à condução.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Citro%C3%ABn_Elys%C3%A9e_Sanming_01_2022-07-27.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('SUV Urbana Híbrida', 189900.0, TIMESTAMP '2025-06-24 08:43:00', TIMESTAMP '2025-07-18 09:04:00', 'SUV híbrida com baixo consumo, espaço interno generoso e teto solar.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Toyota_Century_GRMN_SUV_(White)_side_view_at_Japan_Mobility_Show_Kansai_2025.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Picape Cabine Dupla Diesel', 249900.0, TIMESTAMP '2024-10-07 14:08:00', TIMESTAMP '2024-10-19 21:07:00', 'Picape de cabine dupla com motor diesel, tração 4x4 e grande capacidade de carga.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/2024_Toyota_Hilux_2.4_V_4x4_Crew_Cab_(Indonesia)_front_view.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Carro Elétrico Compacto City', 159900.0, TIMESTAMP '2025-07-25 15:09:00', TIMESTAMP '2025-08-07 23:18:00', 'Carro elétrico compacto com autonomia de 300 km e recarga rápida.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Electric_Car_recharging.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Motocicleta Street 160cc', 15490.0, TIMESTAMP '2025-06-06 16:19:00', TIMESTAMP '2025-06-14 16:28:00', 'Motocicleta urbana de 160 cc, econômica, com painel digital e freio ABS.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Mahindra_Mojo_UT_300_-_Blue.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Motocicleta Trail 300cc', 27900.0, TIMESTAMP '2025-08-03 10:39:00', TIMESTAMP '2025-08-31 17:07:00', 'Motocicleta trail de 300 cc com suspensão de longo curso, ideal para estrada e terra.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Husqvarna_Norden_901_Expedition_(1).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Scooter Elétrica Urban', 12989.0, TIMESTAMP '2025-09-13 16:21:00', TIMESTAMP '2025-09-14 17:37:00', 'Scooter elétrica silenciosa com bateria removível, ideal para deslocamentos urbanos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Scooter,_Berlin_(P1080138).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Capacete Integral Pro Safe', 429.0, TIMESTAMP '2024-08-26 09:52:00', TIMESTAMP '2024-10-04 13:20:00', 'Capacete integral com viseira antiembaçante, ventilação e forro removível.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Full-Face_Helmet_9274.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Kit de Ferramentas Automotivo 120 Peças', 389.9, TIMESTAMP '2025-03-06 15:19:00', TIMESTAMP '2025-03-22 16:17:00', 'Maleta com 120 peças entre chaves, soquetes e acessórios para manutenção básica de veículos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Socket_set_with_two_ratchets_in_box.jpeg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Macaco Hidráulico Jacaré 2T', 259.0, TIMESTAMP '2026-06-10 12:14:00', TIMESTAMP '2026-06-16 20:36:00', 'Macaco hidráulico tipo jacaré com capacidade de 2 toneladas.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Red_floor_jack_for_a_motor_vehicle_2026-08-30_(trolley_jack)_01.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Compressor de Ar Portátil 12V', 178.9, TIMESTAMP '2024-02-27 10:29:00', TIMESTAMP '2024-03-14 19:12:00', 'Compressor de ar portátil 12 V com manômetro digital e desligamento automático.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Compressor_Tire_Pump_Tyre_Pump_Edited_2020_(49906791062).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Carregador Veicular Inteligente', 78.9, TIMESTAMP '2026-04-08 16:34:00', TIMESTAMP '2026-05-05 21:18:00', 'Carregador veicular com duas portas USB e carregamento rápido para celulares.', true, 'https://live.staticflickr.com/7290/8740848377_9dc24d213c_b.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Scanner OBD2 Bluetooth', 148.0, TIMESTAMP '2024-08-07 08:15:00', TIMESTAMP '2024-08-30 12:53:00', 'Scanner OBD2 Bluetooth que lê códigos de falha e dados do veículo pelo smartphone.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Bluetooth_ELM327_OBD2-Scanner_IMG_6322.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Relógio Analógico Clássico Aço', 897.0, TIMESTAMP '2025-11-10 08:37:00', TIMESTAMP '2025-12-04 13:58:00', 'Relógio analógico com caixa e pulseira de aço inoxidável e resistência à água.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Montinari_Milano.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Relógio Automático Skeleton', 2490.0, TIMESTAMP '2026-07-30 16:04:00', TIMESTAMP '2026-08-25 23:41:00', 'Relógio automático com mostrador skeleton que revela o mecanismo interno.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Mechanics_movement_feinmechanik_wrist_watch_clock_automatic_gmt_master_gmt-932709.jpg%21d.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Relógio Digital Esportivo Resistente', 329.0, TIMESTAMP '2025-12-04 13:45:00', TIMESTAMP '2026-01-11 15:53:00', 'Relógio digital resistente a impactos e à água, com cronômetro e alarme.', false, 'https://commons.wikimedia.org/wiki/Special:FilePath/Casio_G-Shock_analog-digital_watch.jpeg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Luminária de Mesa LED Articulada', 138.0, TIMESTAMP '2024-04-13 08:06:00', TIMESTAMP '2024-05-17 13:31:00', 'Luminária de mesa LED articulada com três níveis de brilho e temperatura de cor ajustável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/A_desk_lamp.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Pendente Industrial Preto', 188.0, TIMESTAMP '2024-05-12 17:23:00', TIMESTAMP '2024-06-05 20:07:00', 'Luminária pendente em estilo industrial, ideal para bancadas e mesas de jantar.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Stuttgart_-_West_-_Feuerseeplatz_-_H%C3%A4ngelampe_mit_Reflexion_(1).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Fita LED 5m RGB com Controle', 69.9, TIMESTAMP '2025-11-04 17:43:00', TIMESTAMP '2025-12-04 21:30:00', 'Fita LED de 5 metros com controle remoto e efeitos de cor.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Adafruit_NeoPixel_Digital_RGB.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Organizador de Cabos Kit 30 Peças', 39.9, TIMESTAMP '2026-04-05 11:00:00', TIMESTAMP '2026-04-26 18:16:00', 'Kit com presilhas e abraçadeiras para organizar cabos em mesas e racks.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Cable_ties.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Caixa Organizadora Empilhável 40L', 64.9, TIMESTAMP '2024-07-01 16:46:00', TIMESTAMP '2024-07-20 17:28:00', 'Caixa plástica de 40 litros com tampa e encaixe empilhável.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Orange_plastic_storage_container_on_top_of_a_stack.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Estante Modular 5 Nichos', 459.0, TIMESTAMP '2025-05-20 17:42:00', TIMESTAMP '2025-05-26 20:05:00', 'Estante modular de madeira com cinco nichos para livros e decoração.', true, 'https://live.staticflickr.com/3412/4553439610_b584e5b587_b.jpg');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Cadeira Ergonômica Home Office', 1289.0, TIMESTAMP '2025-07-09 15:05:00', TIMESTAMP '2025-08-02 15:46:00', 'Cadeira ergonômica com apoio lombar ajustável, encosto em tela e braços 3D.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Aeron_Chair.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Escrivaninha Gamer 140cm', 896.0, TIMESTAMP '2024-06-29 12:49:00', TIMESTAMP '2024-07-04 13:42:00', 'Escrivaninha ampla de 140 cm com passagem de cabos e superfície texturizada.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Velocity_Micro_%22ProMagix_HD60%22_Workstation_Computer.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Mesa Ajustável Elétrica Standing', 2390.0, TIMESTAMP '2026-03-11 16:53:00', TIMESTAMP '2026-04-06 21:13:00', 'Mesa com regulagem elétrica de altura e memória para posições sentado e em pé.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Height_adjustable_workstation.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Air Fryer 5L Digital', 448.0, TIMESTAMP '2025-11-13 16:14:00', TIMESTAMP '2025-12-10 21:10:00', 'Fritadeira sem óleo de 5 litros com painel digital e programas pré-definidos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Air_Fryer_Presets.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Cafeteira Elétrica Programável', 218.0, TIMESTAMP '2024-12-28 11:51:00', TIMESTAMP '2025-02-03 21:37:00', 'Cafeteira elétrica com jarra de vidro para 30 xícaras e função de programação.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Hamilton_Beach_home_coffee_maker.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Aspirador Robô Inteligente', 1690.0, TIMESTAMP '2025-12-08 08:44:00', TIMESTAMP '2025-12-25 11:16:00', 'Aspirador robô com mapeamento a laser, controle por aplicativo e retorno automático à base.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Robot_Vacuum_-_Charging_(50841204983).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Liquidificador Power 1200W', 198.0, TIMESTAMP '2024-11-14 10:38:00', TIMESTAMP '2024-12-14 17:41:00', 'Liquidificador de 1200 W com jarra de 2 litros e lâminas de aço inox.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Elektrische_blender_van_het_merk_Princess_-_INDUS_V09861.JPG?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Kit de Blocos de Montar 1000 Peças', 188.9, TIMESTAMP '2026-01-12 15:27:00', TIMESTAMP '2026-02-04 18:04:00', 'Kit com 1000 blocos coloridos que estimulam a criatividade e a coordenação motora.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Lego_Color_Bricks.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Drone Infantil com Câmera', 347.0, TIMESTAMP '2026-05-19 11:16:00', TIMESTAMP '2026-06-28 19:31:00', 'Drone leve com câmera, estabilização de altitude e proteção das hélices.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/DJI_-_Drohne_Mavic_Air_2_(b).JPG?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Jogo de Tabuleiro Estratégia Épica', 217.0, TIMESTAMP '2025-10-07 12:33:00', TIMESTAMP '2025-11-14 14:26:00', 'Jogo de tabuleiro estratégico para 2 a 5 jogadores com partidas de cerca de 60 minutos.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Castles_of_Burgundy_(Board_Game).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Kit de Ferramentas de Jardinagem', 98.9, TIMESTAMP '2024-11-11 12:31:00', TIMESTAMP '2024-11-17 21:37:00', 'Kit com pá, rastelo, tesoura de poda e luvas para cuidar do jardim.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Line_of_used_garden_tools_(50054718038).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Mangueira Retrátil 20m', 127.0, TIMESTAMP '2026-04-26 16:20:00', TIMESTAMP '2026-05-26 19:24:00', 'Mangueira retrátil de 20 metros com esguicho multifunção.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Garden_hose.jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Balança Digital Corporal Bluetooth', 157.0, TIMESTAMP '2025-06-05 17:34:00', TIMESTAMP '2025-07-02 02:38:00', 'Balança digital com Bluetooth que registra peso e composição corporal no aplicativo.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Fitness_digital_scale_weight_loss_(33837094584).jpg?width=640');
INSERT INTO tb_product (name, price, created_at, updated_at, description, active, img_url) VALUES ('Jaqueta Corta-Vento Impermeável', 328.0, TIMESTAMP '2025-11-15 15:04:00', TIMESTAMP '2025-12-02 17:53:00', 'Jaqueta leve, corta-vento e impermeável, dobrável para levar na mochila.', true, 'https://commons.wikimedia.org/wiki/Special:FilePath/Windjack_-_Windbreaker.jpg?width=640');

-- =========================
-- RELATIONSHIPS (product <-> category)
-- =========================

-- ELECTRONICS
INSERT INTO tb_product_category (product_id, category_id) VALUES (2, 1); -- Smart TV
INSERT INTO tb_product_category (product_id, category_id) VALUES (37, 1); -- Smartphone Orion X 256GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (38, 1); -- Smartphone Orion Lite 128GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (39, 1); -- Smartphone Pulse Pro 512GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (40, 1); -- Smartphone Essential 64GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (41, 1); -- Tablet Slate 11 128GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (42, 1); -- Tablet Slate Pro 12.9
INSERT INTO tb_product_category (product_id, category_id) VALUES (43, 1); -- Tablet Kids Explorer 8
INSERT INTO tb_product_category (product_id, category_id) VALUES (50, 1); -- Headset Gamer Surround 7.1
INSERT INTO tb_product_category (product_id, category_id) VALUES (51, 1); -- Headset Office Bluetooth NC
INSERT INTO tb_product_category (product_id, category_id) VALUES (52, 1); -- Fone de Ouvido Studio Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (69, 1); -- TV OLED 65 4K Lumen
INSERT INTO tb_product_category (product_id, category_id) VALUES (70, 1); -- TV QLED 55 4K Prisma
INSERT INTO tb_product_category (product_id, category_id) VALUES (71, 1); -- Smart TV LED 43 Full HD
INSERT INTO tb_product_category (product_id, category_id) VALUES (72, 1); -- Smart TV 32 HD Essencial
INSERT INTO tb_product_category (product_id, category_id) VALUES (73, 1); -- Projetor Full HD Cine Home
INSERT INTO tb_product_category (product_id, category_id) VALUES (74, 1); -- Câmera Mirrorless Zeta 24MP
INSERT INTO tb_product_category (product_id, category_id) VALUES (75, 1); -- Câmera Compacta Travel Zoom
INSERT INTO tb_product_category (product_id, category_id) VALUES (76, 1); -- Câmera de Ação 4K Trail
INSERT INTO tb_product_category (product_id, category_id) VALUES (77, 1); -- Fone de Ouvido Bluetooth Air
INSERT INTO tb_product_category (product_id, category_id) VALUES (78, 1); -- Caixa de Som Portátil Boom
INSERT INTO tb_product_category (product_id, category_id) VALUES (79, 1); -- Soundbar 2.1 Cinema
INSERT INTO tb_product_category (product_id, category_id) VALUES (80, 1); -- Smartwatch Pulse Fit
INSERT INTO tb_product_category (product_id, category_id) VALUES (81, 1); -- Smartband Active 5
INSERT INTO tb_product_category (product_id, category_id) VALUES (82, 1); -- Console Nexus Edição Digital
INSERT INTO tb_product_category (product_id, category_id) VALUES (83, 1); -- Console Portátil Handy Play
INSERT INTO tb_product_category (product_id, category_id) VALUES (84, 1); -- Controle Sem Fio Nexus Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (85, 1); -- Lâmpada Inteligente Wi-Fi RGB
INSERT INTO tb_product_category (product_id, category_id) VALUES (86, 1); -- Tomada Inteligente Wi-Fi 10A
INSERT INTO tb_product_category (product_id, category_id) VALUES (87, 1); -- Câmera de Segurança Wi-Fi 360
INSERT INTO tb_product_category (product_id, category_id) VALUES (139, 1); -- Carregador Veicular Inteligente
INSERT INTO tb_product_category (product_id, category_id) VALUES (158, 1); -- Drone Infantil com Câmera

-- BOOKS
INSERT INTO tb_product_category (product_id, category_id) VALUES (1, 2); -- Eloquent JavaScript
INSERT INTO tb_product_category (product_id, category_id) VALUES (5, 2); -- Ruby on Rails For Dummies
INSERT INTO tb_product_category (product_id, category_id) VALUES (88, 2); -- Head First Java
INSERT INTO tb_product_category (product_id, category_id) VALUES (89, 2); -- Java Concurrency in Practice
INSERT INTO tb_product_category (product_id, category_id) VALUES (90, 2); -- Spring Boot in Action
INSERT INTO tb_product_category (product_id, category_id) VALUES (91, 2); -- Spring Security in Action
INSERT INTO tb_product_category (product_id, category_id) VALUES (92, 2); -- Spring in Action
INSERT INTO tb_product_category (product_id, category_id) VALUES (93, 2); -- HTML and CSS: Design and Build Websites
INSERT INTO tb_product_category (product_id, category_id) VALUES (94, 2); -- Patterns of Enterprise Application Architecture
INSERT INTO tb_product_category (product_id, category_id) VALUES (95, 2); -- JavaScript: The Good Parts
INSERT INTO tb_product_category (product_id, category_id) VALUES (96, 2); -- Clean Code
INSERT INTO tb_product_category (product_id, category_id) VALUES (97, 2); -- Design Patterns: Elements of Reusable Object-Oriented Software
INSERT INTO tb_product_category (product_id, category_id) VALUES (98, 2); -- Introduction to Algorithms
INSERT INTO tb_product_category (product_id, category_id) VALUES (99, 2); -- Effective Java
INSERT INTO tb_product_category (product_id, category_id) VALUES (100, 2); -- Designing Data-Intensive Applications
INSERT INTO tb_product_category (product_id, category_id) VALUES (101, 2); -- Growing Object-Oriented Software, Guided by Tests
INSERT INTO tb_product_category (product_id, category_id) VALUES (102, 2); -- Clean Architecture
INSERT INTO tb_product_category (product_id, category_id) VALUES (103, 2); -- Enterprise Integration Patterns
INSERT INTO tb_product_category (product_id, category_id) VALUES (104, 2); -- xUnit Test Patterns
INSERT INTO tb_product_category (product_id, category_id) VALUES (105, 2); -- Test-Driven Development: By Example
INSERT INTO tb_product_category (product_id, category_id) VALUES (106, 2); -- The Clean Coder
INSERT INTO tb_product_category (product_id, category_id) VALUES (107, 2); -- Continuous Delivery
INSERT INTO tb_product_category (product_id, category_id) VALUES (108, 2); -- The DevOps Handbook
INSERT INTO tb_product_category (product_id, category_id) VALUES (109, 2); -- Pro Git
INSERT INTO tb_product_category (product_id, category_id) VALUES (110, 2); -- Domain-Driven Design: Tackling Complexity in the Heart of Software
INSERT INTO tb_product_category (product_id, category_id) VALUES (111, 2); -- Refactoring: Improving the Design of Existing Code
INSERT INTO tb_product_category (product_id, category_id) VALUES (112, 2); -- Head First Design Patterns

-- COMPUTER
INSERT INTO tb_product_category (product_id, category_id) VALUES (3, 3); -- Macbook Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (4, 3); -- PC Gamer
INSERT INTO tb_product_category (product_id, category_id) VALUES (6, 3); -- PC Gamer Ex
INSERT INTO tb_product_category (product_id, category_id) VALUES (7, 3); -- PC Gamer X
INSERT INTO tb_product_category (product_id, category_id) VALUES (8, 3); -- PC Gamer Alfa
INSERT INTO tb_product_category (product_id, category_id) VALUES (9, 3); -- PC Gamer Tera
INSERT INTO tb_product_category (product_id, category_id) VALUES (10, 3); -- PC Gamer Y
INSERT INTO tb_product_category (product_id, category_id) VALUES (11, 3); -- PC Gamer Nitro
INSERT INTO tb_product_category (product_id, category_id) VALUES (12, 3); -- PC Gamer Card
INSERT INTO tb_product_category (product_id, category_id) VALUES (13, 3); -- PC Gamer Plus
INSERT INTO tb_product_category (product_id, category_id) VALUES (14, 3); -- PC Gamer Hera
INSERT INTO tb_product_category (product_id, category_id) VALUES (15, 3); -- PC Gamer Weed
INSERT INTO tb_product_category (product_id, category_id) VALUES (16, 3); -- PC Gamer Max
INSERT INTO tb_product_category (product_id, category_id) VALUES (17, 3); -- PC Gamer Turbo
INSERT INTO tb_product_category (product_id, category_id) VALUES (18, 3); -- PC Gamer Hot
INSERT INTO tb_product_category (product_id, category_id) VALUES (19, 3); -- PC Gamer Ez
INSERT INTO tb_product_category (product_id, category_id) VALUES (20, 3); -- PC Gamer Tr
INSERT INTO tb_product_category (product_id, category_id) VALUES (21, 3); -- PC Gamer Tx
INSERT INTO tb_product_category (product_id, category_id) VALUES (22, 3); -- PC Gamer Er
INSERT INTO tb_product_category (product_id, category_id) VALUES (23, 3); -- PC Gamer Min
INSERT INTO tb_product_category (product_id, category_id) VALUES (24, 3); -- PC Gamer Boo
INSERT INTO tb_product_category (product_id, category_id) VALUES (25, 3); -- PC Gamer Foo
INSERT INTO tb_product_category (product_id, category_id) VALUES (26, 3); -- Notebook Aurora Pro 14
INSERT INTO tb_product_category (product_id, category_id) VALUES (27, 3); -- Notebook Aurora Ultra 15
INSERT INTO tb_product_category (product_id, category_id) VALUES (28, 3); -- Notebook Business Vertex 14
INSERT INTO tb_product_category (product_id, category_id) VALUES (29, 3); -- Notebook Gamer Titan 16
INSERT INTO tb_product_category (product_id, category_id) VALUES (30, 3); -- Notebook Developer Nimbus 15
INSERT INTO tb_product_category (product_id, category_id) VALUES (31, 3); -- Notebook Essential Lite 14

-- HOME APPLIANCES
INSERT INTO tb_product_category (product_id, category_id) VALUES (153, 4); -- Air Fryer 5L Digital
INSERT INTO tb_product_category (product_id, category_id) VALUES (154, 4); -- Cafeteira Elétrica Programável
INSERT INTO tb_product_category (product_id, category_id) VALUES (155, 4); -- Aspirador Robô Inteligente
INSERT INTO tb_product_category (product_id, category_id) VALUES (156, 4); -- Liquidificador Power 1200W

-- FURNITURE
INSERT INTO tb_product_category (product_id, category_id) VALUES (149, 5); -- Estante Modular 5 Nichos
INSERT INTO tb_product_category (product_id, category_id) VALUES (150, 5); -- Cadeira Ergonômica Home Office
INSERT INTO tb_product_category (product_id, category_id) VALUES (151, 5); -- Escrivaninha Gamer 140cm
INSERT INTO tb_product_category (product_id, category_id) VALUES (152, 5); -- Mesa Ajustável Elétrica Standing

-- TOYS
INSERT INTO tb_product_category (product_id, category_id) VALUES (157, 6); -- Kit de Blocos de Montar 1000 Peças
INSERT INTO tb_product_category (product_id, category_id) VALUES (158, 6); -- Drone Infantil com Câmera
INSERT INTO tb_product_category (product_id, category_id) VALUES (159, 6); -- Jogo de Tabuleiro Estratégia Épica

-- CLOTHING
INSERT INTO tb_product_category (product_id, category_id) VALUES (126, 7); -- Camiseta Dry Fit Running
INSERT INTO tb_product_category (product_id, category_id) VALUES (163, 7); -- Jaqueta Corta-Vento Impermeável

-- SHOES
INSERT INTO tb_product_category (product_id, category_id) VALUES (120, 8); -- Tênis de Corrida Aero Boost
INSERT INTO tb_product_category (product_id, category_id) VALUES (121, 8); -- Tênis de Corrida Trail Grip

-- SPORTS
INSERT INTO tb_product_category (product_id, category_id) VALUES (113, 9); -- Bicicleta Mountain Bike Aro 29 Trail
INSERT INTO tb_product_category (product_id, category_id) VALUES (114, 9); -- Bicicleta Speed Carbon Road Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (115, 9); -- Bicicleta Urbana Aro 26 Cidade
INSERT INTO tb_product_category (product_id, category_id) VALUES (116, 9); -- Bicicleta Elétrica City Motion
INSERT INTO tb_product_category (product_id, category_id) VALUES (117, 9); -- Capacete de Ciclismo Aero
INSERT INTO tb_product_category (product_id, category_id) VALUES (118, 9); -- Ciclocomputador GPS Route
INSERT INTO tb_product_category (product_id, category_id) VALUES (119, 9); -- Luvas de Ciclismo Gel
INSERT INTO tb_product_category (product_id, category_id) VALUES (120, 9); -- Tênis de Corrida Aero Boost
INSERT INTO tb_product_category (product_id, category_id) VALUES (121, 9); -- Tênis de Corrida Trail Grip
INSERT INTO tb_product_category (product_id, category_id) VALUES (122, 9); -- Esteira Elétrica Dobrável 12 km/h
INSERT INTO tb_product_category (product_id, category_id) VALUES (123, 9); -- Kit Halteres Ajustáveis 20 kg
INSERT INTO tb_product_category (product_id, category_id) VALUES (124, 9); -- Tapete de Yoga Antiderrapante
INSERT INTO tb_product_category (product_id, category_id) VALUES (125, 9); -- Squeeze Térmica 900ml
INSERT INTO tb_product_category (product_id, category_id) VALUES (126, 9); -- Camiseta Dry Fit Running

-- HEALTH
INSERT INTO tb_product_category (product_id, category_id) VALUES (122, 10); -- Esteira Elétrica Dobrável 12 km/h
INSERT INTO tb_product_category (product_id, category_id) VALUES (124, 10); -- Tapete de Yoga Antiderrapante
INSERT INTO tb_product_category (product_id, category_id) VALUES (162, 10); -- Balança Digital Corporal Bluetooth

-- AUTOMOTIVE
INSERT INTO tb_product_category (product_id, category_id) VALUES (139, 12); -- Carregador Veicular Inteligente

-- GARDEN
INSERT INTO tb_product_category (product_id, category_id) VALUES (160, 13); -- Kit de Ferramentas de Jardinagem
INSERT INTO tb_product_category (product_id, category_id) VALUES (161, 13); -- Mangueira Retrátil 20m

-- OFFICE SUPPLIES
INSERT INTO tb_product_category (product_id, category_id) VALUES (65, 15); -- Suporte Ergonômico para Notebook
INSERT INTO tb_product_category (product_id, category_id) VALUES (147, 15); -- Organizador de Cabos Kit 30 Peças

-- NOTEBOOKS
INSERT INTO tb_product_category (product_id, category_id) VALUES (3, 16); -- Macbook Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (26, 16); -- Notebook Aurora Pro 14
INSERT INTO tb_product_category (product_id, category_id) VALUES (27, 16); -- Notebook Aurora Ultra 15
INSERT INTO tb_product_category (product_id, category_id) VALUES (28, 16); -- Notebook Business Vertex 14
INSERT INTO tb_product_category (product_id, category_id) VALUES (29, 16); -- Notebook Gamer Titan 16
INSERT INTO tb_product_category (product_id, category_id) VALUES (30, 16); -- Notebook Developer Nimbus 15
INSERT INTO tb_product_category (product_id, category_id) VALUES (31, 16); -- Notebook Essential Lite 14

-- MONITORS
INSERT INTO tb_product_category (product_id, category_id) VALUES (32, 17); -- Monitor UltraWide 34 Curvo
INSERT INTO tb_product_category (product_id, category_id) VALUES (33, 17); -- Monitor 4K Creator 27
INSERT INTO tb_product_category (product_id, category_id) VALUES (34, 17); -- Monitor Gamer 24 144Hz
INSERT INTO tb_product_category (product_id, category_id) VALUES (35, 17); -- Monitor Office 22 Full HD
INSERT INTO tb_product_category (product_id, category_id) VALUES (36, 17); -- Monitor Portátil 15.6 USB-C

-- SMARTPHONES
INSERT INTO tb_product_category (product_id, category_id) VALUES (37, 18); -- Smartphone Orion X 256GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (38, 18); -- Smartphone Orion Lite 128GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (39, 18); -- Smartphone Pulse Pro 512GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (40, 18); -- Smartphone Essential 64GB

-- TABLETS
INSERT INTO tb_product_category (product_id, category_id) VALUES (41, 19); -- Tablet Slate 11 128GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (42, 19); -- Tablet Slate Pro 12.9
INSERT INTO tb_product_category (product_id, category_id) VALUES (43, 19); -- Tablet Kids Explorer 8

-- KEYBOARDS
INSERT INTO tb_product_category (product_id, category_id) VALUES (44, 20); -- Teclado Mecânico Forge TKL
INSERT INTO tb_product_category (product_id, category_id) VALUES (45, 20); -- Teclado Sem Fio Slim Office
INSERT INTO tb_product_category (product_id, category_id) VALUES (46, 20); -- Teclado Gamer RGB Vortex

-- MICE
INSERT INTO tb_product_category (product_id, category_id) VALUES (47, 21); -- Mouse Gamer Viper 16000 DPI
INSERT INTO tb_product_category (product_id, category_id) VALUES (48, 21); -- Mouse Ergonômico Vertical Flow
INSERT INTO tb_product_category (product_id, category_id) VALUES (49, 21); -- Mouse Sem Fio Compact

-- HEADSETS
INSERT INTO tb_product_category (product_id, category_id) VALUES (50, 22); -- Headset Gamer Surround 7.1
INSERT INTO tb_product_category (product_id, category_id) VALUES (51, 22); -- Headset Office Bluetooth NC

-- GRAPHICS CARDS
INSERT INTO tb_product_category (product_id, category_id) VALUES (53, 23); -- Placa de Vídeo Nova RX 8GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (54, 23); -- Placa de Vídeo Nova RX Ti 16GB
INSERT INTO tb_product_category (product_id, category_id) VALUES (55, 23); -- Placa de Vídeo Spectra 12GB

-- PROCESSORS
INSERT INTO tb_product_category (product_id, category_id) VALUES (56, 24); -- Processador Quantum 8 Núcleos
INSERT INTO tb_product_category (product_id, category_id) VALUES (57, 24); -- Processador Quantum 16 Núcleos
INSERT INTO tb_product_category (product_id, category_id) VALUES (58, 24); -- Processador Helix 6 Núcleos

-- STORAGE
INSERT INTO tb_product_category (product_id, category_id) VALUES (59, 25); -- SSD NVMe 1TB Velocity
INSERT INTO tb_product_category (product_id, category_id) VALUES (60, 25); -- SSD SATA 500GB Core
INSERT INTO tb_product_category (product_id, category_id) VALUES (61, 25); -- HD Externo 2TB Portátil
INSERT INTO tb_product_category (product_id, category_id) VALUES (62, 25); -- HD Interno 12TB NAS

-- COMPUTER ACCESSORIES
INSERT INTO tb_product_category (product_id, category_id) VALUES (63, 26); -- Webcam Full HD Stream
INSERT INTO tb_product_category (product_id, category_id) VALUES (64, 26); -- Hub USB-C 7 em 1
INSERT INTO tb_product_category (product_id, category_id) VALUES (65, 26); -- Suporte Ergonômico para Notebook

-- NETWORKING
INSERT INTO tb_product_category (product_id, category_id) VALUES (66, 27); -- Roteador Wi-Fi 6 AX3000
INSERT INTO tb_product_category (product_id, category_id) VALUES (67, 27); -- Switch Gigabit 8 Portas
INSERT INTO tb_product_category (product_id, category_id) VALUES (68, 27); -- Kit Mesh Wi-Fi 3 Unidades

-- TELEVISIONS
INSERT INTO tb_product_category (product_id, category_id) VALUES (2, 28); -- Smart TV
INSERT INTO tb_product_category (product_id, category_id) VALUES (69, 28); -- TV OLED 65 4K Lumen
INSERT INTO tb_product_category (product_id, category_id) VALUES (70, 28); -- TV QLED 55 4K Prisma
INSERT INTO tb_product_category (product_id, category_id) VALUES (71, 28); -- Smart TV LED 43 Full HD
INSERT INTO tb_product_category (product_id, category_id) VALUES (72, 28); -- Smart TV 32 HD Essencial
INSERT INTO tb_product_category (product_id, category_id) VALUES (73, 28); -- Projetor Full HD Cine Home

-- CAMERAS
INSERT INTO tb_product_category (product_id, category_id) VALUES (74, 29); -- Câmera Mirrorless Zeta 24MP
INSERT INTO tb_product_category (product_id, category_id) VALUES (75, 29); -- Câmera Compacta Travel Zoom
INSERT INTO tb_product_category (product_id, category_id) VALUES (76, 29); -- Câmera de Ação 4K Trail
INSERT INTO tb_product_category (product_id, category_id) VALUES (87, 29); -- Câmera de Segurança Wi-Fi 360

-- AUDIO
INSERT INTO tb_product_category (product_id, category_id) VALUES (52, 30); -- Fone de Ouvido Studio Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (77, 30); -- Fone de Ouvido Bluetooth Air
INSERT INTO tb_product_category (product_id, category_id) VALUES (78, 30); -- Caixa de Som Portátil Boom
INSERT INTO tb_product_category (product_id, category_id) VALUES (79, 30); -- Soundbar 2.1 Cinema

-- WEARABLES
INSERT INTO tb_product_category (product_id, category_id) VALUES (80, 31); -- Smartwatch Pulse Fit
INSERT INTO tb_product_category (product_id, category_id) VALUES (81, 31); -- Smartband Active 5

-- GAME CONSOLES
INSERT INTO tb_product_category (product_id, category_id) VALUES (82, 32); -- Console Nexus Edição Digital
INSERT INTO tb_product_category (product_id, category_id) VALUES (83, 32); -- Console Portátil Handy Play
INSERT INTO tb_product_category (product_id, category_id) VALUES (84, 32); -- Controle Sem Fio Nexus Pro

-- SMART HOME
INSERT INTO tb_product_category (product_id, category_id) VALUES (85, 33); -- Lâmpada Inteligente Wi-Fi RGB
INSERT INTO tb_product_category (product_id, category_id) VALUES (86, 33); -- Tomada Inteligente Wi-Fi 10A
INSERT INTO tb_product_category (product_id, category_id) VALUES (87, 33); -- Câmera de Segurança Wi-Fi 360
INSERT INTO tb_product_category (product_id, category_id) VALUES (155, 33); -- Aspirador Robô Inteligente

-- BICYCLES
INSERT INTO tb_product_category (product_id, category_id) VALUES (113, 34); -- Bicicleta Mountain Bike Aro 29 Trail
INSERT INTO tb_product_category (product_id, category_id) VALUES (114, 34); -- Bicicleta Speed Carbon Road Pro
INSERT INTO tb_product_category (product_id, category_id) VALUES (115, 34); -- Bicicleta Urbana Aro 26 Cidade
INSERT INTO tb_product_category (product_id, category_id) VALUES (116, 34); -- Bicicleta Elétrica City Motion

-- CYCLING ACCESSORIES
INSERT INTO tb_product_category (product_id, category_id) VALUES (117, 35); -- Capacete de Ciclismo Aero
INSERT INTO tb_product_category (product_id, category_id) VALUES (118, 35); -- Ciclocomputador GPS Route
INSERT INTO tb_product_category (product_id, category_id) VALUES (119, 35); -- Luvas de Ciclismo Gel

-- RUNNING
INSERT INTO tb_product_category (product_id, category_id) VALUES (120, 36); -- Tênis de Corrida Aero Boost
INSERT INTO tb_product_category (product_id, category_id) VALUES (121, 36); -- Tênis de Corrida Trail Grip
INSERT INTO tb_product_category (product_id, category_id) VALUES (126, 36); -- Camiseta Dry Fit Running

-- FITNESS EQUIPMENT
INSERT INTO tb_product_category (product_id, category_id) VALUES (122, 37); -- Esteira Elétrica Dobrável 12 km/h
INSERT INTO tb_product_category (product_id, category_id) VALUES (123, 37); -- Kit Halteres Ajustáveis 20 kg
INSERT INTO tb_product_category (product_id, category_id) VALUES (124, 37); -- Tapete de Yoga Antiderrapante

-- CARS
INSERT INTO tb_product_category (product_id, category_id) VALUES (127, 38); -- Carro Hatch Compacto 1.0 Flex
INSERT INTO tb_product_category (product_id, category_id) VALUES (128, 38); -- Carro Sedan Executivo 2.0
INSERT INTO tb_product_category (product_id, category_id) VALUES (129, 38); -- SUV Urbana Híbrida
INSERT INTO tb_product_category (product_id, category_id) VALUES (130, 38); -- Picape Cabine Dupla Diesel
INSERT INTO tb_product_category (product_id, category_id) VALUES (131, 38); -- Carro Elétrico Compacto City

-- MOTORCYCLES
INSERT INTO tb_product_category (product_id, category_id) VALUES (132, 39); -- Motocicleta Street 160cc
INSERT INTO tb_product_category (product_id, category_id) VALUES (133, 39); -- Motocicleta Trail 300cc
INSERT INTO tb_product_category (product_id, category_id) VALUES (134, 39); -- Scooter Elétrica Urban
INSERT INTO tb_product_category (product_id, category_id) VALUES (135, 39); -- Capacete Integral Pro Safe

-- AUTOMOTIVE TOOLS
INSERT INTO tb_product_category (product_id, category_id) VALUES (136, 40); -- Kit de Ferramentas Automotivo 120 Peças
INSERT INTO tb_product_category (product_id, category_id) VALUES (137, 40); -- Macaco Hidráulico Jacaré 2T
INSERT INTO tb_product_category (product_id, category_id) VALUES (138, 40); -- Compressor de Ar Portátil 12V
INSERT INTO tb_product_category (product_id, category_id) VALUES (140, 40); -- Scanner OBD2 Bluetooth

-- WATCHES
INSERT INTO tb_product_category (product_id, category_id) VALUES (141, 41); -- Relógio Analógico Clássico Aço
INSERT INTO tb_product_category (product_id, category_id) VALUES (142, 41); -- Relógio Automático Skeleton
INSERT INTO tb_product_category (product_id, category_id) VALUES (143, 41); -- Relógio Digital Esportivo Resistente

-- LIGHTING
INSERT INTO tb_product_category (product_id, category_id) VALUES (85, 42); -- Lâmpada Inteligente Wi-Fi RGB
INSERT INTO tb_product_category (product_id, category_id) VALUES (144, 42); -- Luminária de Mesa LED Articulada
INSERT INTO tb_product_category (product_id, category_id) VALUES (145, 42); -- Pendente Industrial Preto
INSERT INTO tb_product_category (product_id, category_id) VALUES (146, 42); -- Fita LED 5m RGB com Controle

-- HOME ORGANIZATION
INSERT INTO tb_product_category (product_id, category_id) VALUES (147, 43); -- Organizador de Cabos Kit 30 Peças
INSERT INTO tb_product_category (product_id, category_id) VALUES (148, 43); -- Caixa Organizadora Empilhável 40L
INSERT INTO tb_product_category (product_id, category_id) VALUES (149, 43); -- Estante Modular 5 Nichos
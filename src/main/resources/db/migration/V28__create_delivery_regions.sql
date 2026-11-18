CREATE TABLE delivery_regions (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

INSERT INTO delivery_regions (code, name)
VALUES
    ('KARAKALPAKSTAN', 'Республика Каракалпакстан'),
    ('ANDIJAN', 'Андижанская область'),
    ('BUKHARA', 'Бухарская область'),
    ('FERGANA', 'Ферганская область'),
    ('JIZZAKH', 'Джизакская область'),
    ('KASHKADARYA', 'Кашкадарьинская область'),
    ('KHOREZM', 'Хорезмская область'),
    ('NAMANGAN', 'Наманганская область'),
    ('NAVOI', 'Навоийская область'),
    ('SAMARKAND', 'Самаркандская область'),
    ('SIRDARYA', 'Сырдарьинская область'),
    ('SURKHANDARYA', 'Сурхандарьинская область'),
    ('TASHKENT_REGION', 'Ташкентская область'),
    ('TASHKENT_CITY', 'Город Ташкент');


# Makon Marketplace

Модульный Spring Boot backend и два приложения в `frontend-v2`: клиентская витрина Next.js и кабинет продавца/администратора React + Vite.

## Локальный запуск без Docker

Нужны Java 21, Node.js 22 и PostgreSQL. Создайте базу `marketplace` и задайте переменные из `.env.example`. `JWT_SECRET_BASE64` должен содержать случайный ключ минимум 32 байта в Base64. Пример генерации в PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
```

Из корня проекта запустите `./gradlew.bat bootRun`. Flyway применит миграции автоматически. В другом терминале:

```powershell
cd frontend-v2
npm ci
npm run dev:all
```

Адреса: витрина `http://localhost:3000/ru`, кабинет `http://localhost:4200`, API `http://localhost:8080`, документация API `http://localhost:8080/swagger-ui.html`.

В профиле `local` фотографии хранятся на диске в `~/MakonMarketplace/uploads`; путь можно изменить через `STORAGE_LOCAL_DIRECTORY`. Для запуска с MinIO задайте `STORAGE_MODE=minio`, `STORAGE_ENDPOINT` для backend и `STORAGE_PUBLIC_ENDPOINT` для браузера.

Для чистой локальной базы загрузите демо-данные командой `./scripts/seed-demo.ps1` (нужен `psql`), затем фотографии командой `./scripts/seed-demo-images.ps1`. Демо-скрипт можно запускать повторно: он создаёт 5 категорий, 12 товаров с ценами и остатками и тестовые аккаунты. Пароли демо-аккаунтов предназначены только для локальной проверки.

## Docker Compose

Скопируйте `.env.example` в `.env`, заполните `JWT_SECRET_BASE64` и при необходимости измените пароли и порты. Если на компьютере уже запущен PostgreSQL или backend, укажите свободные `DB_PORT` и `SERVER_PORT`. При изменении `STORAGE_PORT` задайте такой же порт в `STORAGE_PUBLIC_ENDPOINT`, например `http://localhost:19000`.

```powershell
docker compose up --build -d
docker compose ps
docker compose logs --tail=50 backend
```

Compose поднимает PostgreSQL, MinIO, backend, витрину и кабинет. База и файлы хранятся в именованных томах. Для заполнения **новой** контейнерной базы:

```powershell
docker compose cp scripts/demo-data.sql postgres:/tmp/demo-data.sql
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -f /tmp/demo-data.sql'
./scripts/seed-demo-images.ps1
./scripts/verify-demo.ps1 -CheckImages
```

Проверка должна показать 5 категорий, 12 товаров, успешный вход и доступные фотографии. Если изменены порты, передайте скриптам `-ApiUrl`, `-CustomerUrl` и `-DashboardUrl`.

Резервная копия контейнерной базы и файлов MinIO:

```powershell
./scripts/backup-compose.ps1
```

Файлы сохраняются в `backups/`, которая исключена из Git. Для согласованной копии в рабочей среде на время резервирования остановите операции записи.

## Проверки

```powershell
./gradlew.bat test
cd frontend-v2
npm run typecheck
npm run build
```

Интеграционные тесты заказов и чистого демо-запуска используют PostgreSQL через Testcontainers и требуют работающий Docker. Без Docker JUnit пропускает их; остальные тесты выполняются. Файл `.github/workflows/ci.yml` запускает проверки после публикации проекта в GitHub; локально обе сборки также нужно проверять указанными командами.

## Возможности

Регистрация и вход, JWT с обновлением и выходом, профиль, продавцы и магазины с модерацией, категории, товары с вариантами SKU и фотографиями, поиск и фильтры, остатки, корзина, заказы нескольких продавцов, резервирование, история статусов и складских операций, избранное, отзывы и промокоды. Онлайн-оплата и управление фактической доставкой пока не реализованы.

## Первый этап доставки

При оформлении заказа покупатель указывает имя, телефон, область, город и адрес. Эти данные сохраняются в заказе как снимок на момент покупки и доступны только покупателю и продавцам, которым принадлежит часть заказа. Для старых заказов поля могут оставаться пустыми. На этом этапе предполагается доставка силами каждого продавца; расчёт стоимости, отслеживание и фактическая передача заказа ещё не реализованы.

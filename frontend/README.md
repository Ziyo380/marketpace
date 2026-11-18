# Makon Marketplace Frontend

## Start locally

```powershell
cd frontend
npm install
npm run dev
```

Vite opens `http://127.0.0.1:5173` and proxies `/api` to Spring Boot at `http://localhost:8080`.

If backend is unavailable, the storefront automatically switches to local demo mode. Demo workspaces are available from the login dialog.

## Backend demo accounts

Load demo records after Flyway has created V1–V5:

```powershell
.\scripts\seed-demo.ps1
```

Accounts:

- `demo-admin` / `Admin123!`
- `demo-seller` / `Seller123!`
- `demo-customer` / `Customer123!`
- `demo-applicant` / `Applicant123!` (pending seller application)

These credentials are only for a local development database.

# Local marketplace demonstration

Run against the already running local backend:

```powershell
./scripts/seed-showcase.ps1
./scripts/verify-showcase.ps1
```

The script adds demonstration records through the API. It does not reset passwords, replace existing products or change existing orders. A repeat run skips records that already exist. Stock is added only when a new demonstration product or variant is created.

## Accounts

All accounts below use the local demonstration password `Demo12345!`.

| Login | Purpose |
| --- | --- |
| demo-showcase-buyer | Seven orders, delivery timelines, reviews and favorites |
| demo-showcase-tech | Pixel Avenue: electronics |
| demo-showcase-home | Mizu Living: home and kitchen |
| demo-showcase-beauty | Aura Care: beauty |
| demo-showcase-sport | Move Studio: sport |

Existing admin account: `demo-admin` / `Admin123!`.

The seed adds 32 published products, one illustration per product, 16 variants, five subcategories, three promocodes and delivery zones for all 14 regions. It also adds missing zones to the original Nova Market demonstration store.

Promocodes: `DEMO10` (10%, minimum 100,000 UZS), `DEMO50000` (50,000 UZS, minimum 300,000), `DEMOSPORT` (15%, minimum 200,000). The backend permits each customer to use a promocode once.

Order examples include PENDING, PACKING, READY_FOR_DISPATCH, IN_TRANSIT, DELIVERED, DELIVERY_FAILED and CANCELLED. One delivered order contains items from two stores. All status changes, stock reservations, reviews and history entries use the normal backend services.

Names, prices, addresses and images are illustrative local demonstration data. No payment or external carrier integration is called.

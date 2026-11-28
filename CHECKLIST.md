# Stockwise checklist

We take one step at a time, in order. A box is ticked only when the step works and its "Done when" check has passed.

## Decisions to confirm first

Each has a proposed answer. Change any of them before the step that uses it.

- [ ] **D1. Stock levels** (step 2). "Minimum" is the record's minimum threshold, and quantities are in base units.

  | Status | Rule | Example with minimum 20 |
  |---|---|---|
  | In stock | more than the minimum | 21 or more |
  | Warning | at or below the minimum, but more than half of it | 11 to 20 |
  | Danger | half the minimum or less, including 0 | 0 to 10 |

  With a minimum of 0, only 0 is Danger. Discontinued stays a manual setting.

- [ ] **D2. What subtracts stock** (step 4). The order becoming **Shipped**, set by hand or by one of its shipments going In transit. Cancelling a shipment alone does not change stock.

- [ ] **D3. Allowed order status changes** (step 3). "Returned" is new.

  | From | Can change to | Stock |
  |---|---|---|
  | Pending | Confirmed | none |
  | Pending | Cancelled | none |
  | Confirmed | Pending | none |
  | Confirmed | Shipped | **subtract** |
  | Confirmed | Cancelled | none |
  | Shipped | Delivered | none |
  | Shipped | Cancelled or Returned | **add back** |
  | Delivered | Returned | **add back** |
  | Cancelled, Returned | nothing (final) | none |

- [ ] **D4. Orders already shipped or delivered** (step 5). Nothing was ever subtracted for them, so cancelling or returning one adds nothing back. Correct their stock by hand if goods really come back.

- [ ] **D5. One inventory record per product** (step 1), so there is only one place to subtract from and add to. Existing duplicates are merged.

- [ ] **D6. Returns are whole orders** (step 5). Returning only some items comes later.

## Part A: stock flow (current focus)

### Step 0. Safe place to test

Starting the backend applies new migrations to whatever database `.env` points at.

- [ ] Point `.env` at a development copy of the database
- [ ] Start the backend and frontend once, before any change, to confirm both run

**Done when:** the app runs against a database that is safe to change.

### Step 1. Stock movement history

- [ ] Migration `V23`: table `stock_movements` (product, order or reorder, type, signed base quantity, quantity afterwards)
- [ ] Same migration: merge duplicate inventory records, then allow only one active record per product (D5)
- [ ] One `StockService` that is the only code changing quantities: locks the inventory row, refuses to go below 0, writes the movement
- [ ] Hand edits on the Inventory page go through it and are recorded as adjustments
- [ ] Tests: add, subtract, not enough stock, movement recorded

**Done when:** every quantity change leaves a movement row, and a second record for the same product is refused.

### Step 2. In stock, Warning, Danger

- [ ] Replace `LOW_STOCK` with `WARNING` and `OUT_OF_STOCK` with `DANGER` (D1)
- [ ] Status is calculated from quantity and minimum every time either changes
- [ ] Migration: recalculate existing inventory rows, and rename the old values in stored notifications (otherwise they fail to load)
- [ ] Inventory form: remove the status dropdown, keep a "Discontinued" tick box
- [ ] Alerts fire when a product drops to Warning or Danger, and go only to users who may view inventory
- [ ] Update colours and labels (`StatusBadge`, dashboard, notification bell) and the three seeders that use the old names
- [ ] Remove the "set to In stock" shortcut in `ReorderService.onReceived`
- [ ] Tests: the boundaries (0, half, minimum, minimum + 1, minimum of 0, discontinued)

**Done when:** a product with 0 on hand shows Danger, and no screen or API call can mark it In stock.

### Step 3. Order status rules

- [ ] Add `RETURNED` (backend, frontend list, badge colour, dashboard colour, and the two seeders that go through every status: `DataSeeder`, `LogisticsSeeder`)
- [ ] Refuse any change that is not in the D3 table
- [ ] A new order can only start as Pending or Confirmed
- [ ] An order with no items cannot be Shipped
- [ ] A Shipped or Delivered order cannot be deleted
- [ ] Status dropdowns offer only the allowed next statuses
- [ ] Returned orders count as closed and are left out of revenue
- [ ] Tests: every allowed change, and a sample of refused ones

**Done when:** a Delivered order cannot go back to Pending, from the screen or the API.

### Step 4. Subtract stock when shipped

- [ ] Changing to Shipped subtracts every item, converted to base units (2 cases of 12 = 24)
- [ ] All or nothing: if any product is short or has no inventory record, nothing changes and the message names it
- [ ] Never subtracts twice, even if two people click at once
- [ ] The first shipment going In transit marks its Confirmed order Shipped (D2). Later shipments of a Shipped order subtract nothing more. Shipments of Pending, Cancelled or Returned orders cannot go In transit
- [ ] No new shipments for Delivered, Cancelled or Returned orders
- [ ] Order lines and the projection round unit conversions the same way
- [ ] Tests: ship, ship twice, short stock, mixed units, shipment trigger

**Done when:** shipping 5 of a product with 30 on hand leaves 25, and saving the order again leaves 25. Pending and Confirmed orders never change stock.

### Step 5. Add stock back when cancelled or returned

- [ ] Cancelling or returning a Shipped order, or returning a Delivered one, adds back exactly what the movement history says was taken (not recalculated, so a later conversion change cannot skew it)
- [ ] Cancelling a Pending or Confirmed order changes nothing
- [ ] Never adds back twice
- [ ] Orders shipped before this change add nothing back (D4)
- [ ] Tests for each of the above

**Done when:** 30, ship 5, return gives 30 again. Cancelling an unshipped order leaves 30.

### Step 6. Receiving a reorder adds stock

- [ ] Marking Received adds the quantity, converted to base units, and the status is recalculated
- [ ] A product with no inventory record gets one
- [ ] Only an Ordered reorder can be Received, and only once
- [ ] An Ordered reorder can still be received or cancelled after its supplier link or unit was removed
- [ ] Update the confirmation and success messages on the reorder page
- [ ] Tests: receive at 0, receive in cases, receive twice, draft cannot be received

**Done when:** a product at 0 that receives 5 with a minimum of 20 shows 5 and Danger. Receiving 100 shows In stock.

### Step 7. Live updates

Java's counterpart of SignalR is Spring WebSocket with STOMP. The project already uses it for notifications (`WebSocketConfig`).

- [ ] After a stock change is saved, send product, quantity and status to `/user/queue/inventory`, only to users who may view inventory
- [ ] Share one WebSocket connection between notifications and stock updates
- [ ] Inventory page, dashboard and order items refresh by themselves
- [ ] Reload after a reconnect so nothing is missed

**Done when:** with two browsers open, shipping an order in one changes the quantity and status in the other without a refresh. A refused or rolled-back change sends nothing.

## Part B: records that get stuck

- [x] **Deleted customer.** Deleting stays allowed, because it only hides the customer (soft delete)
  - [x] Their orders can still be updated, keeping the same customer
  - [x] Orders, shipments, search and the PDF report still show the name, marked "(deleted)"
  - [x] A deleted customer cannot be chosen for a new order, or moved onto another order
  - [x] Deleted customers are not listed in the customer dropdown. Editing their order still shows the name in the box
- [x] **Deleted unit of measure.** Deleting stays allowed
  - [x] Reorders, inventory records, order items and unit conversions that use it can still be saved
  - [x] Such a reorder can be marked received (adding the quantity to stock is Part A, step 6)
  - [x] Existing records still show the unit's name, marked "(deleted)"
  - [x] A deleted unit cannot be chosen for anything new
- [ ] Removing a supplier-product link with an open reorder is refused
- [ ] Deleting a product handles its inventory records and open order lines
- [ ] Unit conversions reject duplicates and loops

## Part C: security and access

- [ ] Only an admin can change an admin account or grant the admin role
- [ ] A user cannot add permissions to their own role
- [ ] The orders report needs `orders:view`, the customers report `customers:view`
- [ ] Order notifications go only to users who may view orders
- [ ] Change-password screen for signed-in users
- [ ] Password reset sends real email
- [ ] Signing in on a second device keeps the first signed in, and two tabs refreshing at once stay signed in
- [ ] History records sign-ins, password resets and what changed
- [ ] The login limit sees real client addresses behind a proxy

## Part D: data integrity and scale

- [ ] Two people editing the same record cannot silently overwrite each other (`@Version`)
- [ ] Database unique rules for truck plates and order items
- [ ] Dropdowns, reports and the projection stop loading whole tables
- [ ] Old notifications are cleared out
- [ ] History can page past the latest 500 entries
- [ ] Shipments prevent double-booking a truck and update its status

## Part E: missing features

- [ ] Stock movement screen, picking and transfers between containers
- [ ] Keep container quantities and inventory in agreement
- [ ] Reserve stock for confirmed orders
- [ ] Order total calculated from its items
- [ ] Reorders with several products, and partial receipts
- [ ] Partial returns
- [ ] Restore deleted records
- [ ] CSV import and export
- [ ] Column sorting in tables

## Part F: project readiness

- [ ] README with setup steps and screenshots
- [ ] Database tests run on a throwaway database (Testcontainers)
- [ ] Automated build and tests on every push
- [ ] Frontend tests and linting
- [ ] Dockerfile, health endpoint and API docs
- [ ] Sample data is off unless asked for (`SEED_DATA`)
- [ ] Error screen instead of a blank page when the frontend crashes

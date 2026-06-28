# Invoice Generation Cycle

Explains how Odin determines **when** to generate invoices for a charge plan.

---

## The Algorithm

Odin runs daily at **00:00 UTC**. For each `ACTIVE` charge plan, it computes:

```
next_date = cycle_anchor_date + (N × cycle_interval × cycle_unit)

N = smallest integer ≥ 0 such that next_date ≥ today
```

- If `next_date == today` → generate invoices
- If `next_date > today` → skip, try again tomorrow

`cycle_anchor_date` is a **fixed reference point** set at plan creation. It never changes automatically. `N` is not stored — recalculated from scratch every time Odin runs.

---

## Mental Model: The Ruler

Think of `cycle_anchor_date` as the origin of a ruler. Each tick is one `cycle_interval × cycle_unit` apart. Odin wakes up every day and asks: *"Does today land exactly on a tick?"*

```
anchor = 2026-06-27, MONTHLY, interval = 1

Jun/27   Jul/27   Aug/27   Sep/27   Oct/27
  |        |        |        |        |
 N=0      N=1      N=2      N=3      N=4
```

Days between ticks → Odin skips silently. No back-fill, no catch-up.

---

## Examples — anchor = 2026-06-27

> **Note:** if the plan is created **after** Odin's 00:00 UTC run on 2026-06-27, the first invoice will not be generated today — Odin has already run. The first invoice will be generated on the next tick.

### DAILY — interval = 1 (every day)

```
Jun/27  Jun/28  Jun/29  Jun/30  Jul/01  Jul/02 ...
  |       |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4     N=5
```

Invoice generated: **every single day**.

---

### DAILY — interval = 3 (every 3 days)

```
Jun/27  Jun/30  Jul/03  Jul/06  Jul/09 ...
  |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4
```

| Odin runs on | Next tick | Action |
|---|---|---|
| 2026-06-27 | Jun/27 | **generates** |
| 2026-06-28 | Jun/30 | skip |
| 2026-06-29 | Jun/30 | skip |
| 2026-06-30 | Jun/30 | **generates** |
| 2026-07-01 | Jul/03 | skip |
| 2026-07-03 | Jul/03 | **generates** |

---

### WEEKLY — interval = 1 (every week)

```
Jun/27  Jul/04  Jul/11  Jul/18  Jul/25 ...
  |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4
```

| Odin runs on | Action |
|---|---|
| 2026-06-27 | **generates** |
| 2026-06-28 → Jul/03 | skip |
| 2026-07-04 | **generates** |
| 2026-07-05 → Jul/10 | skip |
| 2026-07-11 | **generates** |

---

### WEEKLY — interval = 2 (every 2 weeks)

```
Jun/27  Jul/11  Jul/25  Aug/08  Aug/22 ...
  |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4
```

Jul/04, Jul/18, Aug/01... do not exist on this ruler — Odin skips them.

---

### MONTHLY — interval = 1 (every month)

```
Jun/27  Jul/27  Aug/27  Sep/27  Oct/27 ...
  |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4
```

Invoice generated on the **27th of every month**.

> **Edge case — months shorter than the anchor day:** if `cycle_anchor_date` were the 31st and a month has fewer days, generation occurs on the last day of that month (e.g. Feb/28 or Feb/29).

---

### MONTHLY — interval = 3 (quarterly)

```
Jun/27  Sep/27  Dec/27  Mar/27  Jun/27 ...
  |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4
```

Invoice generated once per quarter. July, August, October, November... are all skipped.

---

## Past Anchor Date

`cycle_anchor_date` can be set to a past date. The algorithm handles it naturally — it skips all past ticks and waits for the next future one.

**Example:** anchor = 2026-01-05, MONTHLY, interval = 1. Plan created on 2026-06-27.

```
Jan/05  Feb/05  Mar/05  Apr/05  May/05  Jun/05  Jul/05 ...
  |       |       |       |       |       |       |
 N=0     N=1     N=2     N=3     N=4     N=5     N=6
                                                ↑ first invoice
```

On 2026-06-27, Odin finds N=6 (Jul/05) as the first tick ≥ today. Jul/05 ≠ today → skip. First invoice generated on **2026-07-05**.

This is intentional: past anchor lets you model a pre-existing billing rhythm without generating back-fill invoices for skipped cycles.

---

## Key Rules

- `cycle_anchor_date` **never changes automatically**. The value set at plan creation is the permanent reference point for the entire plan lifetime. Odin never touches it.
- The only way `cycle_anchor_date` changes is if the **admin explicitly updates it** via `PATCH /charge-plans/{id}`. That is a deliberate manual action — not a side effect of invoice generation or any scheduler run.
- No back-fill: paused plans or past-anchor plans do not catch up on missed cycles.
- `N` is a runtime variable, never persisted.

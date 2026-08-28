# Demo Workspace

This is a small, safe workspace used while building and demonstrating the local coding agent.

The workspace contains a tiny Python pricing project with one intentional bug. It is designed for the agent to inspect files, edit code, and run tests without installing third-party dependencies.

## Demo task

Fix the failing pricing test by updating the implementation in `src/price_calculator.py`, then verify the fix with:

```bash
python3 -m unittest discover -s tests -v
```

Expected behavior: percentage discounts reduce the pre-tax subtotal, and tax is then applied to the discounted amount.

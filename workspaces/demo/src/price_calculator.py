from dataclasses import dataclass
from typing import Iterable


@dataclass(frozen=True)
class LineItem:
    sku: str
    unit_price: float
    quantity: int


def subtotal(items: Iterable[LineItem]) -> float:
    total = 0.0
    for item in items:
        if item.quantity <= 0:
            raise ValueError("quantity must be positive")
        if item.unit_price < 0:
            raise ValueError("unit_price must not be negative")
        total += item.unit_price * item.quantity
    return round(total, 2)


def calculate_total(items: Iterable[LineItem], discount_percent: float = 0.0, tax_rate: float = 0.0) -> float:
    if discount_percent < 0 or discount_percent > 100:
        raise ValueError("discount_percent must be between 0 and 100")
    if tax_rate < 0:
        raise ValueError("tax_rate must not be negative")

    base = subtotal(items)
    discounted = base * (1 - discount_percent / 100)
    taxed = discounted + base * tax_rate
    return round(taxed, 2)

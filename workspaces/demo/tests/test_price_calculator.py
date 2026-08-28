import unittest

from src.price_calculator import LineItem, calculate_total, subtotal


class PriceCalculatorTests(unittest.TestCase):
    def test_subtotal_adds_line_items(self):
        items = [
            LineItem("notebook", 5.50, 3),
            LineItem("pen", 1.25, 4),
            LineItem("sticker", 4.00, 1),
        ]

        self.assertEqual(subtotal(items), 25.50)

    def test_discount_is_applied_before_tax(self):
        items = [
            LineItem("desk-lamp", 40.00, 1),
            LineItem("cable", 10.00, 2),
        ]

        self.assertEqual(calculate_total(items, discount_percent=10, tax_rate=0.08), 58.32)

    def test_full_discount_leaves_only_zero_taxable_amount(self):
        items = [LineItem("coupon-item", 19.99, 1)]

        self.assertEqual(calculate_total(items, discount_percent=100, tax_rate=0.08), 0.00)

    def test_rejects_invalid_inputs(self):
        with self.assertRaises(ValueError):
            subtotal([LineItem("bad", 1.00, 0)])
        with self.assertRaises(ValueError):
            calculate_total([LineItem("bad", 1.00, 1)], discount_percent=101)
        with self.assertRaises(ValueError):
            calculate_total([LineItem("bad", 1.00, 1)], tax_rate=-0.01)


if __name__ == "__main__":
    unittest.main()

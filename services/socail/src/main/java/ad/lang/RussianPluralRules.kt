package ad.lang

class RussianPluralRules : PluralRules() {
    override fun getIndexForQuantity(quantity: Int): Int {
        if (quantity / 10 % 10 == 1)
            return 2
        val r = quantity % 10
        if (r == 1)
        // 1 хрень
            return 0
        return if (r > 1 && r < 5) 1 else 2
// 6 хреней
    }
}

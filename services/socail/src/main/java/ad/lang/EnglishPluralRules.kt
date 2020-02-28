package ad.lang

class EnglishPluralRules : PluralRules() {
    override fun getIndexForQuantity(quantity: Int): Int {
        return if (quantity == 1) 0 else 1
    }
}

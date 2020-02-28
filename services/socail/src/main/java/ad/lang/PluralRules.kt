package ad.lang

abstract class PluralRules {
    abstract fun getIndexForQuantity(quantity: Int): Int
}

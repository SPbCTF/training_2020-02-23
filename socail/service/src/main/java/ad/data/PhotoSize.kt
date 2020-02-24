package ad.data

import java.net.URI

class PhotoSize(var src: URI, var width: Int, var height: Int, var type: Type, var format: Format) {

    enum class Type {
        LARGE,
        XLARGE;


        fun suffix(): String {
            when (this) {
                LARGE -> return "l"
                XLARGE -> return "xl"
                else -> throw IllegalArgumentException()
            }
        }

        companion object {

            fun fromSuffix(s: String): Type {
                when (s) {
                    "l" -> return LARGE
                    "xl" -> return XLARGE
                }
                throw IllegalArgumentException("Unknown size suffix $s")
            }
        }
    }

    enum class Format {
        WEBP;

        fun fileExtension(): String {
            when (this) {
                WEBP -> return "webp"
                else -> throw IllegalArgumentException()
            }
        }
    }

    companion object {

        val UNKNOWN = -1
    }
}

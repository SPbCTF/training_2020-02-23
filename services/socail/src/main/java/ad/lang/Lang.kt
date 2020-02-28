package ad.lang

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone

class Lang @Throws(IOException::class, JSONException::class)
private constructor(localeID: String) {

    private val data: JSONObject
    private val locale: Locale
    private val pluralRules: PluralRules
    private val dateFormat = ThreadLocal<DateFormat>()
    val name: String

    init {
        val `in` = DataInputStream(Lang::class.java.classLoader.getResourceAsStream("langs/$localeID.json")!!)
        val buf = ByteArray(`in`.available())
        `in`.readFully(buf)
        `in`.close()
        data = JSONObject(String(buf, StandardCharsets.UTF_8))
        locale = Locale.forLanguageTag(localeID)
        when (localeID) {
            "ru" -> pluralRules = RussianPluralRules()
            "en" -> pluralRules = EnglishPluralRules()
            else -> pluralRules = EnglishPluralRules()
        }
        name = data.getString("_name")
    }

    operator fun get(key: String): String {
        try {
            return data.getString(key)
        } catch (x: JSONException) {
            return key
        }

    }

    operator fun get(key: String, vararg formatArgs: Any): String {
        try {
            return String.format(locale, data.getString(key), *formatArgs)
        } catch (x: JSONException) {
            return key + " " + Arrays.toString(formatArgs)
        }

    }

    fun plural(key: String, quantity: Int, vararg formatArgs: Any): String {
        try {
            val v = data.getJSONArray(key)
            if (formatArgs.size > 0) {
                val args = arrayOfNulls<Any>(formatArgs.size + 1)
                args[0] = quantity
                System.arraycopy(formatArgs, 0, args, 1, formatArgs.size)
                return String.format(locale, v.getString(pluralRules.getIndexForQuantity(quantity)), *args)
            } else {
                return String.format(locale, v.getString(pluralRules.getIndexForQuantity(quantity)), quantity)
            }
        } catch (x: JSONException) {
            return quantity.toString() + " " + key + " " + Arrays.toString(formatArgs)
        }

    }

    fun formatDate(date: Date, timeZone: TimeZone): String {
        var format: DateFormat? = dateFormat.get()
        if (format == null) {
            format = SimpleDateFormat("dd MMMM yyyy, HH:mm", locale)
            dateFormat.set(format)
        }
        format.timeZone = timeZone
        return format.format(date)
    }

    fun raw(key: String): Any {
        return data.opt(key)
    }

    companion object {
        private val langsByLocale = HashMap<String, Lang>()
        var list: MutableList<Lang>

        fun getLangByLocale(locale: Locale): Lang {
            val l = langsByLocale[locale.language]
            return l ?: langsByLocale["en"]!!
        }

        init {
            val files = ArrayList<String>()
            list = ArrayList()
            try {
                val `in` = DataInputStream(Lang::class.java.classLoader.getResourceAsStream("langs/index.json")!!)
                val buf = ByteArray(`in`.available())
                `in`.readFully(buf)
                `in`.close()
                val arr = JSONArray(String(buf, StandardCharsets.UTF_8))
                for (i in 0 until arr.length())
                    files.add(arr.getString(i))
            } catch (x: IOException) {
                System.err.println("Error reading langs/index.json")
                x.printStackTrace()
            } catch (x: JSONException) {
                System.err.println("Error parsing langs/index.json")
                x.printStackTrace()
            }

            require(!files.isEmpty()) { "No language files to load; check langs/index.json" }
            for (langFileName in files) {
                try {
                    val l = Lang(langFileName)
                    langsByLocale[langFileName] = l
                    list.add(l)
                } catch (x: IOException) {
                    System.err.println("Error reading langs/$langFileName.json")
                    x.printStackTrace()
                } catch (x: JSONException) {
                    System.err.println("Error parsing langs/$langFileName.json")
                    x.printStackTrace()
                }

            }

            list.sortWith(Comparator { o1, o2 -> o1.locale.toString().compareTo(o2.locale.toString()) })
        }
    }
}

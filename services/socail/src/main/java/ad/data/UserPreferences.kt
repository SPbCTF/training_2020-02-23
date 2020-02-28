package ad.data

import org.json.JSONObject

import java.util.Locale
import java.util.TimeZone

class UserPreferences {
    var locale: Locale? = null
    var timeZone: TimeZone? = null

    fun toJSON(): JSONObject {
        val o = JSONObject()
        if (locale != null)
            o.put("lang", locale!!.toLanguageTag())
        if (timeZone != null)
            o.put("tz", timeZone!!.id)
        return o
    }

    companion object {

        fun fromJSON(o: JSONObject): UserPreferences {
            val prefs = UserPreferences()

            val locale = o.optString("lang", null)
            if (locale != null) {
                prefs.locale = Locale.forLanguageTag(locale)
            }
            val timezone = o.optString("tz", null)
            if (timezone != null) {
                prefs.timeZone = TimeZone.getTimeZone(timezone)
            }

            return prefs
        }
    }
}

package ad.data

import ad.storage.User
import org.json.JSONException
import org.json.JSONObject

import java.sql.ResultSet

import ad.storage.User.UserStorage

class Account {

    var id: Int = 0
    lateinit var email: String
    lateinit var user: User
    var accessLevel: Int = 0
    lateinit var prefs: UserPreferences

    override fun toString(): String {
        return "Account{" +
                "id=" + id +
                ", email='" + email + '\''.toString() +
                ", user=" + user +
                ", accessLevel=" + accessLevel +
                '}'.toString()
    }

    companion object {
        val ACCESS_LEVEL_BANNED = 0
        val ACCESS_LEVEL_REGULAR = 1
        val ACCESS_LEVEL_MODERATOR = 2
        val ACCESS_LEVEL_ADMIN = 3


        fun fromResultSet(res: ResultSet): Account {
            val acc = Account()
            acc.id = res.getInt("id")
            acc.email = res.getString("email")
            acc.accessLevel = res.getInt("access_level")
            acc.user = UserStorage.getById(res.getInt("user_id"))
            val prefs = res.getString("preferences")
            if (prefs == null) {
                acc.prefs = UserPreferences()
            } else {
                try {
                    acc.prefs = UserPreferences.fromJSON(JSONObject(prefs))
                } catch (x: JSONException) {
                    acc.prefs = UserPreferences()
                }

            }
            return acc
        }
    }
}

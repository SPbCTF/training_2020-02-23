package ad.data

import ad.storage.User

import java.sql.ResultSet
import java.sql.SQLException

class FriendRequest {
    lateinit var from: User
    lateinit var message: String

    companion object {


        fun fromResultSet(res: ResultSet): FriendRequest {
            val req = FriendRequest()
            val msg = res.getString("message")
            when (msg) {
                null -> req.message = ""
                else -> req.message = msg
            }
            req.from = User.fromResultSet(res)
            return req
        }
    }
}

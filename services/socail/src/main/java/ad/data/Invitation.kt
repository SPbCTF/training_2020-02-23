package ad.data

import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp

import ad.Utils

class Invitation {
    lateinit var code: String
    lateinit var createdAt: Timestamp

    companion object {


        fun fromResultSet(res: ResultSet): Invitation {
            val inv = Invitation()
            inv.code = Utils.byteArrayToHexString(res.getBytes("code"))
            inv.createdAt = res.getTimestamp("created")
            return inv
        }
    }
}

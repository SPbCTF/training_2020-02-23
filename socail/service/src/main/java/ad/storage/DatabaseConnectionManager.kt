package ad.storage

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import ad.Config

object DatabaseConnectionManager {
    private val connection = ThreadLocal<ConnectionWrapper>()


    fun getConnection2(): Connection {
        var conn: ConnectionWrapper? = connection.get()
        if (conn != null) {
            if (System.currentTimeMillis() - conn.lastUsed < 5 * 60000) {
                conn.lastUsed = System.currentTimeMillis()
                val res = conn.connection!!
                res.autoCommit = true
                return res
            } else {
                conn.connection!!.close()
            }
        }
        println("Opening new database connection for thread " + Thread.currentThread().name)
        if (conn == null)
            conn = ConnectionWrapper()
        val c = DriverManager.getConnection("jdbc:postgresql://" + Config.dbHost + "/" + Config.dbName + "?user=" + Config.dbUser + "&password=" + Config.dbPassword + "&useGmtMillisForDatetimes=true&serverTimezone=GMT&useUnicode=true&characterEncoding=UTF-8&connectTimeout=0&socketTimeout=0&autoReconnect=true")
        c.autoCommit = true
        conn.connection = c
        conn.lastUsed = System.currentTimeMillis()
        connection.set(conn)
        return conn.connection!!
    }

    private class ConnectionWrapper {
        var connection: Connection? = null
        var lastUsed: Long = 0
    }
}

package ad.storage

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types
import java.util.Base64
import java.util.Locale

import ad.Utils
import ad.data.Account
import ad.data.SessionInfo
import ad.data.UserPreferences
import spark.Request
import spark.Session

object SessionStorage {

    private val random = SecureRandom()


    fun putNewSession(sess: Session): String {
        val sid = ByteArray(64)
        val info = sess.attribute<SessionInfo>("info")
        val account = info.account ?: throw IllegalArgumentException("putNewSession requires a logged in session")
        random.nextBytes(sid)
        val stmt = DatabaseConnectionManager.getConnection2().prepareStatement("INSERT INTO \"sessions\" (\"id\", \"account_id\") VALUES (?, ?)")
        stmt.use {
            stmt.setBytes(1, sid)
            stmt.setInt(2, account.id)
            stmt.execute()
        }
        return Base64.getEncoder().encodeToString(sid)
    }


    fun fillSession(psid: String, sess: Session, req: Request): Boolean {
        val sid: ByteArray
        try {
            sid = Base64.getDecoder().decode(psid)
        } catch (x: Exception) {
            return false
        }

        if (sid.size != 64)
            return false

        val stmt = DatabaseConnectionManager.getConnection2().prepareStatement("SELECT * FROM \"accounts\" WHERE \"id\" IN (SELECT \"account_id\" FROM \"sessions\" WHERE \"id\"=?)")
        stmt.use {

            stmt.setBytes(1, sid)
            stmt.executeQuery().use { res ->
                if (!res.next())
                    return false
                val info = SessionInfo()
                val account = Account.fromResultSet(res)
                info.account = account
                info.csrfToken = Utils.csrfTokenFromSessionID(sid)
                if (account.prefs.locale == null) {
                    val requestLocale = req.raw().locale
                    if (requestLocale != null) {
                        account.prefs.locale = requestLocale
                        updatePreferences(account.id, account.prefs)
                    }
                }
                sess.attribute("info", info)
            }
        }
        return true
    }


    fun getAccountForUsernameAndPassword(usernameOrEmail: String, password: String): Account? {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            val hashedPassword = md.digest(password.toByteArray(StandardCharsets.UTF_8))
            val stmt: PreparedStatement
            if (usernameOrEmail.contains("@")) {
                stmt = DatabaseConnectionManager.getConnection2().prepareStatement("SELECT * FROM \"accounts\" WHERE \"email\"=? AND \"password\"=?")
            } else {
                stmt = DatabaseConnectionManager.getConnection2().prepareStatement("SELECT * FROM \"accounts\" WHERE \"user_id\" IN (SELECT \"id\" FROM \"users\" WHERE \"username\"=?) AND \"password\"=?")
            }
            stmt.use {

                stmt.setString(1, usernameOrEmail)
                stmt.setBytes(2, hashedPassword)
                stmt.executeQuery().use { res ->
                    return if (res.next()) {
                        Account.fromResultSet(res)
                    } else null
                }
            }
        } catch (ignore: NoSuchAlgorithmException) {
        }

        throw AssertionError()
    }


    fun deleteSession(psid: String) {
        val sid = Base64.getDecoder().decode(psid)
        if (sid.size != 64)
            return

        val stmt = DatabaseConnectionManager.getConnection2().prepareStatement("DELETE FROM \"sessions\" WHERE \"id\"=?")
        stmt.use {
            stmt.setBytes(1, sid)
            stmt.execute()
        }
    }


    fun registerNewAccount(username: String, password: String, email: String, firstName: String, lastName: String, invite: String, sid: String?): SignupResult {
        val conn = DatabaseConnectionManager.getConnection2()
        conn.autoCommit = false


        try {
            var stmt = conn.prepareStatement("UPDATE \"signup_invitations\" SET \"signups_remaining\"=\"signups_remaining\"-1 WHERE \"signups_remaining\">0 AND \"code\"=?")
            stmt.use {

                stmt.setBytes(1, Utils.hexStringToByteArray(invite))
                if (stmt.executeUpdate() != 1) {
                    conn.rollback()
                    conn.autoCommit = true
                    return SignupResult.INVITE_INVALID
                }
            }

            stmt = conn.prepareStatement("SELECT \"owner_id\" FROM \"signup_invitations\" WHERE \"code\"=?")
            var inviterAccountID = 0
            var userID: Int = 0
            stmt.use {
                stmt.setBytes(1, Utils.hexStringToByteArray(invite))
                stmt.executeQuery().use { res ->
                    res.next()
                    inviterAccountID = res.getInt(1)
                }
                if (inviterAccountID == 0 && sid != null && sid.length > 0) {
                    inviterAccountID = sid.toInt()
                }
            }

            stmt = conn.prepareStatement("INSERT INTO \"users\" (\"fname\", \"lname\", \"username\" ) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
            stmt.use {
                stmt.setString(1, firstName)
                stmt.setString(2, lastName)
                stmt.setString(3, username)
                stmt.execute()

                stmt.generatedKeys.use { res ->
                    res.next()
                    userID = res.getInt(1)
                }
            }

            val md = MessageDigest.getInstance("SHA-256")
            val hashedPassword = md.digest(password.toByteArray(StandardCharsets.UTF_8))
            stmt = conn.prepareStatement("INSERT INTO \"accounts\" (\"user_id\", \"email\", \"password\", \"invited_by\") VALUES (?, ?, ?, ?)")
            stmt.use {
                stmt.setInt(1, userID)
                stmt.setString(2, email)
                stmt.setBytes(3, hashedPassword)
                if (inviterAccountID != 0)
                    stmt.setInt(4, inviterAccountID)
                else
                    stmt.setNull(4, Types.INTEGER)
                stmt.execute()
            }

            if (inviterAccountID != 0) {
                var inviterUserID = 0
                stmt = conn.prepareStatement("SELECT \"user_id\" FROM \"accounts\" WHERE \"id\"=?")
                stmt.use {

                    stmt.setInt(1, inviterAccountID)
                    stmt.executeQuery().use { res ->
                        res.next()
                        inviterUserID = res.getInt(1)
                    }
                }

                stmt = conn.prepareStatement("INSERT INTO \"followings\" (\"follower_id\", \"followee_id\", \"mutual\") VALUES (?, ?, true), (?, ?, true)")
                stmt.use {
                    stmt.setInt(1, inviterUserID)
                    stmt.setInt(2, userID)
                    stmt.setInt(3, userID)
                    stmt.setInt(4, inviterUserID)
                    stmt.execute()
                }
            }


            conn.commit()
            conn.autoCommit=true
        } catch (x: SQLException) {
            x.printStackTrace()
            conn.rollback()
            conn.autoCommit = false
            throw SQLException(x)
        } catch (ignore: NoSuchAlgorithmException) {
        }

        return SignupResult.SUCCESS
    }


    fun updatePassword(accountID: Int, oldPassword: String, newPassword: String): Boolean {

            val md = MessageDigest.getInstance("SHA-256")
            val hashedOld = md.digest(oldPassword.toByteArray(StandardCharsets.UTF_8))
            val hashedNew = md.digest(newPassword.toByteArray(StandardCharsets.UTF_8))
            val conn = DatabaseConnectionManager.getConnection2()
            val stmt = conn.prepareStatement("UPDATE \"accounts\" SET \"password\"=? WHERE \"id\"=? AND \"password\"=?")
            stmt.use {

                stmt.setBytes(1, hashedNew)
                stmt.setInt(2, accountID)
                stmt.setBytes(3, hashedOld)
                return stmt.executeUpdate() == 1
            }

        return false

    }


    fun updatePreferences(accountID: Int, prefs: UserPreferences) {
        val conn = DatabaseConnectionManager.getConnection2()
        val stmt = conn.prepareStatement("UPDATE \"accounts\" SET \"preferences\"=? WHERE \"id\"=?")
        stmt.use {

            stmt.setString(1, prefs.toJSON().toString())
            stmt.setInt(2, accountID)
            stmt.execute()
        }
    }

    enum class SignupResult {
        SUCCESS,
        USERNAME_TAKEN,
        INVITE_INVALID
    }
}

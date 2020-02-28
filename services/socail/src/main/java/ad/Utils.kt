package ad

import org.json.JSONObject
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import org.owasp.html.ElementPolicy
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.HtmlSanitizer
import org.owasp.html.PolicyFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.sql.SQLException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.zip.CRC32

import ad.data.SessionInfo
import ad.data.WebDeltaResponseBuilder
import ad.lang.Lang
import ad.storage.User.UserStorage
import spark.Request
import spark.Response
import spark.utils.StringUtils

object Utils {

    private val RESERVED_USERNAMES = Arrays.asList("account", "settings", "feed", "activitypub", "api", "system", "users", "groups", "posts", "session")
    private val HTML_SANITIZER: PolicyFactory
    private val ISO_DATE_FORMAT: SimpleDateFormat
    private val staticFileHash: String

    init {
        HTML_SANITIZER = HtmlPolicyBuilder()
                .allowStandardUrlProtocols()
                .allowElements("b", "strong", "i", "em", "u", "s", "p", "code", "br")
                .allowElements(ElementPolicy { el, attrs ->
                    val hrefIndex = attrs.indexOf("href")
                    if (hrefIndex != -1 && attrs.size > hrefIndex + 1) {
                        val href = attrs[hrefIndex + 1].toLowerCase()
                        try {
                            val uri = URI(href)
                            if (uri.isAbsolute && !Config.isLocal(uri)) {
                                attrs.add("target")
                                attrs.add("_blank")
                            }
                        } catch (x: URISyntaxException) {
                            attrs.add("target")
                            attrs.add("_blank")
                        }

                    }
                    "a"
                }, "a")
                .allowAttributes("href").onElements("a")
                .toFactory()

        ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        ISO_DATE_FORMAT.timeZone = TimeZone.getTimeZone("GMT")
        staticFileHash = Random().nextLong().toString() + ""
    }


    fun csrfTokenFromSessionID(sid: ByteArray): String {
        val crc = CRC32()
        crc.update(sid, 10, 10)
        val v1 = crc.value
        crc.update(sid, 5, 10)
        val v2 = crc.value
        return String.format(Locale.ENGLISH, "%08x%08x", v1, v2)
    }

    fun addGlobalParamsToTemplate(req: Request, model: JtwigModel) {
        val jsConfig = JSONObject()
        if (req.session(false) != null) {
            var info: SessionInfo? = req.session().attribute<SessionInfo>("info")
            if (info == null) {
                info = SessionInfo()
                req.session().attribute("info", info)
            }
            val account = info.account
            if (account != null) {
                model.with("currentUser", account.user)
                model.with("csrf", info.csrfToken)
                jsConfig.put("csrf", info.csrfToken)
                jsConfig.put("uid", account.user.id)
                try {
                    val notifications = UserStorage.getNotificationsForUser(account.user.id)
                    model.with("userNotifications", notifications)
                } catch (x: SQLException) {
                    throw RuntimeException(x)
                }

            }
        }
        val tz = timeZoneForRequest(req)
        jsConfig.put("timeZone", tz?.id)
        val jsLang = JSONObject()
        val k = req.attribute<ArrayList<String>>("jsLang")
        if (k != null) {
            val lang = lang(req)
            for (key in k) {
                jsLang.put(key, lang.raw(key))
            }
        }
        model.with("locale", localeForRequest(req)).with("timeZone", tz
                ?: TimeZone.getDefault()).with("jsConfig", jsConfig.toString()).with("jsLangKeys", jsLang).with("staticHash", staticFileHash)
    }

    fun renderTemplate(req: Request, name: String, model: JtwigModel): String {
        addGlobalParamsToTemplate(req, model)
        val template = JtwigTemplate.classpathTemplate("templates/desktop/$name.twig", Main.jtwigEnv)
        return template.render(model)
    }

    fun requireAccount(req: Request, resp: Response): Boolean {
        if (req.session(false) == null || req.session().attribute<SessionInfo>("info") == null || (req.session().attribute<SessionInfo>("info") as SessionInfo).account == null) {
            var to = req.pathInfo()
            val query = req.queryString()
            if (StringUtils.isNotEmpty(query))
                to += "?$query"
            resp.redirect("/account/login?to=" + URLEncoder.encode(to))
            return false
        }
        return true
    }

    fun verifyCSRF(req: Request, resp: Response): Boolean {
        return true;// disable for ctf
//        val info = req.session().attribute<SessionInfo>("info")
//        val reqCsrf = req.queryParams("csrf")
//        if (reqCsrf != null && reqCsrf == info.csrfToken) {
//            return true
//        }
//        resp.status(403)
//        return false
    }

    fun jsLangKey(req: Request, vararg keys: String) {
        var k: ArrayList<String>? = req.attribute<ArrayList<String>>("jsLang")
        if (k == null) {
            k = ArrayList<String>()
            req.attribute("jsLang", k)
        }
        Collections.addAll(k, *keys)
    }

    fun parseIntOrDefault(s: String?, d: Int): Int {
        if (s == null)
            return d
        try {
            return Integer.parseInt(s)
        } catch (x: NumberFormatException) {
            return d
        }

    }

    fun wrapError(req: Request, resp: Response, errorKey: String, vararg formatArgs: Any): String {
        val info = req.session().attribute<SessionInfo>("info")
        val l = Lang.getLangByLocale(localeForRequest(req))
        val msg = if (formatArgs.size > 0) l.get(errorKey, *formatArgs) else l.get(errorKey)
        if (isAjax(req)) {
            resp.type("application/json")
            return WebDeltaResponseBuilder().messageBox(l.get("error"), msg, l.get("ok")).json().toString()
        }
        return renderTemplate(req, "generic_error", JtwigModel.newModel().with("error", msg).with("back", info.history.last()))
    }

    fun localeForRequest(req: Request): Locale {
        val info = sessionInfo(req)
        if (info != null) {
            val account = info.account
            if (account != null && account.prefs.locale != null)
                return account.prefs.locale!!
            if (info.preferredLocale != null)
                return info.preferredLocale!!
        }
        return if (req.raw().locale != null) req.raw().locale else Locale.US
    }

    fun timeZoneForRequest(req: Request): TimeZone? {
        val info = sessionInfo(req)
        if (info != null) {
            val account = info.account
            if (account != null && account.prefs.timeZone != null)
                return account.prefs.timeZone
            if (info.preferredLocale != null)
                return info.timeZone
        }
        return null
    }

    fun lang(req: Request): Lang {
        return Lang.getLangByLocale(localeForRequest(req))
    }

    fun isValidUsername(username: String): Boolean {
        return username.matches("^[a-zA-Z][a-zA-Z0-9._-]+$".toRegex())
    }

    fun isReservedUsername(username: String): Boolean {
        return RESERVED_USERNAMES.contains(username.toLowerCase())
    }

    fun isValidEmail(email: String): Boolean {
        return email.matches("^[^@]+@[^@]+\\.[^@]{2,}$".toRegex())
    }

    fun byteArrayToHexString(arr: ByteArray): String {
        val chars = CharArray(arr.size * 2)
        val hex = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
        for (i in arr.indices) {
            chars[i * 2] = hex[arr[i].toInt() shr 4 and 0x0F]
            chars[i * 2 + 1] = hex[arr[i].toInt() and 0x0F]
        }
        return String(chars)
    }

    fun hexStringToByteArray(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Even-length string required" }
        val res = ByteArray(hex.length / 2)
        for (i in res.indices) {
            res[i] = (Character.digit(hex[i * 2], 16) shl 4 or Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return res
    }

    fun sanitizeHTML(src: String): String {
        val sb = StringBuilder()
        HtmlSanitizer.sanitize(src, HTML_SANITIZER.apply(SimpleHtmlStreamRenderer(sb)))
        return sb.toString()
    }

    fun formatDateAsISO(date: Date): String {
        return ISO_DATE_FORMAT.format(date)
    }

    fun parseISODate(date: String): Date? {
        try {
            return ISO_DATE_FORMAT.parse(date)
        } catch (e: ParseException) {
            return null
        }

    }

    fun userIdFromKeyId(keyID: URI): URI {
        try {
            // most AP servers use key IDs in the form http://example.com/users/username#main-key so removing the URI fragment is enough
            if (keyID.fragment != null) {
                return URI(keyID.scheme, keyID.schemeSpecificPart, null)
            }
            // Misskey does this: https://misskey.io/users/7rkrarq81i/publickey
            val uri = keyID.resolve("./")
            return URI(uri.scheme, uri.schemeSpecificPart.replace("/$".toRegex(), ""), null)
        } catch (x: URISyntaxException) {
            throw RuntimeException("checked exceptions are stupid")
        }

    }

    fun parseFileSize(size: String): Long {
        var size = size
        size = size.toUpperCase()
        require(size.matches("^\\d+[KMGT]?$".toRegex())) { "String '$size' does not have the correct format" }
        val unit = size[size.length - 1]
        if (Character.isDigit(unit)) {
            return java.lang.Long.parseLong(size)
        }
        var n = java.lang.Long.parseLong(size.substring(0, size.length - 1))
        when (unit) {
            'K' -> n *= 1024L
            'M' -> n *= 1024L * 1024L
            'G' -> n *= 1024L * 1024L * 1024L
            'T' -> n *= 1024L * 1024L * 1024L * 1024L
        }
        return n
    }

    fun getLastPathSegment(uri: URI): String? {
        val path = uri.path
        val index = path.lastIndexOf('/')
        return if (index == -1) null else path.substring(index + 1)
    }

    fun sessionInfo(req: Request): SessionInfo? {
        return req.session().attribute<SessionInfo>("info")
    }

    fun deserializeIntArray(a: ByteArray?): IntArray? {
        if (a == null || a.size % 4 != 0)
            return null
        val result = IntArray(a.size / 4)
        try {
            val `in` = DataInputStream(ByteArrayInputStream(a))
            for (i in result.indices)
                result[i] = `in`.readInt()
        } catch (ignore: IOException) {
        }

        return result
    }

    fun serializeIntArray(a: IntArray?): ByteArray? {
        if (a == null || a.size == 0)
            return null
        val os = ByteArrayOutputStream()
        try {
            val out = DataOutputStream(os)
            for (i in a)
                out.writeInt(i)
        } catch (ignore: IOException) {
        }

        return os.toByteArray()
    }

    fun back(req: Request): String {
        val redir = req.queryParams("_redir")
        if (redir != null)
            return redir
        val ref = req.headers("referer")
        return ref ?: sessionInfo(req)!!.history.last()
    }

    fun truncateOnWordBoundary(s: String, maxLen: Int): String {
        var s = s
        s = s.replace("</p><p>", "\n\n").replace("<br/>", "\n").replace("<[^>]+>".toRegex(), "")
        if (s.length <= maxLen + 20)
            return s
        val len = Math.min(s.indexOf(' ', maxLen), maxLen + 20)
        return s.substring(0, len) + "..."
    }

    fun isAjax(req: Request): Boolean {
        return req.queryParams("_ajax") != null
    }

    fun fillFields(o: Any, req: Request): Any {
        val ps = req.queryParams()
        ps.forEach { s ->
            try {
                val p = req.queryParams(s)
                val f = o.javaClass.getDeclaredField(s)
                f.isAccessible = true
                var `val`: Any? = null
                if (f.type == String::class.java)
                    `val` = p
                else if (f.type == Int::class.javaPrimitiveType)
                    `val` = Integer.parseInt(p)
                else if (f.type == Boolean::class.javaPrimitiveType) `val` = "on" == p
                f.set(if (f.modifiers and 8 == 0) o else null, `val`)
            } catch (e: NoSuchFieldException) {
            } catch (e: IllegalAccessException) {
            }
        }
        return o
    }
}

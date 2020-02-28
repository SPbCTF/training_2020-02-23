package ad.routes

import org.jtwig.JtwigModel

import java.util.Base64

import ad.Utils
import ad.data.Account
import ad.data.SessionInfo
import ad.storage.SessionStorage
import ad.storage.User.UserStorage
import spark.Request
import spark.Response
import spark.utils.StringUtils

object SessionRoutes {
    
    private fun setupSessionWithAccount(req: Request, resp: Response, acc: Account) {
        val info = SessionInfo()
        info.account = acc
        req.session(true).attribute("info", info)
        val psid = SessionStorage.putNewSession(req.session())
        info.csrfToken = Utils.csrfTokenFromSessionID(Base64.getDecoder().decode(psid))
        if (acc.prefs.locale == null) {
            val requestLocale = req.raw().locale
            if (requestLocale != null) {
                acc.prefs.locale = requestLocale
                SessionStorage.updatePreferences(acc.id, acc.prefs)
            }
        }
        resp.cookie("/", "psid", psid, 10 * 365 * 24 * 60 * 60, false)
    }

    
    fun login(req: Request, resp: Response): Any {
        val info = Utils.sessionInfo(req)
        if (info != null && info.account != null) {
            resp.redirect("/feed")
            return ""
        }
        val model = JtwigModel.newModel()
        if (req.requestMethod().equals("post", ignoreCase = true)) {
            val acc = SessionStorage.getAccountForUsernameAndPassword(req.queryParams("username"), req.queryParams("password"))
            if (acc != null) {
                setupSessionWithAccount(req, resp, acc)
                val to = req.queryParams("to")
                if (StringUtils.isNotEmpty(to))
                    resp.redirect(to)
                else
                    resp.redirect("/feed")
                return ""
            }
            model.with("message", Utils.lang(req).get("login_incorrect"))
        } else if (StringUtils.isNotEmpty(req.queryParams("to"))) {
            model.with("message", Utils.lang(req).get("login_needed"))
        }
        model.with("additionalParams", req.queryString())
        return Utils.renderTemplate(req, "login", model)
    }

    
    fun logout(req: Request, resp: Response): Any? {
        if (Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)) {
            SessionStorage.deleteSession(req.cookie("psid"))
            resp.removeCookie("psid")
            val info = req.session().attribute<SessionInfo>("info")
            info.account = null
            info.csrfToken = null
            resp.redirect("/")
            return ""
        }
        return null
    }

    private fun regError(req: Request, errKey: String): String {
        val model = JtwigModel.newModel()
                .with("message", Utils.lang(req).get(errKey))
                .with("username", req.queryParams("username"))
                .with("password", req.queryParams("password"))
                .with("password2", req.queryParams("password2"))
                .with("email", req.queryParams("email"))
                .with("first_name", req.queryParams("first_name"))
                .with("last_name", req.queryParams("last_name"))
                .with("invite", req.queryParams("invite"))
        return Utils.renderTemplate(req, "register", model)
    }

    
    fun register(req: Request, resp: Response): Any {
        val username = req.queryParams("username")
        val password = req.queryParams("password")
        val sid = req.queryParams("sid")
        val password2 = req.queryParams("password2")
        val email = req.queryParams("email")
        val first = req.queryParams("first_name")
        val last = req.queryParams("last_name")
        val invite = req.queryParams("invite")

        if (!Utils.isValidUsername(username))
            return regError(req, "err_reg_invalid_username")
        if (Utils.isReservedUsername(username))
            return regError(req, "err_reg_reserved_username")
        if (UserStorage.getByUsername(username) != null)
            return regError(req, "err_reg_username_taken")
        if (password.length < 4)
            return regError(req, "err_password_short")
        if (password != password2)
            return regError(req, "err_passwords_dont_match")
        if (!Utils.isValidEmail(email))
            return regError(req, "err_invalid_email")
        if (first.length < 2)
            return regError(req, "err_name_too_short")
        if (!invite.matches("[A-Fa-f0-9]{32}".toRegex()))
            return regError(req, "err_invalid_invitation")


        val res = SessionStorage.registerNewAccount(username, password, email, first, last, invite, sid)
        if (res === SessionStorage.SignupResult.SUCCESS) {
            val acc = SessionStorage.getAccountForUsernameAndPassword(username, password)
            setupSessionWithAccount(req, resp, acc!!)
            resp.redirect("/feed")
        } else if (res === SessionStorage.SignupResult.USERNAME_TAKEN) {
            return regError(req, "err_reg_username_taken")
        } else if (res === SessionStorage.SignupResult.INVITE_INVALID) {
            return regError(req, "err_invalid_invitation")
        }

        return ""
    }
}

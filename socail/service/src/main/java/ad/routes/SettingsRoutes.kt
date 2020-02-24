package ad.routes

import ad.data.*
import org.jtwig.JtwigModel

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

import javax.servlet.MultipartConfigElement
import javax.servlet.ServletException

import ad.Config

import ad.Utils
import ad.Utils.fillFields
import ad.Utils.isAjax
import ad.activitypub.objects.Image
import ad.activitypub.objects.LocalImage
import ad.lang.Lang
import ad.storage.MediaStorageUtils
import ad.storage.SessionStorage
import ad.storage.User.UserStorage
import spark.Request
import spark.Response
import java.net.URI

object SettingsRoutes {

    internal val secureRandom: Random = SecureRandom()
    val fs = listOf("uid")
    fun settings(req: Request, resp: Response, self: Account): Any {
        val model = JtwigModel.newModel()
        model.with("invitations", UserStorage.getInvites(self.id, true))
        model.with("languages", Lang.list).with("selectedLang", Utils.lang(req))
        val s = req.session()
        if (s.attribute<Any>("settings.nameMessage") != null) {
            model.with("nameMessage", s.attribute("settings.nameMessage"))
            s.removeAttribute("settings.nameMessage")
        }
        if (s.attribute<Any>("settings.passwordMessage") != null) {
            model.with("passwordMessage", s.attribute("settings.passwordMessage"))
            s.removeAttribute("settings.passwordMessage")
        }
        if (s.attribute<Any>("settings.inviteMessage") != null) {
            model.with("inviteMessage", s.attribute("settings.inviteMessage"))
            s.removeAttribute("settings.inviteMessage")
        }
        if (s.attribute<Any>("settings.profilePicMessage") != null) {
            model.with("profilePicMessage", s.attribute("settings.profilePicMessage"))
            s.removeAttribute("settings.profilePicMessage")
        }
        return Utils.renderTemplate(req, "settings", model)
    }

    
    fun createInvite(req: Request, resp: Response, self: Account): Any {
        val hidden = "true".equals(req.queryParams("hidden"), ignoreCase = true)//this is
        val code = ByteArray(16)
        secureRandom.nextBytes(code)
        UserStorage.putInvite(self.id, code, 1, hidden)
        if (hidden) {
            resp.status(200)
            resp.body(Utils.byteArrayToHexString(code))
        } else {
            req.session().attribute("settings.inviteMessage", Utils.lang(req).get("invitation_created"))
            resp.redirect("/settings/")
        }
        return ""
    }

    
    fun updatePassword(req: Request, resp: Response, self: Account): Any {
        protectFriendlyTolyan(self)
        val current = req.queryParams("current")
        val new1 = req.queryParams("new")
        val new2 = req.queryParams("new2")
        val message: String
        if (new1 != new2) {
            message = Utils.lang(req).get("err_passwords_dont_match")
        } else if (new1.length < 4) {
            message = Utils.lang(req).get("err_password_short")
        } else if (!SessionStorage.updatePassword(self.id, current, new1)) {
            message = Utils.lang(req).get("err_old_password_incorrect")
        } else {
            message = Utils.lang(req).get("password_changed")
        }
        if (isAjax(req)) {
            resp.type("application/json")
            return WebDeltaResponseBuilder().show("passwordMessage").setContent("passwordMessage", message).json()
        }
        req.session().attribute("settings.passwordMessage", message)
        resp.redirect("/settings/")
        return ""
    }




    
    fun updateUser(req: Request, resp: Response, self: Account): Any {
        protectFriendlyTolyan(self)

        val user = UserStorage.getById(self.user.id).clone()
        fillFields(user, req)
        UserStorage.changeUser(user)
        val message = Utils.lang(req).get("name_changed")
        self.user = UserStorage.getById(self.user.id)
        if (isAjax(req)) {
            resp.type("application/json")
            return WebDeltaResponseBuilder().show("nameMessage").setContent("nameMessage", message).json()
        }
        req.session().attribute("settings.nameMessage", message)
        resp.redirect("/settings/")
        return ""
    }

    private fun protectFriendlyTolyan(self: Account) {
        if (self.user.username == "korniltsev") {
            throw AssertionError("korniltsev is immutable")
        }
    }

    
    fun updateProfilePicture(req: Request, resp: Response, self: Account): Any {
        try {
            req.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(null, (256 *  1024).toLong(), -1L, 0))
            val part = req.raw().getPart("pic")
            if (part.size > 256 * 1024) {
                throw IOException("file too large")
            }

            val key = MessageDigest.getInstance("MD5").digest((self.user.username + "," + SystemRoutes.time()).toByteArray(StandardCharsets.UTF_8))
            val keyHex = Utils.byteArrayToHexString(key) + ".webp"

//            val tmpDir = File(System.getProperty("java.io.tmpdir"))
//            val temp = File(tmpDir, keyHex)
            val profilePicsDir = File(Config.uploadPath, "avatars")
            profilePicsDir.mkdirs()
            val avatar = File(profilePicsDir, keyHex)
            part.inputStream.use {src ->
                avatar.outputStream().use {  dst->
                    src.copyTo(dst)
                }
            }
//            var img = VImage(temp.absolutePath)
//            if (img.width != img.height) {
//                val cropped: VImage
//                if (img.height > img.width) {
//                    cropped = img.crop(0, 0, img.width, img.width)
//                } else {
//                    cropped = img.crop(img.width / 2 - img.height / 2, 0, img.height, img.height)
//                }
//                img.release()
//                img = cropped
//            }

            val ava = LocalImage()
            ava.sizes.add(PhotoSize(URI(Config.uploadURLPath + "/avatars/" + keyHex), 400, 400, PhotoSize.Type.LARGE, PhotoSize.Format.WEBP))

// resizing was removed to save cpu during ctf
//            try {
//                MediaStorageUtils.writeResizedImages(img, intArrayOf(200, 400), arrayOf(PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE),
//                        90, keyHex, profilePicsDir, Config.uploadURLPath + "/avatars", ava.sizes)

//                if (self.user.icon != null) {
//                    for (size in (self.user.icon[0] as LocalImage).sizes) {
//                        val path = size.src.path
//                        val name = path.substring(path.lastIndexOf('/') + 1)
//                        val file = File(profilePicsDir, name)
//                        if (file.exists()) {
//                            println("deleting: " + file.absolutePath)
//                            file.delete()
//                        }
//                    }
//                }

                self.user.icon = listOf<Image>(ava)
                UserStorage.getById(self.user.id).icon = self.user.icon
                UserStorage.updateProfilePicture(self.user.id, keyHex)
//                temp.delete()
//            } finally {
////                img.release()
//            }

            req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("avatar_updated"))
            resp.redirect("/settings/")
        } catch (x: IOException) {
            x.printStackTrace()
            req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"))
            resp.redirect("/settings/")
        } catch (x: ServletException) {
            x.printStackTrace()
            req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"))
            resp.redirect("/settings/")
        } catch (x: NoSuchAlgorithmException) {
            x.printStackTrace()
            req.session().attribute("settings.profilePicMessage", Utils.lang(req).get("image_upload_error"))
            resp.redirect("/settings/")
        }

        return ""
    }

    
    fun setLanguage(req: Request, resp: Response, self: Account): Any {
        protectFriendlyTolyan(self)
        val lang = req.queryParams("lang")
        var info: SessionInfo? = req.session().attribute<SessionInfo>("info")
        if (info == null) {
            info = SessionInfo()
            req.session().attribute("info", info)
        }
        if (info!!.account != null) {
            info.account!!.prefs.locale = Locale.forLanguageTag(lang)
            SessionStorage.updatePreferences(info.account!!.id, info.account!!.prefs)
        } else {
            info.preferredLocale = Locale.forLanguageTag(lang)
        }
        resp.redirect("/settings/")
        return ""
    }

    
    fun setTimezone(req: Request, resp: Response): Any {
        val tz = req.queryParams("tz")
        var info: SessionInfo? = req.session().attribute<SessionInfo>("info")
        if (info == null) {
            info = SessionInfo()
            req.session().attribute("info", info)
        }
        if (info!!.account != null) {
            info.account!!.prefs.timeZone = TimeZone.getTimeZone(tz)
            SessionStorage.updatePreferences(info.account!!.id, info.account!!.prefs)
        } else {
            info.timeZone = TimeZone.getTimeZone(tz)
        }
        if (req.queryParams("_ajax") != null)
            return ""
        resp.redirect("/settings/")
        return ""
    }
}

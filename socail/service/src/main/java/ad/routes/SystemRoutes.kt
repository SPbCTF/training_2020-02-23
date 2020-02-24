package ad.routes

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import javax.servlet.MultipartConfigElement
import javax.servlet.ServletException
import javax.servlet.http.Part

import ad.Config
import ad.Utils
import ad.activitypub.objects.ActivityPubObject
import ad.activitypub.objects.Document
import ad.activitypub.objects.LocalImage
import ad.data.Account
import ad.data.PhotoSize
import ad.data.SessionInfo
import ad.storage.MediaStorageUtils
import spark.Request
import spark.Response

object SystemRoutes {


    fun uploadPostPhoto(req: Request, resp: Response, self: Account): Any {
        try {
            req.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement(null, (256 * 1024).toLong(), -1L, 0))
            val part = req.raw().getPart("file")
            if (part.size > 256 * 1024 ) {
                throw IOException("file too large")
            }

            val key = MessageDigest.getInstance("MD5").digest((self.user.username + "," + time() + "," + part.submittedFileName).toByteArray(StandardCharsets.UTF_8))
            val keyHex = Utils.byteArrayToHexString(key)
            val fname = keyHex + ".webp"

            val mime = part.contentType
            if (!mime.startsWith("image/"))
                throw IOException("incorrect mime type")

            val dstpath = Config.uploadURLPath + "/post_media/" + fname

            val tmpDir = File(System.getProperty("java.io.tmpdir"))
            val temp = File(tmpDir, keyHex)


//            val img = VImage(temp.absolutePath)

            val photo = LocalImage()
            val postMediaDir = File(Config.uploadPath, "post_media")
            postMediaDir.mkdirs()
            val dst = File(postMediaDir, fname)
            part.inputStream.use { src->
                dst.outputStream().use { dst->
                    src.copyTo(dst)
                }
            }

            try {
//                MediaStorageUtils.writeResizedImages(img, intArrayOf(1280, 2560), arrayOf(PhotoSize.Type.LARGE, PhotoSize.Type.XLARGE),
//                        100, keyHex, postMediaDir, Config.uploadURLPath + "/post_media", photo.sizes)

                val sess = Utils.sessionInfo(req)
                photo.localID = dstpath
                photo.mediaType = mime
                photo.path = "post_media"
                sess!!.postDraftAttachments.add(photo)

                temp.delete()
            } finally {
//                img.release()
            }

            resp.redirect(Utils.back(req))
        } catch (x: IOException) {
            x.printStackTrace()
        } catch (x: ServletException) {
            x.printStackTrace()
        } catch (x: NoSuchAlgorithmException) {
            x.printStackTrace()
        }

        return ""
    }

    fun time(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun deleteDraftAttachment(req: Request, resp: Response, self: Account): Any {
        val sess = Utils.sessionInfo(req)
        val id = req.queryParams("id")
        if (id == null) {
            resp.status(400)
            return ""
        }
        for (o in sess!!.postDraftAttachments) {
            if (o is Document) {
                if (id == o.localID) {
                    sess.postDraftAttachments.remove(o)
                    MediaStorageUtils.deleteAttachmentFiles(o)
                    break
                }
            }
        }
        resp.redirect(Utils.back(req))
        return ""
    }
}

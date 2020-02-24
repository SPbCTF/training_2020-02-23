package ad.jtwigext

import org.jtwig.escape.EscapeEngine
import org.jtwig.escape.NoneEscapeEngine
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.render.context.RenderContextHolder

import java.math.BigDecimal

import ad.data.PhotoSize
import ad.storage.MediaStorageUtils
import kotlin.math.sign

class PictureForAvatarFunction : SimpleJtwigFunction() {
    override fun name(): String {
        return "pictureForAvatar"
    }

    override fun execute(functionRequest: FunctionRequest): Any {
        RenderContextHolder.get().set(EscapeEngine::class.java, NoneEscapeEngine.instance())

        val get = functionRequest.get(0)
        val get1 = functionRequest.get(1)
        val sizes = get as List<PhotoSize> ?
        val _type = get1 as String?
//        val type: PhotoSize.Type
//        val type2x: PhotoSize.Type
        var size: Int
        when (_type) {
            "s" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
                size = 50
            }
            "m" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
                size = 100
            }
            "l" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
                size = 200
            }
            "xl" -> {
//                type = PhotoSize.Type.XLARGE
//                type2x = type
                size = 400
            }
            else -> throw IllegalArgumentException("Wrong size type $_type")
        }
        if (functionRequest.numberOfArguments > 2)
            size = (functionRequest.get(2) as BigDecimal).toInt()
        if (sizes == null || sizes.isEmpty())
            return "<span class=\"ava avaPlaceholder\" style=\"width: " + size + "px;height: " + size + "px\"></span>"

        val webp1x = sizes[0]
//        val webp1x = MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type)
//        val webp2x = MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type2x)

        return "<picture>" +
                "<source srcset=\"" + webp1x.src + ""  + "\" type=\"image/webp\"/>" +
                "<img src=\"" + webp1x.src + "\" width=\"" + size + "\" height=\"" + size + "\" class=\"ava\"/>" +
                "</picture>"
    }
}

package ad.jtwigext

import org.jtwig.escape.EscapeEngine
import org.jtwig.escape.NoneEscapeEngine
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.render.context.RenderContextHolder

import ad.data.PhotoSize
import ad.storage.MediaStorageUtils

class PhotoSizeFunction : SimpleJtwigFunction() {
    override fun name(): String {
        return "photoPicture"
    }

    override fun execute(functionRequest: FunctionRequest): Any {
        RenderContextHolder.get().set(EscapeEngine::class.java, NoneEscapeEngine.instance())

        val arg0 = functionRequest.get(0) as String?

        // nice resizing was removed to save some CPU during ctf


//        if (arg0 !is List<*>) {
//            return "oops " + functionRequest.position
//        }
//        val sizes = functionRequest.get(0) as List<PhotoSize>
//        val _type = functionRequest.get(1) as String
//        val type: PhotoSize.Type
//        val  type2x: PhotoSize.Type
//        when (_type) {
//            "xs" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
//            }
//            "s" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
//            }
//            "m" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
//            }
//            "l" -> {
//                type = PhotoSize.Type.LARGE
//                type2x = PhotoSize.Type.XLARGE
//            }
//            "xl" ->  {
//                type = PhotoSize.Type.XLARGE
//                type2x = type
//            }
//
//            else -> throw IllegalArgumentException("Wrong size type $_type")
//        }
//
//        val webp1x = MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type)
//        val webp2x = MediaStorageUtils.findBestPhotoSize(sizes, PhotoSize.Format.WEBP, type2x)

        return "<picture>" +
//                "<source srcset=\"" + arg0 +  " \" type=\"image/webp\"/>" +
                "<img src=\"" + arg0 + "\" />" +
                "</picture>"
    }
}

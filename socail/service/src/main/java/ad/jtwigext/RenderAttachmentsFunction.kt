package ad.jtwigext

import org.jtwig.escape.EscapeEngine
import org.jtwig.escape.NoneEscapeEngine
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.render.context.RenderContextHolder

import java.util.ArrayList

import ad.data.PhotoSize
import ad.data.attachments.Attachment
import ad.data.attachments.PhotoAttachment
import ad.data.attachments.VideoAttachment
//import ad.storage.MediaCache;
import ad.storage.MediaStorageUtils

class RenderAttachmentsFunction : SimpleJtwigFunction() {

    override fun name(): String {
        return "renderAttachments"
    }

    override fun execute(functionRequest: FunctionRequest): Any {
        functionRequest.minimumNumberOfArguments(1)
        RenderContextHolder.get().set(EscapeEngine::class.java, NoneEscapeEngine.instance())

        val arg = functionRequest.get(0)
        if (arg !is List<*>)
            return ""
        val attachment = functionRequest.get(0) as List<Attachment>

        val lines = ArrayList<String>()
        for (obj in attachment) {
            // TODO content warnings

            if (obj is PhotoAttachment) {
                lines.add("<picture>" +
                        "<source srcset=\"" + obj.localId + "\" type=\"image/webp\"/>" +
                        "<img src=\"" + obj.localId + "\"/>" +
                        "</picture>")
            } else if (obj is VideoAttachment) {
                lines.add("<video src=\"" + obj.url + "\" controls></video>")
            }
        }
        return lines.joinToString("\n")
    }
}

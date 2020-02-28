package ad.activitypub.objects

import org.json.JSONArray
import org.json.JSONObject

import java.util.ArrayList

import ad.Config
import ad.activitypub.ContextCollector
import ad.data.PhotoSize

class LocalImage : Image() {
    var sizes = ArrayList<PhotoSize>()
    lateinit var path: String


    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        localID = obj.getString("_lid")
        val s = obj.getJSONArray("_sz")
        val types = s.getString(0).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        path = "post_media"
        var offset = 0
        for (t in types) {
            val type = PhotoSize.Type.fromSuffix(t)
            val width = s.getInt(++offset)
            val height = s.getInt(++offset)
            val name = Config.uploadURLPath + "/" + path + "/" + localID + "_" + type.suffix()
            sizes.add(PhotoSize(Config.localURI("$name.webp"), width, height, type, PhotoSize.Format.WEBP))
        }

        return this
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)

        var biggest: PhotoSize? = null
        var biggestArea = 0
        for (s in sizes) {
            if (s.format != PhotoSize.Format.WEBP)
                continue
            val area = s.width * s.height
            if (area > biggestArea) {
                biggestArea = area
                biggest = s
            }
        }
        if (biggest != null) {
            obj.put("url", biggest.src.toString())
        }

        return obj
    }
}

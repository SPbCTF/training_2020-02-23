package ad.activitypub.objects

import org.json.JSONObject

import ad.activitypub.ContextCollector

open class Image : Document() {

    var width: Int = 0
    var height: Int = 0

    override fun getType(): String {
        return "Image"
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        if (width > 0)
            obj.put("width", width)
        if (height > 0)
            obj.put("height", height)
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        width = obj.optInt("width")
        height = obj.optInt("height")
        return this
    }
}

package ad.activitypub.objects

import org.json.JSONObject

import java.net.URI

import ad.activitypub.ContextCollector

class CollectionPage(ordered: Boolean) : ActivityPubCollection(ordered) {

    var partOf: URI? = null
    var prev: URI? = null
    var next: URI? = null

    override fun getType(): String {
        return if (ordered) "OrderedCollectionPage" else "CollectionPage"
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        if (partOf != null)
            obj.put("partOf", partOf!!.toString())
        if (prev != null)
            obj.put("prev", prev!!.toString())
        if (next != null)
            obj.put("next", next!!.toString())
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        partOf = tryParseURL(obj.optString("partOf"))
        prev = tryParseURL(obj.optString("prev"))
        next = tryParseURL(obj.optString("next"))
        return this
    }
}

package ad.activitypub.objects

import org.json.JSONObject

import java.net.URI

import ad.activitypub.ContextCollector

open class ActivityPubCollection(var ordered: Boolean) : ActivityPubObject() {

    var totalItems: Int = 0
    var current: URI? = null
    var first: LinkOrObject? = null
    var last: URI? = null
    var items: List<LinkOrObject>? = null

    override fun getType(): String {
        return if (ordered) "OrderedCollection" else "Collection"
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        obj.put("totalItems", totalItems)
        if (current != null)
            obj.put("current", current!!.toString())
        if (first != null)
            obj.put("first", first!!.serialize(contextCollector))
        if (last != null)
            obj.put("last", last!!.toString())
        if (items != null)
            obj.put(if (ordered) "orderedItems" else "items", serializeLinkOrObjectArray(items!!, contextCollector))
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        totalItems = obj.optInt("totalItems")
        current = tryParseURL(obj.optString("current"))
        first = tryParseLinkOrObject(obj.optString("first"))
        last = tryParseURL(obj.optString("last"))
        items = tryParseArrayOfLinksOrObjects(obj.opt(if (ordered) "orderedItems" else "items"))
        return this
    }
}

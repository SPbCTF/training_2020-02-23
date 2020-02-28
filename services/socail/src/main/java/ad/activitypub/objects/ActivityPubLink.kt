package ad.activitypub.objects

import org.json.JSONObject

import java.net.URI

import ad.activitypub.ContextCollector

abstract class ActivityPubLink : ActivityPubObject() {
    var href: URI? = null

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        obj.put("href", href!!.toString())
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        href = tryParseURL(obj.getString("href"))
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder("$type{")
        sb.append(super.toString())
        if (href != null) {
            sb.append("href=")
            sb.append(href)
        }
        sb.append('}')
        return sb.toString()
    }
}

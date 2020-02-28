package ad.activitypub.objects

import org.json.JSONObject

import java.net.URI

import ad.activitypub.ContextCollector

class Relationship : ActivityPubObject() {

    var subject: LinkOrObject? = null
    var `object`: LinkOrObject? = null
    var relationship: URI? = null

    override fun getType(): String {
        return "Relationship"
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        obj.put("relationship", relationship!!.toString())
        obj.put("object", `object`!!.serialize(contextCollector))
        obj.put("subject", subject!!.serialize(contextCollector))
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        relationship = tryParseURL(obj.getString("relationship"))
        `object` = tryParseLinkOrObject(obj.get("object"))
        subject = tryParseLinkOrObject(obj.get("subject"))
        return this
    }

    companion object {

        val FRIEND_OF = URI.create("http://purl.org/vocab/relationship/friendOf")
    }
}

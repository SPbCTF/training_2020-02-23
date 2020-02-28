package ad.activitypub.objects

import org.json.JSONObject

import ad.activitypub.ContextCollector

abstract class Activity : ActivityPubObject() {

    var actor: LinkOrObject? = null
    var `object`: LinkOrObject? = null
    var target: LinkOrObject? = null
    var result: List<LinkOrObject>? = null
    var origin: LinkOrObject? = null
    var instrument: LinkOrObject? = null

    abstract override fun getType(): String

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        obj.put("actor", actor!!.serialize(contextCollector))
        obj.put("object", `object`!!.serialize(contextCollector))
        if (target != null)
            obj.put("target", target!!.serialize(contextCollector))
        if (result != null && !result!!.isEmpty())
            obj.put("result", serializeLinkOrObjectArray(result!!, contextCollector))
        if (origin != null)
            obj.put("origin", origin!!.serialize(contextCollector))
        if (instrument != null)
            obj.put("instrument", instrument!!.serialize(contextCollector))
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        actor = tryParseLinkOrObject(obj.get("actor"))
        `object` = tryParseLinkOrObject(obj.get("object"))
        target = tryParseLinkOrObject(obj.opt("target"))
        result = tryParseArrayOfLinksOrObjects(obj.opt("result"))
        origin = tryParseLinkOrObject(obj.opt("origin"))
        instrument = tryParseLinkOrObject(obj.opt("instrument"))
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder(type)
        sb.append('{')
        sb.append(super.toString())
        if (actor != null) {
            sb.append("actor=")
            sb.append(actor)
        }
        if (`object` != null) {
            sb.append(", object=")
            sb.append(`object`)
        }
        if (target != null) {
            sb.append(", target=")
            sb.append(target)
        }
        if (result != null) {
            sb.append(", result=")
            sb.append(result)
        }
        if (origin != null) {
            sb.append(", origin=")
            sb.append(origin)
        }
        if (instrument != null) {
            sb.append(", instrument=")
            sb.append(instrument)
        }
        sb.append('}')
        return sb.toString()
    }
}

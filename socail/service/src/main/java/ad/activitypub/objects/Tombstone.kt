package ad.activitypub.objects

import org.json.JSONObject

import java.util.Date

import ad.Utils
import ad.activitypub.ContextCollector

class Tombstone : ActivityPubObject() {

    var formerType: String? = null
    var deleted: Date? = null

    override fun getType(): String {
        return "Tombstone"
    }

    override fun asActivityPubObject(obj: JSONObject, contextCollector: ContextCollector): JSONObject {
        var obj = obj
        obj = super.asActivityPubObject(obj, contextCollector)
        if (formerType != null)
            obj.put("formerType", formerType)
        if (deleted != null)
            obj.put("deleted", Utils.formatDateAsISO(deleted!!))
        return obj
    }

    @Throws(Exception::class)
    override fun parseActivityPubObject(obj: JSONObject): ActivityPubObject {
        super.parseActivityPubObject(obj)
        formerType = obj.optString("formerType", null)
        if (obj.has("deleted"))
            deleted = tryParseDate(obj.getString("deleted"))
        return this
    }
}

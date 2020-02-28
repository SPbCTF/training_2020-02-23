package ad.activitypub.objects

import java.net.URI
import java.util.Objects

import ad.activitypub.ContextCollector

class LinkOrObject {
    val link: URI?
    val `object`: ActivityPubObject?

    constructor(link: URI) {
        this.link = link
        `object` = null
    }

    constructor(`object`: ActivityPubObject) {
        this.`object` = `object`
        link = null
    }

    fun serialize(contextCollector: ContextCollector): Any {
        return link?.toString() ?: `object`!!.asActivityPubObject(null, contextCollector)
    }

    override fun toString(): String {
        return link?.toString() ?: `object`!!.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o is URI)
            return link == o
        if (o is ActivityPubObject)
            return `object` == o
        if (o == null || javaClass != o.javaClass) return false
        val that = o as LinkOrObject?
        return link == that!!.link && `object` == that.`object`
    }

    override fun hashCode(): Int {
        return Objects.hash(link, `object`)
    }
}

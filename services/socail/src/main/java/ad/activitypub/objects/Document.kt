package ad.activitypub.objects

open class Document : ActivityPubObject() {

    public var localID: String? = null

    override fun getType(): String {
        return "Document"
    }
}

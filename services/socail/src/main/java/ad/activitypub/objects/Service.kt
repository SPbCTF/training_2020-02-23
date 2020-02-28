package ad.activitypub.objects

class Service : ActivityPubObject() {
    override fun getType(): String {
        return "Service"
    }
}

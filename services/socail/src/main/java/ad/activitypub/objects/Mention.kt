package ad.activitypub.objects

class Mention : ActivityPubLink() {
    override fun getType(): String {
        return "Mention"
    }
}

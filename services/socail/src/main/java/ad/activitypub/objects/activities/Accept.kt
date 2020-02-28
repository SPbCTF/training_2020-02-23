package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Accept : Activity() {
    override fun getType(): String {
        return "Accept"
    }
}

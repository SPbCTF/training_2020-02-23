package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Announce : Activity() {
    override fun getType(): String {
        return "Announce"
    }
}

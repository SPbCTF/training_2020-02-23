package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Reject : Activity() {
    override fun getType(): String {
        return "Reject"
    }
}

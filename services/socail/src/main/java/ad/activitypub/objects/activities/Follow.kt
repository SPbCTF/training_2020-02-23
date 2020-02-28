package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Follow : Activity() {
    override fun getType(): String {
        return "Follow"
    }
}

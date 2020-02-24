package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Like : Activity() {
    override fun getType(): String {
        return "Like"
    }
}

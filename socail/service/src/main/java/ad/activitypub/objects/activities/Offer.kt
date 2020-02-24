package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Offer : Activity() {
    override fun getType(): String {
        return "Offer"
    }
}

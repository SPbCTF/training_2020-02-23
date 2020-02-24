package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Update : Activity() {
    override fun getType(): String {
        return "Update"
    }
}

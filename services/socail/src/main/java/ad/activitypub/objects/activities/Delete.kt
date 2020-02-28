package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Delete : Activity() {
    override fun getType(): String {
        return "Delete"
    }
}

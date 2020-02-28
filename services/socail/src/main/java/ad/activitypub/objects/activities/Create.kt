package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Create : Activity() {
    override fun getType(): String {
        return "Create"
    }
}

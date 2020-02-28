package ad.activitypub.objects.activities

import ad.activitypub.objects.Activity

class Undo : Activity() {
    override fun getType(): String {
        return "Undo"
    }
}

package ad.data

import java.util.ArrayList
import java.util.Locale
import java.util.TimeZone

import ad.activitypub.objects.ActivityPubObject

class SessionInfo {
    var account: Account? = null
    var preferredLocale: Locale? = null
    var history = PageHistory()
    var csrfToken: String? = null
    var timeZone: TimeZone? = null
    var postDraftAttachments = ArrayList<ActivityPubObject>()

    class PageHistory {
        var entries = ArrayList<String>()

        fun add(path: String) {
            if (last() == path)
            // don't record page refreshes
                return
            entries.add(path)
            while (entries.size > 5)
                entries.removeAt(0)
        }

        fun last(): String {
            return if (entries.isEmpty()) "/" else entries[entries.size - 1]
        }
    }
}

package ad.data

import java.util.ArrayList

class UserNotifications {
    @get:Synchronized
    var newFriendRequestCount = 0
        private set

    private val notifications = ArrayList<Notification>()

    @Synchronized
    fun incNewFriendRequestCount(amount: Int) {
        newFriendRequestCount += amount
    }
}

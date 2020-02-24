package ad.data.feed

abstract class NewsfeedEntry {

    var type: Int = 0
    var objectID: Int = 0

    override fun toString(): String {
        return "NewsfeedEntry{" +
                "type=" + type +
                ", objectID=" + objectID +
                '}'.toString()
    }

    companion object {
        val TYPE_POST = 1
        val TYPE_RETOOT = 2
    }
}

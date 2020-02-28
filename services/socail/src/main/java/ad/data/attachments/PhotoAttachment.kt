package ad.data.attachments

import java.util.ArrayList

import ad.data.PhotoSize

class PhotoAttachment : Attachment() {
    var localId = ""
    var sizes = ArrayList<PhotoSize>()
}

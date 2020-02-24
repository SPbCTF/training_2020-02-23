package ad.storage

import java.io.File
import java.io.IOException

import ad.Config
import ad.activitypub.objects.ActivityPubObject
import ad.activitypub.objects.Document
import ad.activitypub.objects.LocalImage
import ad.data.PhotoSize
// picture resizing was removed to save cpu during ctf
object MediaStorageUtils {
    // picture resizing was removed to save cpu during ctf
//    @Throws(IOException::class)
//    fun writeResizedImages(img: VImage, dimensions: IntArray, sizes: Array<PhotoSize.Type>, webpQuality: Int, keyHex: String, basePath: File, baseURLPath: String, outSizes: MutableList<PhotoSize>): Long {
//        var totalSize: Long = 0
//        for (i in sizes.indices) {
//            val baseName = keyHex + "_" + sizes[i].suffix()
//            val webp = File(basePath, "$baseName.webp")
//            val factor = dimensions[i].toDouble() / Math.max(img.width, img.height).toDouble()
//            var skipBiggerSizes = false
//            val width: Int
//            val height: Int
//            if (factor >= 1.0) {
//                img.writeToFile(arrayOf(webp.absolutePath + "[Q=" + webpQuality + ",strip=true]"))
//                width = img.width
//                height = img.height
//                skipBiggerSizes = true
//            } else {
//                val resized = img.resize(factor)
//                try {
//                    resized.writeToFile(arrayOf(webp.absolutePath + "[Q=" + webpQuality + ",strip=true]"))
//                } catch (x: IOException) {
//                    resized.release()
//                    throw IOException(x)
//                }
//
//                width = resized.width
//                height = resized.height
//                resized.release()
//            }
//            outSizes.add(PhotoSize(Config.localURI("$baseURLPath/$baseName.webp"), width, height, sizes[i], PhotoSize.Format.WEBP))
//            totalSize += webp.length()
//            if (skipBiggerSizes)
//                break
//        }
//        return totalSize
//    }

    fun findBestPhotoSize(sizes: List<PhotoSize>, format: PhotoSize.Format, type: PhotoSize.Type): PhotoSize? {
        if (sizes.size == 0) {
            return null;
        }
        return sizes.first()
        // nice resizing was removed to not waste cpu
//        if (sizes.size == 1) {
//        }
//        for (size in sizes) {
//            if (size.format === format && size.type === type){}
//                return size
//        }
//        val smaller: PhotoSize.Type
//        when (type) {
//            PhotoSize.Type.XLARGE -> smaller = PhotoSize.Type.LARGE
//            PhotoSize.Type.LARGE -> smaller = PhotoSize.Type.LARGE
//            else -> smaller = PhotoSize.Type.LARGE
//        }
//        return findBestPhotoSize(sizes, format, smaller)
    }

    fun deleteAttachmentFiles(attachments: List<ActivityPubObject>) {
        for (o in attachments) {
            if (o is Document)
                deleteAttachmentFiles(o)
        }
    }

    fun deleteAttachmentFiles(doc: Document) {
        if (doc is LocalImage) {
            for (sz in doc.sizes) {
                val file = File(Config.uploadPath, doc.path + "/" + doc.localID + "_" + sz.type.suffix() + "." + sz.format.fileExtension())
                if (file.exists())
                    file.delete()
                else
                    println(file.absolutePath + " does not exist")
            }
        }
    }
}

package ad

import java.io.IOException
import java.io.InputStream
import java.util.Properties

object BuildInfo {

    val VERSION: String

    init {
        val props = Properties()
        try {
            BuildInfo::class.java.getResourceAsStream("/version.properties").use { `in` -> props.load(`in`) }
        } catch (ignore: IOException) {
        }

        if (props.containsKey("build.version")) {
            VERSION = props.getProperty("build.version")
        } else {
            VERSION = "unknown"
        }
    }
}

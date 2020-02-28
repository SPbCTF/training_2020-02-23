package ad.data

import org.json.JSONArray
import org.json.JSONObject

class WebDeltaResponseBuilder {
    private val commands = JSONArray()

    fun setContent(containerID: String, html: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "setContent")
        cmd.put("id", containerID)
        cmd.put("c", html)
        commands.put(cmd)
        return this
    }

    fun remove(vararg ids: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "remove")
        cmd.put("ids", JSONArray(ids))
        commands.put(cmd)
        return this
    }

    fun messageBox(title: String, msg: String, button: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "msgBox")
        cmd.put("m", msg)
        cmd.put("t", title)
        cmd.put("b", button)
        commands.put(cmd)
        return this
    }

    fun show(vararg ids: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "show")
        cmd.put("ids", JSONArray(ids))
        commands.put(cmd)
        return this
    }

    fun hide(vararg ids: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "hide")
        cmd.put("ids", JSONArray(ids))
        commands.put(cmd)
        return this
    }

    fun insertHTML(mode: ElementInsertionMode, id: String, html: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "insert")
        cmd.put("id", id)
        cmd.put("c", html)
        cmd.put("m", arrayOf("bb", "ab", "be", "ae")[mode.ordinal])
        commands.put(cmd)
        return this
    }

    fun setInputValue(id: String, value: String): WebDeltaResponseBuilder {
        val cmd = JSONObject()
        cmd.put("a", "setValue")
        cmd.put("id", id)
        cmd.put("v", value)
        commands.put(cmd)
        return this
    }

    fun json(): JSONArray {
        return commands
    }

    enum class ElementInsertionMode {
        BEFORE_BEGIN,
        AFTER_BEGIN,
        BEFORE_END,
        AFTER_END
    }
}

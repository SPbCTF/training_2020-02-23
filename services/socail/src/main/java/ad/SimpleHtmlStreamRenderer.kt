package ad

import org.owasp.html.HtmlStreamEventReceiver

/**
 * An HtmlStreamEventReceiver that doesn't meddle with text
 */
class SimpleHtmlStreamRenderer(private val sb: StringBuilder) : HtmlStreamEventReceiver {

    override fun openDocument() {

    }

    override fun closeDocument() {

    }

    override fun openTag(name: String, attrs: List<String>) {
        sb.append('<')
        sb.append(name)
        if (!attrs.isEmpty()) {
            var i = 0
            while (i < attrs.size) {
                val attr = attrs[i]
                val value = attrs[i + 1]
                sb.append(' ')
                sb.append(attr)
                if (!value.isEmpty()) {
                    sb.append("=\"")
                    sb.append(value.replace("\"", "&quot;"))
                    sb.append('"')
                }
                i += 2
            }
        }
        sb.append('>')
    }

    override fun closeTag(tag: String) {
        sb.append("</")
        sb.append(tag)
        sb.append('>')
    }

    override fun text(s: String) {
        sb.append(s.replace(">", "&gt;").replace("<", "&lt;"))
    }
}

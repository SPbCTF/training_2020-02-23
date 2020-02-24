package ad.routes

import ad.data.*
import ad.storage.User
import org.json.JSONArray
import org.json.JSONObject
import org.jtwig.JtwigModel

import java.net.URLEncoder
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.regex.Pattern

import ad.Config

import ad.Utils
import ad.Utils.isAjax
//import ad.activitypub.ActivityPub;
//import ad.activitypub.ActivityPubWorker;
import ad.activitypub.ContextCollector
import ad.activitypub.objects.ActivityPubObject
import ad.activitypub.objects.Document
import ad.activitypub.objects.LocalImage
import ad.data.attachments.PhotoAttachment
import ad.data.feed.PostNewsfeedEntry
import ad.storage.MediaStorageUtils
import ad.storage.PostStorage
import ad.storage.User.UserStorage
import spark.Request
import spark.Response
import spark.utils.StringUtils

object PostRoutes {
    private fun serializeAttachment(att: ActivityPubObject): JSONObject {
        val o = att.asActivityPubObject(JSONObject(), ContextCollector())
        if (att is Document) {
            if (StringUtils.isNotEmpty(att.localID)) {
                o.put("_lid", att.localID)
                if (att is LocalImage) {
                    val sizes = JSONArray()
                    sizes.put(0)
                    val sizeTypes = ArrayList<String>()
                    for (size in att.sizes) {
                        if (size.format != PhotoSize.Format.WEBP)
                            continue
                        sizeTypes.add(size.type.suffix())
                        sizes.put(size.width)
                        sizes.put(size.height)
                    }
                    sizes.put(0, sizeTypes.joinToString(" "))
                    o.put("_sz", sizes)
                    o.put("type", "_LocalImage")
                }
                o.remove("url")
                o.remove("id")
            }
        }
        return o
    }

    
    fun createWallPost(req: Request, resp: Response, self: Account): Any {
        val username = req.params(":username")

        val user = UserStorage.getByUsername(username)

        if (user != null) {
            var text = Utils.sanitizeHTML(req.queryParams("text")).replace("\r", "").trim { it <= ' ' }
            if (text.length == 0)
                return "Empty post"
            if (!text.startsWith("<p>")) {
                val paragraphs = text.replace("\n{3,}".toRegex(), "\n\n").split("\n\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val sb = StringBuilder()
                for (paragraph in paragraphs) {
                    val p = paragraph.trim { it <= ' ' }.replace("\n", "<br/>")
                    if (p.isEmpty())
                        continue
                    sb.append("<p>")
                    sb.append(p)
                    sb.append("</p>")
                }
                text = sb.toString()
            }
            val userID = self.user.id
            val replyTo = Utils.parseIntOrDefault(req.queryParams("replyTo"), 0)
            val postID: Int

            val sb = StringBuffer()
            val mentionedUsers = ArrayList<User>()
            val mentionRegex = Pattern.compile("@([a-zA-Z0-9._-]+)(?:@([a-zA-Z0-9._-]+[a-zA-Z0-9-]+))?")
            val matcher = mentionRegex.matcher(text)
            while (matcher.find()) {
                val u = matcher.group(1)
                val d = matcher.group(2)
                val mentionedUser: User?
                if (d == null) {
                    mentionedUser = UserStorage.getByUsername(u)
                } else {
                    mentionedUser = UserStorage.getByUsername("$u@$d")
                }
                if (mentionedUser != null) {
                    matcher.appendReplacement(sb, "<span class=\"h-card\"><a href=\"" + mentionedUser.url + "\" class=\"u-url mention\">$0</a></span>")
                    mentionedUsers.add(mentionedUser)
                } else {
                    println("ignoring mention " + matcher.group())
                    matcher.appendReplacement(sb, "$0")
                }
            }
            if (!mentionedUsers.isEmpty()) {
                matcher.appendTail(sb)
                text = sb.toString()
            }
            val isprivate = "on" == req.queryParams("private")
            var attachments: String? = null
            val sess = Utils.sessionInfo(req)
            if (!sess!!.postDraftAttachments.isEmpty()) {
                if (sess.postDraftAttachments.size == 1) {
                    attachments = serializeAttachment(sess.postDraftAttachments[0]).toString()
                } else {
                    val ar = JSONArray()
                    for (o in sess.postDraftAttachments) {
                        ar.put(serializeAttachment(o))
                    }
                    attachments = ar.toString()
                }
            }

            if (replyTo != 0) {
                val parent = PostStorage.getPostByID(replyTo)
                if (parent == null) {
                    resp.status(404)
                    return Utils.wrapError(req, resp, "err_post_not_found")
                }
                val replyKey = IntArray(parent.replyKey.size + 1)
                System.arraycopy(parent.replyKey, 0, replyKey, 0, parent.replyKey.size)
                replyKey[replyKey.size - 1] = parent.id
                // comment replies start with mentions, but only if it's a reply to a comment, not a top-level post
                if (parent.replyKey.size > 0 && text.startsWith("<p>" + parent.user.firstName + ", ")) {
                    text = "<p><span class=\"h-card\"><a href=\"" + parent.user.url + "\" class=\"u-url mention\">" + parent.user.firstName + "</a></span>" + text.substring(parent.user.firstName.length + 3)
                }
                mentionedUsers.add(parent.user)
                if (parent.replyKey.size > 1) {
                    val topLevel = PostStorage.getPostByID(parent.replyKey[0])
                    if (topLevel != null)
                        mentionedUsers.add(topLevel.user)
                }
                postID = PostStorage.createUserWallPost(userID, user.id, text, replyKey, mentionedUsers, attachments, isprivate)
            } else {
                postID = PostStorage.createUserWallPost(userID, user.id, text, null, mentionedUsers, attachments, isprivate)
            }

            val post = PostStorage.getPostByID(postID)
            //			ActivityPubWorker.getInstance().sendCreatePostActivity(post);

            sess.postDraftAttachments.clear()
            if (isAjax(req)) {
                val postHTML = Utils.renderTemplate(req, if (replyTo != 0) "wall_reply" else "wall_post", JtwigModel.newModel().with("post", post))
                resp.type("application/json")
                resp.header("X-New-post", "" + post!!.id)
                val rb: WebDeltaResponseBuilder
                if (replyTo == 0)
                    rb = WebDeltaResponseBuilder().insertHTML(WebDeltaResponseBuilder.ElementInsertionMode.AFTER_BEGIN, "postList", postHTML)
                else
                    rb = WebDeltaResponseBuilder().insertHTML(WebDeltaResponseBuilder.ElementInsertionMode.BEFORE_END, "postReplies$replyTo", postHTML)
                return rb.setInputValue("postFormText", "")
                        .setContent("postFormAttachments", "")
                        .json()
            }
            resp.redirect(Utils.back(req))
        } else {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_user_not_found")
        }
        return ""
    }

    
    fun feed(req: Request, resp: Response, self: Account): Any {
        val userID = self.user.id
        val feed = PostStorage.getFeed(userID)
        for (e in feed) {
            if (e is PostNewsfeedEntry) {
                val post = e.post
                if (post != null)
                    post.replies = PostStorage.getRepliesForFeed(e.objectID)
                else
                    System.err.println("No post: $e")
            }
        }
        Utils.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm")
        val model = JtwigModel.newModel().with("title", Utils.lang(req).get("feed")).with("feed", feed).with("draftAttachments", Utils.sessionInfo(req)!!.postDraftAttachments)
        return Utils.renderTemplate(req, "feed", model)
    }

    
    fun standalonePost(req: Request, resp: Response, self: Account): Any {
        val postID = Utils.parseIntOrDefault(req.params(":postID"), 0)
        if (postID == 0) {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_post_not_found")
        }
        val post = PostStorage.getPostByID(postID)
        if (post == null) {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_post_not_found")
        }
        if (post.isPrivate) {
            if (self.id != post.owner.id) {

                val status = UserStorage.getFriendshipStatus(self.user.id, post.owner.id)
                if (status != FriendshipStatus.FRIENDS) {
                    resp.status(404)
                    return Utils.wrapError(req, resp, "err_post_not_found")
                }
            }
        }

        val replyKey = IntArray(post.replyKey.size + 1)
        System.arraycopy(post.replyKey, 0, replyKey, 0, post.replyKey.size)
        replyKey[replyKey.size - 1] = post.id
        post.replies = PostStorage.getReplies(replyKey)
        val model = JtwigModel.newModel()
        model.with("post", post)
        val info = Utils.sessionInfo(req)
        if (info != null && info.account != null)
            model.with("draftAttachments", info.postDraftAttachments)
        if (post.replyKey.size > 0) {
            model.with("prefilledPostText", post.user.firstName + ", ")
        }
        if (info == null || info.account == null) {
            val meta = LinkedHashMap<String, String>()
            meta["og:site_name"] = "ad"
            meta["og:type"] = "article"
            meta["og:title"] = post.user.fullName
            meta["og:url"] = post.url.toString()
            meta["og:published_time"] = Utils.formatDateAsISO(post.published)
            meta["og:author"] = post.user.url.toString()
            if (StringUtils.isNotEmpty(post.content)) {
                meta["og:description"] = Utils.truncateOnWordBoundary(post.content, 250)
            }
            var hasImage = false
            if (!post.attachment.isEmpty()) {
                for (att in post.processedAttachments) {
                    if (att is PhotoAttachment) {
//                        val size = MediaStorageUtils.findBestPhotoSize(att.sizes, PhotoSize.Format.WEBP, PhotoSize.Type.LARGE)
//                        if (size != null) {
//                            meta["og:image"] = size.src.toString()
//                            meta["og:image:width"] = size.width.toString() + ""
//                            meta["og:image:height"] = size.height.toString() + ""
//                            hasImage = true
//                        }
                        break
                    }
                }
            }
            if (!hasImage) {
                if (post.user.hasAvatar()) {
//                    val size = MediaStorageUtils.findBestPhotoSize(post.user.avatar!!, PhotoSize.Format.WEBP, PhotoSize.Type.LARGE)
//                    if (size != null) {
//                        meta["og:image"] = size.src.toString()
//                        meta["og:image:width"] = size.width.toString() + ""
//                        meta["og:image:height"] = size.height.toString() + ""
//                    }
                }
            }
            model.with("metaTags", meta)
        }
        Utils.jsLangKey(req, "yes", "no", "delete_post", "delete_post_confirm", "delete_reply", "delete_reply_confirm")
        return Utils.renderTemplate(req, "wall_post_standalone", model)
    }

    
    fun confirmDelete(req: Request, resp: Response, self: Account): Any {
        req.attribute("noHistory", true)
        val postID = Utils.parseIntOrDefault(req.params(":postID"), 0)
        if (postID == 0) {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_post_not_found")
        }
        val back = Utils.back(req)
        return Utils.renderTemplate(req, "generic_confirm", JtwigModel.newModel().with("message", Utils.lang(req).get("delete_post_confirm")).with("formAction", Config.localURI("/posts/" + postID + "/delete?_redir=" + URLEncoder.encode(back))).with("back", back))
    }

    
    fun delete(req: Request, resp: Response, self: Account): Any {
        val postID = Utils.parseIntOrDefault(req.params(":postID"), 0)
        if (postID == 0) {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_post_not_found")
        }
        val post = PostStorage.getPostByID(postID)
        if (post == null) {
            resp.status(404)
            return Utils.wrapError(req, resp, "err_post_not_found")
        }
        if (!post.canBeManagedBy(self.user)) {
            resp.status(403)
            return Utils.wrapError(req, resp, "err_access")
        }
        PostStorage.deletePost(post.id)
        if (Config.isLocal(post.activityPubID) && post.attachment != null && !post.attachment.isEmpty()) {
            MediaStorageUtils.deleteAttachmentFiles(post.attachment)
        }
        //		ActivityPubWorker.getInstance().sendDeletePostActivity(post);
        if (isAjax(req)) {
            resp.type("application/json")
            return WebDeltaResponseBuilder().remove("post$postID").json()
        }
        resp.redirect(Utils.back(req))
        return ""
    }
}

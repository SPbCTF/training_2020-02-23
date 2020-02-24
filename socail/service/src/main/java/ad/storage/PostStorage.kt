package ad.storage

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.URI
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp
import java.util.ArrayList
import java.util.HashMap

import ad.Config
import ad.ObjectNotFoundException
import ad.Utils
import ad.data.feed.NewsfeedEntry
import ad.data.Post
import ad.data.feed.PostNewsfeedEntry
import ad.data.feed.RetootNewsfeedEntry

object PostStorage {

    val AND_PRIVATE_FALSE = " AND \"private\"=false "


    fun createUserWallPost(userID: Int, ownerID: Int, text: String, replyKey: IntArray?, mentionedUsers: List<User>, attachments: String?, isPrivate: Boolean): Int {
        val conn = DatabaseConnectionManager.getConnection2()

        var stmt = conn.prepareStatement("INSERT INTO \"wall_posts\" (\"author_id\", \"owner_user_id\", \"text\", \"reply_key\", \"mentions\", \"attachments\", \"private\") VALUES (?, ?, ?, ?, ?, ?, $isPrivate)", Statement.RETURN_GENERATED_KEYS)
        stmt.use {


            stmt.setInt(1, userID)
            stmt.setInt(2, ownerID)
            stmt.setString(3, text)
            var _replyKey: ByteArray? = null
            if (replyKey != null) {
                val b = ByteArrayOutputStream(replyKey.size * 4)
                try {
                    val o = DataOutputStream(b)
                    for (id in replyKey)
                        o.writeInt(id)
                } catch (ignore: IOException) {
                }

                _replyKey = b.toByteArray()
            }
            stmt.setBytes(4, _replyKey)
            var mentions: ByteArray? = null
            if (!mentionedUsers.isEmpty()) {
                val b = ByteArrayOutputStream(mentionedUsers.size * 4)
                try {
                    val o = DataOutputStream(b)
                    for (user in mentionedUsers)
                        o.writeInt(user.id)
                } catch (ignore: IOException) {
                }

                mentions = b.toByteArray()
            }
            stmt.setBytes(5, mentions)
            stmt.setString(6, attachments)
            stmt.execute()
            stmt.generatedKeys.use { keys ->
                keys.next()
                val id = keys.getInt(1)
                if (userID == ownerID && replyKey == null) {
                    stmt = conn.prepareStatement("INSERT INTO \"newsfeed\" (\"type\", \"author_id\", \"object_id\") VALUES (?, ?, ?)")
                    stmt.setInt(1, NewsfeedEntry.TYPE_POST)
                    stmt.setInt(2, userID)
                    stmt.setInt(3, id)
                    stmt.execute()
                }
                return id
            }
        }
    }


    fun getFeed(userID: Int): List<NewsfeedEntry> {
        val conn = DatabaseConnectionManager.getConnection2()
        val stmt = conn.prepareStatement("SELECT \"type\", \"object_id\", \"author_id\" FROM \"newsfeed\" WHERE \"author_id\" IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) ORDER BY \"time\" DESC LIMIT 25")
        val posts = ArrayList<NewsfeedEntry>()
        val needPosts = ArrayList<Int>()
        val postMap = HashMap<Int, Post>()
        stmt.use {

            stmt.setInt(1, userID)
            stmt.setInt(2, userID)

            stmt.executeQuery().use { res ->
                if (res.next()) {
                    do {
                        val type = res.getInt(1)
                        var _entry: NewsfeedEntry? = null
                        when (type) {
                            NewsfeedEntry.TYPE_POST -> {
                                val entry = PostNewsfeedEntry()
                                entry.objectID = res.getInt(2)
                                posts.add(entry)
                                needPosts.add(entry.objectID)
                                _entry = entry
                            }
                            NewsfeedEntry.TYPE_RETOOT -> {
                                val entry = RetootNewsfeedEntry()
                                entry.objectID = res.getInt(2)
                                entry.author = User.UserStorage.getById(res.getInt(3))
                                posts.add(entry)
                                needPosts.add(entry.objectID)
                                _entry = entry
                            }
                        }
                        if (_entry != null)
                            _entry.type = type
                    } while (res.next())
                }
            }
        }
        if (!needPosts.isEmpty()) {
            val sb = StringBuilder()
            sb.append("SELECT * FROM \"wall_posts\" WHERE \"id\" IN (")
            var first = true
            for (id in needPosts) {
                if (!first) {
                    sb.append(',')
                } else {
                    first = false
                }
                sb.append(id)
            }
            sb.append(')')
            conn.createStatement().use { st ->
                st.executeQuery(sb.toString()).use { res ->
                    if (res.next()) {
                        do {
                            val post = Post.fromResultSet(res)
                            postMap[post.id] = post
                        } while (res.next())
                    }
                }
            }
            for (e in posts) {
                if (e is PostNewsfeedEntry) {
                    val post = postMap[e.objectID]
                    if (post != null)
                        e.post = post
                }
            }
        }

        return posts
    }


    fun getUserWall(userID: Int, minID: Int, maxID: Int, total: IntArray?, privateAllowed: Boolean): List<Post> {
        val conn = DatabaseConnectionManager.getConnection2()

        if (total != null) {
            var query = "SELECT COUNT(*) FROM \"wall_posts\" WHERE \"owner_user_id\"=? AND \"reply_key\" IS NULL"
            if (!privateAllowed) {
                query += AND_PRIVATE_FALSE
            }
            var stmt: PreparedStatement
            stmt = conn.prepareStatement(query)
            stmt.use {
                stmt.setInt(1, userID)
                stmt.executeQuery().use { res ->
                    res.next()
                    total[0] = res.getInt(1)
                }
            }
        }
        var stmt: PreparedStatement
        if (minID > 0) {
            stmt = conn.prepareStatement("SELECT * FROM \"wall_posts\" WHERE \"owner_user_id\"=? AND \"id\">? AND \"reply_key\" IS NULL ORDER BY created_at DESC ")
            stmt.setInt(2, minID)
        } else if (maxID > 0) {
            stmt = conn.prepareStatement("SELECT * FROM \"wall_posts\" WHERE \"owner_user_id\"=? AND \"id\"<? AND \"reply_key\" IS NULL ORDER BY created_at DESC ")
            stmt.setInt(2, maxID)
        } else {
            var filter = ""
            if (!privateAllowed) {
                filter = AND_PRIVATE_FALSE
            }
            stmt = conn.prepareStatement("SELECT * FROM \"wall_posts\" WHERE \"owner_user_id\"=? AND \"reply_key\" IS NULL $filter  ORDER BY created_at DESC ")
        }
        val posts = ArrayList<Post>()
        stmt.use {
            stmt.setInt(1, userID)
            stmt.executeQuery().use { res ->
                if (res.next()) {
                    do {
                        posts.add(Post.fromResultSet(res))
                    } while (res.next())
                }
            }
        }
        return posts
    }


    fun getPostByID(postID: Int): Post? {
        val stmt = DatabaseConnectionManager.getConnection2().prepareStatement("SELECT * FROM wall_posts WHERE id=?")
        stmt.use {
            stmt.setInt(1, postID)
            stmt.executeQuery().use { res ->
                if (res.next()) {
                    return Post.fromResultSet(res)
                }
            }
            return null
        }
    }


    fun deletePost(id: Int) {
        val conn = DatabaseConnectionManager.getConnection2()
        var stmt = conn.prepareStatement("DELETE FROM \"wall_posts\" WHERE \"id\"=?")
        stmt.use {
            stmt.setInt(1, id)
            stmt.execute()
        }
        stmt = conn.prepareStatement("DELETE FROM \"newsfeed\" WHERE (\"type\"=1 OR \"type\"=2) AND \"object_id\"=?")
        stmt.use {
            stmt.setInt(1, id)
            stmt.execute()
        }
    }


    fun getRepliesForFeed(postID: Int): List<Post> {
        val conn = DatabaseConnectionManager.getConnection2()
        val stmt = conn.prepareStatement("SELECT * FROM \"wall_posts\" WHERE \"reply_key\"=? ORDER BY \"id\" ASC LIMIT 3")
        val posts = ArrayList<Post>()
        stmt.use {

            stmt.setBytes(1, byteArrayOf((postID shr 24 and 0xFF).toByte(), (postID shr 16 and 0xFF).toByte(), (postID shr 8 and 0xFF).toByte(), (postID and 0xFF).toByte()))
            stmt.executeQuery().use { res ->
                if (res.next()) {
                    do {
                        posts.add(Post.fromResultSet(res))
                    } while (res.next())
                }
            }
        }
        return posts
    }


    fun getReplies(prefix: IntArray): List<Post> {
        val conn = DatabaseConnectionManager.getConnection2()
        val stmt = conn.prepareStatement("SELECT * FROM \"wall_posts\" WHERE encode(\"reply_key\", 'hex') LIKE ?  ORDER BY \"reply_key\" ASC, \"id\" ASC LIMIT 100")
        val posts = ArrayList<Post>()
        val postMap = HashMap<Int, Post>()
        stmt.use {
            val b = ByteArrayOutputStream(prefix.size * 4)
            try {
                val o = DataOutputStream(b)
                for (id in prefix)
                    o.writeInt(id)
            } catch (ignore: IOException) {
            }

            val replyKey = Utils.byteArrayToHexString(b.toByteArray()) + "%"

            stmt.setString(1, replyKey)

            stmt.executeQuery().use { res ->
                if (res.next()) {
                    do {
                        val post = Post.fromResultSet(res)
                        postMap[post.id] = post
                        posts.add(post)
                    } while (res.next())
                }
            }
        }
        for (post in posts) {
            if (post.replyLevel > prefix.size) {
                val parent = postMap[post.replyKey[post.replyKey.size - 1]]
                parent?.replies?.add(post)
            }
        }
        val itr = posts.iterator()
        while (itr.hasNext()) {
            val post = itr.next()
            if (post.replyLevel > prefix.size)
                itr.remove()
        }

        return posts
    }


    fun getActivityPubID(postID: Int): URI? {
        val conn = DatabaseConnectionManager.getConnection2()
        val stmt = conn.prepareStatement("SELECT \"ap_id\",\"owner_user_id\" FROM \"wall_posts\" WHERE \"id\"=?")
        stmt.use {

            stmt.setInt(1, postID)
            stmt.executeQuery().use { res ->
                if (res.next()) {
                    return if (res.getString(1) != null) URI.create(res.getString(1)) else Config.localURI("/posts/$postID")
                }
            }
            return null
        }
    }

}

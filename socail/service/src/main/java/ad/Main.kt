package ad

//import ad.data.ForeignUser;
//import ad.routes.ActivityPubRoutes;

import ad.data.SessionInfo
import ad.jtwigext.*
import ad.routes.*
import ad.sparkext.CSRFRoute
import ad.sparkext.LoggedInRoute
import ad.sparkext.SparkExtension.*
import ad.storage.SessionStorage
import ad.storage.User.UserStorage
import org.eclipse.jetty.server.session.*
import org.jtwig.JtwigModel
import org.jtwig.environment.EnvironmentConfiguration
import org.jtwig.environment.EnvironmentConfigurationBuilder
import spark.*
import spark.Spark.*
import spark.embeddedserver.EmbeddedServerFactory
import spark.embeddedserver.EmbeddedServers
import spark.embeddedserver.jetty.EmbeddedJettyServer
import spark.embeddedserver.jetty.JettyHandler
import spark.http.matching.MatcherFilter
import spark.route.Routes
import spark.staticfiles.StaticFilesConfiguration
import spark.utils.StringUtils
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter


object Main {

    val jtwigEnv: EnvironmentConfiguration

    init {
        jtwigEnv = EnvironmentConfigurationBuilder.configuration()
                .functions()
                .add(LangFunction())
                .add(LangPluralFunction())
                .add(LangDateFunction())
                .add(PictureForAvatarFunction())
                .add(RenderAttachmentsFunction())
                .add(PhotoSizeFunction())
                .and()
                .build()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Config.load(args[0])
        } catch (x: IOException) {
            throw RuntimeException(x)
        }
        createCustomJettyServer()
        ipAddress(Config.serverIP)
        port(Config.serverPort)
        if (Config.staticFilesPath != null)
            externalStaticFileLocation(Config.staticFilesPath)
        else
            staticFileLocation("/public")
        staticFiles.expireTime((24 * 60 * 60).toLong())
        before(Filter { request, response ->
            request.attribute("start_time", System.currentTimeMillis())
            if (request.session(false) == null || request.session().attribute<SessionInfo>("info") == null) {
                val psid = request.cookie("psid")
                if (psid != null) {
                    if (!SessionStorage.fillSession(psid, request.session(true), request)) {
                        response.removeCookie("/", "psid")
                    } else {
                        response.cookie("/", "psid", psid, 10 * 365 * 24 * 60 * 60, false)
                    }
                }
            }

        })

        get("/", Route { req, resp -> indexPage(req, resp) })

        getLoggedIn("/feed", LoggedInRoute { req, resp, self -> PostRoutes.feed(req, resp, self) })

        path("/account") {
            post("/login", Route { req, resp -> SessionRoutes.login(req, resp) })
            get("/login", Route { req, resp -> SessionRoutes.login(req, resp) })
            get("/logout", Route { req, resp -> SessionRoutes.logout(req, resp) })
            post("/register", Route { req, resp -> SessionRoutes.register(req, resp) })
        }

        path("/settings") {
            getLoggedIn("/", LoggedInRoute { req, resp, self -> SettingsRoutes.settings(req, resp, self) })
            postWithCSRF("/createInvite", CSRFRoute { req, resp, self -> SettingsRoutes.createInvite(req, resp, self) })
            postWithCSRF("/updatePassword", CSRFRoute { req, resp, self -> SettingsRoutes.updatePassword(req, resp, self) })
            postWithCSRF("/updateName", CSRFRoute { req, resp, self -> SettingsRoutes.updateUser(req, resp, self) })
            postLoggedIn("/updateProfilePicture", LoggedInRoute { req, resp, self -> SettingsRoutes.updateProfilePicture(req, resp, self) })
            postLoggedIn("/setLanguage", LoggedInRoute { req, resp, self -> SettingsRoutes.setLanguage(req, resp, self) })
            post("/setTimezone", Route { req, resp -> SettingsRoutes.setTimezone(req, resp) })
        }

        //		path("/activitypub", ()->{
        //			post("/sharedInbox", ActivityPubRoutes::sharedInbox);
        //			getLoggedIn("/externalInteraction", ActivityPubRoutes::externalInteraction);
        //			get("/nodeinfo/2.0", ActivityPubRoutes::nodeInfo);
        //		});

        //		path("/.well-known", ()->{
        //			get("/webfinger", WellKnownRoutes::webfinger);
        //			get("/nodeinfo", WellKnownRoutes::nodeInfo);
        //		});

        path("/system") {
            //			get("/downloadExternalMedia", SystemRoutes::downloadExternalMedia);
            getLoggedIn("/deleteDraftAttachment", LoggedInRoute { req, resp, self -> SystemRoutes.deleteDraftAttachment(req, resp, self) })
            path("/upload") { postLoggedIn("/postPhoto", LoggedInRoute { req, resp, self -> SystemRoutes.uploadPostPhoto(req, resp, self) }) }
        }

        path("/users/:id") {
            //			get("", "application/activity+json", ActivityPubRoutes::userActor);
            //			get("", "application/ld+json", ActivityPubRoutes::userActor);
            get("") { req, resp ->
                val id = Utils.parseIntOrDefault(req.params(":id"), 0)
                val user = UserStorage.getById(id)
                if (user == null || false) {
                    resp.status(404)
                } else {
                    resp.redirect("/" + user.username)
                }
                ""
            }

            //			post("/inbox", ActivityPubRoutes::inbox);
            //			get("/outbox", ActivityPubRoutes::outbox);
            post("/outbox") { req, resp ->
                resp.status(405)
                ""
            }
            //			get("/followers", ActivityPubRoutes::userFollowers);
            //			get("/following", ActivityPubRoutes::userFollowing);
        }

        path("/posts/:postID") {
            //			get("", "application/activity+json", ActivityPubRoutes::post);
            //			get("", "application/ld+json", ActivityPubRoutes::post);
            getLoggedIn("", LoggedInRoute { req, resp, self -> PostRoutes.standalonePost(req, resp, self) })

            getLoggedIn("/confirmDelete", LoggedInRoute { req, resp, self -> PostRoutes.confirmDelete(req, resp, self) })
            postWithCSRF("/delete", CSRFRoute { req, resp, self -> PostRoutes.delete(req, resp, self) })
        }

        path("/:username") {
            //			get("", "application/activity+json", ActivityPubRoutes::userActor);
            //			get("", "application/ld+json", ActivityPubRoutes::userActor);
            get("", Route { req, resp -> ProfileRoutes.profile(req, resp) })
            postWithCSRF("/createWallPost", CSRFRoute { req, resp, self -> PostRoutes.createWallPost(req, resp, self) })

            //			postWithCSRF("/remoteFollow", ActivityPubRoutes::remoteFollow);

            getLoggedIn("/confirmSendFriendRequest", LoggedInRoute { req, resp, self -> ProfileRoutes.confirmSendFriendRequest(req, resp, self) })
            postWithCSRF("/doSendFriendRequest", CSRFRoute { req, resp, self -> ProfileRoutes.doSendFriendRequest(req, resp, self) })
            postWithCSRF("/respondToFriendRequest", CSRFRoute { req, resp, self -> ProfileRoutes.respondToFriendRequest(req, resp, self) })
            postWithCSRF("/doRemoveFriend", CSRFRoute { req, resp, self -> ProfileRoutes.doRemoveFriend(req, resp, self) })
            getLoggedIn("/confirmRemoveFriend", LoggedInRoute { req, resp, self -> ProfileRoutes.confirmRemoveFriend(req, resp, self) })
            get("/friends", Route { req, resp -> ProfileRoutes.friends(req, resp) })
            getLoggedIn("/incomingFriendRequests", LoggedInRoute { req, resp, self -> ProfileRoutes.incomingFriendRequests(req, resp, self) })
            get("/followers", Route { req, resp -> ProfileRoutes.followers(req, resp) })
            get("/following", Route { req, resp -> ProfileRoutes.following(req, resp) })
        }


        exception<Exception>(Exception::class.java) { exception, req, res ->
            res.status(500)
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            res.body("<h1 style='color: red;'>Unhandled exception</h1><pre>" + sw.toString().replace("<", "&gt;") + "</pre>")
        }

        after(Filter { req, resp ->
            val l = req.attribute<Long>("start_time")
            if (l != null) {
                val t = l as Long
                val t1 = System.currentTimeMillis() - t
//                println(t1)
                resp.header("X-Generated-In", t1.toString() + "")
            }

            if (req.headers("accept") == null || !req.headers("accept").startsWith("application/")) {
                if (req.session().attribute<SessionInfo>("info") == null)
                    req.session().attribute("info", SessionInfo())
                if (req.requestMethod().equals("get", ignoreCase = true) && req.attribute<Boolean>("noHistory") == null) {
                    val info = req.session().attribute<SessionInfo>("info")
                    var path = req.pathInfo()
                    val query = req.raw().getQueryString()
                    if (StringUtils.isNotEmpty(query)) {
                        path += '?' + query
                    }
                    info.history.add(path)
                }
            }
        })
    }


    // these tricks are to enable  SessionCache eviction
    private fun createCustomJettyServer() {
        val serverFactory = MyJettyServer()
        EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, EmbeddedServerFactory { routeMatcher: Routes,
                                                                                       staticFilesConfiguration: StaticFilesConfiguration,
                                                                                       exceptionMapper: ExceptionMapper,
                                                                                       hasMultipleHandler: Boolean ->
            val matcherFilter = MatcherFilter(routeMatcher, staticFilesConfiguration, exceptionMapper, false, hasMultipleHandler)
            matcherFilter.init(null)

            val handler = JettyHandler(matcherFilter)
            handler.sessionCookieConfig.isHttpOnly = true
            handler.sessionCache = DefaultSessionCache(handler)
            handler.sessionCache.sessionDataStore = NullSessionDataStore()
            handler.sessionCache.evictionPolicy = 8 * 60
            EmbeddedJettyServer(serverFactory, handler).withThreadPool(null)
        })
    }

    private fun indexPage(req: Request, resp: Response): Any {
        val info = req.session().attribute<SessionInfo>("info")
        if (info != null && info.account != null) {
            resp.redirect("/feed")
            return ""
        }
        val model = JtwigModel.newModel().with("title", "AD")
        return Utils.renderTemplate(req, "index", model)
    }
}

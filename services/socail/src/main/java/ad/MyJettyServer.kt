/*
 * Copyright 2015 - Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ad

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.DefaultSessionIdManager
import org.eclipse.jetty.server.session.HouseKeeper
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.thread.ThreadPool
import spark.embeddedserver.jetty.JettyServerFactory

/**
 * Creates Jetty Server instances.
 * copypaste from JettyServer
 * these tricks are to enable  SessionCache eviction
 */
class MyJettyServer : JettyServerFactory {
    /**
     * Creates a Jetty server.
     *
     * @param maxThreads          maxThreads
     * @param minThreads          minThreads
     * @param threadTimeoutMillis threadTimeoutMillis
     * @return a new jetty server instance
     */
    override fun create(maxThreads: Int, minThreads: Int, threadTimeoutMillis: Int): Server {
        val server: Server
        server = if (maxThreads > 0) {
            val min = if (minThreads > 0) minThreads else 8
            val idleTimeout = if (threadTimeoutMillis > 0) threadTimeoutMillis else 60000
            Server(QueuedThreadPool(maxThreads, min, idleTimeout))
        } else {
            Server()
        }
        craeteSessionIdManager(server)
        return server
    }

    // copypaste from org.eclipse.jetty.server.session.SessionHandler.doStart
    fun craeteSessionIdManager(server: Server) {
        val hk = HouseKeeper()
        hk.intervalSec = 4 * 60
        val sessionIdManager = DefaultSessionIdManager(server)
        sessionIdManager.sessionHouseKeeper = hk
        server.sessionIdManager = sessionIdManager
        server.manage(sessionIdManager)
        sessionIdManager.start()
    }

    /**
     * Creates a Jetty server with supplied thread pool
     * @param threadPool thread pool
     * @return a new jetty server instance
     */
    override fun create(threadPool: ThreadPool): Server {
        return threadPool?.let { Server(it) } ?: Server()
    }
}
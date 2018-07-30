/*  
Copyright [2013-2015] eBay Software Foundation
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package io.parallec.core.actor;

import akka.dispatch.sysmsg.Terminate;
import io.parallec.core.util.PcConstants;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


/**
 * The static akka actor system
 */
/** AKKA最老的父节点,所有的actor都是它的子节点*/
public final class ActorConfig {

    /** The conf. */
    private static Config conf = null;

    /** The actor system. */
    private static ActorSystem actorSystem = null;

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(ActorConfig.class);
    static {

        // load default
        conf = ConfigFactory.load("actorconfig.conf");
        logger.debug("Load Actor config {}", conf.toString());
        /** 生成系统节点*/
        actorSystem = ActorSystem.create(PcConstants.ACTOR_SYSTEM, conf);

    }

    /**
     * Create and get actor system.
     *
     * @return the actor system
     */
    public static ActorSystem createAndGetActorSystem() {
        if (actorSystem == null || actorSystem.terminate().isCompleted()) {
            actorSystem = ActorSystem.create(PcConstants.ACTOR_SYSTEM, conf);
        }
        return actorSystem;
    }


    /** The Constant timeOutDuration. */
    // wait for 10 seconds
    public static final FiniteDuration timeOutDuration = Duration.create(10,
            TimeUnit.SECONDS);

    /**
     * Shut down actor system force.
     */
    public static void shutDownActorSystemForce() {
        if (!actorSystem.terminate().isCompleted()) {
            logger.info("shutting down actor system...");
            actorSystem.terminate();
            try {
                actorSystem.terminate().wait(timeOutDuration.toMillis());
            }catch (InterruptedException e){
                logger.error(e.getMessage(),e);
            }
//            actorSystem.shutdown();
//            actorSystem.awaitTermination(timeOutDuration);
            logger.info("Actor system has been shut down.");
        } else {
            logger.info("Actor system has been terminated already. NO OP.");
        }

    }




    /**
     * Gets the timeoutduration.
     *
     * @return the timeoutduration
     */
    public static FiniteDuration getTimeoutduration() {
        return timeOutDuration;
    }

}

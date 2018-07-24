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
package io.parallec.core.util;

import java.util.concurrent.ThreadFactory;


/**
 * A factory for creating DaemonThread objects.
 */

/**
 * 一个创建守护线程的工厂对象
 */
public class DaemonThreadFactory implements ThreadFactory {

    /** The instance. */
    private static DaemonThreadFactory instance = new DaemonThreadFactory();

    /**
     * Gets the single instance of DaemonThreadFactory.
     *
     * @return single instance of DaemonThreadFactory
     */
    public static DaemonThreadFactory getInstance() {
        return instance;
    }

    /**
     * Instantiates a new daemon thread factory.
     */
    private DaemonThreadFactory() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
     */
    @Override
    public Thread newThread(Runnable arg0) {
        Thread t = new Thread(arg0); 
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}

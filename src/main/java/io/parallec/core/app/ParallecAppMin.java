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
package io.parallec.core.app;

import io.parallec.core.*;

import java.util.Map;

/**
 * The Class ParallecAppMin to demonstrate Parallec with 10 lines.
 */
public class ParallecAppMin {

    /**
     * The main method.
     *
     * @param args
     *            the arguments
     */
    public static void main(String[] args) {
//
        ParallelClient pc = new ParallelClient();
        pc.prepareHttpGet("")
                .setConcurrency(1000)
//                .setTargetHostsFromLineByLineText("/Users/simo/IdeaProjects/parallec/userdata/sample_target_hosts_top500.txt",HostsSourceType.LOCAL_FILE)
                .setTargetHostsFromString(
                        "www.okex.com/api/v1/kline.do?symbol=xas_eth&type=1min&size=20")
                .setProtocol(RequestProtocol.HTTPS)
                .execute(new ParallecResponseHandler() {

                    @Override
                    public void onCompleted(ResponseOnSingleTask res,
                            Map<String, Object> responseContext) {
                        System.out.println(res);
                    }
                });
        pc.releaseExternalResources();
    }
}

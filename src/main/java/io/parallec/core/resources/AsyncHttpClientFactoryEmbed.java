/*  

Copyright [2013-2014] eBay Software Foundation

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
package io.parallec.core.resources;

import io.parallec.core.config.ParallecGlobalConfig;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;


/**
 * The Class HttpClientFactoryEmbed.
 *
 * @author Yuanteng (Jeff) Pei
 */

/**
 * 几年来我阅码无数,觉得作者写的代码真的是简洁,逻辑清晰,易懂
 */
public final class AsyncHttpClientFactoryEmbed {

    /** The logger. */
    private static Logger logger = LoggerFactory
            .getLogger(AsyncHttpClientFactoryEmbed.class);

    /** The fast client. */
    private final AsyncHttpClient fastClient;

    /** The slow client. */
    private final AsyncHttpClient slowClient;

    /**
     * Instantiates a new http client factory embed.
     */
    public AsyncHttpClientFactoryEmbed() {
        AsyncHttpClient fastClient = null;
        AsyncHttpClient slowClient = null;

        try {
            // create and configure async http client
            /** 使用的是github(https://github.com/AsyncHttpClient/async-http-client)开源的http异步客户端*/
            /** 设置http客户端连接的超时时间*/
            /** 设置http客户端请求超时时间 */
            DefaultAsyncHttpClientConfig.Builder configFastClient = config().setConnectTimeout(ParallecGlobalConfig.ningFastClientConnectionTimeoutMillis)
                    .setRequestTimeout(ParallecGlobalConfig.ningSlowClientRequestTimeoutMillis);

            logger.info(
                    "FastClient: ningFastClientConnectionTimeoutMillis: {}",
                    ParallecGlobalConfig.ningFastClientConnectionTimeoutMillis);
            /** 设置http客户端连接的超时时间*/

            logger.info("FastClient: ningFastClientRequestTimeoutMillis: {}",
                    ParallecGlobalConfig.ningFastClientRequestTimeoutMillis);
            /** 设置http客户端请求超时时间 */

            fastClient = asyncHttpClient(configFastClient);

            // TODO added
            // configFastClient.setMaxRequestRetry(3);
            //设置请求重试次数

            DefaultAsyncHttpClientConfig.Builder configSlowClient = config().setConnectTimeout(ParallecGlobalConfig.ningFastClientConnectionTimeoutMillis)
                    .setRequestTimeout(ParallecGlobalConfig.ningSlowClientRequestTimeoutMillis);

            slowClient = asyncHttpClient(configSlowClient);


            disableCertificateVerification();
        } catch (Exception e) {
            logger.error("ERROR IN HttpClientFactoryEmbed "
                    + e.getLocalizedMessage());
        }

        this.fastClient = fastClient;
        this.slowClient = slowClient;
    }

    /**
     * Gets the fast client.
     *
     * @return the fast client
     */
    public AsyncHttpClient getFastClient() {
        return fastClient;
    }

    /**
     * Gets the slow client.
     *
     * @return the slow client
     */
    public AsyncHttpClient getSlowClient() {
        return slowClient;
    }

    /**
     * Disable certificate verification.
     *
     * @throws KeyManagementException
     *             the key management exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     */
    private void disableCertificateVerification()
            throws KeyManagementException, NoSuchAlgorithmException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] { new CustomTrustManager() };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        final HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname,
                    final SSLSession session) {
                return true;
            }
        };

        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    }


    /**
     * class CustomTrustManager.
     */
    public static class CustomTrustManager implements X509TrustManager {

        /**
         * Gets the accepted issuers.
         *
         * @return certificate.
         */
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        /**
         * Check client trusted.
         *
         * @param certs
         *            the certs
         * @param authType
         *            the auth type
         */
        public void checkClientTrusted(final X509Certificate[] certs,
                final String authType) {
            /* no op */
        }

        /**
         * Check server trusted.
         *
         * @param certs
         *            the certs
         * @param authType
         *            the auth type
         */
        public void checkServerTrusted(final X509Certificate[] certs,
                final String authType) {
            /* no op */
        }
    }
}
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

import io.parallec.core.RequestProtocol;
import io.parallec.core.ResponseOnSingleTask;
import io.parallec.core.actor.message.ResponseOnSingeRequest;
import io.parallec.core.actor.message.type.OperationWorkerMsgType;
import io.parallec.core.actor.message.type.RequestWorkerMsgType;
import io.parallec.core.actor.poll.HttpPollerProcessor;
import io.parallec.core.actor.poll.PollerData;
import io.parallec.core.bean.TaskRequest;
import io.parallec.core.config.ParallelTaskConfigDefault;
import io.parallec.core.exception.ActorMessageTypeInvalidException;
import io.parallec.core.resources.HttpMethod;
import io.parallec.core.util.PcConstants;
import io.parallec.core.util.PcDateUtils;
import io.parallec.core.util.PcHttpUtils;
import io.parallec.core.util.PcStringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
//import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;




/**
 * AHC based.
 *
 * @author Yuanteng (Jeff) Pei
 */
/** 这个类主要是为了帮{@link ExecutionManager}生成http actor */
public class OperationWorker extends UntypedActor {

    /** The client. */
    private final AsyncHttpClient client;

    /** The logger. */
    private static Logger logger = LoggerFactory
            .getLogger(OperationWorker.class);

    /** The polling error count. */
    // 20150710: poller: max poller error; and retry for polling
    private int pollingErrorCount = 0;

    /** The request. */
    private final TaskRequest request;

    /** The response. */
    private ResponseOnSingleTask response = new ResponseOnSingleTask();

    /** The async worker. */
    private ActorRef asyncWorker = null;

    /** The sender. */
    private ActorRef sender = null;

    /** The timeout message cancellable. */
    private Cancellable timeoutMessageCancellable = null;

    /** The poll message cancellable. */
    private Cancellable pollMessageCancellable = null;

    /** The timeout duration. */
    private FiniteDuration timeoutDuration = null;

    /** The actor max operation timeout sec. */
    private int actorMaxOperationTimeoutSec = ParallelTaskConfigDefault.actorMaxOperationTimeoutSec;

    /** The start time millis. */
    private long startTimeMillis = 0;

    /** The sent reply. */
    private boolean sentReply = false;

    /** The true target node. */
    // 20130917: change to add uniform target node capability
    private String trueTargetNode;

    /** Http Poller. */
    private PollerData pollerData = null;

    /** The http poller processor. */
    private HttpPollerProcessor httpPollerProcessor = null;

    /**
     * Instantiates a new operation worker.
     *
     * @param request
     *            the request
     * @param client
     *            the client
     * @param httpPollerProcessor
     *            the http poller processor
     */
    public OperationWorker(final TaskRequest request,
            final AsyncHttpClient client,
            final HttpPollerProcessor httpPollerProcessor) {
        super();

        this.client = client;
        this.request = request;
        /**
         * 20130917: change to add uniform target node capability
         */
        this.trueTargetNode = (request.getHostUniform() == null) ? request
                .getHost() : request.getHostUniform();

        // if needs poller; init
        /** 初始化长连接配置*/
        if (request.isPollable()) {
            pollerData = new PollerData();
            this.httpPollerProcessor = httpPollerProcessor;
            logger.info("Request is Pollable: poller info: "
                    + httpPollerProcessor.toString());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
     */
    @Override
    public void onReceive(Object message) throws Exception {
        try {
            if (message instanceof OperationWorkerMsgType) {
                switch ((OperationWorkerMsgType) message) {

                case PROCESS_REQUEST:
                    /** 处理父actor ExecutorManager发送过来的进行请求的命令 */
                    processMainRequest();
                    break;
                    /** 轮训请求处理 */
                case POLL_PROGRESS:
                    pollProgress();
                    break;
                    /** 处理超时情况,超时信息由自己发送自己处理 */
                case OPERATION_TIMEOUT:
                    operationTimeout();
                    break;
                    /** 取消的情况由父节点ExecutorManager发出 */
                case CANCEL:
                    // use the same function
                    cancel();
                    break;
                /** 又没有缺省值,让老夫来补全(强迫症) */
                default:
                    break;
                }// end switch
            } else if (message instanceof ResponseOnSingeRequest) {
                final ResponseOnSingeRequest myResponse = (ResponseOnSingeRequest) message;
                handleHttpWorkerResponse(myResponse);
            } else {
                /** 不需要处理unhandled的情况*/
                unhandled(message);
                throw new ActorMessageTypeInvalidException(
                        "invalid message type to OperationWorker");
            }
        } catch (Exception e) {
            replyErrors(e.toString(), PcStringUtils.printStackTrace(e),
                    PcConstants.NA, PcConstants.NA_INT);
        }
    }

    /**
     * Poller.
     */
    private final void pollProgress() {

        final String pollUrl = this.httpPollerProcessor
                .getPollerRequestUrl(pollerData.getJobId());

        final HttpMethod pollerHttpMethod = HttpMethod.GET;
        final String postBodyForPoller = "";
        final ActorRef pollerWorker = getContext().actorOf(
                Props.create(HttpWorker.class, actorMaxOperationTimeoutSec,
                        client, String.format("%s://%s:%d%s", request
                                .getProtocol().toString(), trueTargetNode,
                                request.getPort(), pollUrl), pollerHttpMethod,
                        postBodyForPoller, request.getHttpHeaderMap(), request.getResponseHeaderMeta()));

        logger.info("POLL_REQ_SEND" + pollUrl + " "
                + PcDateUtils.getNowDateTimeStrStandard());

        pollerData
                .getPollingHistoryMap()
                .put("POLL_REQ_SEND_"
                        + PcDateUtils.getNowDateTimeStrConciseNoZone(), pollUrl);

        pollerWorker.tell(RequestWorkerMsgType.PROCESS_REQUEST, getSelf());

        // reschedule cancel
        cancelExistingIfAnyAndScheduleTimeoutCall();

    }

    /**
     * Handle http worker response.
     *
     * @param respOnSingleReq
     *            the my response
     * @throws Exception
     *             the exception
     */
    /** 用于处理httpWorker,PingWorker等请求回来的结果 */
    private final void handleHttpWorkerResponse(
            ResponseOnSingeRequest respOnSingleReq) throws Exception {
        // Successful response from GenericAsyncHttpWorker

        // Jeff 20310411: use generic response
        /** 为啥在这里总是新建一个ResponseOnSingleRequest不是已经从子actor中接收到了一个response了吗*/
        String responseContent = respOnSingleReq.getResponseBody();
        response.setResponseContent(respOnSingleReq.getResponseBody());

        /**
         * Poller logic if pollable: check if need to poll/ or already complete
         * 1. init poller data and HttpPollerProcessor 2. check if task
         * complete, if not, send the request again.
         */
        /** 检查轮训逻辑是否可用,如果需要轮训或者轮训已经往常,初始化轮训数据和HttpPollerProcessor
         * 检查任务是否完成,若没有,继续发送请求
         * */
        if (request.isPollable()) {
            boolean scheduleNextPoll = false;
            boolean errorFindingUuid = false;

            // set JobId of the poller
            if (!pollerData.isUuidHasBeenSet()) {
                String jobId = httpPollerProcessor
                        .getUuidFromResponse(respOnSingleReq);

                if (jobId.equalsIgnoreCase(PcConstants.NA)) {
                    errorFindingUuid = true;
                    pollingErrorCount++;
                    logger.error("!!POLLING_JOB_FAIL_FIND_JOBID_IN_RESPONSE!! FAIL FAST NOW. PLEASE CHECK getJobIdRegex or retry. "

                            + "DEBUG: REGEX_JOBID: "
                            + httpPollerProcessor.getJobIdRegex()

                            + "RESPONSE: "
                            + respOnSingleReq.getResponseBody()
                            + " polling Error count"
                            + pollingErrorCount
                            + " at " + PcDateUtils.getNowDateTimeStrStandard());
                    // fail fast
                    pollerData.setError(true);
                    pollerData.setComplete(true);

                } else {
                    pollerData.setJobIdAndMarkHasBeenSet(jobId);
                    // if myResponse has other errors, mark poll data as error.
                    pollerData.setError(httpPollerProcessor
                            .ifThereIsErrorInResponse(respOnSingleReq));
                }

            }
            if (!pollerData.isError()) {

                pollerData
                        .setComplete(httpPollerProcessor
                                .ifTaskCompletedSuccessOrFailureFromResponse(respOnSingleReq));
                pollerData.setCurrentProgress(httpPollerProcessor
                        .getProgressFromResponse(respOnSingleReq));
            }

            // poll again only if not complete AND no error; 2015: change to
            // over limit
            scheduleNextPoll = !pollerData.isComplete()
                    && (pollingErrorCount <= httpPollerProcessor
                            .getMaxPollError());

            // Schedule next poll and return. (not to answer back to manager yet
            // )
            if (scheduleNextPoll
                    && (pollingErrorCount <= httpPollerProcessor
                            .getMaxPollError())) {

                pollMessageCancellable = getContext()
                        .system()
                        .scheduler()
                        .scheduleOnce(
                                Duration.create(httpPollerProcessor
                                        .getPollIntervalMillis(),
                                        TimeUnit.MILLISECONDS), getSelf(),
                                OperationWorkerMsgType.POLL_PROGRESS,
                                getContext().system().dispatcher(), getSelf());

                logger.info("\nPOLLER_NOW_ANOTHER_POLL: POLL_RECV_SEND"
                        + String.format("PROGRESS:%.3f, BODY:%s ",
                                pollerData.getCurrentProgress(),
                                responseContent,
                                PcDateUtils.getNowDateTimeStrStandard()));

                String responseContentNew = errorFindingUuid ? responseContent
                        + "_PollingErrorCount:" + pollingErrorCount
                        : responseContent;
                logger.info(responseContentNew);
                // log
                pollerData.getPollingHistoryMap().put(
                        "RECV_" + PcDateUtils.getNowDateTimeStrConciseNoZone(),
                        String.format("PROGRESS:%.3f, BODY:%s",
                                pollerData.getCurrentProgress(),
                                responseContent));
                return;
            } else {
                pollerData
                        .getPollingHistoryMap()
                        .put("RECV_"
                                + PcDateUtils.getNowDateTimeStrConciseNoZone(),
                                String.format(
                                        "POLL_COMPLETED_OR_ERROR: PROGRESS:%.3f, BODY:%s ",
                                        pollerData.getCurrentProgress(),
                                        responseContent));
            }

        }// end if (request.isPollable())

        reply(respOnSingleReq.isFailObtainResponse(),
                respOnSingleReq.getErrorMessage(),
                respOnSingleReq.getStackTrace(),
                respOnSingleReq.getStatusCode(),
                respOnSingleReq.getStatusCodeInt(),
                respOnSingleReq.getReceiveTime(), respOnSingleReq.getResponseHeaders());

    }// end func

    /**
     * the 1st request from the manager.
     */
    private final void processMainRequest() {

        sender = getSender();
        startTimeMillis = System.currentTimeMillis();
        /** 设置持续时间 */
        timeoutDuration = Duration.create(
                request.getActorMaxOperationTimeoutSec(), TimeUnit.SECONDS);

        actorMaxOperationTimeoutSec = request.getActorMaxOperationTimeoutSec();

        if (request.getProtocol() == RequestProtocol.HTTP 
                || request.getProtocol() == RequestProtocol.HTTPS) {
            /** 这里会根据request Protocol,target url, request port,request source path组装请求的最终url
             * 因为数据采集中的url都是已经定义好了的,所以这一步跳过
             * */
//            String urlComplete = String.format("%s://%s:%d%s", request
//                    .getProtocol().toString(), trueTargetNode, request
//                    .getPort(), request.getResourcePath());

            String urlComplete = trueTargetNode;

            // http://stackoverflow.com/questions/1600291/validating-url-in-java
            if (!PcHttpUtils.isUrlValid(urlComplete.trim())) {
                String errMsg = "INVALID_URL";
                logger.error("INVALID_URL: " + urlComplete + " return..");
                replyErrors(errMsg, errMsg, PcConstants.NA, PcConstants.NA_INT);
                return;
            } else {
                logger.debug("url pass validation: " + urlComplete);
            }

            /** 创建一个httpWork去执行http请求*/
            asyncWorker = getContext().actorOf(
                    Props.create(HttpWorker.class, actorMaxOperationTimeoutSec,
                            client, urlComplete, request.getHttpMethod(),
                            request.getPostData(), request.getHttpHeaderMap(), request.getResponseHeaderMeta()));

        } else if (request.getProtocol() == RequestProtocol.SSH ){
            asyncWorker = getContext().actorOf(
                    Props.create(SshWorker.class, actorMaxOperationTimeoutSec,
                            request.getSshMeta(), trueTargetNode));
        } else if (request.getProtocol() == RequestProtocol.TCP ){
            asyncWorker = getContext().actorOf(
                    Props.create(TcpWorker.class, actorMaxOperationTimeoutSec,
                            request.getTcpMeta(), trueTargetNode));            
        } else if (request.getProtocol() == RequestProtocol.UDP ){
            asyncWorker = getContext().actorOf(
                    Props.create(UdpWorker.class, actorMaxOperationTimeoutSec,
                            request.getUdpMeta(), trueTargetNode));                        
        } else if (request.getProtocol() == RequestProtocol.PING ){
            asyncWorker = getContext().actorOf(
                    Props.create(PingWorker.class, actorMaxOperationTimeoutSec, request.getPingMeta(),
                             trueTargetNode));
        }

        asyncWorker.tell(RequestWorkerMsgType.PROCESS_REQUEST, getSelf());

        cancelExistingIfAnyAndScheduleTimeoutCall();

    }

    /**
     * 
     * 201412: now consider the poller. With poller, will cancel this future
     * task and reschedule
     */
    /** 定时调度,如果了timeout时间则触发该方法 */
    private void cancelExistingIfAnyAndScheduleTimeoutCall() {
        // To handle cases where this operation takes extremely long, schedule a
        // 'timeout' message to be sent to us

        if (timeoutMessageCancellable != null
                && !timeoutMessageCancellable.isCancelled()) {
            timeoutMessageCancellable.cancel();

        }

        // now reschedule
        /** 发送消息给自己 */
        timeoutMessageCancellable = getContext()
                .system()
                .scheduler()
                .scheduleOnce(timeoutDuration, getSelf(),
                        OperationWorkerMsgType.OPERATION_TIMEOUT,
                        getContext().system().dispatcher(), getSelf());
    }

    /**
     * will trigger workers to cancel then wait for it to report back.
     */
    @SuppressWarnings("deprecation")
    private final void operationTimeout() {

        /**
         * first kill async http worker; before suicide LESSON: MUST KILL AND
         * WAIT FOR CHILDREN to reply back before kill itself.
         */
        cancelCancellable();
        if (asyncWorker != null && !asyncWorker.isTerminated()) {
            asyncWorker
                    .tell(RequestWorkerMsgType.PROCESS_ON_TIMEOUT, getSelf());

        } else {
            logger.info("asyncWorker has been killed or uninitialized (null). "
                    + "Not send PROCESS ON TIMEOUT.\nREQ: "
                    + request.toString());
            replyErrors(PcConstants.OPERATION_TIMEOUT,
                    PcConstants.OPERATION_TIMEOUT, PcConstants.NA,
                    PcConstants.NA_INT);
        }

    }

    /**
     * Cancel.
     */
    @SuppressWarnings("deprecation")
    private final void cancel() {
        /**
         * first kill async http worker; before suicide LESSON: MUST KILL AND
         * WAIT FOR CHILDREN to reply back before kill itself.
         */
        cancelCancellable();
        if (asyncWorker != null && asyncWorker.isTerminated()) {
            asyncWorker.tell(RequestWorkerMsgType.CANCEL, getSelf());

        } else {
            logger.info("asyncWorker has not been initilized (null). "
                    + "Will not tell it cancel");
            // in case this is the 1st request. currently only manager can send
            // this.
            if (sender == null)
                sender = getSender();
            replyErrors(PcConstants.REQUEST_CANCELED,
                    PcConstants.REQUEST_CANCELED, PcConstants.NA,
                    PcConstants.NA_INT);
        }

    }

    /**
     * Cancel cancellable.
     */
    private final void cancelCancellable() {
        if (timeoutMessageCancellable != null
                && !timeoutMessageCancellable.isCancelled()) {
            timeoutMessageCancellable.cancel();
        }
        if (pollMessageCancellable != null
                && !pollMessageCancellable.isCancelled()) {
            pollMessageCancellable.cancel();
        }
    }

    /**
     * Reply used in error cases. set the response header as null.
     *
     * @param errorMessage the error message
     * @param stackTrace the stack trace
     * @param statusCode the status code
     * @param statusCodeInt the status code int
     */
    private final void replyErrors(final String errorMessage,
            final String stackTrace, final String statusCode,
            final int statusCodeInt) {
        reply(true, errorMessage, stackTrace, statusCode, statusCodeInt,
                PcConstants.NA, null);

    }

    /**
     * Reply.
     *
     * @param error the error
     * @param errorMessage the error message
     * @param stackTrace the stack trace
     * @param statusCode the status code
     * @param statusCodeInt the status code int
     * @param receiveTime the receive time
     * @param responseHeaders the response headers
     */
    @SuppressWarnings("deprecation")
    private final void reply(final boolean error, final String errorMessage,
            final String stackTrace, final String statusCode,
            final int statusCodeInt, final String receiveTime,  Map<String, String > responseHeaders) {
        
        if (!sentReply) {
            sentReply = true;
            //make sure not trigger timeout anymore
            cancelCancellable();
            /** 统计operationWorker及其子孙actor所花费的总时间 */
            final long operationTimeMillis = System.currentTimeMillis()
                    - startTimeMillis;

            if (sender != null
                    && !sender.equals(getContext().system().deadLetters())) {

                response.setReceiveTimeInManager(PcDateUtils
                        .getNowDateTimeStrStandard());
                response.setError(error);
                response.setErrorMessage(errorMessage);
                response.setStackTrace(stackTrace);
                response.setOperationTimeMillis(operationTimeMillis);
                response.setRequest(request);
                response.setStatusCode(statusCode);
                response.setStatusCodeInt(statusCodeInt);
                response.setReceiveTime(receiveTime);
                response.setResponseHeaders(responseHeaders);

                // add history.
                if (request.isPollable() && pollerData != null) {
                    response.getPollingHistoryMap().putAll(
                            pollerData.getPollingHistoryMap());
                }
                
                // handle response. if handle in manager, getHandler() will be null
                if (request != null && request.getHandler() != null) {
                    try {
                        //logger.debug("HANDLE IN WORKER : " + response.getHost());
                        request.getHandler().onCompleted(response,
                                request.getResponseContext());
                    } catch (Exception t) {
                        logger.error("Error handling onCompleted in op worker for response: {} Error {}"
                                , response.toString(),  t.getLocalizedMessage());
                    }
                }
                sender.tell(response, getSelf());
            }
            
            if (asyncWorker != null && !asyncWorker.isTerminated()) {
                getContext().stop(asyncWorker);
            } 
        }
    }

}

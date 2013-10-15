package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.RPCommunication;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.base64.Base64Dialect;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * The MIT License
 * <p/>
 * Copyright (c) 2013 Adam Smolarek
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
@Component
public class HTTPClient extends Client {
    @InjectLogger
    private Logger logger;

    @Inject
    public HTTPClient(@Value("${client_worker_threads:10}") int workerThreads) {
        bootstrap.setFactory(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(), workerThreads));
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                StopWatch stopWatch = new StopWatch("getPipeline");
                stopWatch.start();

                ChannelPipeline p = Channels.pipeline();

                if (enableTrafficLogging) {
                    p.addLast("logger", new LoggingHandler(InternalLogLevel.DEBUG));
                }

                p.addLast("codec", new HttpClientCodec());
                p.addLast("chunkAggregator", new HttpChunkAggregator(MAX_CONTENT_LENGTH));
                p.addLast("chunkedWriter", new ChunkedWriteHandler());
                p.addLast("decompressor", new HttpContentDecompressor());

                p.addLast("handler", new SimpleChannelUpstreamHandler() {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        StopWatch stopWatch = new StopWatch("messageReceived");
                        stopWatch.start();

                        HttpResponse httpResponse = (HttpResponse) e.getMessage();

                        ChannelBuffer cb = Base64.decode(httpResponse.getContent(), Base64Dialect.STANDARD);

                        RPCommunication.Response response = RPCommunication.Response
                                .parseFrom(CodedInputStream.newInstance(cb.copy(0, cb.readableBytes()).array()));
                        logger.trace("received response:" + response);

                        //encryption
                        if (enableAsymmetricEncryption) {
                            response = getAsymDecryptedResponse(response);
                            logger.trace("asymmetric encryption enabled, encrypted request: " + response.toString());
                        }

                        if (enableSymmetricEncryption) {
                            response = getDecryptedResponse(response);
                            logger.trace("symmetric encryption enabled, encrypted request: " + response.toString());
                        }

                        Message msg = descriptorProtoMap.remove(response.getRequestId());
                        Message m = msg
                                .getParserForType()
                                .parseFrom(response.getResponse());
                        callbackMap.remove(response.getRequestId()).run(m);

                        super.messageReceived(ctx, e);
                        stopWatch.stop();
                        logger.trace(stopWatch.shortSummary());
                    }
                });

                stopWatch.stop();
                logger.trace(stopWatch.shortSummary());
                return p;
            }
        });
    }

    @Override
    public RpcChannel connect(final InetSocketAddress sa) {
        RpcChannel rpcChannel = new RpcChannel() {
            private Channel c = bootstrap.connect(sa).awaitUninterruptibly().getChannel();

            @Override
            public void callMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype, RpcCallback<Message> done) {
                StopWatch stopWatch = new StopWatch("callMethod");
                stopWatch.start();

                long id = seqNum.addAndGet(1);

                logger.trace("calling method: " + method.getFullName());

                HttpRequest httpRequest = new DefaultHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.POST, "http://" + sa.getHostName() + ":" + sa.getPort());
                httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
                httpRequest.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
                httpRequest.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);

                RPCommunication.Request protoRequest = RPCommunication.Request.newBuilder().setServiceName(method.getService().getFullName())
                        .setMethodName(method.getName())
                        .setMethodArgument(request.toByteString())
                        .setRequestId(id)
                        .build();

                logger.trace("request built: " + request.toString());

                if (enableSymmetricEncryption) {
                    protoRequest = getEncryptedRequest(protoRequest);
                    logger.trace("symmetric encryption enabled, encrypted request: " + protoRequest.toString());
                }

                if (enableAsymmetricEncryption) {
                    protoRequest = getAsymEncryptedRequest(protoRequest);
                    logger.trace("asymmetric encryption enabled, encrypted request: " + protoRequest.toString());
                }

                byte[] arr = protoRequest.toByteArray();

                ChannelBuffer s = Base64.encode(ChannelBuffers.copiedBuffer(arr), Base64Dialect.STANDARD);

                httpRequest.setContent(s);

                httpRequest.addHeader(HttpHeaders.Names.CONTENT_LENGTH, s.readableBytes());

                httpRequest.setChunked(false);


                callbackMap.put(id, done);
                descriptorProtoMap.put(id, responsePrototype);

                c.write(httpRequest);
                logger.trace("request sent: " + protoRequest.toString());

                stopWatch.stop();
                logger.trace(stopWatch.shortSummary());
            }
        };
        logger.trace("connected to address: " + sa.toString());
        return rpcChannel;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

}

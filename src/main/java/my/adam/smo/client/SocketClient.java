package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.RPCommunication;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
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
public class SocketClient extends Client {
    private static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

    @InjectLogger
    private Logger logger;

    @Inject
    public SocketClient(@Value("${client_worker_threads:10}") int workerThreads) {
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

                p.addLast("frameEncoder", new LengthFieldPrepender(4));//DownstreamHandler
                p.addLast("protobufEncoder", new ProtobufEncoder());//DownstreamHandler

                p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_FRAME_BYTES_LENGTH, 0, 4, 0, 4));//UpstreamHandler
                p.addLast("protobufDecoder", new ProtobufDecoder(RPCommunication.Response.getDefaultInstance()));//UpstreamHandler
                p.addLast("handler", new SimpleChannelUpstreamHandler() {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        StopWatch stopWatch = new StopWatch("messageReceived");
                        stopWatch.start();

                        RPCommunication.Response response = (RPCommunication.Response) e.getMessage();
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

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                        if (!standardExceptionHandling(ctx, e)) {
                            super.exceptionCaught(ctx, e);
                        }
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

                //infinit reconnection loop
                while (reconnect && !c.isOpen()) {
                    logger.debug("channel closed " + sa);
                    logger.debug("trying to reconnect");
                    c.disconnect().awaitUninterruptibly();
                    c.unbind().awaitUninterruptibly();
                    c = bootstrap.connect(sa).awaitUninterruptibly().getChannel();
                    try {
                        Thread.sleep(reconnect_delay);
                    } catch (InterruptedException e) {
                        logger.error("error while sleeping", e);
                    }
                }

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


                callbackMap.put(id, done);
                descriptorProtoMap.put(id, responsePrototype);

                c.write(protoRequest);
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

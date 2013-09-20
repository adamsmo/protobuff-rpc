package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.POC;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

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
public class Client {

    private final ClientBootstrap bootstrap;
    private static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

    private final AtomicLong seqNum = new AtomicLong(0);

    @InjectLogger
    private Logger logger;

    @Value("${reconnect}")
    private boolean reconnect;
    @Value("${reconnect_delay}")
    private int reconnect_delay;

    private ConcurrentHashMap<Long, RpcCallback<Message>> callbackMap = new ConcurrentHashMap<Long, RpcCallback<Message>>();
    private ConcurrentHashMap<Long, Message> descriptorProtoMap = new ConcurrentHashMap<Long, Message>();

    @Inject
    public Client(@Value("${client_worker_threads}") int workerThreads) {
        bootstrap = new ClientBootstrap();
        bootstrap.setFactory(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(), workerThreads));
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {

                ChannelPipeline p = Channels.pipeline();
                p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_FRAME_BYTES_LENGTH, 0, 4, 0, 4));
                p.addLast("protobufDecoder", new ProtobufDecoder(POC.Response.getDefaultInstance()));

                p.addLast("frameEncoder", new LengthFieldPrepender(4));
                p.addLast("protobufEncoder", new ProtobufEncoder());

                p.addLast("handler", new SimpleChannelUpstreamHandler() {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        POC.Response response = (POC.Response) e.getMessage();

                        Message m = descriptorProtoMap.remove(response.getRequestId())
                                .newBuilderForType().mergeFrom(response.getResponse()).build();
                        callbackMap.remove(response.getRequestId()).run(m);

                        super.messageReceived(ctx, e);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                        if (e.getCause() instanceof ClosedChannelException
                                || e.getCause() instanceof ConnectException) {
                            logger.error("Server is down ", e.getCause());
                        } else {
                            super.exceptionCaught(ctx, e);
                        }
                    }
                });
                return p;
            }
        });
    }

    public RpcChannel connect(final SocketAddress sa) {
        return new RpcChannel() {
            private Channel c = bootstrap.connect(sa).awaitUninterruptibly().getChannel();

            @Override
            public void callMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype, RpcCallback<Message> done) {
                long id = seqNum.addAndGet(1);

                //infinit reconnection loop
                while (reconnect && !c.isOpen()) {
                    logger.debug("channel closed " + sa);
                    c.disconnect().awaitUninterruptibly();
                    c.unbind().awaitUninterruptibly();
                    c = bootstrap.connect(sa).awaitUninterruptibly().getChannel();
                    try {
                        Thread.sleep(reconnect_delay);
                    } catch (InterruptedException e) {
                        logger.error("error while sleeping", e);
                    }
                }

                c.write(POC.Request.newBuilder().setServiceName(method.getService().getFullName())
                        .setMethodName(method.getName())
                        .setMethodArgument(request.toByteString())
                        .setRequestId(id)
                        .build());
                callbackMap.put(id, done);
                descriptorProtoMap.put(id, responsePrototype);
            }
        };
    }

    public BlockingRpcChannel blockingConnect(final SocketAddress sa) {
        return new BlockingRpcChannel() {
            private int ARBITRARY_CONSTANT = 1;
            private final CountDownLatch callbackLatch =
                    new CountDownLatch(ARBITRARY_CONSTANT);

            private Message result;
            private RpcChannel rpc = connect(sa);

            @Override
            public Message callBlockingMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype) throws ServiceException {
                RpcCallback<Message> done = new RpcCallback<Message>() {
                    @Override
                    public void run(Message parameter) {
                        result = parameter;
                        callbackLatch.countDown();
                    }
                };

                rpc.callMethod(method, controller, request, responsePrototype, done);
                try {
                    callbackLatch.await();
                } catch (InterruptedException e) {
                    logger.error("call failed", e);
                }
                return result;
            }
        };
    }

    public void disconnect() {
        bootstrap.shutdown();
        bootstrap.releaseExternalResources();
    }
}

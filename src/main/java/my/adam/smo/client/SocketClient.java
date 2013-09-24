package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.POC;
import my.adam.smo.common.InjectLogger;
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

    @InjectLogger
    private Logger logger;

    @Inject
    public SocketClient(@Value("${client_worker_threads}") int workerThreads) {
        bootstrap.setFactory(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(), workerThreads));
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {

                ChannelPipeline p = Channels.pipeline();

                p.addLast("frameEncoder", new LengthFieldPrepender(4));//DownstreamHandler
                p.addLast("protobufEncoder", new ProtobufEncoder());//DownstreamHandler

                p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_FRAME_BYTES_LENGTH, 0, 4, 0, 4));//UpstreamHandler
                p.addLast("protobufDecoder", new ProtobufDecoder(POC.Response.getDefaultInstance()));//UpstreamHandler
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

    @Override
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

    @Override
    public Logger getLogger() {
        return logger;
    }

}

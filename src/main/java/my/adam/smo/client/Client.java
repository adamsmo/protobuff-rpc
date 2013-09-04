package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.POC;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
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
public class Client {

    private final ClientBootstrap bootstrap;
    private static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

    private final AtomicLong seqNum = new AtomicLong(0);

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private ConcurrentHashMap<Long, RpcCallback<Message>> callbackConcurrentHashMap = new ConcurrentHashMap<Long, RpcCallback<Message>>();
    private ConcurrentHashMap<Long, Message> descriptorProtoConcurrentHashMap = new ConcurrentHashMap<Long, Message>();

    public Client(int workerThreads) {
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

                        Message m = descriptorProtoConcurrentHashMap.remove(response.getRequestId())
                                .newBuilderForType().mergeFrom(response.getResponse()).build();
                        callbackConcurrentHashMap.remove(response.getRequestId()).run(m);

                        super.messageReceived(ctx, e);
                    }
                });
                return p;
            }
        });
    }

    public RpcChannel connect(SocketAddress sa) {
        final Channel c = bootstrap.connect(sa).awaitUninterruptibly().getChannel();
        return new RpcChannel() {
            @Override
            public void callMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype, RpcCallback<Message> done) {
                long id = seqNum.addAndGet(1);
                c.write(POC.Request.newBuilder().setServiceName(method.getService().getFullName())
                        .setMethodName(method.getName())
                        .setMethodArgument(request.toByteString())
                        .setRequestId(id)
                        .build());
                callbackConcurrentHashMap.put(id, done);
                descriptorProtoConcurrentHashMap.put(id, responsePrototype);
            }
        };
    }

    public void disconnect() {
        bootstrap.releaseExternalResources();
    }
}

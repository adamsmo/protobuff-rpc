package my.adam.smo.server;

import com.google.protobuf.*;
import my.adam.smo.DummyRpcController;
import my.adam.smo.POC;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64Decoder;
import org.jboss.netty.handler.codec.base64.Base64Encoder;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
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
public class HTTPServer implements Server {
    private final ServerBootstrap bootstrap;

    @InjectLogger
    private Logger logger;

    private ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<String, Service>();

    @Inject
    public HTTPServer(@Value("${server_worker_threads}") int workerCount){
        bootstrap = new ServerBootstrap();

        bootstrap.setFactory(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(),
                workerCount));

        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline p = Channels.pipeline();

                p.addLast("encoder", new HttpResponseEncoder());//DownstreamHandler
                p.addLast("base64Encoder", new Base64Encoder());//DownstreamHandler
                p.addLast("protobufEncoder", new ProtobufEncoder());//DownstreamHandler


                p.addLast("decoder", new HttpRequestDecoder());//UpstreamHandler
                p.addLast("compressor", new HttpContentCompressor());//UpstreamHandler
                p.addLast("base64Decoder", new Base64Decoder());
                p.addLast("protobufDecoder", new ProtobufDecoder(POC.Request.getDefaultInstance()));
                p.addLast("handler", new SimpleChannelUpstreamHandler(){
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
                        final POC.Request request = (POC.Request) e.getMessage();
                        RpcController dummyController = new DummyRpcController();
                        Service service = serviceMap.get(request.getServiceName());

                        Descriptors.MethodDescriptor methodToCall = service
                                .getDescriptorForType()
                                .findMethodByName(request.getMethodName());

                        Message methodArguments = service
                                .getRequestPrototype(methodToCall)
                                .newBuilderForType()
                                .mergeFrom(request.getMethodArgument())
                                .build();

                        RpcCallback<Message> callback = new RpcCallback<Message>() {
                            @Override
                            public void run(Message parameter) {
                                POC.Response resp = POC
                                        .Response
                                        .newBuilder()
                                        .setResponse(parameter.toByteString())
                                        .setRequestId(request.getRequestId())
                                        .build();
                                e.getChannel().write(resp);
                                logger.debug("finishing call, response sended");
                            }
                        };
                        logger.debug("calling " + methodToCall.getFullName());
                        service.callMethod(methodToCall, dummyController, methodArguments, callback);
                    }
                });
                return p;
            }
        };
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    public void start(SocketAddress sa) {
        try {
            bootstrap.bind(sa);
        } catch (ChannelException e) {
            logger.error("error while starting server ", e);
        }
    }

    public void stop() {
        bootstrap.releaseExternalResources();
    }

    public void register(Service service) {
        serviceMap.put(service.getDescriptorForType().getFullName(), service);
    }
}

package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.POC;
import my.adam.smo.common.InjectLogger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.base64.Base64Dialect;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.SocketAddress;
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
public class HTTPCient extends Client {
    private static final int MAX_CONTENT_LENGTH = Integer.MAX_VALUE;

    @InjectLogger
    private Logger logger;

    @Inject
    public HTTPCient(@Value("${client_worker_threads}") int workerThreads) {
        bootstrap.setFactory(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(), workerThreads));
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {

                ChannelPipeline p = Channels.pipeline();

                if (enableTrafficLogging) {
                    p.addLast("logger", new LoggingHandler(InternalLogLevel.DEBUG));
                }

                p.addLast("codec", new HttpClientCodec());
                p.addLast("chunkAggregator", new HttpChunkAggregator(MAX_CONTENT_LENGTH));
                p.addLast("decompressor", new HttpContentDecompressor());

                p.addLast("handler", new SimpleChannelUpstreamHandler() {
                    @Override
                    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                        HttpResponse httpResponse = (HttpResponse) e.getMessage();

                        ChannelBuffer cb = Base64.decode(httpResponse.getContent(), Base64Dialect.STANDARD);

                        POC.Response response = POC.Response.parseFrom(cb.copy(0, cb.readableBytes()).array());

                        //encryption
                        if (enableAsymmetricEncryption) {
                            response = getAsymDecryptedResponse(response);
                        }

                        if (enableSymmetricEncryption) {
                            response = getDecryptedResponse(response);
                        }

                        Message m = descriptorProtoMap.remove(response.getRequestId())
                                .newBuilderForType().mergeFrom(response.getResponse()).build();
                        callbackMap.remove(response.getRequestId()).run(m);

                        super.messageReceived(ctx, e);
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

                HttpRequest httpRequest = new DefaultHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost:8090");
                httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
                httpRequest.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
                httpRequest.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);

                POC.Request protoRequest = POC.Request.newBuilder().setServiceName(method.getService().getFullName())
                        .setMethodName(method.getName())
                        .setMethodArgument(request.toByteString())
                        .setRequestId(id)
                        .build();

                if (enableSymmetricEncryption) {
                    protoRequest = getEncryptedRequest(protoRequest);
                }

                if (enableAsymmetricEncryption) {
                    protoRequest = getAsymEncryptedRequest(protoRequest);
                }

                byte[] arr = protoRequest.toByteArray();

                ChannelBuffer s = Base64.encode(ChannelBuffers.copiedBuffer(arr), Base64Dialect.STANDARD);

                httpRequest.setContent(s);

                httpRequest.addHeader(HttpHeaders.Names.CONTENT_LENGTH, s.readableBytes());

                httpRequest.setChunked(false);

                c.write(httpRequest);

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

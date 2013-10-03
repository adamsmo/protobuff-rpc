package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.POC;
import my.adam.smo.common.SymmetricEncryptionBox;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
public abstract class Client {
    protected final ClientBootstrap bootstrap = new ClientBootstrap();
    protected final AtomicLong seqNum = new AtomicLong(0);

    @Autowired
    private SymmetricEncryptionBox symmetricEncryptionBox;

    @Value("${reconnect}")
    protected boolean reconnect;
    @Value("${reconnect_delay}")
    protected int reconnect_delay;
    @Value("${blocking_method_call_timeout}")
    protected int blocking_method_call_timeout;
    @Value("${enable_traffic_logging:false}")
    protected boolean enableTrafficLogging;

    protected ConcurrentHashMap<Long, RpcCallback<Message>> callbackMap = new ConcurrentHashMap<Long, RpcCallback<Message>>();
    protected ConcurrentHashMap<Long, Message> descriptorProtoMap = new ConcurrentHashMap<Long, Message>();

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
                    callbackLatch.await(blocking_method_call_timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    getLogger().error("call failed", e);
                }
                return result;
            }
        };
    }

    public void disconnect() {
        bootstrap.shutdown();
        bootstrap.releaseExternalResources();
    }

    protected POC.Response getDecryptedResponse(POC.Response response) {
        byte[] encryptedResponse = response.getResponse().toByteArray();
        ByteString decryptedResponse = ByteString
                .copyFrom(symmetricEncryptionBox.decrypt(encryptedResponse));
        response = response.toBuilder().setResponse(decryptedResponse).build();
        return response;
    }

    public abstract RpcChannel connect(final SocketAddress sa);

    public abstract Logger getLogger();
}

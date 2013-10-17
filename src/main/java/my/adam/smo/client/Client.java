package my.adam.smo.client;

import com.google.protobuf.*;
import my.adam.smo.common.AbstractCommunicator;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
public abstract class Client extends AbstractCommunicator {
    protected final ClientBootstrap bootstrap = new ClientBootstrap();
    protected final AtomicLong seqNum = new AtomicLong(0);

    @Value("${reconnect:false}")
    protected boolean reconnect;
    @Value("${reconnect_delay:100}")
    protected int reconnect_delay;

    @Value("${blocking_method_call_timeout:100}")
    protected int blocking_method_call_timeout;

    protected ConcurrentHashMap<Long, RpcCallback<Message>> callbackMap = new ConcurrentHashMap<Long, RpcCallback<Message>>();
    protected ConcurrentHashMap<Long, Message> descriptorProtoMap = new ConcurrentHashMap<Long, Message>();

    public BlockingRpcChannel blockingConnect(final InetSocketAddress sa) {
        return new BlockingRpcChannel() {
            private int countDownCallTimesToRelease = 1;
            private RpcChannel rpc = connect(sa);

            @Override
            public Message callBlockingMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype) throws ServiceException {
                StopWatch stopWatch = new StopWatch("callBlockingMethod");
                stopWatch.start();

                final CountDownLatch callbackLatch = new CountDownLatch(countDownCallTimesToRelease);

                final AtomicReference<Message> result = new AtomicReference<Message>();

                RpcCallback<Message> done = new RpcCallback<Message>() {
                    @Override
                    public void run(Message parameter) {
                        result.set(parameter);
                        callbackLatch.countDown();
                    }
                };

                rpc.callMethod(method, controller, request, responsePrototype, done);
                try {
                    //TODO czeka a po timeoucie nie rzuca wyjątku!!!
                    callbackLatch.await(blocking_method_call_timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    getLogger().error("call failed", e);
                    stopWatch.stop();
                }

                stopWatch.stop();
                getLogger().trace(stopWatch.shortSummary());

                if (result.get() == null) {
                    throw new ServiceException("blocking method timeout");
                }

                return result.get();
            }
        };
    }

    public void disconnect() {
        bootstrap.shutdown();
        bootstrap.releaseExternalResources();
    }

    public boolean standardExceptionHandling(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (e.getCause() instanceof IOException
                || e.getCause() instanceof ClosedChannelException
                || e.getCause() instanceof ConnectException) {
            getLogger().error("Server is down ", e.getCause());
            return true;
        }
        return false;
    }

    public abstract RpcChannel connect(final InetSocketAddress sa);

    public abstract Logger getLogger();
}

package my.adam.smo;

import com.google.protobuf.*;
import junit.framework.Assert;
import my.adam.smo.client.HTTPClient;
import my.adam.smo.client.SocketClient;
import my.adam.smo.common.DummyRpcController;
import my.adam.smo.server.HTTPServer;
import my.adam.smo.server.SocketServer;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;
import java.security.SecureRandom;

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
public class ServerCorrectnesTest {
    private RpcChannel httpChannel;
    private BlockingRpcChannel httpBlockingChannel;
    private RpcChannel socketChannel;
    private BlockingRpcChannel socketBlockingChannel;
    private HTTPServer httpServer;
    private SocketServer socketServer;

    private final int result = 2;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Before
    public void init() {

        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        ApplicationContext clientContext = new ClassPathXmlApplicationContext("Context.xml");
        ApplicationContext serverContext = new ClassPathXmlApplicationContext("Context.xml");

        httpServer = serverContext.getBean(HTTPServer.class);
        socketServer = serverContext.getBean(SocketServer.class);
        HTTPClient httpClient = clientContext.getBean(HTTPClient.class);
        SocketClient socketClient = clientContext.getBean(SocketClient.class);

        final TestServices.Out out = TestServices.Out.newBuilder().setResult(result).build();

        Service service = TestServices.NewUsefullTestService.newReflectiveService(new TestServices.NewUsefullTestService.Interface() {
            @Override
            public void doGoodJob(RpcController controller, TestServices.In request, RpcCallback<TestServices.Out> done) {
                done.run(out);
            }

            @Override
            public void doHighWeightGoodJob(RpcController controller, TestServices.HighWeightRequest request, RpcCallback<TestServices.HighWeightResponse> done) {
                byte[] out = getMegaBytes(request.getLoad());
                done.run(TestServices.HighWeightResponse
                        .newBuilder()
                        .setResponse(ByteString.copyFrom(out))
                        .build());
            }
        });

        httpServer.register(service);
        socketServer.register(service);

        httpServer.start(new InetSocketAddress(8081));
        socketServer.start(new InetSocketAddress(8091));

        httpChannel = httpClient.connect(new InetSocketAddress(8081));
        httpBlockingChannel = httpClient.blockingConnect(new InetSocketAddress(8081));
        socketChannel = socketClient.connect(new InetSocketAddress(8091));
        socketBlockingChannel = socketClient.blockingConnect(new InetSocketAddress(8091));

        logger.debug("before");
    }

    @After
    public void tearDown() {
        httpServer.stop();
        socketServer.stop();
        logger.debug("after");
    }

    @Test
    public void correctCommunication() {
        int arg1 = 1;
        int arg2 = 2;

        TestServices.NewUsefullTestService httpService = TestServices.NewUsefullTestService.newStub(httpChannel);
        TestServices.NewUsefullTestService socketService = TestServices.NewUsefullTestService.newStub(socketChannel);

        TestServices.NewUsefullTestService.BlockingInterface httpBlockingService = TestServices.NewUsefullTestService.newBlockingStub(httpBlockingChannel);
        TestServices.NewUsefullTestService.BlockingInterface socketBlockingService = TestServices.NewUsefullTestService.newBlockingStub(socketBlockingChannel);

        TestServices.In in = TestServices.In.newBuilder().setOperand1(arg1).setOperand2(arg2).build();

        httpService.doGoodJob(new DummyRpcController(), in, new RpcCallback<TestServices.Out>() {
            @Override
            public void run(TestServices.Out parameter) {
                Assert.assertEquals(result, parameter.getResult());
                logger.debug("httpAsyncCallDone");
            }
        });

        socketService.doGoodJob(new DummyRpcController(), in, new RpcCallback<TestServices.Out>() {
            @Override
            public void run(TestServices.Out parameter) {
                Assert.assertEquals(result, parameter.getResult());
                logger.debug("socketAsyncCallDone");
            }
        });

        try {
            Assert.assertEquals(result, httpBlockingService.doGoodJob(new DummyRpcController(), in).getResult());
            Assert.assertEquals(result, socketBlockingService.doGoodJob(new DummyRpcController(), in).getResult());
        } catch (ServiceException e) {
            logger.error("err", e);
            Assert.fail("call failed");
        }
    }

    @Test
    public void ultraHighPayloadTest() {
        // given
        TestServices.NewUsefullTestService.BlockingInterface httpBlockingService = TestServices.NewUsefullTestService.newBlockingStub(httpBlockingChannel);
        TestServices.NewUsefullTestService.BlockingInterface socketBlockingService = TestServices.NewUsefullTestService.newBlockingStub(socketBlockingChannel);
        int dataAmount = 500;

        TestServices.HighWeightRequest in = TestServices.HighWeightRequest
                .newBuilder()
                .setRequest(ByteString.copyFrom(getMegaBytes(dataAmount)))
                .setLoad(dataAmount)
                .build();
        int httpResponseSize = 0;
        int socketResponseSize = 0;

        // when
        try {
            httpResponseSize = httpBlockingService.doHighWeightGoodJob(new DummyRpcController(), in).getResponse().size();
            socketResponseSize = socketBlockingService.doHighWeightGoodJob(new DummyRpcController(), in).getResponse().size();
        } catch (ServiceException e) {
            logger.error("err", e);
            Assert.fail("call failed");
        }

        // then
        Assert.assertEquals(1024 * dataAmount, httpResponseSize);
        Assert.assertEquals(1024 * dataAmount, socketResponseSize);
    }

    public static byte[] getMegaBytes(int amount) {
        SecureRandom sr = new SecureRandom();
        byte[] out = new byte[1024 * amount];
        sr.nextBytes(out);
        return out;
    }
}

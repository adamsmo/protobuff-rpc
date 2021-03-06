package my.adam.smo;

import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.ServiceException;
import my.adam.smo.client.HTTPClient;
import my.adam.smo.client.SocketClient;
import my.adam.smo.common.DummyRpcController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
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
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String args[]) {
        HTTPClient httpClient = Refero.getHttpClient();
        SocketClient socketClient = Refero.getSocketClient();

        BlockingRpcChannel httpBChannel = httpClient.blockingConnect(new InetSocketAddress(8080));
        BlockingRpcChannel socketBChannel = socketClient.blockingConnect(new InetSocketAddress(8090));

        RpcChannel httpChannel = httpClient.connect(new InetSocketAddress(8080));
        RpcChannel socketChannel = socketClient.connect(new InetSocketAddress(8090));

        final Example.NewUsefullService.BlockingInterface httpNewUsefullService = Example.NewUsefullService
                .newBlockingStub(httpBChannel);
        final Example.NewUsefullService.BlockingInterface socketNewUsefullService = Example.NewUsefullService
                .newBlockingStub(socketBChannel);
        final Example.AwsomeSearch.BlockingInterface httpAwsomeSearch = Example.AwsomeSearch
                .newBlockingStub(httpBChannel);
        final Example.AwsomeSearch.BlockingInterface socketAwsomeSearch = Example.AwsomeSearch
                .newBlockingStub(socketBChannel);

        Example.AwsomeSearch httpawsomeSearch = Example.AwsomeSearch.newStub(httpChannel);
        Example.NewUsefullService httpnewUsefullService = Example.NewUsefullService.newStub(httpChannel);

        Example.AwsomeSearch sockawsomeSearch = Example.AwsomeSearch.newStub(socketChannel);
        Example.NewUsefullService socknewUsefullService = Example.NewUsefullService.newStub(socketChannel);

        // manually without spring
        BlockingRpcChannel reffChannel = Refero.getHttpClient().blockingConnect(new InetSocketAddress(18080));

        final Example.NewUsefullService.BlockingInterface reffSocketNewUsefullService = Example.NewUsefullService
                .newBlockingStub(reffChannel);
        final Example.AwsomeSearch.BlockingInterface reffHttpAwsomeSearch = Example.AwsomeSearch
                .newBlockingStub(reffChannel);
        // -----------------------

        while (true) {
                httpawsomeSearch.search(new DummyRpcController(), Example.Hello
                        .newBuilder()
                        .setMessag("hello")
                        .build(), new RpcCallback<Example.AwsomeAnswer>() {
                    @Override
                    public void run(Example.AwsomeAnswer parameter) {
                        logger.debug("return from search callback!!");
                    }
                });

                httpnewUsefullService.doGoodJob(new DummyRpcController(), Example.In
                        .newBuilder()
                        .setOperand1(32)
                        .setOperand2(54)
                        .build(), new RpcCallback<Example.Out>() {
                    @Override
                    public void run(Example.Out parameter) {
                        logger.debug("return from doGoodJob callback!!");
                    }
                });

                sockawsomeSearch.search(new DummyRpcController(), Example.Hello
                        .newBuilder()
                        .setMessag("hello")
                        .build(), new RpcCallback<Example.AwsomeAnswer>() {
                    @Override
                    public void run(Example.AwsomeAnswer parameter) {
                        logger.debug("return from search callback!!");
                    }
                });

                socknewUsefullService.doGoodJob(new DummyRpcController(), Example.In
                        .newBuilder()
                        .setOperand1(32)
                        .setOperand2(54)
                        .build(), new RpcCallback<Example.Out>() {
                    @Override
                    public void run(Example.Out parameter) {
                        logger.debug("return from doGoodJob callback!!");
                    }
                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Executor e = Executors.newFixedThreadPool(20);

                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(httpAwsomeSearch.search(new DummyRpcController(), Example.Hello
                                    .newBuilder()
                                    .setMessag("hello")
                                    .build()).getAnswer());
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(socketAwsomeSearch.search(new DummyRpcController(), Example.Hello
                                    .newBuilder()
                                    .setMessag("hello")
                                    .build()).getAnswer());
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(httpNewUsefullService.doGoodJob(new DummyRpcController(), Example.In
                                    .newBuilder()
                                    .setOperand1(32)
                                    .setOperand2(54)
                                    .build()).getSerializedSize()+"");
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });


                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(socketNewUsefullService.doGoodJob(new DummyRpcController(), Example.In
                                    .newBuilder()
                                    .setOperand1(32)
                                    .setOperand2(54)
                                    .build()).getResult()+"");
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });


                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(reffSocketNewUsefullService.doGoodJob(new DummyRpcController(), Example.In
                                    .newBuilder()
                                    .setOperand1(32)
                                    .setOperand2(54)
                                    .build()).getResult()+"");
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });


                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.debug(reffHttpAwsomeSearch.search(new DummyRpcController(), Example.Hello
                                    .newBuilder()
                                    .setMessag("hello")
                                    .build()).getAnswer());
                        } catch (ServiceException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
        }
    }
}

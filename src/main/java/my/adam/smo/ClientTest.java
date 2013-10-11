package my.adam.smo;

import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.ServiceException;
import my.adam.smo.client.HTTPClient;
import my.adam.smo.client.SocketClient;
import my.adam.smo.common.DummyRpcController;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;

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
public class ClientTest {

    private static Logger logger = LoggerFactory.getLogger(ClientTest.class);

    public static void main(String[] args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("Context.xml");
        SocketClient c = applicationContext.getBean(SocketClient.class);
        HTTPClient hc = applicationContext.getBean(HTTPClient.class);

        RpcChannel rpcChannel = hc.connect(new InetSocketAddress("localhost", 8090));

        POC.SearchService searchService = POC.SearchService.newStub(rpcChannel);
        POC.AwsomeSearch awsomeSearch = POC.AwsomeSearch.newStub(rpcChannel);
        POC.NewUsefullService usefullService = POC.NewUsefullService.newStub(rpcChannel);

        // blocking socket test
        BlockingRpcChannel brc = c.blockingConnect(new InetSocketAddress("localhost", 8080));
        POC.SearchService.BlockingInterface blockingSearchService = POC.SearchService.newBlockingStub(brc);
        try {
            POC.hello resp = blockingSearchService.search(new DummyRpcController(), POC.hello.newBuilder().setMessag("ala ma 32 koty").build());
            logger.debug("-----------------------------------");
            logger.debug("-    return from blocking call    -");
            logger.debug("-----------------------------------");
            logger.debug(resp.getMessag());
        } catch (ServiceException e) {
            logger.error("call failed", e);
        }

        // blocking http test
        BlockingRpcChannel bhc = hc.blockingConnect(new InetSocketAddress("localhost", 8090));
        POC.SearchService.BlockingInterface blockingSearchServiceHTTP = POC.SearchService.newBlockingStub(bhc);
        try {
            POC.hello resp = blockingSearchServiceHTTP.search(new DummyRpcController(), POC.hello.newBuilder().setMessag("ala ma 32 koty").build());
            logger.debug("-----------------------------------");
            logger.debug("- return from blocking HTTP call  -");
            logger.debug("-----------------------------------");
            logger.debug(resp.getMessag());
        } catch (ServiceException e) {
            logger.error("call failed", e);
        }

        for (int i = 0; i < 10; i++) {
            searchService.search(new DummyRpcController(), POC.hello.newBuilder().setMessag("le mess").build(),
                    new RpcCallback<POC.hello>() {
                        @Override
                        public void run(POC.hello parameter) {
                            logger.debug(parameter.getMessag());
                        }
                    }
            );

            awsomeSearch.search(new DummyRpcController(), POC.hello.newBuilder().setMessag("ala ma kota").build(),
                    new RpcCallback<POC.awsomeAnswer>() {
                        @Override
                        public void run(POC.awsomeAnswer parameter) {
                            logger.debug(parameter.getAnswer());
                        }
                    });

            usefullService.doGoodJob(new DummyRpcController(), POC.In.newBuilder().setOperand1(15).setOperand2(29).build(), new RpcCallback<POC.Out>() {
                @Override
                public void run(POC.Out parameter) {
                    logger.debug("" + parameter.getResult());
                }
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        c.disconnect();
        hc.disconnect();
    }
}

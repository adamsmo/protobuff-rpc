package my.adam.smo;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import my.adam.smo.client.Client;
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

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("Context.xml");
        Client c = (Client) applicationContext.getBean(Client.class);

        RpcChannel rpcc = c.connect(new InetSocketAddress("localhost", 8080));

        POC.SearchService searchService = POC.SearchService.newStub(rpcc);
        POC.AwsomeSearch awsomeSearch = POC.AwsomeSearch.newStub(rpcc);
        POC.NewUsefullService usefullService = POC.NewUsefullService.newStub(rpcc);

        while (true) {
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
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //c.disconnect();
    }
}

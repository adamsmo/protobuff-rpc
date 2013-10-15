package my.adam.smo;

import my.adam.smo.server.HTTPServer;
import my.adam.smo.server.SocketServer;
import my.adam.smo.services.AwesomeSearchService;
import my.adam.smo.services.NewUsefullService;
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
public class Main {
    public static void main(String args[]){
        HTTPServer httpServer = Refero.getHttpServer();
        SocketServer socketServer = Refero.getSocketServer();

        httpServer.register(Example.AwsomeSearch.newReflectiveService(new AwesomeSearchService()));
        httpServer.register(Example.NewUsefullService.newReflectiveService(new NewUsefullService()));

        socketServer.register(Example.AwsomeSearch.newReflectiveService(new AwesomeSearchService()));
        socketServer.register(Example.NewUsefullService.newReflectiveService(new NewUsefullService()));

        httpServer.start(new InetSocketAddress(8080));
        socketServer.start(new InetSocketAddress(8090));

        // manually without spring
        Refero.getHttpServer()
                .register(Example.AwsomeSearch.newReflectiveService(new AwesomeSearchService()))
                .register(Example.NewUsefullService.newReflectiveService(new NewUsefullService()))
                .start(new InetSocketAddress(18080));
    }
}

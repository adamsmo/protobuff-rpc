package my.adam.smo;

import my.adam.smo.client.Client;
import my.adam.smo.client.HTTPClient;
import my.adam.smo.client.SocketClient;
import my.adam.smo.server.HTTPServer;
import my.adam.smo.server.Server;
import my.adam.smo.server.SocketServer;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
public class Refero {
    private static ClassPathXmlApplicationContext applicationContext;

    private static ClassPathXmlApplicationContext getClientServerContext() {
        if (applicationContext == null) {
            applicationContext = new ClassPathXmlApplicationContext("refero-context.xml");
        }
        return applicationContext;
    }

    public static Server getServer() {
        return getSocketServer();
    }

    public static Client getClient() {
        return getSocketClient();
    }

    public static HTTPServer getHttpServer() {
        return getClientServerContext().getBean(HTTPServer.class);
    }

    public static SocketServer getSocketServer() {
        return getClientServerContext().getBean(SocketServer.class);
    }

    public static HTTPClient getHttpClient() {
        return getClientServerContext().getBean(HTTPClient.class);
    }

    public static SocketClient getSocketClient() {
        return getClientServerContext().getBean(SocketClient.class);
    }
}

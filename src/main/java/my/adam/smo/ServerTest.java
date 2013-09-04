package my.adam.smo;

import my.adam.smo.server.Server;
import my.adam.smo.serviceimpl.AwsomeSearchServiceImpl;
import my.adam.smo.serviceimpl.SearchServiceImpl;
import my.adam.smo.serviceimpl.UsefullThing;

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
public class ServerTest {
    public static void main(String[] args) {
        Server s = new Server(40);
        s.register(POC.SearchService.newReflectiveService(new SearchServiceImpl()));
        s.register(POC.AwsomeSearch.newReflectiveService(new AwsomeSearchServiceImpl()));
        s.register(POC.NewUsefullService.newReflectiveService(new UsefullThing()));
        s.start(new InetSocketAddress(8080));
    }
}

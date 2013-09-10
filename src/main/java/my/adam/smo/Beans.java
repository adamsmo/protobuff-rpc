package my.adam.smo;

import my.adam.smo.client.Client;
import my.adam.smo.server.Server;
import my.adam.smo.serviceimpl.AwsomeSearchServiceImpl;
import my.adam.smo.serviceimpl.SearchServiceImpl;
import my.adam.smo.serviceimpl.UsefullThing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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
@Configuration
public class Beans {

    @Bean(name = "my_server")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Server getServer(@Value("${server_worker_threads}") int threadCount,
                            @Value("${server_port}") int serverPort) {
        Server s = new Server(threadCount);
        s.start(new InetSocketAddress(serverPort));
        return s;
    }

    @Bean(name = "my_client")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Client getClient(@Value("${client_worker_threads}") int threadCount) {
        return new Client(threadCount);
    }

    @Bean(name = "usefulThing")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Autowired
    public UsefullThing getUsefullThing(Server server) {
        UsefullThing ut = new UsefullThing();
        server.register(POC.NewUsefullService.newReflectiveService(ut));
        return ut;
    }

    @Bean(name = "searchServiceImpl")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Autowired
    public SearchServiceImpl getSearchService(Server server) {
        SearchServiceImpl ss = new SearchServiceImpl();
        server.register(POC.SearchService.newReflectiveService(ss));
        return ss;
    }

    @Bean(name = "awsomeSearchServiceImpl")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Autowired
    public AwsomeSearchServiceImpl getAwsomeSearchService(Server server) {
        AwsomeSearchServiceImpl ass = new AwsomeSearchServiceImpl();
        server.register(POC.AwsomeSearch.newReflectiveService(ass));
        return ass;
    }
}

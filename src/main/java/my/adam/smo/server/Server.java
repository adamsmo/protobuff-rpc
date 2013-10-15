package my.adam.smo.server;

import com.google.protobuf.Service;
import my.adam.smo.common.AbstractCommunicator;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;

import javax.crypto.BadPaddingException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentHashMap;

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
public abstract class Server extends AbstractCommunicator {
    protected final ServerBootstrap bootstrap = new ServerBootstrap();
    protected ConcurrentHashMap<String, Service> serviceMap = new ConcurrentHashMap<String, Service>();
    protected static final int MAX_FRAME_BYTES_LENGTH = Integer.MAX_VALUE;

    public Server start(SocketAddress sa) {
        try {
            bootstrap.bind(sa);
            getLogger().trace("server started on " + sa.toString());
        } catch (ChannelException e) {
            getLogger().error("error while starting server ", e);
        }
        return this;
    }

    public void stop() {
        bootstrap.releaseExternalResources();
        getLogger().debug("server stoped");
    }

    public Server register(Service service) {
        serviceMap.put(service.getDescriptorForType().getFullName(), service);
        getLogger().trace("service " + service.getClass().toString()
                + " registered with name " + service.getDescriptorForType().getFullName());
        return this;
    }

    /**
     * @param e
     * @return true if exception was handled, false otherwise
     */
    public boolean standardExceptionHandling(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (e.getCause() instanceof ClosedChannelException
                || e.getCause() instanceof ConnectException) {
            getLogger().error("Client which made request is down", e.getCause());
            return true;
        } else if (e.getCause() instanceof IllegalStateException
                && e.getCause().getCause() instanceof BadPaddingException) {
            getLogger().error("Client which made request (from "
                    + ctx.getChannel().getRemoteAddress()
                    + ") probably has wrong encryption config", e.getCause());
            return true;
        }
        return false;
    }

    public abstract Logger getLogger();
}

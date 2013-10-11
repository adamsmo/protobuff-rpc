package my.adam.smo.common;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

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
public class DummyRpcController implements RpcController {
    @Override
    public void reset() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean failed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String errorText() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void startCancel() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setFailed(String reason) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isCanceled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> callback) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

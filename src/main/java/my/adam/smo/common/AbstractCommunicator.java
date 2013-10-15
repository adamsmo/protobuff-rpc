package my.adam.smo.common;

import com.google.protobuf.ByteString;
import my.adam.smo.RPCommunication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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
public abstract class AbstractCommunicator {
    protected static final int MAX_CONTENT_LENGTH = Integer.MAX_VALUE;
    @Autowired
    private SymmetricEncryptionBox symmetricEncryptionBox;
    @Autowired
    private AsymmetricEncryptionBox asymmetricEncryptionBox;

    private SecureRandom secureRandom = new SecureRandom();

    @Value("${enable_traffic_logging:false}")
    protected boolean enableTrafficLogging;

    @Value("${enable_symmetric_encryption:false}")
    protected boolean enableSymmetricEncryption;
    @Value("${enable_asymmetric_encryption:false}")
    protected boolean enableAsymmetricEncryption;

    public RPCommunication.Response getDecryptedResponse(RPCommunication.Response response) {
        byte[] encryptedResponse = response.getResponse().toByteArray();
        ByteString plainTextResponse = ByteString
                .copyFrom(symmetricEncryptionBox.decrypt(encryptedResponse));
        response = response.toBuilder().setResponse(plainTextResponse).build();
        return response;
    }

    public RPCommunication.Response getEncryptedResponse(RPCommunication.Response response) {
        byte[] plainTextResponse = response.getResponse().toByteArray();
        ByteString encryptedResponse = ByteString
                .copyFrom(symmetricEncryptionBox.encrypt(plainTextResponse));
        response = response.toBuilder().setResponse(encryptedResponse).build();
        return response;
    }


    public RPCommunication.Request getDecryptedRequest(RPCommunication.Request request) {
        byte[] encryptedRequest = request.getMethodArgument().toByteArray();
        ByteString plainTextRequest = ByteString
                .copyFrom(symmetricEncryptionBox.decrypt(encryptedRequest));
        request = request.toBuilder().setMethodArgument(plainTextRequest).build();
        return request;
    }

    public RPCommunication.Request getEncryptedRequest(RPCommunication.Request request) {
        byte[] plainTextResponse = request.getMethodArgument().toByteArray();
        ByteString encryptedResponse = ByteString
                .copyFrom(symmetricEncryptionBox.encrypt(plainTextResponse));
        request = request.toBuilder().setMethodArgument(encryptedResponse).build();
        return request;
    }

    public RPCommunication.Response getAsymDecryptedResponse(RPCommunication.Response response) {
        byte[] encryptedAesKey = response.getSecret().toByteArray();
        byte[] aesKey = asymmetricEncryptionBox.decrypt(encryptedAesKey);
        byte[] encryptedResponse = response.getResponse().toByteArray();
        ByteString plainTextResponse = ByteString
                .copyFrom(symmetricEncryptionBox.decrypt(encryptedResponse, aesKey));
        response = response.toBuilder().setResponse(plainTextResponse).build();
        return response;
    }

    public RPCommunication.Response getAsymEncryptedResponse(RPCommunication.Response response) {
        byte[] aesKey = new byte[32];
        secureRandom.nextBytes(aesKey);
        byte[] plainTextResponse = response.getResponse().toByteArray();
        ByteString encryptedResponse = ByteString
                .copyFrom(symmetricEncryptionBox.encrypt(plainTextResponse, aesKey));
        byte[] encryptedAesKey = asymmetricEncryptionBox.encrypt(aesKey);
        response = response.toBuilder()
                .setResponse(encryptedResponse)
                .setSecret(ByteString.copyFrom(encryptedAesKey))
                .build();
        return response;
    }

    public RPCommunication.Request getAsymDecryptedRequest(RPCommunication.Request request) {
        byte[] encryptedAesKey = request.getSecret().toByteArray();
        byte[] aesKey = asymmetricEncryptionBox.decrypt(encryptedAesKey);

        byte[] encryptedRequest = request.getMethodArgument().toByteArray();
        ByteString plainTextRequest = ByteString
                .copyFrom(symmetricEncryptionBox.decrypt(encryptedRequest, aesKey));
        request = request.toBuilder().setMethodArgument(plainTextRequest).build();
        return request;
    }

    public RPCommunication.Request getAsymEncryptedRequest(RPCommunication.Request request) {
        byte[] aesKey = new byte[32];
        secureRandom.nextBytes(aesKey);
        byte[] plainTextRequest = request.getMethodArgument().toByteArray();
        ByteString encryptedRequest = ByteString
                .copyFrom(symmetricEncryptionBox.encrypt(plainTextRequest, aesKey));
        byte[] encryptedAesKey = asymmetricEncryptionBox.encrypt(aesKey);
        request = request.toBuilder()
                .setMethodArgument(encryptedRequest)
                .setSecret(ByteString.copyFrom(encryptedAesKey))
                .build();
        return request;
    }


}

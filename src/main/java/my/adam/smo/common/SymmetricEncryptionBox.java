package my.adam.smo.common;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.OperationMode;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.util.SimpleByteSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.security.Key;
import java.util.Date;

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
@Component
public class SymmetricEncryptionBox {

    private AesCipherService cipher;

    @Value("${cipher_key: }")
    private String key;

    private SecureRandomNumberGenerator randomGenerator = new SecureRandomNumberGenerator();

    @Inject
    public SymmetricEncryptionBox(@Value("${aes_key_length:128}") int keyLength) {
        randomGenerator.setSeed(String.valueOf(new Date().getTime()).getBytes());
        cipher = new AesCipherService();
        cipher.setMode(OperationMode.CTR);
        cipher.setKeySize(keyLength);
    }

    public byte[] getKey(int length) {
        Key key = cipher.generateNewKey(128);
        return key.getEncoded();
    }

    public byte[] encrypt(byte[] key, byte[] plainText) {
        return new SimpleByteSource(cipher.encrypt(plainText, key)).getBytes();
    }

    public byte[] decrypt(byte[] key, byte[] encrypted) {
        return new SimpleByteSource(cipher.decrypt(encrypted, key)).getBytes();
    }

    public byte[] encrypt(byte[] plainTextKey) {
        return encrypt(Base64.decode(key), plainTextKey);
    }

    public byte[] decrypt(byte[] encryptedKey) {
        return decrypt(Base64.decode(key), encryptedKey);
    }
}

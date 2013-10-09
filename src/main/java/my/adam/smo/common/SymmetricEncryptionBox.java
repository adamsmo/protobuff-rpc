package my.adam.smo.common;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;

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
    private CipherParameters cp;

    @Value("${cipher_key: }")
    private String key;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        OpenSSLPBEParametersGenerator gen = new OpenSSLPBEParametersGenerator();
        gen.init(key.getBytes(), Base64.decode(key));
        cp = gen.generateDerivedParameters(256, 128);
    }

    public byte[] encrypt(byte[] plainTextKey) {
        byte[] out = plainTextKey.clone();
        PaddedBufferedBlockCipher encCipher;
        encCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()), new PKCS7Padding());
        encCipher.init(true, cp);
        encCipher.processBytes(plainTextKey, 0, plainTextKey.length, out, 0);
        return out;
    }

    public byte[] decrypt(byte[] encryptedKey) {
        byte[] out = encryptedKey.clone();
        PaddedBufferedBlockCipher descCipher;
        descCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()), new PKCS7Padding());
        descCipher.init(false, cp);
        descCipher.processBytes(encryptedKey, 0, encryptedKey.length, out, 0);
        return out;
    }
}

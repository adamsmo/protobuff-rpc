package my.adam.smo.common;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

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
    //    private CipherParameters cp;
    private SecureRandom secureRandom;
    private MessageDigest md;

    public static final int ivLength = 16;
    //must be at least long enough that after adding it to message, message will be at least 17 bytes long
    private final int seedLength = 16;

    @Value("${cipher_key: }")
    private String key;

    @InjectLogger
    private Logger logger;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        secureRandom = new SecureRandom(key.getBytes());
        md = MessageDigest.getInstance("SHA-256");

        OpenSSLPBEParametersGenerator gen = new OpenSSLPBEParametersGenerator();
        gen.init(key.getBytes(), Base64.decode(key));
    }

    public byte[] encrypt(byte[] plainText) {
        byte[] seed = new byte[seedLength];
        secureRandom.nextBytes(seed);
        byte[] seededPlainText = addSeedToMessage(plainText, seed);

        byte[] out = seededPlainText.clone();

        byte[] iv = new byte[ivLength];
        secureRandom.nextBytes(iv);

        CipherParameters cp = new ParametersWithIV(new KeyParameter(md.digest(key.getBytes())), iv);

        PaddedBufferedBlockCipher encCipher;
        encCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESFastEngine()), new ISO7816d4Padding());
        encCipher.init(true, cp);


        encCipher.processBytes(seededPlainText, 0, seededPlainText.length, out, 0);
        return appendIV(out, iv);
    }

    public byte[] decrypt(byte[] cryptogram) {
        byte[] out = Arrays.copyOfRange(cryptogram, ivLength, cryptogram.length);

        CipherParameters cp = new ParametersWithIV(new KeyParameter(md.digest(key.getBytes())), getIV(cryptogram));

        PaddedBufferedBlockCipher descCipher;
        descCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(
                new AESEngine()), new PKCS7Padding());
        descCipher.init(false, cp);
        descCipher.processBytes(cryptogram, ivLength, cryptogram.length - ivLength, out, 0);
        return getMessageWithoutSeed(out);
    }

    public byte[] getIV(byte[] cryptogram) {
        return Arrays.copyOfRange(cryptogram, 0, ivLength);
    }

    public byte[] appendIV(byte[] cryptogram, byte[] iv) {
        byte[] result = new byte[ivLength + cryptogram.length];
        System.arraycopy(iv, 0, result, 0, ivLength);
        System.arraycopy(cryptogram, 0, result, ivLength, cryptogram.length);
        return result;
    }

    public byte[] addSeedToMessage(byte[] cryptogram, byte[] seed) {
        byte[] result = new byte[seedLength + cryptogram.length];
        System.arraycopy(seed, 0, result, 0, seedLength);
        System.arraycopy(cryptogram, 0, result, seedLength, cryptogram.length);
        return result;
    }

    public byte[] getMessageWithoutSeed(byte[] cryptogram) {
        return Arrays.copyOfRange(cryptogram, seedLength, cryptogram.length);
    }
}

package my.adam.smo.common;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sun.security.rsa.RSAPrivateCrtKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.crypto.Cipher;
import java.security.*;

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
public class AsymmetricEncryptionBox {

    private KeyPairGenerator keyGen;
    @Value("${pub: }")
    private String pubKey;
    @Value("${prv: }")
    private String privKey;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public AsymmetricEncryptionBox() throws NoSuchAlgorithmException {
        keyGen = KeyPairGenerator.getInstance("RSA");
    }

    /**
     * decrypt with key configured in app.properties
     *
     * @param cryptogram
     * @return
     */
    public byte[] decrypt(byte[] cryptogram) {
        PrivateKey prvKey;
        try {
            prvKey = RSAPrivateCrtKeyImpl.newKey(Base64.decode(this.privKey));
        } catch (InvalidKeyException e) {
            logger.error("invalid key", e);
            return null;
        }
        return decrypt(prvKey, cryptogram);
    }

    /**
     * encrypt with key configured in app.properties
     *
     * @param plaintext
     * @return
     */
    public byte[] encrypt(byte[] plaintext) {
        PublicKey pubKey;
        try {
            pubKey = new RSAPublicKeyImpl(Base64.decode(this.pubKey));
        } catch (InvalidKeyException e) {
            logger.error("invalid key", e);
            return null;
        }
        return encrypt(pubKey, plaintext);
    }

    public byte[] decrypt(PrivateKey prvKey, byte[] cryptogram) {
        logger.debug("key class " + prvKey.getClass().getName());
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, prvKey);
            return cipher.doFinal(cryptogram);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public byte[] encrypt(PublicKey pubKey, byte[] plaintext) {
        logger.debug("key class " + pubKey.getClass().getName());
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            return cipher.doFinal(plaintext);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public KeyPair generateKeyPair(int keyLength) {
        keyGen.initialize(keyLength);
        return keyGen.generateKeyPair();
    }
}

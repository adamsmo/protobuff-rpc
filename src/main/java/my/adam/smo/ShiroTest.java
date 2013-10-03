package my.adam.smo;

import my.adam.smo.common.AsymmetricEncryptionBox;
import my.adam.smo.common.SymmetricEncryptionBox;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.OperationMode;
import org.apache.shiro.util.ByteSource;
import org.apache.shiro.util.SimpleByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
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
public class ShiroTest {
    private static Logger logger = LoggerFactory.getLogger(ShiroTest.class);

    public static void main(String[] args) {
        String secret = "Tell nobody!";
        AesCipherService cipher = new AesCipherService();
        cipher.setMode(OperationMode.CTR);
        cipher.setKeySize(128);

        //generate key with default 128 bits size
        Key key = cipher.generateNewKey(128);
        byte[] keyBytes = key.getEncoded();

        logger.debug("base64 key: \"" + Base64.encodeToString(keyBytes) + "\"");

        //encrypt the secret
        byte[] secretBytes = CodecSupport.toBytes(secret);
        ByteSource encrypted = cipher.encrypt(secretBytes, keyBytes);

        //decrypt the secret
        byte[] encryptedBytes = encrypted.getBytes();
        ByteSource decrypted = cipher.decrypt(encryptedBytes, keyBytes);
        String secret2 = CodecSupport.toString(decrypted.getBytes());

        //verify correctness
        logger.debug(secret);
        logger.debug(secret2);

        //another
        logger.debug("-----------------RSA-----------------");

        String s = "a tereaz jakaś gigantyczna wiadomość";

        byte[] ala = s.getBytes();


        try {
            AsymmetricEncryptionBox asb = new AsymmetricEncryptionBox();
            KeyPair kp = asb.generateKeyPair(2048);

            logger.debug("conf entries as below:");
            logger.debug("prv=" + Base64.encodeToString(kp.getPrivate().getEncoded()));
            logger.debug("pub=" + Base64.encodeToString(kp.getPublic().getEncoded()));

            ala = asb.encrypt(kp.getPublic(), ala);
            ala = asb.decrypt(kp.getPrivate(), ala);

            logger.debug("enc dec is same = " + Arrays.equals(ala, s.getBytes()));


            //spring
            ApplicationContext ctx = new ClassPathXmlApplicationContext("Context.xml");
            asb = ctx.getBean(AsymmetricEncryptionBox.class);

            ala = asb.encrypt(ala);
            ala = asb.decrypt(ala);

            logger.debug("enc dec is same with key from conf = " + Arrays.equals(ala, s.getBytes()));

            SymmetricEncryptionBox seb = ctx.getBean(SymmetricEncryptionBox.class);
            ByteSource bs = new SimpleByteSource(s.getBytes());

            logger.debug("enc dec is same with key from conf = " + Arrays.equals(bs.getBytes(), seb.decrypt(seb.encrypt(bs.getBytes()))));


        } catch (NoSuchAlgorithmException e) {
            logger.error("error", e);
        }

        //why sign random bytes insted of message - there could be
        // pattern in messages that could be spotted when encoding random bytes
        // and than using it as key it is safer,
        // encrypted message contains sha 256 of actual message before encryption
    }
}

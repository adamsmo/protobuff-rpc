package my.adam.smo;

import my.adam.smo.common.AsymmetricEncryptionBox;
import my.adam.smo.common.SymmetricEncryptionBox;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
public class EncTest {
    private static Logger logger = LoggerFactory.getLogger(EncTest.class);

    public static void main(String[] args) {

        //another
        logger.debug("-----------------RSA-----------------");

        String s = "a tereaz jakaś gigantyczna wiadomość";

        byte[] ala = s.getBytes();


        try {
            AsymmetricEncryptionBox asb = new AsymmetricEncryptionBox();
            KeyPair kp = asb.generateKeyPair(2048);

            logger.debug("conf entries as below:");
            logger.debug("prv=" + new String(Base64.encode(kp.getPrivate().getEncoded())), "UTF-8");
            logger.debug("pub=" + new String(Base64.encode(kp.getPublic().getEncoded())), "UTF-8");

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
            byte[] bs = s.getBytes();

            logger.debug("enc dec is same with key from conf = " + Arrays.equals(bs, seb.decrypt(seb.encrypt(bs))));


        } catch (NoSuchAlgorithmException e) {
            logger.error("error", e);
        }

        //why sign random bytes insted of message - there could be
        // pattern in messages that could be spotted when encoding random bytes
        // and than using it as key it is safer,
        // encrypted message contains sha 256 of actual message before encryption
    }
}

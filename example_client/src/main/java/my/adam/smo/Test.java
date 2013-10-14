package my.adam.smo;

import com.google.protobuf.InvalidProtocolBufferException;

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
public class Test {
    static byte[] err = new byte[] {8, 3, 18, 23, 12, -103, 15, -5, 21, 6, -55, 97, 75, -120, 49, 76, 48, 25, 69, -29, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err2 = new byte[] {8, 15, 18, 23, -7, 105, 118, 121, -65, -88, -121, 94, -60, 6, 81, -22, -74, -107, 38, 113, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err3 = new byte[] {8, 21, 18, 23, -107, -125, -22, -65, -87, -4, 5, 67, 22, -93, 93, -128, 47, 83, -37, -12, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err4 = new byte[] {8, 24, 18, 23, -115, -119, -6, 82, -88, -90, 90, -101, -25, -37, 45, -67, 53, -69, 69, -102, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err5 = new byte[] {8, 25, 18, 23, 98, 123, -56, 83, 33, -97, 60, 15, 59, -8, -4, -36, -73, 66, 36, -120, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err6 = new byte[] {8, 28, 18, 23, 54, -47, -25, -49, -71, -7, -32, 109, -108, 24, 3, -37, 97, -109, -86, -63, 114, 32, 104, 101, 108, 108, 111};
    static byte[] err7 = new byte[] {8, -79, 1, 18, 23, -29, -82, -47, -49, -33, -110, 120, -106, 76, -93, -26, -102, -76, -20, -100, 0, 114, 32, 104, 101, 108, 108, 111};
    public static void main(String[] args){
        try {
            Example.AwsomeAnswer.parseFrom(err);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err2);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err3);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err4);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err5);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err6);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        try {
            Example.AwsomeAnswer.parseFrom(err7);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}

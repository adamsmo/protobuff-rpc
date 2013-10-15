refero-rpc
==========

This project contains strong cryptography which is illegal in some countries (AES 256, RSA with 2048 bit key), 
you are using this software on your own responsibility.

Aim of this project is to learn how to use protocol buffer with netty as RPC. 
Concept is that netty is transport layer and protobuffs are used as serializer/deserializer of messages passed through netty.
Protocol buffers also provides way to call method from service by passing method descriptor obtained from protocol buffers.
Buffers also provides client side service stubs that generates that descriptors.

Another nice feature of using Protocol Buffers is that it provides blocking as well as non blocking way of calling methods.
Combined that with netty NIO (non blocking I/O) channels results in very scalable RPC solution.

I was trying to put all components in separate packages that it is easily recognizable what class belongs to what.

Everything is connected with spring context and annotation based configuration, which makes it ready to use with spring based applications as pluggable components.

For non spring applications threre is class Refero with srtatic factory methodes that can be used to create server and client side endpoints.

Folders example_* contains example usage of RPC.

Setup
-----

Required files for starting server with default logging framework (logback) and encryption are:
* `refero.properties`

  example file for server:
  ```
  server_worker_threads=30
  
  #AES conf
  enable_symmetric_encryption=true
  #long plain text password
  cipher_key=nr2ih4$%#$sdfgfghdl@#$DRRDEWewekj+#$_@#$'gfkdfkghiwrFDDS
  
  #rsa conf
  enable_asymmetric_encryption=true
  #base64 encoded server keys
  prv=MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCZmjtK8sFVLRg7BIoNzj4xUxSIWZAuECcdK4BdRPn1hcpa/dt1QuYbUr72spxTxkvCkAv4i+Y1UD70N0slU6FXAZLEJUGlmlZDekLHdBQOb5FBRhxnOYoJL0WxHow6Dq7r6sE7mOCNTb6CxHraxIF4dsmLOU6XCRPIFDpT7sfDT+NHvNy9c3RvSkJdMRv6qdRCuVG+KLnWaIFS0NU5denmYLcXr+xSum2lOG9wVc5NyaVCuIZuYAv8gn2qaQw4lUI/SOE5CaMmvKKrKzeaKUKeg9BwlpN2eCnkfLrDg31k61w0lFzik4P0gZfhNlAHD7/kgaMSJez0MhUNZcjsWgQhAgMBAAECggEAJRU1J10AC949MVJhOgAGx8W0+U4WHujLaKFt9haG+h14wwryhpbGEuahL9HjJ96Bv9Ei8rhjtY9QA4mt3K8aDnVUx4HvHVnrZZMIuDpv9SQ+PzH7/XfIuXruKlqjpAiTdmVQjWDVJYgVbGCfAF3cnAahlbkaHiCU5ALuKgFFvQMWecjk/YjG5/AoulC8n07CPdg2X1bds28y1lFujQO2ehCT2EF/pukaJwhbr6zEDrCaL00p3Kq0Tag9N72Rb0UmGMLE1z2nJhpAv3K4AE1GOt+zC/A+3G+y7oWVRdc7XVWLVj6+Vti5AoPpNJeISty2zPPuHVfXvBv6MKnIzj5QGQKBgQDTuAGArohO4NmxkpAzw2MHnwJ9+DCmkS/2CFmsxeeLuCh54SB0pq2vC/VBVQ39xEcDUc4Z16NVubHl0l2XC8OYuZaOod/5xsbGBVaWgpP/gxxeT3nf/0qxXXg32naGAOe/JR5LJNAUgyYA4kbQPdx03m21Webtc+4gXYWv+j23wwKBgQC5uoWhI9Ottzp7MyCufCYO73YrHsb345OA8YkrIUrwGGyhpSUqlXuPcq5YRWfriMFdTpvFQgq3WvqdWG/BNSBGX2aTBOM1JEQP8XbscSrSTDRA6mcduYn3Rym6i+kPQ2FqEH7UVB+IwrpxB4gHqMJk+1rYLgE82zvs+ZeFxaE6SwKBgCNdUgrvnF8omdRaefHbijgkrGLkSSQZjlxXars8V3/0I/avzx8NFGfv5GikT3D7Dy2TxYx1g5f3K6sK540qbuKc5XQvv9zWAi/jhmIfr573hI6QUfhDiXN0/Ha/7BpYHl3EUnoUzRkQCxIMue1g1+lBmiu3t6YkPjtzw7fwYczHAoGBAIxSs2Gi+ux10jaA04FhBTQlkgdUMonZpHk4hCVU+xRZr8GVT3ZUCE3CHJ1Omwxmf4mquaAYnJYZo927knvMUr04mPwdJjjhVJcjHXHYsry+nIArWYsowmdUmd96k7RXeUM0gU6U3FpsnYaGZS08QNsg2WsZnw/GqAkMLoIAZibvAoGBAKMXfIR6/3Kv4vbkTl7zsVASSIiJ8MFhsIQjT8p7p8QOS8k7oWAx4Et3im7yUoOxu8sHWZshQO4y3sDe052bjwz1CJZVkq/A6SyzXzoX3opOmtQ0lEZtqd3cPkY13392gm13xRix7ouoCQguAENeeX4G+XH0PXbwo1R+hQb5CUZU
  #base64 encoded client keys
  pub=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzd/d64TV7Pcsymggl1L0cpT786kzgK8+/AVc7h4iLmt5DWdJNUQwxi85sUQfef6mCLQzIAnRXoJ9IN034h1mq2926Lso6WiOt0KrnqNcw+8iCxsyS3zDpG5ObNzAuHiEmT79Rs5/TVJXTB6aBli7lsHf87J7F1NwohtBLK9M62sHTWz0/kKCBMP/X9fCES+XXOcMUl4EcJaJyWPIbBYwkPRPVkqgK0cP9R3gy5UFgfQ6WknW47IJhjz/69YMT5RUwtuFm/r4AA1VWN5PIVXJogaqs0Iw7STe07UZqn7ruuwU63Ma7JZ2u3qbzD5Go9R0W9FtblW67KWlvKbNg98erwIDAQAB
  
  enable_traffic_logging=false
  ```

  example file for client:
  ```
  client_worker_threads=50
  
  reconnect=true
  #in seconds
  reconnect_delay=10
  blocking_method_call_timeout=15
  
  #AES conf
  enable_symmetric_encryption=true
  #long plain text password
  cipher_key=nr2ih4$%#$sdfgfghdl@#$DRRDEWewekj+#$_@#$'gfkdfkghiwrFDDS
  
  #rsa conf
  enable_asymmetric_encryption=true
  #base64 encoded client key
  prv=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDN393rhNXs9yzKaCCXUvRylPvzqTOArz78BVzuHiIua3kNZ0k1RDDGLzmxRB95/qYItDMgCdFegn0g3TfiHWarb3bouyjpaI63Qqueo1zD7yILGzJLfMOkbk5s3MC4eISZPv1Gzn9NUldMHpoGWLuWwd/zsnsXU3CiG0Esr0zrawdNbPT+QoIEw/9f18IRL5dc5wxSXgRwlonJY8hsFjCQ9E9WSqArRw/1HeDLlQWB9DpaSdbjsgmGPP/r1gxPlFTC24Wb+vgADVVY3k8hVcmiBqqzQjDtJN7TtRmqfuu67BTrcxrslna7epvMPkaj1HRb0W1uVbrspaW8ps2D3x6vAgMBAAECggEAQ3N1YhTDgj/5jNdQ1kN3k5L8eXiWRzMjj66yI9H7oPnVT27nrWwKDuSqIkoPhgjInVFs7rJIxo46MOpJxOudF84NR3MKlG6NU72kPPWJDyjqHnlqoRDfiKAbUc+B1O6ngw4ALZK6Yrr3nxjnney0iVA777wqWBoebP5A/ELzEK4JJCGzYAyALgykvLwl8hP3OTY+o5pRd8kKAZ0IBgxeevv0Xq/Sw8swpjYaReaSe5iNDcLGwDG6HIQekOKQM3GOZn7cCS9Z/Jde2m4ocyBtf5ifFvY7byVJs41IBnHC9T0GB4VchYjHrC+DDVR/65/d2zoZSQsZhwd2aOwH5UySAQKBgQD95Q60hiRKUopioP7OwUL0FSsQWXa4Fqc8a7KGFGLDMmJ3L78YqgIY3HtOBnSziQiVbZJZMYYBUJ/mQ6ZpdZ5PxUFQALppAz6y6o2fChZRjVY5AtIukNRcc7E1Lb6UMYBWho3FkWnLsNhM4gp6fVfDg4HR5JSEPatUkjSoqZuxKQKBgQDPlOBzknHEisaM/cmoXVmkRIWGJ+oZZK7Nz67a6fYjpfr1+RgaKASgSKBHRtVAZYWs3IESEmed+h4c1xl+ueRylOhkDlXix5GlRVZgVnlz4kFv+jK/JiFJO38tH1pdLRfdn+VJBIGW89kQw8T/p3GDgKhB1+oBb6I3aJrEHxMUFwKBgAYm6rhNF8wFO/0v0XzJCgpICQcYY9wKEHtSUL0W+V2pduFUctjyJaLclNBLL8C57U9RnF406P5XKci2ku2zP+bG7NI3ckzhGz/SlyPnZqsLNa+j9dvS/pJO61hSh2+n02hFUmIbJ+kbm+IhCAf3yYyfPyIK295ocCJYeJYtpewhAoGBAIuQnkQ2HvW1FH+xFodzlBSk2V10sqm2Du1jiD25dftL2mrMuo7DfzDBA7pIfpKYb4LkkoczBqWph9t+J68TjuwXABRdqxMZeOw0rTmCipAGfFasUrhAC7swHHabEOd6rQQeQEuSkwqKEtR5u5bj3qc6tmXmpHeotdkpJH1eIzarAoGAMMr+kCdHbGIjQrTfowTU+C0SObLpVZxh1eFhXEehukHJmdM6s9T9Ey17Hstsnt9JMVx7OALZIsRiVTJASsRIGyRhtOw1rK6gscEGdhaaF36SzmcnNGPGqEhVfzqYRSSlBCgCxLvfEr1Gxzd3qWO9KNRQ6vtyvzAIxMZWBxTHdHM=
  #base64 encoded server key
  pub=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmZo7SvLBVS0YOwSKDc4+MVMUiFmQLhAnHSuAXUT59YXKWv3bdULmG1K+9rKcU8ZLwpAL+IvmNVA+9DdLJVOhVwGSxCVBpZpWQ3pCx3QUDm+RQUYcZzmKCS9FsR6MOg6u6+rBO5jgjU2+gsR62sSBeHbJizlOlwkTyBQ6U+7Hw0/jR7zcvXN0b0pCXTEb+qnUQrlRvii51miBUtDVOXXp5mC3F6/sUrptpThvcFXOTcmlQriGbmAL/IJ9qmkMOJVCP0jhOQmjJryiqys3milCnoPQcJaTdngp5Hy6w4N9ZOtcNJRc4pOD9IGX4TZQBw+/5IGjEiXs9DIVDWXI7FoEIQIDAQAB
  
  enable_traffic_logging=false
  ```
* `logback.xml`

  Example file:
  ```
  <configuration scan="true" scanPeriod="30 seconds">
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
          <encoder>
              <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
          </encoder>
      </appender>
  
      <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
          <file>logFile.log</file>
          <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
              <!-- daily rollover -->
              <fileNamePattern>logFile.%d{yyyy-MM-dd_HH:mm:ss}.log</fileNamePattern>
              <!-- keep 30 days' worth of history -->
              <maxHistory>30</maxHistory>
          </rollingPolicy>
  
          <encoder>
              <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
          </encoder>
      </appender>
  
      <root>
          <appender-ref ref="FILE"/>
          <appender-ref ref="STDOUT"/>
      </root>
  </configuration>
  ```

Config
------

For server/client configuration add to classpath of your application file named `refero.properties`, specified values are defaults.
String are passed without quotation marks.

* Client server threads count:
  + `server_worker_threads=10`
  + `client_worker_threads=10`

* If trying to reconnect after server went down, and intervals between reconection atempts for socket version of rpc:
  + `reconnect=false`
  + `reconnect_delay=100`

* Timeout to wait for blocking call to finish:
  + `blocking_method_call_timeout=100`

* AES configuration, `cipher_key` is specified as plain text password from which is deriverd 256 bits AES key by performing SHA256 on it:
  + `enable_symmetric_encryption=false`
  + `cipher_key=`

* RSA configuration, `prv` is own private RSA key stored with `PKCS8EncodedKeySpec` and Base64 encoded,
`pub` is public RSA key of other side stored with `X509EncodedKeySpec` and Base64 encoded. Default values results in encryption errors!
2048bit key pair can be generated by running main methode in class class `RsaKeyGen` which logs with debug level apropriate
string ready to copy past to configuration.
  + `enable_asymmetric_encryption=false`
  + `prv=`
  + `pub=`

* Netty trafic logging configuration:
  + `enable_traffic_logging=false`

Logging framework
-----------------

In this project i used slf4j with Logback as pluged implementation, if you wont to use difrent logging implementation
just exclude logback jars from project and add your own implementation.

Credits
-------
This project is using
* [spring](http://spring.io/)
* [bouncycastle](http://www.bouncycastle.org/)
* [logback](http://logback.qos.ch/)
* [netty](http://netty.io/)
* [protocol buffers](https://developers.google.com/protocol-buffers/docs/overview)

License
-------
The MIT License

Copyright (c) 2013 Adam Smolarek

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

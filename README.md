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

Config
------

For server/client configuration add to classpath of your application file named `refero.properties`.

* Client server threads count:
  + `server_worker_threads` default = `false`
  + `client_worker_threads` default = `false`

* If trying to reconnect after server went down, and intervals between reconection atempts:
  + `reconnect` default = `false`
  + `reconnect_delay` default = `false`

* Timeout to wait for blocking call to finish:
  + `blocking_method_call_timeout` default = `false`

* AES configuration, `cipher_key` is specified as plain text password from which is deriverd 256 bits AES key by performing SHA256 on it:
  + `enable_symmetric_encryption` default = `false`
  + `cipher_key`

* RSA configuration, `prv` is private RSA key stored with `PKCS8EncodedKeySpec` and Base64 encoded,
`pub` is public RSA key stored with `X509EncodedKeySpec` and Base64 encoded. Default values results in encryption errors!:
  + `enable_asymmetric_encryption` default = `false`
  + `prv`
  + `pub`

* Netty trafic logging configuration:
  +`enable_traffic_logging` default = `false`

Logging
-------

In this project i used slf4j with Logback as pluged implementation, if you wont to use difrent logging implementation
just exclude logback jars from project and add your own implementation.

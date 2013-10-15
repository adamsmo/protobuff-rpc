refero-rpc
======

Aim of this project is to learn how to use protocol buffer with netty as RPC. Concept is that netty serves as transport layer and protobuffs are used as serializer/deserializer of messages passed through netty. Protocol buffers also provides way to call method from service by passing method descriptor obtained from protocol buffers. Buffers also provides client side service stubs that generates that descriptors. Another nice feature of using Protocol Buffers is that it provides blocking as well as non blocking way of calling methods. Combined that with netty NIO (non blocking I/O) channels results in very scalable RPC solution.

I was trying to put all components in separate packages that it is easily recognizable what class belongs to what application part.

Everything is connected with spring context and annotation based configuration, which makes it ready to use with spring based applications as pluggable component.

netty-prtotbuff-server
======================

Aim of this project is to learn how to use protocol buffer with netty as RPC.
Concept is that nety is transport layer and protobuffs are used as serializer/deserializer of messsages
passed throught netty. Protocol buffers also provides way to call method from service by pasing
methode descriptor obtained from protocol buffers. Buffers also provides client side service stubs
that generates that descriptors.
Another nice feature of using Protocol Buffers is that it provides blockig as well as non blocking way of calling 
methodes. Combinde that with netty NIO (non blocking I/O) channels results in wery scalable RPC solution.

I was trying to put all components in separate packages that it is easly recognizable what class belongs
to what application part.

Ewrything is connected with spring context and anotation based configuration, which makes it ready to
use with spring based applications as plugable component.

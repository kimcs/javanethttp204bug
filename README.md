Build docker image:
`$ docker build -t javanethttp204bug .`

Run docker image and observe "TEST FAILED" at REQUEST 5 after little more than a minute.
`$ docker run -it javanethttp204bug`

This demonstrates a bug in java.net.http related to return status code 204.
It appears that we have to wait for a one minute timeout somewhere in the http (or lower-level part of) stack.

The test uses a java.net.URL instance to confirm that the problem is not the http server.

Here is a complete standard-out log dump from running `$ docker run -it javanethttp204bug`:
```text
Sep 25, 2018 8:10:45 AM org.xnio.Xnio <clinit>
INFO: XNIO version 3.6.5.Final
Sep 25, 2018 8:10:45 AM org.xnio.nio.NioXnio <clinit>
INFO: XNIO NIO Implementation Version 3.6.5.Final
Sep 25, 2018 8:10:45 AM org.jboss.threads.Version <clinit>
INFO: JBoss Threads version 2.3.2.Final


REQUEST 1

Client sending GET request expecting status: 200
Undertow sending response with statuscode: 200
Client received response with Status: 200,  Body: "Hello from Undertow, echoing statuscode 200"

Retrying request using URL client.
Undertow sending response with statuscode: 200

HTTP/1.1 200 OK
Connection: keep-alive
Content-Length: 43
Date: Tue, 25 Sep 2018 08:10:45 GMT

Hello from Undertow, echoing statuscode 200



REQUEST 2

Client sending GET request expecting status: 204
Undertow sending response with statuscode: 204
Client received response with Status: 204,  Body: ""

Retrying request using URL client.
Undertow sending response with statuscode: 204

HTTP/1.1 204 No Content
Date: Tue, 25 Sep 2018 08:11:45 GMT



TEST FAILED. Http request spent 60 seconds to complete (URL retry spent 0 seconds), GET with status 204
```

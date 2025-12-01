# Android System Design

**c4 model**
1. System context diagram - High level diagram which talks about the mobile app as single box and its dependecies - emphasis on user, backend, push provider, connected devices, email, mainframes etc. (focus on what is outside the mobile app, yet relevant for the solution).
2. Container diagram - Zoom into mobile app and show details within, such as image loader, DI, storage service, network service, presentation, relevant flows.
3. Component diagram - Module level details where each item is an independent entity which can be developed in isolation. This helps in providing the vision of how a team can work on the problem. Here talk about functionality and divide each into modules contains repo, use cases, view models etc.

## UI patterns
### MVVM 
### MVP
### MVI

Usecase - If I talk about it, clearly understand what it stands for.
M, V, VM should be OK. So is Repo. Once you go to Network and storage layer, build more expertise

## UI level
Recycler view - need details about pagination, pre-loading etc.
Image, Avatar rendering,

## Image / Media handling
Need details on how to go about this area
How to render this should be learnt.

## Attachments
How to handle attachments. 
Client + Server solution
Retrys
How to quickly and reliably let the user go after attaching

## Pagination
Pagination for a recycler view - client side
Getting paged data from server side - strategies and tradeoffs

## Deeper Android
Lifecycle
Lifecycle components
Jetpack libraries - most important ones
Offline
Intent and Intent filters
Pending intent
Permissions management
Battery life 
Location
Camera
Bluetooth?
Network / wifi & 4G
Broadcast receivers
### Work managers
### Services - Bound and unbound
### APK structure
- Bundles / flavors etc
### Binder / IPC
How to build a separate telemetry / logging APK that can be reused as a service across multuple apps?
### Doze mode
Doze mode is an aggressive battery saving state introduced in Android 6. Doze mode progresses from Light to Deep. First when screen of off - no doze yet. Device is stationary -> light doze mode. Then after some time it enters deep doze where CPU sleeps most of the time, network is blocked, work deferred and only maintenance window work happens every 10 - 30 mins (increasing gaps). During this time Android wakes up breifly, runs pending jobs, delivers queued notifications, allows app to sync, re-enters doze mode.

Foreground apps, services, high priority push notifications FCM, System apps such as clock etc are only allowed. There is a maintenance window concept when background tasks are allowed in a short burst every few minutes. Alarms set using `setAndAllowWhileIdle()` kind of API are allowed.

## Offline support
- Database - SQLite (other options?)
- Shared prefs
- Disk storage (files)
- Key store or secure storage

When to use what
Need details about schema, primary keys, relations, indices
How to encrypt DB? - Specific columns or entire DB?
Encryption methods - Key based - what key to use, how to encryt and decrypt - What algo to use etc.


## Security
How to login a user
How to store password
OAUTH and stuff

## Privacy
How to implement e2e encryption
How to encrypt data base, what to encrypt. 
What access levels are possible to storage such as secure, disk, db etc.

## Networking
Design of backend end points and data exchange should be clear.

### HTTP based REST
[HTTP explored in a later section entirely](#http). REST uses HTTP.

Although HTTP is action oriented (GET, PUT, POST etc), REST is resource oriented. `/users/{id}`, `/payments` etc. REST can work on a variety of payloads such as JSON, XML, YAML, HTML, protobuf etc. 

_REST over HTTP is stateless_. This means that every request should contain context such as auth token, pagination details, resourse identifiers. REST uses HTTP headers and error codes. REST does not support stream, push or bidrectional comms. 

_REST cannot provide granular data fetch_ like GraphQL, so most of the time you fetch more data than you actually need depending on the API design. So if you need to know if something changed or updated, you may end up calling the API and downloading the whole payload even if its unchanged. 

_This can be avoided by using techniques such as `eTag`_. `eTag` is simply a hashcode that is returned for a specific response. Client can then send this back in `If-None-Match` header. Now server may respond with `304 Not Modified` or with new data and a new `eTag`. This can be used for implementing optimistic concurrency for updates as well. Suppose `eTag: v2` was sent by server. Client can then say `PUT order/123, If-Match: v2`. If the version does not match, error `412 Precondition Failed` is recieved.

REST also provides `Cache-Control` header which specifies if the response can be cached by the client / CDN or cannot be cached. 

Idempotency is a key constraint in REST. Client may end up calling the same API multiple times in case of retry, network loss or failure recovery. In such cases, client should ensure that they do this only for idempotent cases. 

REST depends on HTTP semantics:

| HTTP Method | Purpose / Behavior| Idempotent? |
|-------------|-------------------|-------------|
| **GET** | Read-only | ✔ Safe + Idempotent |
| **PUT** | Replace entire resource | ✔ Idempotent |
| **DELETE** | Remove resource | ✔ Idempotent |
| **PATCH** | Partial update | ❌ Not necessarily idempotent |
| **POST** | Create / action / side-effects | ❌ Not idempotent|

_What does it mean to be PUT and DELETE to be idempotent?_ It is idempotent from a single client's perspective. Even if client calls PUT or DELETE on same resource with same payload multiple times, the state on server is exactly the same. But if there are two clients PUT ing same resource, the last one wins.

_POST is not idempotent, but there can be cases you have to retry_. How do we handle this situation? Say we have payment of 100 made. If we send this mutliple times, it will end up adding as many payments. This can be solved by using an `Idempotency-Key`. Idea is similar to `eTag` but this time, client owns the hash. 

_REST uses rate limiting and throttling headers_. They provide error code `429 Too Many Requests` and also `Retry After` header

### GraphQL
_GraphQL is a query language based backend that provides data in the format that the client wants_ using a single structured query. Instead of separate REST calls to fetch data, client can use a single end point and send a query on what data it wants.
```
GET /user/123
GET /user/123/friends
GET /user/123/photos
```
to
```
POST /graphql
{
  user(id: "123") {
    name
    friends {
      name
      avatar
    }
  }
}
```

_GraphQL works on top of HTTP_. It uses JSON payload for communication.

_GraphQL exposes a typed schema_. It supports querys for reading data, mutation for updating data, resolvers that are shortcuts to get to join data and subscriptions that are realtime updates over websocket.

At the server side, GraphQL parses the query that client sends, runs resolver functions per field and then returns the fields that client requested.

From Android standpoint, it simplifies the network stack as there is only one end point, fetches exactly what data is required, batches the fetch compared to REST end points, provides a strong typed result and provides offline capability OOTB using local persistence and optimistic updates.

To integrate GraphQL in Android app, integrate `apollographql` dependency, define `.graphql` query files. Apollo generates kotlin models after which use `ApolloClient` to connect to server and execute query, get the result. Internally Apollo handles caching, retries, OkHttp integration.

### Websocket
[Explored in real time comms.](#websocket-1)

### gRPC
[Explored in real time comms.](#grpc-1)

## Real time updates

|      Technique            |     Type / Notes          |
|---------------------------|---------------------------|
| FCM Push Notifications    | Server → device push using google services |
| Short Polling             | Repeated periodic HTTP    |
| Long Polling              | Long lived HTTP request, client initiated, keep alive and high read timeout |
| WebSockets                | Full-duplex persistent connection, HTTP initiated as upgrade, converted to 2 way binary stream on TCP, ping pong heartbeat. |
| Server-Sent Events (SSE)  | One-way server → client, client initiated, kept alive using heartbeat, client reconnects when dropped, client listens to updates. |
| gRPC Streaming            | HTTP/2 streams, bi-dir    |
| MQTT                      | Lightweight pub/sub       |
| GraphQL Subscriptions     | Real-time GraphQL events  |
| WorkerManager periodic    | Polling when app stopped  |
|    **Not explored** |       
| Socket.IO                 | WebSocket + fallbacks     |
| PubNub / Ably / Pusher    | Hosted real-time channels |
| Firebase Realtime DB      | Built-in real-time sync   |
| Firestore Listeners       | Built-in change streams   |


### Push Notification (FCM - Firebase cloud messaging)
When an Android app is stopped or killed, it cannot maintain background connections. In such cases, it needs to rely on push delivery mechanism for notifications. This happens through FCM which is Google's push service.

```
Your Server  →  Firebase Cloud Messaging (FCM)  →  Google Play Services  → Android System  →  Your App (if needed)
```

When user installs or opens the app, app calls `FirebaseMessaging.getToken()`. Google play service generates unique FCM token for that app+device. App sends this token to backend server. Token can change whenever user reinstall, clear data, update, or due to security rotation. App should update the backend whenever a new token arrives in `FirebaseMessagingService#onNewToken()` callback.

Backend can send notification through FCM using this token as a _normal or silent notification_, and have payload and priority attached to it. Server posts to FCM using HTTP v1 API with OAuth2 token provided by Google IAM, along with the app+device token. FCM then stores the message and determines the device, user, country, connection status, device doze mode, app restrictions etc. Most android devices maintain persistent connection with FCM. This is single connection per device for all apps. (XMPP over TCP). When device is online and criteria are met, a notification message is delivered. 

There are 2 types of notifications: Normal Notifications, Silent Notifications or data messages.

**Normal notifications** show up in the notification bar in Android System UI **without waking the app**. These notifications have fixed structure, and limited configuration. On tap it can wake up the app or launch the app or do some custom actions. Notifications can trigger custom actions by defining a `PendingIntent` to start service, boardcast reciever or activity. Some examples are play/pause button which sends broadcast to `MusicControlReciever` or `Mark as read` button which triggers a background service call without launching any UI. 

**Silent notification** are delivered only if:
- app is in foreground or 
- app in background and notification is high priority (if app is abusive and it sends many high priority messages, wakes up app and runs lots of background jobs, drains battery, its silently marked as abusive and high priority notifications are automatically degraded)  
- device is not in doze mode 
- or device is in doze mode and app is high priority (app being high priority is a heuristic based decision that Android does based on recent user interactions, foreground services, media usage, location usage, whitelisted background processes etc.). 
    
For silent notifications, android may temporarily wake the app in background. Silent notification is delivered to the app callback `FirebaseMessagingService#onMessageReceived`. App can use this time to update local DB, sync data or show custom UI. This is short wake time (~10 seconds) and no long running tasks allowed. Heavy work should be offloaded to a workmanager. App can build a custom notification with cool UI, expanders, custom actions etc and then post it also, so that user can see it as a normal notification.

When app is woken up in background, if it needs to do some sync or network calls, it is better to use work manager. Using background service is not allowed unless in very special cases. Work manager is scheduled and run when Android gives it a chance. So its the right way. App may be able to start a Foreground service. This can work only because app is _given a **temporary foreground** status when a high priority FCM data message arrives_. All work should be over in 10 seconds. Usage of foreground service should follow the guidance of showing foreground notification within 5 seconds of lauching the service. This option is given so that apps can do some realtime message sync, process VoIP calls, urgent health / safety alerts, download data needed for displayed notification. 

Delivery of notifications are not guaranteed. Its best effort. It may be delayed, dropped and availabilty of network at the device. FCM uses adaptive delivery. Notifications are throttled if too many silent notifications per hour, too much background work is triggered, user rarely opens the app, app is battery-heavy, device is on low battery or idle, device is in doze mode. Rough heuristics suggests that 10-20 silent notifications per hour is fine, 50 to 100 may trigger throttling, 200+ will surely result in severe throttling and blocking of background wakeups. Repeating silent notifications to wake the app is discouraged.

FCM server has rate limits per API call. For example `/v1/messages:send` has ~600 QPS per project (varies). Play store policy is the biggest deterent. Sending promotions or spam notifications, irrelevant, too frequent, deceptive pushes will result in suspendng the app and restricting the FCM usages.

#### Topic-based notification
Topic based notifications is a single push message to millions of devices at once. Topic can be something like "weather" or "score" or "global-alerts". Devices can subscribe to these topics and server can send one message to all subscribers. There is no token list, user segmentation or other complications. Topics are global so they can collide. In such case every subscriber recieves every message on the topic. Android app can subsribe thus:

`FirebaseMessaging.getInstance().subscribeToTopic("news")`

Topics are generic and anyone can subscribe to it. So it should never contain userId, email, private info or secrets. Topics can also support normal and data messages.

### HTTP Short polling
This is client specific implementation where client keeps polling server in some interval. A typical implementation is 2s or 5s. 
Server immediately responds, if there is an update or not. 
Server load will be high in this case, client battery drain is higher.

This can work with _legacy systems_ as it does not need any specific support from server side.
This can be used in situations when other realtime updates like notifications / websockets / long polling are not feasible.

### HTTP Long polling
In this model client makes a request, server may respond immediately if it has an update. Else it holds the connection and responds when a response is available or the request times out. Server load is less than short polling. But it is still significant as the connection has to be kept live.

Once the response arrives, client initiates another long poll request.

This works the same with HTTP/1 or HTTP/2 because the concept remains the same. No change in client or server side specific to HTTP protocol supported. With HTTP/2 we can have multiple long polling requests in a single HTTP/2 TCP connection.

#### How long does the server hold the connection?
This is mostly server implementation dependent. There is a timeout associated which client can configure. Typically this is anywhere from 15 to 60 seconds.

In case of Android, we can use network library which can set timeouts like `Read timeout` which can be increased to let the server know that client is willing to wait. HTTP header `keep-alive` should also be used. 


At the server side, there can be intermediate layers which support idle connections also like CDNs or API gateways. _Server has to implement the logic to wait until data is available instead of flushing the response prematurely_. Here server needs to implement waiting strategies involving non blocking IO and it should support sufficient number of concurrent connnections. Some stregies to hold connections is _event loop in Node, Async controllers in Java + Spring boot or similar async frameworks in other languages_.

### Websocket
Websockets provide full duplex (back and forth), persistent, low-latency connection between server and client. It works on messages rather than request / response. Its like upgrading HTTP to a pipe where both server and client can talk freely. 

Websockets starts off as an HTTP call. After server responds, they switch protocols. Once websocket is established, there is no more HTTP. 

First client sends an HTTP request with and `Upgrade` request header to the server, to indicate it wants to switch to websocket. It also passes websocket key and version. 
Server responds back with an accept `HTTP 101` code. After this comms are using websocket frames, using a bidrectional TCP connection.

Websocket communicates using _binary frames_. Websockets are long lived TCP connections. It is kept alive by a heart beat once in 30-60 seconds which server sends ping and client responds with a pong.

Websocket initialization cannot happens using HTTP/2 as it does not support `Connection: Upgrade` functionality which is the core of establishing websocket connection. 

As with HTTP/3 there is a new protocol called `WebTransport` whch is designed for this usecase.

In Android, all popular network libraries like OkHttp, Ktor, WebView etc support websocket.

As for server side, it should also support websocket connections and implement the communication specific to the functionality. Server should maintain connection state, decode/encode frames, handle concurrency, broadcast messages, manage connection lifecycle.

### Server sent events
These are unidirectional, lightweight, simple way for server to push events/messages to the client over HTTP (not using WebSockets). Typically used for Live score, stock updates, notifications, chat message updates, streaming logs etc. 

Client opens a normal HTTP request, server keeps the connection open forever, server sends events as plain text, client recieves and updates automatically.

```
GET /events HTTP/1.1
Accept: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

Server sends periodic heartbeat messages which are ignored by the client. If client is disconnected, it automatically reconnects. 

SSE works well with HTTP/2 because HTTP/2 is by default multiplexed. So SSE streams are cheaper as it does not need additional connections.

Unlike browsers which have built-in support for SSE, Android apps do not. They need to use an OkHttp connection and keep reading the incoming messages continuously. 

Similarly the automatic reconnection does not work in Android. Its supported by browsers. In Android when connection breaks, we should implement retry, reconnect logic and include last-event-id in the header.

Although SSE transmits plain text, when using HTTPS its encrypted by TCP/TLS layers. SSE is nothing but an HTTP call.

### gRPC

gRPC stands for google RPC. It is a high perf, strongly typed, binary RPC framework created by Google. It is designed for microservices, mobile to backend comms, low-latency real-time systems.

gRPC uses protocol buffers (protobuf) to define API schema, serializing data to compact bnary and generating strongly typed client/server code.
This is similar to old style RPC where marshalling and unmarshalling is done across client and server.

gRPC uses HTTP/2 under the hoods including mutiplexing, header compression, persistent connections and bidirectional streaming features.

gRPC makes network calls look like normal local function calls (similar to skeleton and stub concept in RPC). 

gRPC suppots 4 modes of communiacation. 
    - Unary - single request response model
    - Server streamin - where client sends one request and sever streams multiple responses. Useful for live status, log stream, continuous results, info notifications.
    - Client streaming - Client sends a stream and server replies once. Used for file upload, batch upload logs etc. 
    - Bidirectional streaming - Both sides send streams independently. Useful for chat, live multiplayer gane, real-time collab etc.

Android uses Java/Kotlin gRPC library. Define `.proto` file, add gradle plugins. This generates stubs for RPC methods and then use this for making calls. 

Since gRPC uses HTTP/2 internally, it is encrypted (TLS), multiplexed, binary efficient protocol (faster than JSON REST), provides strong typing and streaming support all in one.

gRPC currently uses HTTP/2. gRPC over HTTP/3 is evolving, but not yet common in Android.

gRPC (API layer)
↓
HTTP/2 (application)
↓
TLS (presentation & session = encryption)
↓
TCP (transport)

### MQTT
Message Queing Telemetry Transport. This is a lightweight, pub-sub messaging protocol built on top of TCP, designed for real time communication, especially over unrealiable and low-bandwidth networks. This is widely used on IOT devices, sensors, chat apps and mobile apps.  In this the client connects to a central broker and publish or subscribe to named topics for real-time message delivery. MQTT scales to millions of connections 

MQTT works well on low power devices, unstable networks, intermittent connectivity, minimal bandwidth. MQTT packets are as smallas 2 bytes. There are no headers, no additonal TLS, no cookies, no req / resp. Its just one long TCP session. Only limited packet types and minimal fields. MQTT broker maintains the client id, subscription list, QoS state and pending offline messages. The client can reconnect instantly after a drop. It scales horizontally by adding more brokers and each broker handling thousands of TCP connections, highly optimized IO, fan-out optimization etc. Other standard geo distributes clustering strategies can be used.

MQTT has its own protocol written on TCP, and it has a broker which recieves messages, routes them to subscribers and maintains client session. It also provides QoS of 0 - at most once, 1 - at least once or 2 - exactly once (most costly) guarantees.

#### How to use MQTT in an Android chat app?
MQTT works well when app is in foreground. But must have fallback mechanism when app goes to background.

Initially app connects to MQTT broker and subscribes to topics such as `chat/<userid>/messages` and `chat/<chatid>/events`. Publishes outgoing messages to `chat/<chatid>/send` This works great.

When app goes to background, TCP connection is killed during doze mode. App cannot keep MQTT running, broker will see client disconnect. messages will queue up. So MQTT should pair up with FCM to push silent notifications to periodically connect back to the broker and sync messages (and post a custom notification). There is an interesting concept called `Last will message`. This is registed by the client on the broker when it connects. When broker finds a client has dropped, it publishes that `Last will message` to some topic like `presence/<userid>/status`. Backend can subscribe to this and determine when to send the FCM push.

MQTT message are kept small. Mostly < 64kb. They do not have large files or images, instead they contain metadata. App users HTTPS to get to the attachment and files when needed.

User uploads media → Backend stores in S3/CDN → MQTT publishes message metadata → Receiver gets MQTT → App fetches media via HTTPS

MQTT support TLS, so end to end communication is encrypted. MQTT supports TLS based auth using certificates as explained in TLS 1.3 in HTTP section. It also supports mutual TLS where client can also send a certificate which is verified by the broker. Second way is usoing username and password. Password can be text which is discouraged, can be JWT token or OAith token that is short lived. It can also do token based auth. Once auth'ed broker creates a clientid and creates a persistent session by using a heartbeat. For network drops, MQTT does a TCP fast reconnect by using minimum formalities.

### GraphQL subscriptions
See GraphQL section for basic details.
A sample subscription for a chat whenever a message is added to chatId 123 is as follows:

```
subscription {
  messageAdded(chatId: "123") {
    id
    sender {
      name
    }
    text
  }
}
```

Android client simply subscribes to the subscription object generated from the query json and collect on the flow. 
GraphQL uses WebSocket or SSE for subscriptions. It uses one websocket per app. This allows bidirectional messages. Client can start and stop suscription. Server pushes whenever there is an update.

The limitation here is that this works only when app is in foreground.  

### BLE Notifications

BLE stands for Bluetooth Low Energy. This are really low cost way to push events from a BLE device. Android app can connect to BLE device and subscribe to a characteristic. BLE pushes a notification when its value changes. App recieves `onCharacteristicChanged()`.

If one wants to access BLE device from their app, they should start off with asking for permissions to scan and connect bluetooth and location. WIth this app can use `BluetoothAdapter` and related API along with `LocationManager` to scan for BLE devices. Once device is idenfied use `connectGatt` to connect and then impelment a callback `BluetoothGattCallback` to recieve callbacks on connection, service discovery, read,write and notification events.

### Workmanager periodic workers


### Summary of network stack
| WebSocket                    | SSE      |        GraphQL       | gRPC         | MQTT                      |
|-|-|-|-|-|
| HTTP/1 only for upgrade,<br> TLS + TCP <br> No HTTP/2 (No upgrade) <br> No HTTP/3 (has WebTransport)  | HTTP1/2 | HTTP 1/2/3 <br> WebSocket or SSE for subscription | HTTP2 only | TCP - Implements own TLS |

## HTTP

HTTP is hyper text transfer protocol. It is an application layer protocol used by the web. 
HTTP/1 is textual protocol, HTTP/2,3 is binary protocol. It is stateless and request response based.
HTTP is not directly responsible for reliability, ordering, encryption etc. It uses underlying TCP/TLS or QUIC over UDP to achieve this. 
HTTP is a language spoken between apps. It defines a request and response format. 

Request format:
GET /profile HTTP/1.1
Host: example.com
User-Agent: Chrome
Accept: application/json

Response format:
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42
{"name": "Anand", "role": "Android dev"}

HTTP supports several methods: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS etc
HTTP has several statys codes: 2xx success, 4xx client error, 5xx server error.

HTTP/2 (application)
↓
TLS (presentation / encryption)
↓
TCP (transport)
↓
IP (network)

### HTTP/1.x vs HTTP/2?
HTTP2 supports multiplexing. Which means that in single TCP connection, we can send multiple requests. 
HTTP2 provided support for Server push, which is now deprecated. This allowed server to push to client without request. But this had inherent issues over resource consumption.
Lesser and faster shake hands. Also requests does not block on each other. 

For Android, OKHttp is multiplexing enabled by default. This can be disabled explicitly by setting protocols while creating the client to use only HTTP_1_1

Every HTTP1 connection needs to open TCP connection. Browsers work with 6 to 10 TCP connections in parallel, OKHttp has 5 as default. But more TCP connections results in more handshakes, more memory consumption. 
HTTP2 connection works on single TCP connection and then multiplexing inside that. HTTP/2 does this by switching to binary format from text in HTTP/1. Internally it splits comms into streams, each stream contains messages, and each message contains frames. TCP does not know about this. HTTP2 does this at both ends. So if there is a packet lost, HoL kicks in for TCP and all streams are affected. It uses ALPN to avoid extra round trip to determine the protocol for communication.
HTTP3 is coming up with QUIC protocol (which itself runs on UDP) which removes TCP level HoL also. 

#### Head of Line blocking (HoL)
A previous slower request does not hold a following quick request at ransom. They happen in parallel. The phenomenon where requests wait is called "Head of Line" blocking. For HTTP/2 HoL is a non issue in HTTP layer. In TCP layer HoL still happens. This is significant advantage for HTTP/2 because TCP layer HoL happens only when packets are lost. TCP delivers a byte stream. If some packet is lost, then recieving side must wait. But later packets already arrived, they are stuck behind the missing packet.

### About ALPN
Application Layer Protocol Negotiation. This helps in agreeing upon which protocol to use (like HTTP/1.1, HTTP/2, gRPC etc.)
Without ALPN, client sends a spcial upgrade header in first request and from that server understands. This needs extra round trip. It could not work with encrypted HTTP/2 as server could not peek inside TLS.

In ALPN, in the initial request itself client sends a list of protocols that it can support ["http/1.1", "h2"] and server picks one and reponds when it sends the public key and it is settled.

### About TCP
TCP is a low level network protocol for communication. It starts with 3 way handshake (SYN (c to s)->SYN-ACK (s to c)->ACK (c to s)). This ensures that the both sides can send and recieve. TCP is stateful, reliable, ordered and deterministic connection. Both client and server maintains state. The packets are transmitted in a certain order and assembled in same order at reciever side. Missing packets are re-transmitted and uses TCP HoL blocking while missing packet arrives.
Used by all HTTP, TLS, WebSocket, Git, SSH etc.

Reliability and retransmission happens based on the ACK signal that client sends. If server did not recieve the ACK with the seq num, then it sends the packet again.

TLS is an encryption layer on top of TCP. The stack is HTTP -> TLS -> TCP -> IP

### About UDP
UDP is a stateless, fire and forget kind of connection. There is no inherent reliability or order guarantees. This means UDP can be much faster, and useful where packet loss is not a major concern. Useful in WebRTC, Video streaming, VoIP, Games, DNS lookup etc.

### About TLS
Transport Layer Security gives HTTPS its 3 guarantees: Encryption (attacker cannot read data), Integrity (attacker cannot modify data), Auth (communication is from a real server and not a fake one). TLS runs on top of TCP.

#### TCP + TLS 1.3 handshakes
TCP needs 3 way handshake SYN (c to s)->SYN-ACK (s to c)->ACK (c to s)
TLS needs Client (random + ALPN) to Server -> Server (random + protocol selection + new asymmetric public key / certificate encrypted with long term public key) -> Client (after validating with root CA or pinned CA, secret random encrypted with public key) -> Server and Client (Finished encryption) 
This can be summarized as ClientHello -> ServerHello -> Secret sharing -> Finished encryption.

#### Symmetric and Asymmetric keys
Asymmetric keys are public, private pairs. Private key is the key that can open the mailbox and public key is like the location of the mailbox. This method is slow, but security is very strong. This is typically used for exchanging keys or verifying identities. Clients use this to encrypt their pre-master secret (which is a symmetric key). 

A symmetric key is when both sides use same secret key. Whoever has key can encrypt as well as decrypt. This is very fast and has strong securty (AES-128, AES-256) and is used for bluk data encryption.

#### TLS record protocol
Client starts comms by sending supported TLS version, cipher suites, client random number, ALPN (HTTP protocol negotiation)
Server sends TLS certificate chain, public key, cipher selection, server random number.
Client has to decide if certificate is trustworthy using client validation. It does so using domain match, expiry, chain or trust (server cert -> intermediate cert -> root cert). Root cert must already exist in client's trust store. It also checks the certificate signature. If certificate is modified, then signature becomes invalid. (sort of a SHA?)

Android uses system trust store (OEM managed) or app-added certs to store the root cert.

After cert validation, client generates a pre-master secret (random number). Client encrypts with server's public key (asymmetric key) and sends. Only server can decrypt as it has private key. Now both sides have server random, client random, pre-master secret. Using these values, both derive a session key. Both exchange readyness that they are now encrypted. From this point onwards all communication is encrypted using the pre-master secret (symmetric key).

```
Client Hello  ——>  (supported ciphers, client random, SNI)
              <——  Server Hello (server random, chosen cipher)
              <——  Certificate (server public key)
              <——  Certificate Verify
Key Exchange (client encrypts pre-master with server public key)
             <——/——>  Derive symmetric session keys
Client Finished ——> (encrypted)
Server Finished <—— (encrypted)
             <——/——>  Encrypted HTTP payload

```             
#### Prefect forward secrecy (PFS)
This is supported in TLS 1.3 which generates a private key at the server per session. This way old private keys, if leaked somehow, cannot be used to decrypt old encrypted comms.

There are 2 set of keys. One is a long term key and one is ephimeral pair to exchange secrets. The ephimeral one is generated every handshake. So what actually happens is that server sends the long term public key and an ephimeral public key which is encrypted with the long term public key. So that client knows its server who is sending this ephimeral public key. These session keys are the ones destroyed after each session. The ephimeral key only serves to exchange secret from client to server. After that all comms use symmetric keys (secret)

### About QUIC
QUIC protocol is used with HTTP 3. Its a transport layer developed by Google. Its a modern replacement for TCP + TLS, built on top of UDP. It provides key features like Multiplexing, congestion control, encryption, faster handshake, anti-HoL blocking, connection mitigation.

QUIC works on a connection ID and does not depend on IP address, port or network interface. This way switching network from wifi to 4G does not affect QUIC communication.

HTTP/3 has semantics over QUIC instead of TCP. This is supported in modern browsers and CDNs. For Android support is experimental in OkHttp. Supported for gRPC over HTTP/3. Apps like Search, Gmail, Youtube uses QUIC.

#### How QUIC does TLS 1.3 in 1 RTT?
QUIC integrates TLS 1.3 inside its handshake, exact 3-4 steps, but done in 1 round trip time (RTT)! Faster 0-1 RTT handshake as compared to 2 RTT for TCP.

The reason is that in case of TCP, its own handshake takes 1 RTT, at which point TLS is blocked. Client cannot send ClientHello unless the TCP handshake is done. But in case of QUIC, TCP handshake is avoided.

Client -> QUIC Initial (contains ClientHello)
       <- QUIC Initial + Handshake (contains ServerHello) **1 RTT**
Client -> Finished + app data

In the case of resumed connections, its 0 RTT because when client already has established a server session and is holding the session details, it can simply reuse it and send the data in the first request when it resumes. 0-RTT is not secure, it can be replayed by attackers. Most secure systems disallow 0-RTT.

#### How does QUIC work for HTTP which is reliable and order guaranteed protocol?
QUIC uses UDP which does not have any guarantees on delivery or order. QUIC implements ordering, reliable delivery, congestion control etc itself without the constraints of TCP. For example QUIC retransmits frames and not entire packets which are more efficient. HoL is avoided because QUIC support multiplexing and when data in one stream is lost, only that stream waits. Other communication continues. So, it narrows down the block to specific communication stream than blocking everything.

#### So why we can't get TCP fixed? 
TCP lives in the kernel and changing that takes years. QUIC is in user space and does not need kernel changes. Means QUIC is implemented in the application layer and not in kernel layer. It just uses UDP functionality from the kernel.

### HTTP Layering

+------------------------------------------------------------------------------------------------------------------------+
| LAYER / STACK             |            HTTP/1.1               |             HTTP/2               |       HTTP/3        |
|---------------------------+-----------------------------------+----------------------------------+---------------------|
| Layer 7  Application      |  REST, JSON APIs, GraphQL, HTML   |  REST, gRPC, GraphQL, HTML       |  REST, GraphQL      |
|                           |                                   |  (gRPC requires HTTP/2)          |                     |
|---------------------------+-----------------------------------+----------------------------------+---------------------|
| Layer 6  Presentation     |  TLS (HTTPS)                      |  TLS (HTTPS)                     |  TLS inside QUIC    |
|                           |  separate from TCP                |  separate from TCP               |  (NOT separate)     |
|---------------------------+-----------------------------------+----------------------------------+---------------------|
| Layer 5  Session          |  HTTP/1.1 connection + keepalive  |  HTTP/2 Streams (multiplexing)   |  QUIC Streams       |
|                           |  No multiplexing                  |  HPACK header compression        |  Multiplexing       |
|                           |                                   |                                  |  Connection IDs     |
|---------------------------+-----------------------------------+----------------------------------+---------------------|
| Layer 4  Transport        |                                   TCP                                |         UDP         |
|---------------------------+-----------------------------------+----------------------------------+---------------------|
| Layer 3  Network          |                                  IP (IPv4 / IPv6)                                          |
|---------------------------+--------------------------------------------------------------------------------------------|
| Layer 2  Data Link        |                              Wi-Fi, Ethernet, LTE/5G, etc.                                 |
|---------------------------+--------------------------------------------------------------------------------------------|
| Layer 1  Physical         |                             Radio waves, fiber, copper, etc.                               |
+------------------------------------------------------------------------------------------------------------------------+


### Root certificates
Provided OOTB by Google in Android devices. Derived from recognized gloabl certificate authorities. Audited and approved by Google. This is basis of HTTPS trust on Android. These certs cannot be modified without root access. Root CA updates happen though occassional Google updates, which adds new trusted CAs, removes compromized ones and updates metadata.
Location: `/system/etc/security/cacerts_google`

#### Certificate PINNING in Android
If an app wants to add additional trust CAs, then they can PIN certificates or trust only specific CAs, add user CAs or ignore system CAs totally.
Use can also install extra root CAs in location `/data/misc/keychain/certs-added`. These are not trusted by default apps. They are trusted only if apps opt in.

Normally HTTPs trusts all root CAs. But if an app wants, it can pin certain CAs which it wants and only those certs are trusted for that app's HTTPS comms. So even if there is a mallicious root CA that user installed or wifi provider inserts a proxy or a global CA is compromized, the app wont be affected. Pinning is simply overriding default trust CAs for a particular app (mainly to narrow it down).

There are many options to pin entire certificate, or only public key or an intermediary certificate. Pinning public key is more popular as it allows cert rotation without breaking pin. App can also pin multiple keys for fallback. (say current key and next key, before removing the current one, so that both will work for some time.)

Pinning can be done by XML based `network-security-config` where you put in the public key as base 64 encoded string or in code by using OKHttpClient's `CertificatePinner` while setting up a client.

During TLS handshake, after normal certificate validation succeeds, the app additionally checks the certificate or public key against its pins. If they don’t match, the connection fails. Pinning protects against MITM attacks, compromised CAs, and user-installed root certificates, but requires careful key rotation planning.

### 7 layer network model
+---------------------------------------------------------------------+
| Layer 7  Application        |    HTTP (REST, gRPC, WebSockets)      |
+---------------------------------------------------------------------+
| Layer 6  Presentation       |    TLS/SSL (encryption)               |
+---------------------------------------------------------------------+
| Layer 5  Session            |    TLS session, sockets               |
+---------------------------------------------------------------------+
| Layer 4  Transport          |    TCP   or   UDP (for QUIC/HTTP3)    |
+---------------------------------------------------------------------+
| Layer 3  Network            |    IP (IPv4/IPv6)                     |
+---------------------------------------------------------------------+
| Layer 2  Data Link          |    Wi-Fi, Ethernet, LTE/5G            |
+---------------------------------------------------------------------+
| Layer 1  Physical           |    Radio waves, fiber optics, wires   |
+---------------------------------------------------------------------+

## XMPP


## Backend
Load balancer
CDN
server capacity 
DDoS attack 
Exponential Backoff
API Rate-Limiting

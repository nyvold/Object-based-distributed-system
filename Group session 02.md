Queues are hard to implement, only 3 groups managed to do it last year.

You need to run rmiregistry in target/classes to actually run the registry for the client to find the server function. It does not care what project you are on.

the rmi registry needs ServerInterface.class

mvn clean package

Macen compilation results in a jar file. 

rmiregistry need to be open and run before running the jar file. 

`solution.jar com.ass1.ServerInterface`

`solution.jar com.ass1.client.Client`

if the client returns 30 it has successfully ran the client function.

## Docker runthrough

from openjdk:17

client -> proxy -> randomly choose server from a pool -> client invokes that server and returns the function

Needs to run rmiregistry, does not have to be in Docker apparantly.


Docker runProcessingServerOnDocker.bat

Enter base port in a bat script, prob just input

container is running

run scirpt to start different Server containers. 

Before running the server need to java-cp solution.har com.javarmi.proxy.ProxyServer

To start the Proxy Server for the servers to register the URL, proxy server should return what server is registered

Registered: host.docker.internal:1112

Now run the client through java -CP solution.jar com.javarmi.client.Client in terminal of VScode

When TA runs code gets `Connection regused to host: host.docker.internal: ...` 

Does not want to show the solution. 


Shows docker on the dockerEngine. Port open 1100:1100 the exactly one that he showed. 

You need to make threads for each query for returning the function on the server

Check messages from last year since people are doing the wrong architecture.

## Presentation

Should be 15 minuttes, QA

If you cannot answer question then it is no problem, but there will be a QA with some simple math equations?

## Exam
What degree of access transparancy it had in the distributed system

In the slides there are 7 transparancies, we should understand all the transparancies

Theory is very important

Why do we need Interface Definition Language (IDL)?

Defines the interface of the remote objects in a language independent way

If it comes to oral exam then you need to draw the total time of a RPC call. Like how many time 

5 + 0.5 + 0.5
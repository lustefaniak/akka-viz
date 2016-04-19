# akka-viz

_a visual debugger for Akka actor systems (experimental)_

[![Build Status](https://travis-ci.org/blstream/akka-viz.svg?branch=master)](https://travis-ci.org/blstream/akka-viz)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/f5f10352c6e74aa99d0f996cf0a77124)](https://www.codacy.com/app/lustefaniak/akka-viz)

## Download

Latest version is: [ ![Download](https://api.bintray.com/packages/lustefaniak/maven/monitoring/images/download.svg) ](https://bintray.com/lustefaniak/maven/monitoring/_latestVersion)

To get started you need to add dependencies for "monitoring" jar:

```
resolvers += Resolver.bintrayRepo("lustefaniak", "maven")

libraryDependencies += "com.blstream.akkaviz" %% "monitoring" % "0.1.5"
```

After that you must configure your application to run with AspectJ weaver.


## SBT plugin
Latest version is: [ ![Download](https://api.bintray.com/packages/lustefaniak/sbt-plugins/sbt-akka-viz/images/download.svg) ](https://bintray.com/lustefaniak/sbt-plugins/sbt-akka-viz/_latestVersion)

Simpler way is to use sbt plugin, which will preconfigure required bits, in your project directory create file `project/akkaviz.sbt` with:

```
resolvers += Resolver.url("lustefaniak/sbt-plugins", url("https://dl.bintray.com/lustefaniak/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.blstream.akkaviz" % "sbt-akka-viz" % "0.1.5")
```


## Sample project

Please check [lustefaniak/akka-viz-demo](https://github.com/lustefaniak/akka-viz-demo) for simple, preconfigured SBT project.

## Screenshots

![REPL and Actor messaging graph](../screenshots/docs/repl_with_graph.png)

![Actor hierarchy and Actor details tab](../screenshots/docs/details_with_hierarchy.png)

![Message archive](../screenshots/docs/msg_archive.png)

![FSM monitoring](../screenshots/docs/fsm.png)

## Features

* see which actors exchange messages on a graph
* monitor internal state via reflection
* track FSM transitions
* monitor actor creation
* display messages in realtime (with contents) for selected actors
* filter messages by class
* delay processing of messages
* display information about exceptions in actors and used supervisor strategies

## Running

## Configuration
To change one of the akka-viz default settings create `application.conf` file as defined in [Typesafe Config documentation](https://github.com/typesafehub/config#standard-behavior)
 
Default settings are:
 
```
akkaviz {
  internalSystemName = akka-viz
  interface = 127.0.0.1
  port = 8888
  bufferSize = 10000
  maxSerializationDepth = 3
  inspectObjects = false
  autoStartReporting = true
  enableArchive = true
  cassandra {
    keyspace = akkaviz
    preparedStatementCacheSize = 100
    session {
      contactPoint = 127.0.0.1
    }
  }
}

```

## Message archive

Akka-viz can use Cassandra database to store all messages so they could be replayed in the frontend for easier debugging.

To use it make sure `akkaviz.enableArchive = true` and `akkaviz.cassandra` points to correct Cassandra configuration.

You need to manually create keyspace and tables. Execute [included DDL](monitoring/src/main/resources/ddl.cql) via cqlsh


### Demos
We supply a few demos with the source code, so you can explore without hooking up to an existing ActorSystem.

Clone project and run with `demo/reStart` in SBT. Go to `http://localhost:8888` in your favourite browser and play around!

### Monitoring existing systems
Include jar in classpath of your app and run your `main()`. Server should be listening on `http://localhost:8888`.

## Under the hood
To hook into Akka's internals, we use AspectJ (capturing actor creation, intercepting of messages). WebSocket handling
is provided by Akka HTTP and Akka Streams. Filtering is done per client; slowing down of message processing, however, affects the
whole system. Frontend is powered by Scala.js, Scala.Rx and ScalaTags.

### Serialization for messages
We're using custom-made serialization with help of upickle for transporting the messages to frontend clients.
To serialize some specific type of message - ie. your custom class, that you want to have displayed in web frontend - you have to implement a AkkaVizSerializer.

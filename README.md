# akka-viz

_a visual debugger for Akka actor systems (experimental)_

[![Build Status](https://travis-ci.org/blstream/akka-viz.svg?branch=master)](https://travis-ci.org/blstream/akka-viz)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/f5f10352c6e74aa99d0f996cf0a77124)](https://www.codacy.com/app/lustefaniak/akka-viz)

## Download

Latest version is: [ ![Download](https://api.bintray.com/packages/lustefaniak/maven/monitoring/images/download.svg) ](https://bintray.com/lustefaniak/maven/monitoring/_latestVersion)

```
resolvers += Resolver.bintrayRepo("lustefaniak", "maven")

libraryDependencies += "com.blstream.akkaviz" %% "monitoring" % "0.1.1"
```

## Screenshot

![Screenshot](https://gist.githubusercontent.com/lustefaniak/fae64adc6ad668fe30fc/raw/akka-viz.png)

## Features

* see which actors exchange messages on a graph
* monitor internal state via reflection
* track FSM transitions
* monitor actor creation
* display messages in realtime (with contents) for selected actors
* filter messages by class
* delay processing of messages

## Running

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

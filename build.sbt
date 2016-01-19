name := "akka-message-viz"

version := "1.0"

scalaVersion := "2.11.7"

fork in run := true

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"

AspectjKeys.inputs in Aspectj <+= compiledClasses

products in Compile <<= products in Aspectj

products in Runtime <<= products in Compile
name := "akka-message-viz"

version := "1.0"

scalaVersion := "2.11.7"

fork in run := true

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
libraryDependencies += "com.wacai" %% "config-annotation" % "0.3.4" % "compile"
scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value / "src/main/resources/"

AspectjKeys.inputs in Aspectj <+= compiledClasses

products in Compile <<= products in Aspectj

products in Runtime <<= products in Compile
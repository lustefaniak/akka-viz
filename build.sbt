name := "akka-message-viz"

version := "1.0"

scalaVersion := "2.11.7"

fork in run := true

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
libraryDependencies += "com.wacai" %% "config-annotation" % "0.3.4" % "compile"
scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value / "src/main/resources/"

libraryDependencies ++= {
  val akkaV       = "2.4.1"
  val akkaStreamV = "2.0.1"
  val scalaTestV  = "2.2.5"
  Seq(
    "com.lihaoyi"       %% "upickle"                              % "0.3.6",
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test"
  )
}

AspectjKeys.inputs in Aspectj <+= compiledClasses

products in Compile <<= products in Aspectj

products in Runtime <<= products in Compile
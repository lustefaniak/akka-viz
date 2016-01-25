
name := "akka-message-viz"

version := "1.0"

scalaVersion in ThisBuild := "2.11.7"

lazy val root =
  Project("root", file(".")).disablePlugins(RevolverPlugin)
    .aggregate(frontend, backend)

lazy val frontend =
  Project("frontend", file("frontend"))
    .disablePlugins(RevolverPlugin)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      persistLauncher in Compile := true,
      persistLauncher in Test := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "com.lihaoyi" %%% "upickle" % Dependencies.Versions.upickle,
        "com.lihaoyi" %%% "scalarx" % "0.3.0",
        "com.lihaoyi" %%% "scalatags" % "0.5.4",
        "org.querki" %%% "jquery-facade" % "0.11",
        "org.scalatest" %%% "scalatest" % Dependencies.Versions.scalatest % "test"
      ),
      jsDependencies += RuntimeDOM
    )
    .dependsOn(sharedJs)

lazy val backend =
  Project("backend", file("backend"))
    .enablePlugins(RevolverPlugin)
    .settings(aspectjSettings)
    .settings(
      fork in run := true,
      javaOptions <++= AspectjKeys.weaverOptions in Aspectj,
      javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj,
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
      libraryDependencies += "com.wacai" %% "config-annotation" % "0.3.4" % "compile",
      scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value / "src/main/resources/",
      libraryDependencies ++= Dependencies.backend,
      AspectjKeys.inputs in Aspectj <+= compiledClasses,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile,
      (resourceGenerators in Compile) <+=
        (fastOptJS in Compile in frontend, packageScalaJSLauncher in Compile in frontend, packageJSDependencies in Compile in frontend)
          .map((f1, f2, f3) => {println(f3);Seq(f1.data, f2.data, f3)}),
      watchSources <++= (watchSources in frontend)
    )
    .dependsOn(sharedJvm)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

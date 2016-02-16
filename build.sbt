import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbtassembly.Assembly
import scalariform.formatter.preferences._

lazy val commonSettings: Seq[sbt.Setting[_]] = SbtScalariform.defaultScalariformSettings ++ Seq(
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(DoubleIndentClassDeclaration, true),
  git.useGitDescribe := true,
  organization := "com.blstream.akkaviz",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.11.7"),
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  git.uncommittedSignifier := None,
  publishArtifact in Test := false,
  scalaVersion := "2.11.7",
  homepage := Some(url("https://github.com/blstream/akka-viz")),
  description := "A visual debugger for Akka actor systems",
  pomExtra :=
    <scm>
      <url>git@github.com:blstream/akka-viz.git</url>
      <connection>scm:git:git@github.com:blstream/akka-viz.git</connection>
    </scm>
      <developers>
        <developer>
          <id>lustefaniak</id>
          <url>https://github.com/lustefaniak</url>
        </developer>
        <developer>
          <id>pkoryzna</id>
          <url>https://github.com/pkoryzna</url>
        </developer>
        <developer>
          <id>JJag</id>
          <url>https://github.com/JJag</url>
        </developer>
      </developers>,
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M15" % "test"
) ++ useJGit ++ bintraySettings


lazy val bintraySettings: Seq[sbt.Setting[_]] = Seq(
  bintrayCredentialsFile := file(".bintray_credentials"),
  bintrayVcsUrl := Some("https://github.com/blstream/akka-viz.git")
)

lazy val akkaviz =
  (project in file("."))
    .disablePlugins(RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .aggregate(api, monitoring, plugin, events, backend, `akka-aspects`)

lazy val frontend =
  (project in file("frontend"))
    .disablePlugins(RevolverPlugin, SbtScalariform)
    .enablePlugins(ScalaJSPlugin, GitVersioning)
    .settings(commonSettings)
    .settings(
      persistLauncher in Compile := true,
      persistLauncher in Test := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "com.lihaoyi" %%% "upickle" % Dependencies.Versions.upickle,
        "com.lihaoyi" %%% "scalarx" % "0.3.0",
        "com.lihaoyi" %%% "scalatags" % "0.5.4",
        "org.querki" %%% "jquery-facade" % "0.11"
      ),
      jsDependencies += RuntimeDOM,
      unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala"
    )

lazy val api =
  (project in file("api"))
    .enablePlugins(GitVersioning)
    .disablePlugins(RevolverPlugin)
    .settings(commonSettings)
    .settings(
      //FIXME: don't use AST from Js.Value, define one inside api module
      libraryDependencies += "com.lihaoyi" %%% "upickle" % Dependencies.Versions.upickle
    )

lazy val monitoring =
  (project in file("monitoring"))
    .disablePlugins(SbtScalariform, RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .dependsOn(api, events, `akka-aspects`, backend)

lazy val events =
  (project in file("events"))
    .disablePlugins(SbtScalariform, RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)

lazy val `akka-aspects` =
  (project in file("akka-aspects"))
    .disablePlugins(SbtScalariform, RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .settings(aspectjSettings)
    .settings(
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.12",
      AspectjKeys.compileOnly in Aspectj := true,
      AspectjKeys.outXml in Aspectj := false,
      products in Compile <++= products in Aspectj
    )
    .dependsOn(api, events, backend)

lazy val backend =
  (project in file("backend"))
    .settings(packageBin := assembly.value)
    .disablePlugins(SbtScalariform, RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .settings(inConfig(Aspectj)(defaultAspectjSettings))
    .settings(
      unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala",
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
      libraryDependencies += "com.wacai" %% "config-annotation" % "0.3.4" % "compile",
      libraryDependencies += "org.clapper" %% "classutil" % "1.0.6",
      scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value / "src/main/resources/",
      libraryDependencies ++= Dependencies.backend,
      (resourceGenerators in Compile) <+=
        (fastOptJS in Compile in frontend, packageScalaJSLauncher in Compile in frontend, packageJSDependencies in Compile in frontend)
          .map((f1, f2, f3) => {
            Seq(f1.data, f2.data, f3)
          }),
      watchSources <++= (watchSources in frontend),
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      assemblyShadeRules in assembly := Seq(
        ShadeRule.zap("com.wacai.**").inAll,
        ShadeRule.zap("jline.**").inAll,
        ShadeRule.rename("akka.**" -> "shadeakka.@1").inAll,
        ShadeRule.rename("grizzled.**" -> "shadegrizzled.@1").inAll,
        ShadeRule.rename("com.typesafe.**" -> "shadeconfig.@1").inAll
      ),
      assemblyJarName in assembly <<= (name, version) map { (name, version) => name + "_2.11-" +version + ".jar" }
    )
    .dependsOn(api, events)


lazy val demo =
  (project in file("demo"))
    .disablePlugins(SbtScalariform)
    .enablePlugins(GitVersioning, RevolverPlugin)
    .settings(commonSettings)
    .settings(aspectjSettings)
    .settings(
      publishArtifact := false,
      fork := true,
      javaOptions <++= AspectjKeys.weaverOptions in Aspectj,
      javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % Dependencies.Versions.akka,
        "io.spray" %% "spray-can" % "1.3.3",
        "io.spray" %% "spray-routing" % "1.3.3"
      )
    )
    .dependsOn(monitoring)

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BuildInfoPlugin)
  .disablePlugins(GitVersioning, SbtScalariform, RevolverPlugin)
  .settings(commonSettings)
  .settings(scriptedSettings: _*)
  .settings(
    name := "sbt-akka-viz",
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    sbtPlugin := true,
    buildInfoPackage := "akkaviz.sbt",

    addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.4"),

    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq(
          "-Dproject.version=" + version.value,
          "-Dscala.version=" + scalaVersion.value
        )
    },
    scriptedDependencies := {
      val a = (publishLocal in monitoring).value
      val c = publishLocal.value
    }
  )


addCommandAlias("formatAll", ";scalariformFormat;test:scalariformFormat")
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

cancelable in Global := true

val upickleVersion = "0.3.8"
val boopickleVersion = "1.1.2"
val akkaVersion = "2.4.2"
val scalatestVersion = "3.0.0-M15"

lazy val commonSettings: Seq[sbt.Setting[_]] = SbtScalariform.defaultScalariformSettings ++ Seq(
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.11.7"),
  updateOptions := updateOptions.value.withCachedResolution(true),
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, false)
    .setPreference(DoubleIndentClassDeclaration, true),
  git.useGitDescribe := true,
  organization := "com.blstream.akkaviz",
  licenses +=("MIT", url("http://opensource.org/licenses/MIT")),
  git.uncommittedSignifier := None,
  publishArtifact in Test := false,
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
  {
    import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
    import scala.xml.transform.{RewriteRule, RuleTransformer}

    def omitDep(e:Elem): XmlNodeSeq = {
      val organization = e.child.filter(_.label == "groupId").flatMap(_.text).mkString
      val artifact = e.child.filter(_.label == "artifactId").flatMap(_.text).mkString
      val version = e.child.filter(_.label == "version").flatMap(_.text).mkString
      Comment(s"dependency $organization#$artifact;$version has been omitted")
    }

    pomPostProcess := { (node: XmlNode) =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem if e.label == "dependency" && e.child.exists(child => child.label == "scope" && (child.text == "provided" || child.text == "test")) =>
            omitDep(e)
          //case e: Elem if e.label == "dependency" && e.child.exists(child => child.label == "groupId" && child.text == "com.typesafe.akka") =>
          //  omitDep(e)
          case _ => node
        }
      }).transform(node).head
    }
  }
) ++ useJGit ++ bintraySettings

val commonJava8Settings: Seq[sbt.Setting[_]] = Seq(
  scalacOptions ++= Seq("-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
)

lazy val bintraySettings: Seq[sbt.Setting[_]] = Seq(
  bintrayCredentialsFile := file(".bintray_credentials"),
  bintrayVcsUrl := Some("https://github.com/blstream/akka-viz.git")
)

lazy val akkaviz =
  (project in file("."))
    .disablePlugins(RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .settings(commonJava8Settings)
    .aggregate(api, frontend, monitoring, plugin)
    .settings(
        publish := {},
        publishLocal := {}
      )

lazy val frontend =
  (project in file("frontend"))
    .disablePlugins(RevolverPlugin, SbtScalariform)
    .enablePlugins(ScalaJSPlugin, GitVersioning)
    .settings(commonSettings)
    .settings(commonJava8Settings)
    .settings(
      persistLauncher in Compile := true,
      persistLauncher in Test := false,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.0",
        "com.lihaoyi" %%% "upickle" % upickleVersion,
        "me.chrons" %%% "boopickle" % boopickleVersion,
        "com.lihaoyi" %%% "scalarx" % "0.3.0",
        "com.lihaoyi" %%% "scalatags" % "0.5.4",
        "org.querki" %%% "jquery-facade" % "0.11",
        "org.scalatest" %%% "scalatest" % scalatestVersion % "test"
      ),
      jsDependencies += RuntimeDOM,
      unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala",
      publish := {},
      publishLocal := {}
    )

lazy val api =
  (project in file("api"))
    .enablePlugins(GitVersioning)
    .disablePlugins(RevolverPlugin)
    .settings(commonSettings)
    .settings(commonJava8Settings)
    .settings(
      exportJars := true,
      //FIXME: don't use AST from Js.Value, define one inside api module
      libraryDependencies += "com.lihaoyi" %% "upickle" % upickleVersion,
      autoScalaLibrary := false
    )

lazy val monitoring =
  (project in file("monitoring"))
    .disablePlugins(SbtScalariform, RevolverPlugin)
    .enablePlugins(GitVersioning)
    .settings(commonSettings)
    .settings(commonJava8Settings)
    .settings(aspectjSettings)
    .settings(
      fork := true,
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
      libraryDependencies += "com.wacai" %% "config-annotation" % "0.3.4" % "compile",
      libraryDependencies += "org.clapper" %% "classutil" % "1.0.6",
      libraryDependencies += "me.chrons" %%% "boopickle" % boopickleVersion,
      libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.5.6" cross CrossVersion.full,
      libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion, // FIXME: shadow that dependency and hide it
      libraryDependencies += "org.scalatest" %%% "scalatest" % scalatestVersion % "test",
      scalacOptions += "-Xmacro-settings:conf.output.dir=" + baseDirectory.value / "src/main/resources/",
      (resourceGenerators in Compile) <+=
        (fastOptJS in Compile in frontend, packageScalaJSLauncher in Compile in frontend, packageJSDependencies in Compile in frontend)
          .map((f1, f2, f3) => {
            val f1SourceMap = f1.data.getParentFile / (f1.data.getName + ".map")
            Seq(f1.data, f2.data, f3, f1SourceMap)
          }),
      watchSources <++= (watchSources in frontend),
      AspectjKeys.compileOnly in Aspectj := true,
      AspectjKeys.outXml in Aspectj := false,
      products in Compile <++= products in Aspectj,
      unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala"
    )
    .dependsOn(api)

lazy val demo =
  (project in file("demo"))
    .disablePlugins(SbtScalariform)
    .enablePlugins(GitVersioning, RevolverPlugin)
    .settings(commonSettings)
    .settings(commonJava8Settings)
    .settings(aspectjSettings)
    .settings(
      publishArtifact := false,
      fork := true,
      javaOptions <++= AspectjKeys.weaverOptions in Aspectj,
      javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj,
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
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

    addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.5"),

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
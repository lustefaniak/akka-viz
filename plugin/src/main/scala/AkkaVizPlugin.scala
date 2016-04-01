package akkaviz.sbt

import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj._
import sbt.Keys._
import sbt._

object AkkaVizPlugin extends AutoPlugin {

  override def requires: Plugins = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectConfigurations: Seq[Configuration] = Seq(SbtAspectj.Aspectj)

  object Keys {
    val akkaVizVersion = settingKey[String]("Version of akka-viz to use")
  }

  val autoImport = Keys

  override def projectSettings: Seq[Def.Setting[_]] = {
    import Keys._
    SbtAspectj.aspectjSettings ++ Seq(
      resolvers += Resolver.bintrayRepo("lustefaniak", "maven"),
      akkaVizVersion := "0.1.5",
      libraryDependencies <<= (libraryDependencies, akkaVizVersion) {
        (deps, ver) =>
          deps :+ "com.blstream.akkaviz" %% "monitoring" % ver % "runtime"
      },
      fork := true,
      javaOptions <++= AspectjKeys.weaverOptions in Aspectj
    )
  }
}
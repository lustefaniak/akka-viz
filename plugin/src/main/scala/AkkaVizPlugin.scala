package akkaviz.sbt

import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj._

import sbt._
import sbt.Keys._

object AkkaVizPlugin extends AutoPlugin {

  override def requires: Plugins = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectConfigurations: Seq[Configuration] = Seq(SbtAspectj.Aspectj)

  object autoImport {

  }

  override def projectSettings: Seq[Def.Setting[_]] = SbtAspectj.aspectjSettings ++ Seq(
    resolvers += Resolver.bintrayRepo("lustefaniak", "maven"),
    libraryDependencies += "com.blstream.akkaviz" %% "monitoring" % BuildInfo.version % "runtime",
    fork := true,
    javaOptions <++= AspectjKeys.weaverOptions in Aspectj
  )

}

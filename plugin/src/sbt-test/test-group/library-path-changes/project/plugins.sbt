resolvers += "sbt plugins" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.10.4")

addSbtPlugin("com.blstream.akkaviz" % "sbt-akka-viz" % sys.props.get("project.version").getOrElse("0.1.1-11-ga4c4612"))

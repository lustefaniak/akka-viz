package akka.viz.serialization

import upickle.Js

trait AkkaVizSerializer {
  def canSerialize(obj: Any): Boolean

  def serialize(obj: Any): Js.Value
}

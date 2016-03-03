package akkaviz.frontend

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

package object vis {

  @js.native
  trait Node extends js.Any {
    def id: js.Any

  }

  object Node {
    def apply(id: String, label: String, value: js.UndefOr[Int] = js.undefined, color: js.UndefOr[String] = js.undefined): vis.Node = {
      js.Dynamic.literal(id = id, label = label, value = value, color = color).asInstanceOf[vis.Node]
    }
  }

  @js.native
  trait Edge extends js.Any {
    def id: js.Any
  }

  object Edge {
    def apply(id: String, from: String, to: String, arrows: js.UndefOr[String] = "to", title: js.UndefOr[String] = js.undefined, value: js.UndefOr[Int] = js.undefined): vis.Edge = {
      js.Dynamic.literal(id = id, from = from, to = to, value = value, arrows = arrows).asInstanceOf[vis.Edge]
    }
  }

  @js.native
  trait DataSetOptions extends js.Any {

  }

  @js.native
  @JSName("vis.DataSet")
  class DataSet[T](data: js.Array[T] = js.Array[T](), options: js.UndefOr[DataSetOptions] = js.native) extends js.Any {

    def add(d: T): js.Array[String] = js.native

    def add(d: js.Array[T]): js.Array[String] = js.native

    def remove(d: String): js.Array[String] = js.native

    def remove(d: js.Array[String]): js.Array[String] = js.native

    def get(d: String): T = js.native

    def get(d: js.Array[String]): js.Array[T] = js.native

    def setOptions(o: DataSetOptions): Unit = js.native

  }

  @js.native
  trait NetworkData extends js.Any {
    var nodes: js.Array[Node] | DataSet[Node] = js.native
    var edges: js.Array[Edge] | DataSet[Edge] = js.native
  }

  object NetworkData {
    def apply(nodes: js.Array[Node] | DataSet[Node], edges: js.Array[Edge] | DataSet[Edge]): NetworkData = {
      js.Dynamic.literal(nodes = nodes.asInstanceOf[js.Any], edges = edges.asInstanceOf[js.Any]).asInstanceOf[vis.NetworkData]
    }
  }

  @js.native
  trait NetworkOptions extends js.Any

  object NetworkOptions {
    //TODO: define typesafe builder
    def apply(): NetworkOptions = {
      js.Dynamic.literal().asInstanceOf[NetworkOptions]
    }
  }

  @js.native
  @JSName("vis.Network")
  class Network(element: Element, data: NetworkData, options: NetworkOptions) extends js.Any {

    def destroy(): Unit = js.native

    def fit(): Unit = js.native

  }

}

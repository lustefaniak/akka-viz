package akkaviz.frontend

import org.scalajs.dom.html.Canvas
import org.scalajs.dom.{Element, Event}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

package object vis {

  @js.native
  trait Node extends js.Any {
    var id: js.Any = js.native
  }

  object Node {
    def apply(
      id: String,
      label: String,
      title: js.UndefOr[String] = js.undefined,
      value: js.UndefOr[Int] = js.undefined,
      color: js.UndefOr[String] = js.undefined
    ): vis.Node = {
      js.Dynamic.literal(id = id, label = label, value = value, color = color, title = title).asInstanceOf[vis.Node]
    }
  }

  @js.native
  trait Edge extends js.Any {
    var id: js.Any = js.native
  }

  object Edge {
    def apply(
      id: String,
      from: String,
      to: String,
      arrows: js.UndefOr[String] = "to",
      title: js.UndefOr[String] = js.undefined,
      value: js.UndefOr[Int] = js.undefined
    ): vis.Edge = {
      js.Dynamic.literal(id = id, from = from, to = to, value = value, arrows = arrows).asInstanceOf[vis.Edge]
    }
  }

  @js.native
  trait DataSetOptions extends js.Any

  @js.native
  @JSName("vis.DataSet")
  class DataSet[T](data: js.Array[T] = js.Array[T](), options: js.UndefOr[DataSetOptions] = js.native) extends js.Any {

    def add(d: T): js.Array[String] = js.native

    def add(d: js.Array[T]): js.Array[String] = js.native

    def update(d: T): js.Array[String] = js.native

    def update(d: js.Array[T]): js.Array[String] = js.native

    def remove(d: String): js.Array[String] = js.native

    def remove(d: js.Array[String]): js.Array[String] = js.native

    def get(d: String): T = js.native

    def get(d: js.Array[String]): js.Array[T] = js.native

    def setOptions(o: DataSetOptions): Unit = js.native

    def clear(): js.Array[String] = js.native

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
  trait Coords extends js.Any {
    val x: Double
    val y: Double
  }

  object Coords {
    def apply(x: Double, y: Double): Coords = {
      js.Dynamic.literal(x = x, y = y).asInstanceOf[Coords]
    }
  }

  @js.native
  trait NetworkMoveToOptions extends js.Any {
    var position: Coords = js.native
    var scale: Double = js.native
    var offset: Coords = js.native
    //var animation
  }

  @js.native
  trait NetworkFitOptions extends NetworkMoveToOptions

  @js.native
  trait NetworkFocusOptions extends NetworkMoveToOptions {
    var locked: Boolean = js.native
  }

  @js.native
  trait SelectionOptions extends js.Any {
    var unselectAll: Boolean = js.native
    var highlightEdges: Boolean = js.native
  }

  object SelectionOptions {
    def apply(unselectAll: js.UndefOr[Boolean] = js.undefined, highlightEdges: js.UndefOr[Boolean] = js.undefined) = {
      js.Dynamic.literal(unselectAll = unselectAll, highlightEdges = highlightEdges).asInstanceOf[SelectionOptions]
    }
  }

  @js.native
  @JSName("vis.Network")
  class Network(element: Element, data: NetworkData, options: NetworkOptions) extends js.Any {

    //Global methods for the network
    def destroy(): Unit = js.native

    def setData(data: NetworkData): Unit = js.native

    def setOptions(options: NetworkOptions): Unit = js.native

    def on(event: String, fn: js.Function): Unit = js.native

    def off(event: String, fn: js.Function): Unit = js.native

    def once(event: String, fn: js.Function): Unit = js.native

    //Methods related to the canvas
    def canvasToDOM(coords: Coords): Coords = js.native

    def DOMtoCanvas(coords: Coords): Coords = js.native

    def redraw(): Unit = js.native

    def setSize(width: String, height: String): Unit = js.native

    //Methods to get information on nodes and edges.

    //Physics methods to control when the simulation should run.

    //Selection methods for nodes and edges.
    def getSelection(): Selection = js.native

    def getSelectedNodes(): js.Array[String] = js.native

    def getSelectedEdges(): js.Array[String] = js.native

    def getNodeAt(domPosition: Coords): String = js.native

    def getEdgeAt(domPosition: Coords): String = js.native

    def selectNodes(nodeIds: js.Array[String], highlightEdges: Boolean = ???): Unit = js.native

    def selectEdges(edgeIds: js.Array[String]): Unit = js.native

    def setSellection(selection: Selection, options: js.UndefOr[SelectionOptions] = js.undefined): Unit = js.native

    def unselectAll(): Unit = js.native

    //Methods to control the viewport for zoom and animation.
    def getScale(): Double = js.native

    def getViewPosition(): Coords = js.native

    def fit(options: js.UndefOr[NetworkFitOptions] = js.undefined): Unit = js.native

    def focus(nodeId: String, options: js.UndefOr[NetworkFocusOptions] = js.undefined): Unit = js.native

    def moveTo(options: js.UndefOr[NetworkMoveToOptions] = js.undefined): Unit = js.native

    def releaseNode(): Unit = js.native

  }

  @js.native
  trait PointerCoords extends js.Any {
    var DOM: Coords = js.native
    var canvas: Coords = js.native
  }

  @js.native
  trait ClickEvent extends js.Any with Selection {
    var event: Event = js.native
    var pointer: PointerCoords = js.native
  }

  @js.native
  trait Selection extends js.Any {
    var nodes: js.Array[String] = js.native
    var edges: js.Array[String] = js.native
  }

  object Selection {
    def apply(nodes: js.UndefOr[js.Array[String]] = js.undefined, edges: js.UndefOr[js.Array[String]] = js.undefined): Selection = {
      js.Dynamic.literal(nodes = nodes, edges = edges).asInstanceOf[Selection]
    }
  }

  @js.native
  trait DeselectEvent extends ClickEvent {
    var previousSelection: Selection = js.native
  }

  @js.native
  trait EdgeEvent extends js.Any {
    var edge: String = js.native
  }

  @js.native
  trait NodeEvent extends js.Any {
    var node: String = js.native
  }

  @js.native
  trait ZoomEvent extends js.Any {
    var direction: String = js.native
    var scale: Double = js.native
  }

  implicit class NetworkStaticEvents(val underlying: Network) extends AnyVal {
    def onClick(fn: ClickEvent => Unit) = underlying.on("click", fn)

    def onDoubleClick(fn: ClickEvent => Unit) = underlying.on("doubleClick", fn)

    def onContext(fn: ClickEvent => Unit) = underlying.on("onContext", fn)

    def onHold(fn: ClickEvent => Unit) = underlying.on("hold", fn)

    def onRelease(fn: ClickEvent => Unit) = underlying.on("release", fn)

    def onSelect(fn: ClickEvent => Unit) = underlying.on("select", fn)

    def onSelectNode(fn: ClickEvent => Unit) = underlying.on("selectNode", fn)

    def onSelectEdge(fn: ClickEvent => Unit) = underlying.on("selectEdge", fn)

    def onDeselectNode(fn: DeselectEvent => Unit) = underlying.on("deselectNode", fn)

    def onDeselectEdge(fn: DeselectEvent => Unit) = underlying.on("deselectEdge", fn)

    def onDragStart(fn: ClickEvent => Unit) = underlying.on("dragStart", fn)

    def onDragging(fn: ClickEvent => Unit) = underlying.on("dragging", fn)

    def onDragEnd(fn: ClickEvent => Unit) = underlying.on("dragEnd", fn)

    def onHoverNode(fn: NodeEvent => Unit) = underlying.on("hoverNode", fn)

    def onBlurNode(fn: NodeEvent => Unit) = underlying.on("blurNode", fn)

    def onHoverEdge(fn: EdgeEvent => Unit) = underlying.on("hoverEdge", fn)

    def onBlurEdge(fn: EdgeEvent => Unit) = underlying.on("blurEdge", fn)

    def onZoom(fn: ZoomEvent => Unit) = underlying.on("zoom", fn)

    def onShowPopup(fn: String => Unit) = underlying.on("showPopup", fn)

    def onHidePopup(fn: () => Unit) = underlying.on("hidePopup", fn)

    //Events triggered by the rendering module. Can be used to draw custom elements on the canvas.
    def onInitRedraw(fn: () => Unit) = underlying.on("initRedraw", fn)

    def onBeforeDrawing(fn: (Canvas) => Unit) = underlying.on("beforeDrawing", fn)

    def onAfterDrawing(fn: (Canvas) => Unit) = underlying.on("afterDrawing", fn)

  }

}

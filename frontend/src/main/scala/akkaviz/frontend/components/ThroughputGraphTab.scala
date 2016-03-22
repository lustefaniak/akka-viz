package akkaviz.frontend.components

import akkaviz.frontend.vis._
import akkaviz.frontend.{Persistence, FancyColors}
import akkaviz.protocol.ThroughputMeasurement
import org.scalajs.dom._
import org.scalajs.dom.ext.Color
import rx.{Rx, Var, Ctx}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.{|, Date}
import scalatags.JsDom.all._

class ThroughputGraphViewTab(implicit ctx: Ctx.Owner) extends Tab with FancyColors with Persistence {
  import scala.concurrent.duration._
  import scalatags.rx.all._

  override def name: String = "Throughput"

  override def tabId: String = "throughput-graph-tab"

  private[this] val items = new DataSet[Item]()
  private[this] val groups = new DataSet[Group]()
  private[this] val groupVisibility = Var[Map[String, Boolean]](Map.empty)

  val graphContainer = div(id := "thr-graph-container").render
  val options = literal(
    start = js.Date.now() - 10.seconds.toMillis,
    end = js.Date.now() - 1000,
    interpolation = false,
    drawPoints = false,
    dataAxis = literal(
      showMinorLabels = false,
      left = literal(
        range = literal(
          min = 0
        )

      )
    )
  )
  val graph = new Graph2d(graphContainer, items, groups, options)

  val selector = div(Rx {
    ul(groupVisibility().map {
      case (groupName, visible) =>
        li(
          input(tpe := "checkbox", if (visible) checked else ()),
          " " + groupName, onclick := { () =>
            groupVisibility() = groupVisibility.now.updated(groupName, !visible)
          }, color := colorForActor(groupName).toString(), cursor.pointer
        )
    }.toSeq, listStyleType.none).render
  }).render

  tabBody.appendChild(graphContainer)
  tabBody.appendChild(selector)

  def addMeasurement(tm: ThroughputMeasurement): Unit = {
    val color = colorForActor(tm.actorRef)
    val group = new Group(tm.actorRef, tm.actorRef,
      style = s"""fill: ${color}; stroke: ${color}; fill-opacity:0; stroke-width:2px; """)
    val date = new Date(js.Date.parse(tm.timestamp))
    val item = new Item(date, tm.msgPerSecond, tm.actorRef)
    removeOldItems()
    groups.update(group)
    items.add(item)
  }

  def colorForActor(ref: String): Color = {
    colorForString(ref, 0.2, 0.2)
  }

  groups.on("add", { (event: String, p: Properties[Group], sender: String | Double) =>
    groupVisibility() = groupVisibility.now ++ p.items.map(_ -> false)
  })

  private[this] def removeOldItems(): Unit = {
    val range = graph.getWindow()
    val interval = range.end.valueOf() - range.start.valueOf()

    val oldIds = items.getIds(literal(filter = { (item: Item) =>
      item.x.valueOf() < (range.start.valueOf() - interval)
    }))
    items.remove(oldIds)
  }

  private[this] val groupUpdate = groupVisibility.foreach { g =>
    val options = literal(groups = literal(
      visibility = js.Dictionary[Boolean](g.toSeq: _*)
    ))
    graph.setOptions(options)
  }

  override def onCreate(): Unit = {
    window.requestAnimationFrame(autoScroll _)
  }

  def autoScroll(d: Double): Unit = {
    val graphWindow = graph.getWindow()
    val interval = graphWindow.end.valueOf() - graphWindow.start.valueOf()
    val now = new Date().valueOf()
    val start = new Date(now - interval)
    val end = new Date(now)

    graph.setWindow(start, end, literal(animation = false))
    window.requestAnimationFrame(autoScroll _)
  }
}
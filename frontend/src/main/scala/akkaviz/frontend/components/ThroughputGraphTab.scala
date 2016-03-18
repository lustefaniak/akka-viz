package akkaviz.frontend.components

import akkaviz.frontend.vis._
import akkaviz.frontend.{Persistence, FancyColors}
import akkaviz.protocol.ThroughputMeasurement
import org.scalajs.dom._
import rx.{Rx, Var, Ctx}

import scala.scalajs.js
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

  val graphContainer = div(id := "thr-graph-container", width := 100.pct).render
  val options = js.Dynamic.literal(
    start = js.Date.now(),
    end = js.Date.now() + 2.minutes.toMillis,
    interpolation = false,
    drawPoints = false
  )
  val graph = new Graph2d(graphContainer, items, groups, options)

  private[this] val rxElement = Rx {
    ul(groupVisibility().map {
      case (groupName, visible) =>
        li(
          input(tpe := "checkbox", if (visible) checked else ()),
          " " + groupName, onclick := { () =>
            groupVisibility() = groupVisibility.now.updated(groupName, visible)
          }, color := colorForString(groupName)
        )
    }.toSeq).render
  }

  val selector = div(rxElement).render

  tabBody.appendChild(graphContainer)
  tabBody.appendChild(selector)

  def addMeasurement(tm: ThroughputMeasurement): Unit = {
    val color = colorForString(tm.actorRef)
    val group = new Group(tm.actorRef, tm.actorRef,
      style = s"""fill: ${color}; stroke: ${color}; fill-opacity:0; stroke-width:2px; """)
    val date = new Date(js.Date.parse(tm.timestamp))
    val item = new Item(date, tm.msgPerSecond, tm.actorRef)
    removeOldItems()
    groups.update(group)
    items.add(item)
  }

  groups.on("add", { (event: String, p: Properties[Group], sender: String | Double) =>
    groupVisibility() = groupVisibility.now ++ (p.items.map(_ -> true))
  })

  private[this] def removeOldItems(): Unit = {
    val range = graph.getWindow()
    val interval = range.end.valueOf() - range.start.valueOf()

    val oldIds = items.getIds(js.Dynamic.literal(filter = { (item: Item) =>
      item.x.valueOf() < (range.start.valueOf() - interval)
    }))
    items.remove(oldIds)
  }

  private[this] val groupUpdate = groupVisibility.triggerLater {
    val g = groupVisibility.now
    val options = js.Dynamic.literal(groups = js.Dynamic.literal(
      visibility = js.Dictionary[Boolean](g.toSeq: _*)
    ))
    console.log(options)
    graph.setOptions(options)
  }

  override def onCreate(): Unit = ()
}
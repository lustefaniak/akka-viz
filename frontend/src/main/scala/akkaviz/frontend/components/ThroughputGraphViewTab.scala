package akkaviz.frontend.components

import akkaviz.frontend.vis.{DataSet, Graph2d, Group, Item}
import akkaviz.protocol.ThroughputMeasurement
import org.scalajs.dom.{Element => domElement}

import scala.scalajs.js
import scala.scalajs.js.Date
import scalatags.JsDom.all._

class ThroughputGraphViewTab extends Tab {
  import scala.concurrent.duration._

  override def name: String = "Throughput"

  override def tabId: String = "throughput-graph-tab"

  private[this] val items = new DataSet[Item]()
  private[this] val groups = new DataSet[Group]()

  val graphContainer = div(id := "thr-graph-container", width := 100.pct).render
  val options = js.Dynamic.literal(
    start = js.Date.now(),
    end = js.Date.now() + 2.minutes.toMillis
  )
  val graph = new Graph2d(graphContainer, items, groups, options)

  tabBody.appendChild(graphContainer)

  def addMeasurement(tm: ThroughputMeasurement): Unit = {
    val group = new Group(tm.actorRef, tm.actorRef)
    val date = new Date(js.Date.parse(tm.timestamp))
    val item = new Item(date, tm.msgPerSecond, tm.actorRef)
    groups.update(group)
    items.add(item)
  }
}

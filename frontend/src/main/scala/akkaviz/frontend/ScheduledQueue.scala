package akkaviz.frontend

import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers.SetTimeoutHandle

class ScheduledQueue[T](fn: js.Array[T] => Unit, updateFrequency: FiniteDuration = 500.millis) {
  private[this] var scheduler: js.UndefOr[SetTimeoutHandle] = js.undefined
  private[this] def scheduleGraphOperations(): Unit = {
    if (scheduler.isEmpty) {
      val timer: SetTimeoutHandle = scala.scalajs.js.timers.setTimeout(updateFrequency) {
        scheduler = js.undefined
        applyGraphOperations()
      }
      scheduler = timer
    }
  }

  private[this] var operationsToApply = js.Array[T]()

  private[this] def applyGraphOperations(): Unit = {
    fn(operationsToApply)
    operationsToApply = js.Array()
  }

  def enqueueOperation(op: T): Unit = {
    operationsToApply.append(op)
    scheduleGraphOperations()
  }

}

package akkaviz.frontend

import java.lang.Math.abs

import org.scalajs.dom.ext.Color
import org.scalajs.dom.{Element, window}

import scala.annotation.tailrec
import scala.scalajs.js

object FrontendUtil {
  @inline
  final def isUserActor(actorRef: ActorPath): Boolean = {
    val split = actorRef.split('/')
    split.length > 3 && split(3) == "user"
  }

  @inline
  final def webSocketUrl(path: String) = {
    val l = window.location
    (if (l.protocol == "https:") "wss://" else "ws://") +
      l.hostname +
      (if ((l.port != 80) && (l.port != 443)) ":" + l.port else "") +
      l.pathname + path
  }

  @tailrec
  def findParentWithAttribute(elem: Element, attributeName: String): js.UndefOr[Element] = {
    if (elem == null || elem.hasAttribute(attributeName))
      elem
    else
      findParentWithAttribute(elem.parentNode.asInstanceOf[Element], attributeName)
  }

  @inline
  def shortActorName(actorRef: ActorPath) = actorRef.split('/').drop(3).mkString("/")

  @inline
  def systemName(actorRef: ActorPath): String = actorRef.split('/')(2)

  def parent(actor: ActorPath): Option[String] = {
    val path = actor.stripPrefix("akka://").split("/")
    if (path.length <= 1) None
    else Some("akka://" + path.init.mkString("/"))
  }
}

trait FancyColors {
  //http://tools.medialab.sciences-po.fr/iwanthue/index.php
  private[this] val possibleColors = js.Array[String](
    "#D7C798",
    "#8AD9E5",
    "#ECACC1",
    "#97E3B0",
    "#D5EB85",
    "#F2AE99",
    "#DFC56D",
    "#DAECD0",
    "#D2DCE6",
    "#D4F0AD",
    "#D9C0B3",
    "#E8AD70",
    "#DAC1E1",
    "#82DDC9",
    "#B8BE74",
    "#A8C5AA",
    "#A7C5E2",
    "#A0D38F",
    "#EEDF98",
    "#ADD7D4"
  )

  @inline
  val deadColor = "#666666"

  @inline
  def colorForString(str: String): String = {
    possibleColors(Math.abs(str.hashCode) % possibleColors.size)
  }

  def hsl(hue: Double, saturation: Double, lightness: Double): Color = {
    val c: Double = 1 - abs(2*lightness - 1) * saturation
    val x: Double = c * (1 - abs(((hue / 60) % 2) - 1))
    val m: Double = lightness - c / 2

    val (r: Double, g: Double, b: Double) =
      if (hue < 60) (c, x, 0)
      else if (hue < 120) (x, c, 0)
      else if (hue < 180) (0, c, x)
      else if (hue < 240) (0, x, c)
      else if (hue < 300) (x, 0, c)
      else if (hue < 360) (c, 0, x)
      else throw new Exception("hsl out of range")

    Color(((r + m)*255).toInt, ((g+m)*255).toInt, ((b+m)*255).toInt)
  }

  def goldenRatioHue(n: Int): Double = {
    val goldenRatio = 0.61803398875
    var maxHue = 360

    ((n * goldenRatio) % 1) * maxHue
  }

  def colorForString(s: String, saturation: Double, lightness: Double): Color = {
    hsl(goldenRatioHue(s.hashCode & 0xFFFF), saturation, lightness)
  }

}
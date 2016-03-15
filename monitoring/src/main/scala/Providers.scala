object Types {

  sealed trait Type

  case object SubType1 extends Type

  case object SubType2 extends Type

  case object SubType3 extends Type

  trait Converter[T] {
    def convert(t: T): Int
  }

}

object Implicits {

  import Types._

  implicit object Type1Coverter extends Converter[SubType1.type] {
    override def convert(t: SubType1.type): Int = 1
  }

  implicit object Type2Coverter extends Converter[SubType2.type] {
    override def convert(t: SubType2.type): Int = 2
  }

  implicit object Type3Coverter extends Converter[SubType3.type] {
    override def convert(t: SubType3.type): Int = 3
  }

}

object Conversion {

  import Types._

  def convert[T: Converter](t: T): Int = {
    implicitly[Converter[T]].convert(t)
  }

  def convert2[T <: Type](t: T)(implicit ev1: Converter[SubType1.type], ev2: Converter[SubType2.type], ev3: Converter[SubType3.type]): Int = {
    t match {
      case t1 @ SubType1 =>
        implicitly[Converter[SubType1.type]].convert(t1)
      case t2 @ SubType2 =>
        implicitly[Converter[SubType2.type]].convert(t2)
      case t3 @ SubType3 =>
        implicitly[Converter[SubType3.type]].convert(t3)
    }
  }

}

object Usage {

  import Types._
  import Conversion._
  import Implicits._

  val t1 = SubType1
  val x1: Int = convert(t1)

  val t2 = SubType2
  val x2: Int = convert(t2)

  val t3 = SubType3
  val x3: Int = convert(t3)

  val t: Type = SubType2 // T is of type Type
  //doesn't work
  //val x: Int = convert(t)

  val y: Int = convert2(t)

}

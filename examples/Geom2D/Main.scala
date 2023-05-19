
object Main {

    def main(args: Array[String]): Unit = {
        val points = List(Point2D("A", 12.7, -9.3), Point2D("B", 11.2, 19.4), Point2D("C", 0.8, -7.3))
        for p1 <- points ; p2 <- points if p1 != p2 do {
            println(s"d(${p1.name},${p2.name}) = ${p1.distanceTo(p2)}")
        }
    }

}

final case class Point2D(name: String, x: Double, y: Double){

    def distanceTo(that: Point2D): Double = {
        Math.hypot(this.x - that.x, this.y - that.y)
    }

    override def toString(): String = s"($x,$y)"

}

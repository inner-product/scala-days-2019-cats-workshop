package process
package examples

import doodle.core._
import doodle.syntax._
import doodle.image._
import doodle.image.syntax._
import doodle.java2d._
import scala.util.Random

object BasicLines extends App {
  final case class State(location: Point, heading: Angle, path: List[PathElement]) {
    def forward: State = {
      val newLocation = location + Vec.polar(10, heading)
      this.copy(
        location = newLocation,
        path = PathElement.lineTo(newLocation) +: path
      )
    }

    def clockwise: State =
      this.copy(heading = heading - 30.degrees)

    def anticlockwise: State =
      this.copy(heading = heading - 30.degrees)

    def toImage: Image =
      Image.openPath(path.reverse)
  }
  object State {
    val initial = State(Point.zero, Angle.zero, List.empty)
  }

  type Event = Double

  val basic =
    Fsm[State,Event]{(state, choice) =>
      if(choice < 0.5) state.forward
      else if (choice < 0.75) state.clockwise
      else state.anticlockwise
    }

  def iterate(count: Int, state: State): State = {
    if(count == 0) state
    else {
      val choice = Random.nextDouble()
      iterate(count - 1, basic(state, choice))
    }
  }

  val image =
    iterate(100, State.initial).toImage.strokeWidth(3.0).strokeColor(Color.royalBlue.fadeOutBy(0.5.normalized))

  def go() = image.draw()

  go()
}

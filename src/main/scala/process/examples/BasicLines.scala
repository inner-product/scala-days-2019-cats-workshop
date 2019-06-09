package process
package examples

import cats.implicits._
import doodle.core._
import doodle.syntax._
import doodle.image._
import doodle.image.syntax._
import doodle.java2d._
import scala.util.Random

/** Simple process with no interaction between the elements. */
object BasicProcess extends App {
  final case class State(location: Point, heading: Angle, path: List[Point]) {
    def forward: State = {
      val newLocation = location + Vec.polar(10, heading)
      this.copy(
        location = newLocation,
        path = newLocation +: path
      )
    }

    def clockwise: State =
      this.copy(heading = heading - 15.degrees)

    def anticlockwise: State =
      this.copy(heading = heading - 15.degrees)

    def toImage: Image =
      Image.interpolatingSpline(path)
  }
  object State {
    val initial = State(Point.zero, Angle.zero, List.empty)
  }

  type Event = Double

  /** The finite state machine defines how the state evolves over time. Tweaking
    * the probabilities will arrive at different results. */
  val fsm =
    Fsm[State,Event]{(state, choice) =>
      if(choice < 0.5) state.forward
      else if (choice < 0.75) state.clockwise
      else state.anticlockwise
    }

  /** Execute one step of the FSM */
  def step(state: State): State = {
    val choice = Random.nextDouble()
    fsm(state, choice)
  }

  /** Execute count steps of the FSM */
  def iterate(count: Int, state: State): State = {
    if(count == 0) state
    else {
      iterate(count - 1, step(state))
    }
  }

  def randomColor(): Color =
    Color.hsla(
      (Random.nextDouble() / 3.0 - 0.33).turns, // blues and reds
      Random.nextDouble() / 2.0 + 0.4, // fairly saturated
      Random.nextDouble() / 2.0 + 0.4, // fairly light
      0.7 // Somewhat transparent
    )

  def squiggle(): Image =
    iterate(100, State.initial).toImage.strokeWidth(3.0).strokeColor(randomColor())

  def initialPosition(): Point = {
    // Poisson disk sampling might be more attractive
    Point(Random.nextGaussian() * 150, Random.nextGaussian() * 150)
  }

  def squiggles(): Image =
    (for{
       _ <- 0 to 100
      } yield squiggle().at(initialPosition().toVec)).toList.allOn

  def go() = squiggles().draw()

  go()
}

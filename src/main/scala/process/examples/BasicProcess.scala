package process
package examples

import cats.implicits._
import doodle.core._
import doodle.syntax._
import doodle.image._
import doodle.image.syntax._
import doodle.java2d._
import doodle.java2d.effect.Frame
import scala.util.Random

/** Simple process with no interaction between the elements. */
object BasicProcess extends App {
  /** How much we change the location by when we step forward. */
  val locationDelta = 5.0
  /** How much we change the heading by when we turn. */
  val headingDelta = 30.degrees

  final case class State(location: Point, heading: Angle, path: List[Point]) {
    def forward: State = {
      val newLocation = location + Vec.polar(locationDelta, heading)
      this.copy(
        location = newLocation,
        path = newLocation +: path
      )
    }

    def clockwise: State =
      this.copy(heading = heading + headingDelta)

    def anticlockwise: State =
      this.copy(heading = heading - headingDelta)

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

  def squiggle(initialState: State): Image =
    iterate(100, initialState).toImage.strokeWidth(3.0).strokeColor(randomColor())

  def initialPosition(): Point = {
    // Poisson disk sampling might be more attractive
    Point(Random.nextGaussian() * 150, Random.nextGaussian() * 150)
  }

  def initialDirection(position: Point): Angle =
    (position - Point.zero).angle

  def squiggles(): Image =
    (0 to 500).map{_ =>
      val pt = initialPosition()
      val angle = initialDirection(pt)
      val state = State(pt, angle, List.empty)
      squiggle(state)
    }.toList.allOn

  val frame = Frame.fitToPicture().background(Color.black)
  def go() = squiggles().draw(frame)

  go()
}

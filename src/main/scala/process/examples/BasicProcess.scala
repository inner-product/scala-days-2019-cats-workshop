package process
package examples

import cats.implicits._
import doodle.core._
import doodle.syntax._
import cats.effect.IO
import doodle.image._
import doodle.image.syntax._
import doodle.java2d._
import doodle.java2d.effect.Frame

/** Simple process with no interaction between the elements. */
object BasicProcess extends App {
  implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)

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
  def step(state: State): IO[State] = {
    val choice = Random.double
    choice.map(c => fsm(state, c))
  }

  /** Execute count steps of the FSM */
  def iterate(count: Int, state: State): IO[State] = {
    if(count == 0) IO.pure(state)
    else {
      step(state).flatMap(s => iterate(count - 1, s))
    }
  }

  val randomColor: IO[Color] =
    (Random.double(-0.15, 0.15),
     Random.double(0.4, 0.9),
     Random.double(0.4, 0.9)).parMapN{ (h, s, l) =>
      Color.hsla(h.turns, s, l, 0.7)
    }

  def squiggle(initialState: State): IO[Image] =
    (iterate(100, initialState), randomColor).parMapN((state, color) =>
      state.toImage.strokeWidth(3.0).strokeColor(color)
    )

  val initialPosition: IO[Point] = {
    // Poisson disk sampling might be more attractive
    (Random.gaussian(0.0, 150.0), Random.gaussian(0.0, 150.0)).parMapN((x, y) => Point(x, y))
  }

  def initialDirection(position: Point): Angle =
    (position - Point.zero).angle

  def squiggles(): IO[Image] = {
    val makeSquiggle: IO[Image] =
      for {
        pt <- initialPosition
        angle = initialDirection(pt)
        state = State(pt, angle, List.empty)
        s <- squiggle(state)
      } yield s

    // makeSquiggle.replicateA(500).map(_.allOn)
    (0 to 500)
      .map{_ => makeSquiggle }
      .toList
      .parSequence.map(_.allOn)

    // (0 to 500).toList.parTraverse{ _ => makeSquiggle }.map(_.allOn)
  }

  val frame = Frame.fitToPicture().background(Color.black)
  def go() = squiggles().map(_.draw(frame))

  go().unsafeRunSync()
}

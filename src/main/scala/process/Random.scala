package process

import cats.effect.IO
import scala.util

object Random {
  val double: IO[Double] = IO(util.Random.nextDouble())
  val int: IO[Int] = IO(util.Random.nextInt)

  def double(min: Double, max: Double): IO[Double] =
    IO{
      val range = max - min
      (util.Random.nextDouble() * range) + min
    }

  def int(max: Int): IO[Int] =
    IO(util.Random.nextInt(max))

  val gaussian: IO[Double] =
    IO(util.Random.nextGaussian())

  def gaussian(mean: Double, stdDev: Double): IO[Double] =
    IO((util.Random.nextGaussian * stdDev) + mean)
}

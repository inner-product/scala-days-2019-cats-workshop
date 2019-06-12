# Scala Days 2019 Cats Workshop
## Effects and Monads
- Side effect break substitution
- Effect maintains substitution and would otherwise be a side effect
- Context = Effect
## Cats IO
### Constructing IO
How can we construct an instance of `IO`, that has an effect such as choosing a random `Double` (using, say `scala.util.Random.nextDouble()`)? 

`IO(scala.util.Random.nextDouble())`
vs
```scala
def random: IO[Double] =
  IO.pure(scala.util.Random.nextDouble())
  
val foo = random
for {
 x <- foo
 y <- foo
} yield x + y

for {
 x <- random
 y <- random
} yield x + y
```

(Why is this an effect?) How is this different to constructing a `Future`? Can you observe this difference?

```scala
import cats.effect.IO
import scala.concurrent.{ExecutionContext,Future}
implicit val ec = ExecutionContext.global

val randomIO = IO(scala.util.Random.nextDouble())
val randomFuture = Future(scala.util.Random.nextDouble())
// Future executes immediately
// Future has that annoying ExecutionContext everywhere
def ioExample =
  for {
    x <- randomIO
    y <- randomIO
  } yield x + y

def futureExample =
  for {
    x <- randomFuture
    y <- randomFuture
  } yield x + y
  
println(ioExample.unsafeRunSync)
println(ioExample.unsafeRunSync)

futureExample.foreach(a => println(a))
futureExample.foreach(a => println(a))

val ex1 = IO(Random.nextDouble())
val ex2 = IO(Random.nextDouble())

// vs

val ex1 = IO(Random.nextDouble())
val ex2 = ex1


val ex3 = Future(Random.nextDouble())
val ex4 = Future(Random.nextDouble())

// vs

val ex3 = Future(Random.nextDouble())
val ex4 = ex3
```
### Pure Computations
If we have an `IO[A]` how can we perform a *pure* computation transforming the result of type `A` into a result of type `B`?
`map`
### Impure Computations
If we have an `IO[A]` how can perform a *impure* computation transforming the result of type `A` into a result of type `B` and having some effect?
`flatMap`

```scala
val start: IO[Double] = ???
// Program A
start.flatMap{a =>
  val b = Random.nextDouble()
  IO(a + b)
}

// Program B
start.flatMap{a => IO{ 
    val b = Random.nextDouble()
    a + b
  }
}

start.flatMap(a => IO.pure(a + 42))
```
### Running IO
Eventually we need to run our `IO`. How do we do this?
`unsafeRunSync` (`IO[A] => A`)
`unsafeRunAsync` (`IO[A] => Unit`)
## Finite State Machines
What is a finite state machine?
Why should we care?

A directed graph with a finite number of states (nodes in the graph) and transitions between states (edges in the graph). Transitions are determined by events (which are finite) and are deterministic. Possibly initial and terminal (absorbing) states.

Set `S` of states
Set `E` of events
Transition function `(S, E) => S` (`S => (E => S)`)

Traffic lights
Coffee machine
Regular expression
TCP/IP
Akka messages
UI

Reasoning. Understandability. Comprehensibility.
### Random Cats Tools
Convert `F[G[A]]` to `G[F[A]]` using `sequence`
```scala
import cats.implicits._
import cats.effect.IO

val example: IO[List[Int]] = List(IO(1), IO(2), IO(3)).sequence
```
## Distributed Systems via Abstract Algebra
Associativity a + (b + c) = (a + b) + c
Commutivity a + b = b + a
Hyperloglog
Bloom filter
Count-min sketch
Eventually consistent systems -> CRDTS = commutative idempotent associative operations
## Asynchronous and Concurrent
Thread.currentThread.getName()

Which thread runs IO(println("Hello")).unsafeRunSync()
Which thread runs IO(println("Hello")).unsafeRunAsync(callback...)
If you create an executor service (e.g. the global one)
Which thread runs IO.shift(ec).flatMap(_ => println("Hello"))
## Monoids
combine: (A, A) => A
identity: A

associativity: a + (b + c) = (a + b) + c
identity: a + 0 = a = 0 + a
(commutativity: a + b = b + a)

Integer: +, 0 / *, 1
Non-negative Integers: max, 0
String: ++, ""
Booleans: and, true / or, false / etc.
List: ++, List.empty
Set: union, {}
Option[A] where there is a Monoid[A], identity is None
### Usage in Cats
Easy way: `import cats.implicits._`
or
```scala
import cats._
import cats.syntax.all._
import cats.instances.all._
```

Write a method that accepts two values for all types for which there are `Monoid` instances and combine them.

```scala
def combine[A](a: A, b: A)(implicit m: Monoid[A]): A =
  a |+| b

def combine[A](a: A, b: A)(implicit m: Monoid[A]): A =
  m.combine(a, b)

// Context bound
def combine[A: Moniod](a: A, b: A): A =
  a |+| b
  
// Nope, this won't work
def combine[A <: Moniod[A]](a: A, b: A): A =
  a |+| b
```

Ad-hoc polymorphism (more FP big words)
### Distributed Systems
CRDT = 
- merge which is idempotent commutative monoid (aka bounded semilattice)
- add which is commutative monoid (aka abelian monoid)
=> eventually consistent results always converge to the correct answer
Idempotent: a + a = a
## Higher Kinded Types
`Int` is a type
`List[String]` is a type
`List` is *not* a type. It is a type constructor.
Kind of `Int` is *
Kind of `List` is * => *

F[A] flatMap A => F[B] = F[B]

trait Monad[F[_]] {

}

`Functor` has `map`
`F[A] map A => B = F[B]`

Declare a type class for a `Functor`
Define some instances of `Functor` for, e.g., `List` and `Option`
Define a method that take all types for which there is a `Functor` instance and do something with the input (you'll have to think of this; maybe you want more than one parameter)

```scala
trait Functor[F[_]] {
  def map[A,B](fa: F[A])(f: A => B): F[B]
}
implicit object listFunctor extends Functor[List] {
  def map[A,B](fa: List[A])(f: A => B): List[B] =
    fa.map(f)
}
def doSomething[F[_],A](fa: F[A], f: A => A)(implicit ev: Functor[F]): F[A] =
  ev.map(fa)(f)
```

Define a functor instance for normal types `A` (not higher-kinded). (Hint: you might need a type alias.)

```scala
type Id[A] = A
implicit val idFunctor: Functor[Id] = new Functor[Id] {
  def map[A,B](fa: Id[A])(f: A => B): Id[B] =
    f(fa)
}
```


```scala
type Result[A] = Either[String,A]
implicit def eitherFunctor[E]: Functor[Either[E,?]] =
  new Functor[Either[E,?]] {
    def map[A,B](fa: Either[E,A])(f: A => B): Either[E,B] =
      ???
  }
```

The code above assumes the kind-projector plugin. Without this plugin you must use "type lambdas" which are hideous.

## Monad
What is a monad:
- `flatMap` and `pure`
  `F[A] flatMap (A => F[B]) = F[B]`
  `pure A = F[A]`
- composing sequences of operations within a context or effect
## Applicative
We use the `Semigroupal` not `Applicative` by convention in Scala. It's a different formulation of the same thing.
Parallel composition.
`mapN`
All monads are applicative

product(a,b) 
=
for {
  x <- a
  y <- b
} yield (x,y)
=
a.flatMap(x => b.map(y => (x,y)))

(IO(1),IO(2)).mapN(_ + _)
=
for {
  x <- IO(1)
  y <- IO(2)
} yield x + y

(IO(println("a")), IO(println("b"))).mapN(_ |+| _).unsafeRunSync()

(List("Fail1").asLeft[Unit],  
 List("Fail2").asLeft[Unit]).parMapN((_,_) => 
 println("Hi"))
 
 
(List("Success").asRight[List[String]],   
 List("Fail").asLeft[List[String]]).parMapN((_,_) =>
 println("Hi"))

## Case Studies and Other Explorations
Monad transformers (`EitherT`)
UDP packet reassembly
Error handling / modeling in `Either`
Retry strategies
Have a break

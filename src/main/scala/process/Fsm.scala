package process

final case class Fsm[S,E](f: (S,E) => S) {
  def apply(state: S, event: E): S =
    f(state, event)
}

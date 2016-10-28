package scodec.interop

import scodec._

package object cats extends CatsInstances {

  /** Extension methods for an `Either[Err, A]`. */
  implicit class EitherErrSyntax[A](val self: Either[Err, A]) extends AnyVal {
    /** Converts this either to an attempt. */
    def toAttempt: Attempt[A] = self.fold(Attempt.failure, Attempt.successful)
  }
}

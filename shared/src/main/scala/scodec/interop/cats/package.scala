package scodec.interop

import scodec._

import _root_.cats.data.Xor

package object cats extends CatsInstances {

  /** Extension methods for an `Err Xor A`. */
  implicit class ErrXorSyntax[A](val self: Err Xor A) extends AnyVal {
    /** Converts this xor to an attempt. */
    def toAttempt: Attempt[A] = self.fold(Attempt.failure, Attempt.successful)
  }

  /** Extension methods for an `Attempt[A]`. */
  implicit class AttemptSyntax[A](val self: Attempt[A]) extends AnyVal {
    /** Converts this attempt to an xor. */
    def toXor: Err Xor A = self.fold(Xor.left, Xor.right)
  }
}

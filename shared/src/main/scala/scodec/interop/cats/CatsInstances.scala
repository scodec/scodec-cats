package scodec.interop.cats

import scodec.bits._
import scodec._

import algebra.{ Eq, Monoid, Semigroup }

import _root_.cats._
import _root_.cats.implicits._

private[cats] abstract class CatsInstancesLowPriority {
  implicit final def DecoderSemigroupInstance[A](implicit A: Semigroup[A]): Semigroup[Decoder[A]] =
    new DecoderSemigroup[A]()
}

private[cats] abstract class CatsInstances extends CatsInstancesLowPriority {

  implicit val BitVectorEqInstance: Eq[BitVector] = Eq.fromUniversalEquals
  implicit val BitVectorMonoidInstance: Monoid[BitVector] = new Monoid[BitVector] {
    def empty = BitVector.empty
    def combine(x: BitVector, y: BitVector) = x ++ y
  }

  implicit val ByteVectorEqInstance: Eq[ByteVector] = Eq.fromUniversalEquals
  implicit val ByteVectorMonoidInstance: Monoid[ByteVector] = new Monoid[ByteVector] {
    def empty = ByteVector.empty
    def combine(x: ByteVector, y: ByteVector) = x ++ y
  }

  implicit val ErrEqInstance: Eq[Err] = Eq.fromUniversalEquals

  implicit def AttemptEqInstance[A: Eq]: Eq[Attempt[A]] = Eq.instance { (l, r) =>
    l match {
      case Attempt.Successful(la) =>
        r match {
          case Attempt.Successful(ra) => Eq[A].eqv(la, ra)
          case Attempt.Failure(re) => false
        }
      case Attempt.Failure(le) =>
        r match {
          case Attempt.Successful(ra) => false
          case Attempt.Failure(re) => Eq[Err].eqv(le, re)
        }
    }
  }

  implicit def DecodeResultEqInstance[A: Eq]: Eq[DecodeResult[A]] = Eq.instance { (l, r) =>
    l.value === r.value && l.remainder === r.remainder
  }

  implicit val AttemptMonadErrorInstance: MonadError[Attempt, Err] = new MonadError[Attempt, Err] {
    def pure[A](a: A) = Attempt.successful(a)
    def flatMap[A, B](fa: Attempt[A])(f: A => Attempt[B]) = fa flatMap f
    def raiseError[A](e: Err) = Attempt.failure(e)
    def handleErrorWith[A](fa: Attempt[A])(f: Err => Attempt[A]) =
      fa.fold(f, Attempt.successful)
  }

  implicit val DecoderMonadInstance: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A) = Decoder.point(a)
    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]) = fa flatMap f
  }
  implicit def DecoderMonoidInstance[A](implicit A: Monoid[A]): Monoid[Decoder[A]] =
    new DecoderSemigroup[A]() with Monoid[Decoder[A]] {
      def empty = Decoder.point(A.empty)
    }
}

private class DecoderSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Decoder[A]] {
  def combine(x: Decoder[A], y: Decoder[A]) = new Decoder[A] {
    private lazy val yy = y
    def decode(bits: BitVector) = (for {
      first <- x
      second <- yy
    } yield A.combine(first, second)).decode(bits)
  }
}

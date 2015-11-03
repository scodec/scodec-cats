package scodec.interop.cats

import scodec._
import scodec.bits._

import _root_.cats.implicits._
import _root_.cats.laws.discipline._
import algebra.Eq
import algebra.laws.GroupLaws
import org.scalacheck.{ Arbitrary, Gen, Shrink }
import Arbitrary.arbitrary
import Shrink.shrink
import org.scalatest.{ FunSuite, Matchers }
import org.typelevel.discipline.scalatest.Discipline

class CatsInstancesTests extends FunSuite with Matchers with Discipline {
  implicit val arbBitVector: Arbitrary[BitVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map { b => BitVector(b) })

  implicit val shrinkBitVector: Shrink[BitVector] = Shrink { b =>
    shrink(b.bytes.toArray) map { s => BitVector(s) }
  }

  implicit val arbByteVector: Arbitrary[ByteVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map { b => ByteVector(b) })

  implicit val shrinkByteVector: Shrink[ByteVector] = Shrink { b =>
    shrink(b.toArray) map { s => ByteVector(s) }
  }

  implicit def eqDecoder[A: Eq : Arbitrary]: Eq[Decoder[A]] = Eq.instance { (l, r) =>
    Stream.continually(arbitrary[BitVector].sample).flatten.take(10000).forall { b =>
      Eq[Attempt[DecodeResult[A]]].eqv(l.decode(b), r.decode(b))
    }
  }

  implicit val arbKAttempt: ArbitraryK[Attempt] = new ArbitraryK[Attempt] {
    def synthesize[A: Arbitrary]: Arbitrary[Attempt[A]] = {
      val successful = for {
        value <- arbitrary[A]
      } yield Attempt.successful(value)
      val failure = for {
        err <- arbitrary[Err]
      } yield Attempt.failure(err)
      Arbitrary(Gen.oneOf(successful, failure))
    }
  }

  implicit val eqKAttempt: EqK[Attempt] = new EqK[Attempt] {
    def synthesize[A: Eq]: Eq[Attempt[A]] = Eq[Attempt[A]]
  }

  implicit val arbKDecoder: ArbitraryK[Decoder] = new ArbitraryK[Decoder] {
    def synthesize[A: Arbitrary]: Arbitrary[Decoder[A]] = {
      val successful = for {
        toConsume <- Gen.chooseNum(0, 32)
        value <- arbitrary[A]
      } yield Decoder(b => Attempt.successful(DecodeResult(value, b.drop(toConsume.toLong))))
      val failure = Gen.const(scodec.codecs.fail(Err("failure")))
      Arbitrary(Gen.frequency(10 -> failure, 90 -> successful))
    }
  }

  implicit val arbDecoderInt: Arbitrary[Decoder[Int]] = arbKDecoder.synthesize[Int]

  implicit val arbErr: Arbitrary[Err] = Arbitrary(Gen.alphaStr.map(msg => Err(msg)))

  checkAll("BitVector", GroupLaws[BitVector].monoid)
  checkAll("ByteVector", GroupLaws[ByteVector].monoid)
  checkAll("Decoder[Int]", GroupLaws[Decoder[Int]].monoid)
  // checkAll("Decoder", MonadTests[Decoder].monad[Int, Int, Int]) https://github.com/non/cats/issues/515#issuecomment-137458884
  checkAll("Attempt", MonadErrorTests[Attempt, Err].monadError[Int, Int, Int])
}

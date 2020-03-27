package scodec.interop.cats

import scodec._
import scodec.bits._

import _root_.cats.implicits._
import _root_.cats.laws.discipline._
import _root_.cats.kernel.Eq
import _root_.cats.kernel.laws.discipline.MonoidTests
import org.scalacheck.{ Arbitrary, Cogen, Gen, Shrink }
import Arbitrary.arbitrary
import Shrink.shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline
import org.scalatestplus.scalacheck.Checkers

class CatsInstancesTests extends AnyFunSuite with Matchers with Checkers with FunSuiteDiscipline {
  implicit lazy val arbBitVector: Arbitrary[BitVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map { b => BitVector(b) })

  implicit lazy val shrinkBitVector: Shrink[BitVector] = Shrink { b =>
    shrink(b.bytes.toArray) map { s => BitVector(s) }
  }

  implicit lazy val arbByteVector: Arbitrary[ByteVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map { b => ByteVector(b) })

  implicit lazy val shrinkByteVector: Shrink[ByteVector] = Shrink { b =>
    shrink(b.toArray) map { s => ByteVector(s) }
  }

  implicit val cogenErr: Cogen[Err] =
    Cogen[String].contramap(_.toString)

  // This is pretty dodgy...
  implicit def eqDecoder[A: Eq : Arbitrary]: Eq[Decoder[A]] = Eq.instance { (l, r) =>
    Stream.continually(arbitrary[BitVector].sample).flatten.take(100).forall { b =>
      Eq[Attempt[DecodeResult[A]]].eqv(l.decode(b), r.decode(b))
    }
  }

  implicit def arbAttempt[A: Arbitrary]: Arbitrary[Attempt[A]] = {
    val successful = for {
      value <- arbitrary[A]
    } yield Attempt.successful(value)
    val failure = for {
      err <- arbitrary[Err]
    } yield Attempt.failure(err)
    Arbitrary(Gen.oneOf(successful, failure))
  }

  implicit def arbDecoder[A: Arbitrary]: Arbitrary[Decoder[A]] = {
    val successful = for {
      toConsume <- Gen.chooseNum(0, 32)
      value <- arbitrary[A]
    } yield Decoder(b => Attempt.successful(DecodeResult(value, b.drop(toConsume.toLong))))
    val failure = Gen.const(scodec.codecs.fail(Err("failure")))
    Arbitrary(Gen.frequency(10 -> failure, 90 -> successful))
  }

  implicit lazy val arbErr: Arbitrary[Err] = Arbitrary(Gen.alphaStr.map(msg => Err(msg)))
  implicit def tripleEq[A, C, B](implicit A: Eq[A], B: Eq[B], C: Eq[C]): Eq[(A, B, C)] = new Eq[(A, B, C)] {
    def eqv(x: (A, B, C), y: (A, B, C)) = A.eqv(x._1, y._1) && B.eqv(x._2, y._2) && C.eqv(x._3, y._3)
  }

  checkAll("BitVector", MonoidTests[BitVector].monoid)
  checkAll("ByteVector", MonoidTests[ByteVector].monoid)
  checkAll("Decoder[Int]", MonoidTests[Decoder[Int]].monoid)
  checkAll("Decoder", MonadTests[Decoder].monad[Int, Int, Int])
  checkAll("Attempt", MonadErrorTests[Attempt, Err].monadError[Int, Int, Int])
}

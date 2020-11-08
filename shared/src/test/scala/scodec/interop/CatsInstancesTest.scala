/*
 * Copyright (c) 2013, Scodec
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package scodec.interop.cats

import scodec._
import scodec.bits._

import _root_.cats.implicits._
import _root_.cats.laws.discipline._
import _root_.cats.kernel.Eq
import _root_.cats.kernel.laws.discipline.MonoidTests
import org.scalacheck.{Arbitrary, Cogen, Gen, Shrink}
import Arbitrary.arbitrary
import Shrink.shrink

import munit.DisciplineSuite

class CatsInstancesTests extends DisciplineSuite {
  implicit lazy val arbBitVector: Arbitrary[BitVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map(b => BitVector(b)))

  implicit lazy val shrinkBitVector: Shrink[BitVector] = Shrink { b =>
    shrink(b.bytes.toArray).map(s => BitVector(s))
  }

  implicit lazy val arbByteVector: Arbitrary[ByteVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map(b => ByteVector(b)))

  implicit lazy val shrinkByteVector: Shrink[ByteVector] = Shrink { b =>
    shrink(b.toArray).map(s => ByteVector(s))
  }

  implicit val cogenErr: Cogen[Err] =
    Cogen[String].contramap(_.toString)

  // This is pretty dodgy...
  implicit def eqDecoder[A: Eq: Arbitrary]: Eq[Decoder[A]] = Eq.instance { (l, r) =>
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
  implicit def tripleEq[A, C, B](implicit A: Eq[A], B: Eq[B], C: Eq[C]): Eq[(A, B, C)] =
    new Eq[(A, B, C)] {
      def eqv(x: (A, B, C), y: (A, B, C)) =
        A.eqv(x._1, y._1) && B.eqv(x._2, y._2) && C.eqv(x._3, y._3)
    }

  checkAll("BitVector", MonoidTests[BitVector].monoid)
  checkAll("ByteVector", MonoidTests[ByteVector].monoid)
  checkAll("Decoder[Int]", MonoidTests[Decoder[Int]].monoid)
  checkAll("Decoder", MonadErrorTests[Decoder, Err].monadError[Int, Int, Int])
  checkAll("Attempt (MonadError)", MonadErrorTests[Attempt, Err].monadError[Int, Int, Int])
  checkAll(
    "Attempt (Traverse)",
    TraverseTests[Attempt].traverse[Int, Int, Int, Int, Option, Option]
  )
}

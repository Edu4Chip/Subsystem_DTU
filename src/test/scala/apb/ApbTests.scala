package apb

import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import apb.ApbMux

import misc.FormalHelper
import apb.ApbArbiter
import apb.ApbBfm

class ApbTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.enableProperties()

  "ApbMux" should "satisfy properties" in {
    verify(
      new ApbMux(
        10,
        32,
        Seq(
          ApbTarget("t0", 0x000, 8),
          ApbTarget("t1", 0x100, 8),
          ApbTarget("t2", 0x200, 8),
          ApbTarget("t3", 0x300, 8)
        )
      ),
      Seq(BoundedCheck(10))
    )
  }

  "ApbArbiter" should "satisfy properties" in {
    verify(new apb.ApbArbiter(16, 32), Seq(BoundedCheck(10)))
  }

}

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


  behavior of "ApbMux"

  it should "satisfy properties" in {
    verify(
      new ApbMux(
        10,
        32,
        Seq(
          ApbTarget("t0", 0x000, 8),
          ApbTarget("t1", 0x100, 8),
          ApbTarget("t2", 0x200, 8),
          // 0x300-> 0x3ff is unmapped
        )
      ),
      Seq(BoundedCheck(10))
    )
  }

  it should "respond with error for unmapped address" in {
    test(new ApbMux(
        10,
        32,
        Seq(
          ApbTarget("t0", 0x000, 8),
          ApbTarget("t1", 0x100, 8),
          ApbTarget("t2", 0x200, 8),
        )
      )).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val bfm = new ApbBfm(dut.clock, dut.io.master)
        bfm.write(0x300, 0x12345678).expectError()
      }
  }

  "ApbArbiter" should "satisfy properties" in {
    verify(new apb.ApbArbiter(16, 32), Seq(BoundedCheck(10)))
  }

}

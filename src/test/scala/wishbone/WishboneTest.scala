package wishbone

import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import apb.ApbMux

import misc.FormalHelper
import apb.ApbArbiter
import apb.ApbBfm
import misc.BusTarget

class WishboneTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.enableProperties()

  "WishboneToApb" should "satisfy properties" in {
    verify(
      new WishboneToApb(8),
      Seq(BoundedCheck(15))
    )
  }

  "WishboneMux" should "satisfy properties" in {
    verify(
      new WishboneMux(
        10,
        Seq(
          BusTarget("t0", 0x000, 8),
          BusTarget("t1", 0x100, 8),
          BusTarget("t2", 0x200, 8),
          // 0x300-> 0x3ff is unmapped
        )
      ),
      Seq(BoundedCheck(12))
    )
  }

}
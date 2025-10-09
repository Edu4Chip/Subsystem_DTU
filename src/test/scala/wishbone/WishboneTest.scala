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

class WishboneTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.enableProperties()

  "WishboneToApb" should "satisfy properties" in {
    verify(
      new WishboneToApb(8),
      Seq(BoundedCheck(15))
    )
  }

}
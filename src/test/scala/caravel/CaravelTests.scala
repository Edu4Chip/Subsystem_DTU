package caravel

import chisel3._
import chiseltest._
import chiseltest.formal._

import org.scalatest.flatspec.AnyFlatSpec

class CaravelTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  "RegisterFileTest" should "satisfy properties" in {
    verify(new RegisterFileTest, Seq(BoundedCheck(12)))
  }

  "WishboneGpioTest" should "satisfy properties" in {
    verify(new WishboneGpio(8), Seq(BoundedCheck(12)))
  }


}

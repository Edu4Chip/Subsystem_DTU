package dtu

import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import apb.ApbMux

import misc.FormalHelper
import apb.ApbArbiter
import apb.ApbBfm

class PeripheralTests
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Formal {

  FormalHelper.enableProperties()

  "DataMemMux" should "satisfy formal properties" in {
    verify(
      new dtu.DataMemMux(
        16,
        Seq(
          dtu.DataMemMux.Target("t0", 0x0000, 8),
          dtu.DataMemMux.Target("t1", 0x1000, 8),
          dtu.DataMemMux.Target("t2", 0x2000, 8),
          dtu.DataMemMux.Target("t3", 0x3000, 8)
        )
      ),
      Seq(BoundedCheck(10))
    )
  }

  "RegBlock" should "satisfy Apb properties" in {
    verify(new dtu.peripherals.RegBlock(16), Seq(BoundedCheck(10)))
  }

  "SystemControl" should "satisfy Apb properties" in {
    verify(new dtu.SystemControl, Seq(BoundedCheck(10)))
  }

}

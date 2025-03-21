import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import apb.ApbMux

import misc.FormalHelper

class FormalTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.withPropertiesEnabled {

    "InstructionMemory" should "satisfy properties" in {
      verify(new dtu.InstructionMemory(64), Seq(BoundedCheck(10)))
    }

    "PonteDecoder" should "satisfy properties" in {
      verify(new ponte.PonteDecoder, Seq(BoundedCheck(10)))
    }

    "ApbMux" should "satisfy properties" in {
      verify(
        new ApbMux(
          10,
          32,
          Seq(
            ApbMux.Target("t0", 0x000, 8),
            ApbMux.Target("t1", 0x100, 8),
            ApbMux.Target("t2", 0x200, 8),
            ApbMux.Target("t3", 0x300, 8)
          )
        ),
        Seq(BoundedCheck(10))
      )
    }

    "DataMemMux" should "satisfy properties" in {
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

    "RegBlock" should "satisfy properties" in {
      verify(new dtu.peripherals.RegBlock(16), Seq(BoundedCheck(10)))
    }

  }

}

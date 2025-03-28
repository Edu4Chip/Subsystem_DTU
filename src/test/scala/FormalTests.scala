import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import apb.ApbMux

import misc.FormalHelper
import apb.ApbArbiter
import apb.ApbBfm

class FormalTests extends AnyFlatSpec with ChiselScalatestTester with Formal {

  FormalHelper.enableProperties()

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

  "ApbArbiter" should "satisfy properties" in {
    verify(new apb.ApbArbiter(16, 32), Seq(BoundedCheck(10)))
  }

  "ApbArbiter" should "fail" in {
    test(new ApbArbiter(16, 32)).withAnnotations(Seq(WriteVcdAnnotation)) {
      dut =>
        val left = new ApbBfm(dut.clock, dut.io.masters(0))
        val right = new ApbBfm(dut.clock, dut.io.masters(1))

        dut.io.masters(0).paddr.poke(0xa.U)
        dut.io.masters(0).psel.poke(1.B)
        dut.io.masters(1).paddr.poke(0xb.U)
        dut.io.masters(1).psel.poke(1.B)

        dut.clock.step()

        dut.io.masters(0).penable.poke(1.B)
        dut.io.masters(1).penable.poke(1.B)

        dut.clock.step(2)

        dut.io.merged.pready.poke(1.B)

        dut.clock.step()

    }
  }

}

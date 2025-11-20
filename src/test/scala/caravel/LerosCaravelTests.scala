package caravel

import org.scalatest.flatspec.AnyFlatSpec

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.matchers.should.Matchers
import dtu.DtuSubsystemConfig
import mem._
import misc.TestingHelper.ClockExtension
import apb.ApbTargetBfm.Write


class LerosCaravelAdderTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "LerosCaravel"

  it should "pass adder test" in {
  
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(MemoryFactory.using(RegMemory)(new LerosCaravel(config, "RegMemory"))).withAnnotations(
      Seq(WriteVcdAnnotation)
    ) { dut =>
      val bfm = new LerosCaravelTestHarness(dut, dut.clock, 0)

      dtu.AdderTest(bfm, dut.clock)
    }
  }

}

class LerosCaravelSelfTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "LerosCaravel"

  it should "pass self-test" in {
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(MemoryFactory.using(ChiselSyncMemory)(new LerosCaravel(config, "ChiselSyncMemory"))).withAnnotations(
      Seq(WriteVcdAnnotation)
    ) { dut =>
      val bfm = new LerosCaravelTestHarness(dut, dut.clock, 0)
      dtu.SelfTest(bfm, dut.clock, 0x12345678, math.min(config.gpioPins - 4, 8))
    }
  }

}

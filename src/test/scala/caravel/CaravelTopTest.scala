package caravel

import org.scalatest.flatspec.AnyFlatSpec

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.matchers.should.Matchers
import dtu.DtuSubsystemConfig
import mem._
import misc.TestingHelper.ClockExtension
import org.json4s.reflect.Memo

class CaravelTopAdderTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "CaravelTop"

  it should "pass adder test" in {


    test(MemoryFactory.usingOverride(mem.ChiselSyncMemory)(new CaravelTop(1_000_000))).withAnnotations(
      Seq()
    ) { dut =>
      val cframBfm = new LerosCaravelTestHarness(dut, dut.clock, 0)
      val sky130Bfm = new LerosCaravelTestHarness(dut, dut.clock, 0x1000)
      val dffRamBfm = new LerosCaravelTestHarness(dut, dut.clock, 0x2000)
      val rtlRamBfm = new LerosCaravelTestHarness(dut, dut.clock, 0x3000)

      dtu.AdderTest(cframBfm, dut.clock)
      dtu.AdderTest(sky130Bfm, dut.clock)
      dtu.AdderTest(dffRamBfm, dut.clock)
      dtu.AdderTest(rtlRamBfm, dut.clock)
    }
  }

}

class CaravelTopSelfTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "CaravelTop"

  it should "pass self-test" in {


    test(MemoryFactory.usingOverride(mem.ChiselSyncMemory)(new CaravelTop(1_000_000))).withAnnotations(
      Seq()
    ) { dut =>
      val cframBfm = new LerosCaravelTestHarness(dut, dut.clock, 0)
      val sky130Bfm = new LerosCaravelTestHarness(dut, dut.clock, 0x1000)
      val dffRamBfm = new LerosCaravelTestHarness(dut, dut.clock, 0x2000)
      val rtlRamBfm = new LerosCaravelTestHarness(dut, dut.clock, 0x3000)

      dtu.SelfTest(cframBfm, dut.clock, 0x12345678, CaravelTopConfig.gpioPerLeros - 4)
      dtu.SelfTest(sky130Bfm, dut.clock, 0xdeadbeef, CaravelTopConfig.gpioPerLeros - 4)
      dtu.SelfTest(dffRamBfm, dut.clock, 0xabcdef01, CaravelTopConfig.gpioPerLeros - 4)
      dtu.SelfTest(rtlRamBfm, dut.clock, 0x0fedcba9, CaravelTopConfig.gpioPerLeros - 4)
    }
  }

}

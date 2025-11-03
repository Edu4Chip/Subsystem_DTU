package dtu

import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec
import dtu.DtuSubsystem
import misc.TestingHelper.ClockExtension
import mem.MemoryFactory
import mem.RegMemory
import dtu.DtuSubsystemConfig
import misc.FormalHelper
import java.io.File
import os.group.set
import org.scalatest.matchers.should.Matchers
import mem.DidacticSram
import circt.stage.ChiselStage
import mem.ChiselSyncMemory
import chiseltest.simulator.VerilatorCFlags

class IntegrationTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DTU Subsystem"

  FormalHelper.enableProperties()

  val memoryFactory = RegMemory

  val config = DtuSubsystemConfig.default
    .copy(
      romProgramPath = "leros-asm/didactic.s",
      lerosBaudRate = 100000000
    )

  def testProgram(dut: DtuTestHarness)(prog: String): Unit = {
    val bfm = new DtuTestHarnessBfm(dut)

    bfm.resetLerosEnable()
    bfm.selectBootRam()
    bfm.uploadProgram(prog)
    bfm.resetLerosDisable()

    while (!dut.io.dbg.exit.peekBoolean()) {
      dut.clock.step()
    }

    val res = dut.io.dbg.accu
      .expect(1.U, s"[$prog] Accu shall be one at the end of a test case.\n")
  }

  val progs = new File("leros/asm/test")
    .listFiles()
    .filter(_.isFile)
    .map(_.toString())

  it should "correctly execute all Leros test programs" in {
    test(MemoryFactory.using(memoryFactory)(new DtuTestHarness(config))).withAnnotations(
      Seq()
    ) { dut =>
      progs.foreach(testProgram(dut))
    }
  }
}

object AdderTest {

  def apply(bfm: DtuInterface, clk: Clock) = {
    def ready(): Boolean = {
        bfm.readCrossCoreReg(0) == 1
      }

      def setValid(b: Boolean): Unit = {
        bfm.writeCrossCoreReg(0, if (b) 1 else 0)
      }

      def loadValues(a: Int, b: Int): Unit = {
        bfm.writeCrossCoreReg(1, a)
        bfm.writeCrossCoreReg(2, b)
      }

      def getResult(): Int = {
        bfm.readCrossCoreReg(3)
      }

      def add(a: Int, b: Int): Int = {
        loadValues(a, b)
        setValid(true)
        clk.stepUntil(ready())
        clk.stepUntil(!ready())
        setValid(false)
        clk.stepUntil(ready())
        getResult()
      }

      bfm.resetLerosEnable()
      bfm.selectBootRam()
      bfm.enableUartLoopBack()
      bfm.uploadProgram("leros-asm/didactic_adder.s")
      bfm.resetLerosDisable()

      assert(add(1, 2) == 3, "Adder test failed for 1 + 2")

      assert(add(10, 20) == 30, "Adder test failed for 10 + 20")

      assert(add(100, 200) == 300, "Adder test failed for 100 + 200")

      assert(add(0xffffffff, 0x0001) == 0x0000, "Adder test failed for overflow case")
  }

}

class AdderTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "DTU Subsystem"

  it should "pass adder test" in {
  
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(MemoryFactory.using(RegMemory)(new DtuTestHarness(config))).withAnnotations(
      Seq()
    ) { dut =>
      val bfm = new DtuTestHarnessBfm(dut)

      AdderTest(bfm, dut.clock)
    }
  }

}


object SelfTest {

  def apply(bfm: DtuInterface, clk: Clock, seed: Int, gpioPins: Int): Unit = {
    def startSelfTest(seed: Int) = {
      bfm.writeCrossCoreReg(0, seed)
    }

    def done: Boolean = {
      bfm.readCrossCoreReg(0) == 1
    }

    def result: Int = {
      bfm.readCrossCoreReg(1)
    }

    def selftest(seed: Int): Int = {
      startSelfTest(seed)
      clk.stepUntil(done)
      result
    }

    bfm.resetLerosEnable()
    bfm.selectBootRam()
    bfm.enableUartLoopBack()
    bfm.uploadProgram("leros-asm/selftest.s")
    bfm.resetLerosDisable()

    assert(selftest(seed) == (seed & ((1 << gpioPins) - 1)), s"Self-test failed with seed 0x${seed.toHexString} got 0x${result.toHexString} expected 0x${(seed & ((1 << gpioPins) - 1)).toHexString}")
  }

}

class SelfTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "DTU Subsystem"

  it should "pass self-test" in {
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(MemoryFactory.using(ChiselSyncMemory)(new DtuTestHarness(config))).withAnnotations(
      Seq()
    ) { dut =>
      val bfm = new DtuTestHarnessBfm(dut)

      SelfTest(bfm, dut.clock, 0xdeadbeef, math.min(config.gpioPins - 4, 8))
    }
  }

}

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
import mem.DidacticSpSram
import circt.stage.ChiselStage
import mem.ChiselSyncMemory
import chiseltest.simulator.VerilatorCFlags

class IntegrationTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DTU Subsystem"

  FormalHelper.enableProperties()

  MemoryFactory.use(RegMemory.create)
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
    test(new DtuTestHarness(config)).withAnnotations(
      Seq()
    ) { dut =>
      progs.foreach(testProgram(dut))
    }
  }
}

class AdderTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "DTU Subsystem"

  it should "pass adder test" in {
    MemoryFactory.use(RegMemory.create)
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(new DtuTestHarness(config)).withAnnotations(
      Seq()
    ) { dut =>
      val bfm = new DtuTestHarnessBfm(dut)

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
        dut.clock.stepUntil(ready())
        dut.clock.stepUntil(!ready())
        setValid(false)
        dut.clock.stepUntil(ready())
        getResult()
      }

      bfm.resetLerosEnable()
      bfm.selectBootRam()
      bfm.enableUartLoopBack()
      bfm.uploadProgram("leros-asm/didactic_adder.s")
      bfm.resetLerosDisable()

      add(1, 2) shouldEqual 3

      add(10, 20) shouldEqual 30

      add(100, 200) shouldEqual 300

      add(0xffffffff, 0x0001) shouldEqual 0x0000 // Overflow case
    }
  }

}

class SelfTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  behavior of "DTU Subsystem"

  it should "pass self-test" in {
    MemoryFactory.use(ChiselSyncMemory.create)
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(new DtuTestHarness(config)).withAnnotations(
      Seq()
    ) { dut =>
      val bfm = new DtuTestHarnessBfm(dut)

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
        dut.clock.stepUntil(done)
        result
      }

      bfm.resetLerosEnable()
      bfm.selectBootRam()
      bfm.enableUartLoopBack()
      bfm.uploadProgram("leros-asm/selftest.s")
      bfm.resetLerosDisable()

      selftest(0xdeadbeef) shouldEqual 0xef
    }
  }

}

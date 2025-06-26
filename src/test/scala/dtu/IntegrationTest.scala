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

class IntegrationTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DTU Subsystem"

  FormalHelper.enableProperties()

  MemoryFactory.use(RegMemory.create)
  val config = DtuSubsystemConfig.default
    .copy(
      romProgramPath = "leros-asm/didactic.s",
      lerosBaudRate = 100000000
    )

  def testProgram(prog: String)(dut: DtuTestHarness): Unit = {
    val bfm = new DtuTestHarnessBfm(dut)

    bfm.resetLerosEnable()
    bfm.selectBootRam()
    bfm.uploadProgram(prog)
    bfm.resetLerosDisable()

    while (!dut.io.dbg.exit.peekBoolean()) {
      dut.clock.step()
    }

    val res = dut.io.dbg.accu
      .expect(1.U, "Accu shall be one at the end of a test case.\n")
  }

  val progs = new File("leros/asm/test")
    .listFiles()
    .filter(_.isFile)
    .map(_.toString())

  println(progs.mkString(", "))
  progs.foreach { prog =>
    it should s"upload and execute $prog" in {
      println(s"Testing program: $prog")
      test(new DtuTestHarness(config))(testProgram(prog))
    }
  }
}

class SelfTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DTU Subsystem"

  it should "pass self-test" in {
    MemoryFactory.use(RegMemory.create)
    val config = DtuSubsystemConfig.default
      .copy(
        romProgramPath = "leros-asm/didactic.s",
        lerosBaudRate = 100000000
      )

    test(new DtuTestHarness(config)) { dut =>
      val bfm = new DtuTestHarnessBfm(dut)

      bfm.resetLerosEnable()
      bfm.selectBootRam()
      bfm.enableUartLoopBack()
      bfm.uploadProgram("leros-asm/didactic.s")
      bfm.resetLerosDisable()

      dut.clock.step(100)
    }
  }

}

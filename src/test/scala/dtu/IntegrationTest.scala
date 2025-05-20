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

class IntegrationTest
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Formal {

  behavior of "DTU Subsystem"

  FormalHelper.enableProperties()

  MemoryFactory.use(RegMemory.create)
  val config = DtuSubsystemConfig.default
    .copy(
      romProgramPath = "leros-asm/didactic.s",
      lerosBaudRate = 100000000
    )

  it should "run" in {
    test(new DtuTestHarness(config))
      .withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) {
        dut =>
          val bfm = new DtuTestHarnessBfm(dut)

          bfm.resetLerosEnable()
          bfm.selectBootRam()

          bfm.uploadProgram("leros-asm/didactic.s")
          dut.clock.step(5)

          bfm.resetLerosDisable()
          dut.clock.step(200)

          bfm.resetLerosEnable()
          bfm.selectBootRom()
          dut.clock.step(5)
          bfm.resetLerosDisable()
          dut.clock.step(200)
      }
  }
}

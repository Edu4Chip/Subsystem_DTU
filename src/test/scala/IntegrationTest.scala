import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import dtu.DtuSubsystem
import misc.TestingHelper.ClockExtension
import mem.MemoryFactory
import mem.RegMemory
import dtu.DtuSubsystemConfig
import misc.FormalHelper

class IntegrationTest extends AnyFlatSpec with ChiselScalatestTester {

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
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        val apbBfm = new apb.ApbBfm(dut.clock, dut.io.apb)

        apbBfm.write(0xC00, 0x3).unwrap()

        val blop = leros.util.Assembler.assemble("leros-asm/didactic.s")
        val words = blop
          .grouped(2)
          .map {
            case Array(a, b) => (BigInt(b) << 16) | a
            case Array(a)    => BigInt(a)
          }
          .toSeq

        words.zipWithIndex.foreach {
          case (word, i) =>
            apbBfm.write(i * 4, word).unwrap()
        }
        dut.clock.step(5)

        apbBfm.write(0xC00, 0x2).unwrap()
        dut.clock.step(200)

        apbBfm.write(0xC00, 0x1).unwrap()
        dut.clock.step(5)
        apbBfm.write(0xC00, 0x0).unwrap()
        dut.clock.step(200)
      }
  }
}

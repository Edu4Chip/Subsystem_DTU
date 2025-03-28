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
      uartBaudRate = 50000000
      )
  


  it should "run" in {
    test(new DtuTestHarness(config))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.io.resetLeros.poke(1.B)
        dut.clock.step(4)
        dut.io.bootSel.poke(1.B)
        dut.clock.step()

        val apbBfm = new apb.ApbBfm(dut.clock, dut.io.apb)

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

        dut.io.resetLeros.poke(0.B)
        dut.clock.step(400)

        dut.io.resetLeros.poke(1.B)
        dut.io.bootSel.poke(0.B)
        dut.clock.step(5)
        dut.io.resetLeros.poke(0.B)
        dut.clock.step(200)
      }
  }
}

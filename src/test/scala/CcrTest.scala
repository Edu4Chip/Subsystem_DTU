import scala.sys._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.util.experimental.BoringUtils
import ApbRequester._

import leros._
import config.APB_ADDR_SPACE
import config.APB_CONFIG

class CcrTest extends AnyFlatSpec with ChiselScalatestTester {
  System.setProperty("testpath", "leros/asm/test")
  val progs = leros.shared.Util.getProgs()  

  def testFun(dut: DtuTopTest): Unit = {
  
    var run = true
    var maxCycles = 1000

    while (run) {
      val pc = dut.io.dbg.pc.peekInt()
      val accu = dut.io.dbg.accu.peekInt()
      val instr = dut.io.dbg.instr.peekInt()
      // Predef.printf("pc: 0x%04x instr: 0x%04x accu: 0x%08x\n", pc, instr, accu)
      dut.clock.step(1)
      maxCycles -= 1
      run = dut.io.dbg.exit.peekInt() == 0 && maxCycles > 0
      assert(maxCycles > 0, "Running out of cycles")
    }
    val res = dut.io.dbg.accu.expect(1.U, "Accu shall be one at the end of a test case.\n")
  }

  "Leros HW " should s"pass CCR Test" in { 
    test(new DtuTopTest("sw/ccr.s")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // boot from ROM
      dut.io.pmod0.gpi.poke(0.U)

      // 2 cycle reset sync
      dut.clock.step(2)

      testFun(dut)

      val rdAddr = APB_CONFIG.BASE_ADDR + APB_ADDR_SPACE.READ_CCR_START
      println(apbRead(dut, rdAddr))

    }
  }

  
}

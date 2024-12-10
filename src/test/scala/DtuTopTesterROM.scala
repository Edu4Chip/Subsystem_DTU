import scala.sys._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.util.experimental.BoringUtils

import leros._

class DtuTopTesterROM extends AnyFlatSpec with ChiselScalatestTester {
  System.setProperty("testpath", "leros/asm/test")
  val progs = leros.shared.Util.getProgs()  
  progs.foreach(p => {
    val program = p + ".s"

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

    "Leros HW " should s"pass $program" in {      
      test(new DtuTopTest(prog = program))
        .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        testFun(dut)
      }
    }

  })
}

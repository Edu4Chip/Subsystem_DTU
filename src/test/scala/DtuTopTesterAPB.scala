import scala.sys._
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import leros._
import leros.util._
import config._
import java.io._
import ApbRequester._

class DtuTopTesterAPB extends AnyFlatSpec with ChiselScalatestTester {
  System.setProperty("testpath", "leros/asm/test")     

  def programLeros(dut: DtuTopTest, program: String): Unit = {
    val bin: Array[Int] = Assembler.getProgram(program)

    val resetRegAddr = APB_CONFIG.BASE_ADDR + APB_ADDR_SPACE.RESET_REG
    val iMemAddrStart = APB_CONFIG.BASE_ADDR + APB_ADDR_SPACE.IMEM_START
    val iMemAddrEnd = APB_CONFIG.BASE_ADDR + APB_ADDR_SPACE.IMEM_END
    val iMemSize = iMemAddrEnd - iMemAddrStart

    apbWrite(dut, resetRegAddr, 1)
    for(i <- 0 until bin.length) {
      apbWrite(dut, i + iMemAddrStart, bin(i))
    }

    apbWrite(dut, resetRegAddr, 0)
  }

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
  
  "Leros HW " should "pass" in {      
    test(new DtuTopTest(prog = "leros/asm/test/base.s")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.pmod0.gpi.poke("b0001".U)

      val progs = leros.shared.Util.getProgs() 
      progs.foreach(p => {
        val program = p + ".s"
        dut.clock.step(2) // 2 clock cycle reset sync      

        println(s"Start loading $program")
        programLeros(dut, program)

        println(s"Finished loading, start execution of $program")
        testFun(dut)
        
        println(s"Successfully executed $program")
        println()
      })      

    }
  }
}

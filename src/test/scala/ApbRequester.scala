import chisel3._
import chisel3.util._
import chiseltest._
import io.ApbTargetPort

object ApbRequester {
    def apbWrite(dut: DtuTopTest, addr: Int, wdata: Int) = {
    // setup phase 
    dut.io.apb.paddr.poke(addr.U)
    dut.io.apb.pwrite.poke(true.B)
    dut.io.apb.psel.poke(true.B)
    dut.io.apb.penable.poke(false.B)
    dut.io.apb.pwdata.poke(wdata.U)
    dut.clock.step()

    // access phase
    dut.io.apb.penable.poke(true.B)
    dut.io.apb.pslverr.expect(false.B)
    dut.io.apb.pready.expect(true.B)
    dut.clock.step()

    // finish
    dut.io.apb.paddr.poke(0.U)
    dut.io.apb.pwrite.poke(false.B)
    dut.io.apb.psel.poke(false.B)
    dut.io.apb.penable.poke(false.B)
    dut.io.apb.pwdata.poke(0.U)
    dut.clock.step()              
  }

  def apbRead(dut: DtuTopTest, addr: Int): Int = {
    // setup phase
    dut.io.apb.paddr.poke(addr.U)
    dut.io.apb.pwrite.poke(false.B)
    dut.io.apb.psel.poke(true.B)
    dut.io.apb.penable.poke(false.B)
    dut.io.apb.pwdata.poke(0.U)
    dut.clock.step()

    // access phase
    dut.io.apb.penable.poke(true.B)
    dut.io.apb.pslverr.expect(false.B)
    dut.io.apb.pready.expect(true.B)
    val rdData = dut.io.apb.prdata.peekInt()
    dut.clock.step()                                                    


    // finish + read out read data
    dut.io.apb.paddr.poke(0.U)
    dut.io.apb.pwrite.poke(false.B)
    dut.io.apb.psel.poke(false.B)
    dut.io.apb.penable.poke(false.B)
    dut.io.apb.pwdata.poke(0.U)
    dut.clock.step()

    rdData.toInt
  }  
}
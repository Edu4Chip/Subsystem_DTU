import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

import leros._
import leros.wrmem._

/*
 * DTU Top level 
 * Some notes on reset strategy:
    * By default Chisel generates synchronous active high reset
    * Remainder of SoC uses an asynchronous active low reset 
    * Therefore DTU top level also expects an asyncronous active low reset
    * A verilog blackbox module is added that generates a synchronous active high reset from an asynchrnous active low reset 
*/

class DtuTop(addrWidth:Int = 32, dataWidth:Int = 32, prog:String = "", resetSyncFact:() => ResetSyncBase = () => Module(new ResetSync())) extends Module {
  val io = IO(new Bundle {
    // Interface: APB
    val apb = new ApbTargetPort(addrWidth, dataWidth)    

    // Clock and reset are added implicity
    // Reset is asynchronous active low

    // Interface: IRQ
    val irq1 = Output(Bool())

    // Interface: ss_ctrl
    val irqEn1 = Input(Bool())
    val ssCtrl1 = Input(UInt(8.W))

    // Interface: GPIO pmod 0 
    val pmod0 = new PmodGpioPort()

    // Interface GPIO pmod 1
    val pmod1 = new PmodGpioPort()    
  })

  // Generate a synchronous active high reset
  val ResetSync = resetSyncFact()
  ResetSync.io.clock := clock
  ResetSync.io.resetIn := reset

  val lerosSize = 32
  val lerosMemAddrWidth = 8
  val lerosClockFreq = 100000000
  val lerosUartBaudrate = 115200

  // All modules instantiated here are reset by synchronous active high reset
  // All registers must be instantiated within this to ensure all have the same reset
  val leros = withReset(ResetSync.io.resetOut) {
    val ApbRegs = Module(new ApbRegTarget(addrWidth, dataWidth, 0x01050000, 5))
    io.apb <> ApbRegs.io.apb
    
    // pmod 0 set to output
    val pmod0oeReg = RegInit(0.U(8.W))
    val pmod0gpoReg = RegInit(0.U(1.W))
    io.pmod0.oe := pmod0oeReg
    io.pmod0.gpo := pmod0gpoReg

    if (!prog.isEmpty) {
      val leros = Module(new Leros(prog = "notused", size = lerosSize, memAddrWidth = lerosMemAddrWidth))
      val instrMem = Module(new InstrMem(lerosMemAddrWidth, prog))
      instrMem.io <> leros.imemIO
      
      // val instrMem = Module(new WrInstrMemory(lerosMemAddrWidth, lerosClockFreq, lerosUartBaudrate))
      // instrMem.io.instrAddr := leros.imemIO.addr
      // leros.imemIO.instr := instrMem.io.instr
      // instrMem.io.uartRX := io.pmod0.gpi(0)
      
      val dataMem = Module(new DataMem(lerosMemAddrWidth, false))
      dataMem.io <> leros.dmemIO
      
      // IO is now mapped to 0x0f00, but wrAddr counts in 32-bit words
      when((leros.dmemIO.wrAddr === 0x03c0.U) &&  leros.dmemIO.wr) {
        dataMem.io.wr := false.B
        pmod0oeReg := 1.U
        pmod0gpoReg := leros.dmemIO.wrData(7, 0)
      }
      leros
    } else null.asInstanceOf[Leros]
  }

  // interrup not generated
  io.irq1 := false.B

  // pmod 1 set to output
  io.pmod1.oe := 0.U
  io.pmod1.gpo := 0.U


}

object DtuTop extends App {
  (new ChiselStage).emitSystemVerilog(new DtuTop(addrWidth=32, dataWidth=32), Array("-X", "sverilog", "-e", "sverilog", "--target-dir", "src/sv"))
}

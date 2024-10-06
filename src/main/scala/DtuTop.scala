import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

/*
 * DTU Top level 
 * Some notes on reset strategy:
    * By default Chisel generates synchronous active high reset
    * Remainder of SoC uses an asynchronous active low reset 
    * Therefore DTU top level also expects an asyncronous active low reset
    * A verilog blackbox module is added that generates a synchronous active high reset from an asynchrnous active low reset 
*/

class DtuTop(addrWidth:Int = 10, dataWidth:Int = 32) extends Module {
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
  val ResetSync = Module(new ResetSync())
  ResetSync.io.clock := clock
  ResetSync.io.resetIn := reset

  // All modules instantiated here are reset by synchronous active high reset
  // All registers must be instantiated within this to ensure all have the same reset
  withReset(ResetSync.io.resetOut) {
    val ApbRegs = Module(new ApbRegTarget(addrWidth, dataWidth, 0, 2))
    io.apb <> ApbRegs.io.apb
  }

  // interrup not generated
  io.irq1 := false.B

  // pmod 0 set to output
  io.pmod0.oe := 0.U
  io.pmod0.gpo := 0.U

  // pmod 1 set to output
  io.pmod1.oe := 0.U
  io.pmod1.gpo := 0.U


}

object DtuTop extends App {
  (new ChiselStage).emitSystemVerilog(new DtuTop(addrWidth=10, dataWidth=32), Array("--target-dir", "src/sv"))
}

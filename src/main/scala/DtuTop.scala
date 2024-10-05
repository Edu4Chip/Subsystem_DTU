import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

/**
 * Example design in Chisel.
 * A redesign of the Tiny Tapeout example as a starting point.
 */
class DtuTop(addrWidth:Int = 10, dataWidth:Int = 32) extends Module {
  val io = IO(new Bundle {
    // Interface: APB
    val apb = new ApbTargetPort(addrWidth, dataWidth)
    
    // Clock and Reset Interfaces are added implicitly

    // Interface: IRQ
    val irq1 = Output(Bool())

    // Interface: ss_ctrl
    val irqEn1 = Input(Bool())
    val ssCtrl1 = Input(UInt(8.W))

    // Interface: GPIO pmod 0 
    val pmod0 = new PmodGpioPort()

    // INterface GPIO pmod 1
    val pmod1 = new PmodGpioPort()    
  })

  val ApbRegs = Module(new ApbRegTarget(addrWidth, dataWidth, 0, 2))

  io.apb <> ApbRegs.io.apb

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

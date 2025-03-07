import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import chisel3.util._
import leros._
import leros.uart._
import io._
import config._


/*
 * DTU Top level 
 * Some notes on reset strategy:
    * By default Chisel generates synchronous active high reset
    * Remainder of SoC uses an asynchronous active low reset 
    * Therefore DTU top level also expects an asyncronous active low reset
    * A verilog blackbox module is added that generates a synchronous active high reset from an asynchrnous active low reset 
*/

class DtuTop(progROM:String) extends Module {

  val io = IO(new Bundle {
    // Interface: APB
    val apb = new ApbTargetPort(APB_CONFIG.ADDR_WIDTH, APB_CONFIG.DATA_WIDTH)  

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


  val syncReset = RegNext(RegNext(!reset.asBool))

  // All modules instantiated here are reset by synchronous active high reset
  // All registers must be instantiated within this to ensure all have the same reset
  // MS: maybe we should have yet another top level jut for the reset handling to avoid this withReset
  // MS: what is the meaning of the leros value here?
  
  
  val apbIntf = withReset(syncReset)(Module(new ApbInterface()))
  val instrMem = withReset(syncReset)(Module(new SramSim(LEROS_CONFIG.IMEM_ADDR_WIDTH, LEROS_CONFIG.INSTR_WIDTH)))

  // APB interface
  io.apb <> apbIntf.io.apb
  
  // APB to instruction
  instrMem.io.req := true.B
  instrMem.io.wr := apbIntf.io.imemWr
  instrMem.io.wrAddr := apbIntf.io.imemWrAddr
  instrMem.io.wrData := apbIntf.io.imemWrData
  instrMem.io.wrMask := apbIntf.io.imemWrMask

  // leros with data memory, rom and peripherals
  val lerosController = withReset(apbIntf.io.lerosReset | syncReset)(Module(new LerosController(progROM)))

  // leros writeable instr mem
  lerosController.io.instr := instrMem.io.rdData
  instrMem.io.rdAddr := Mux(apbIntf.io.lerosReset, 0.U, lerosController.io.instrMemAddr)

  // APB to CCR
  apbIntf.io.apbCCR <> lerosController.io.ccrApbPort

  /* pmod 0
    pin 0: boot select
    pin 1: uart rx
    pin 2: uart tx
    pin 3: unused
  */
  io.pmod0.oe := "b0011".U
  val bootSel = io.pmod0.gpi(0)
  lerosController.io.uartRx := io.pmod0.gpi(1)
  io.pmod0.gpo := "b0".U ## lerosController.io.uartTx ## "b00".U

  lerosController.io.bootSel := bootSel

  // pmod1: GPIOs addressable from Leros
  io.pmod1 <> lerosController.io.pmodGpio

  io.irq1 := 0.U
}


object DtuTop extends App {
  (new ChiselStage).emitSystemVerilog(new DtuTop(args(0)), Array("--target-dir", args(1)))
}

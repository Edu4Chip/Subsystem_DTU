import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import chisel3.util._
import leros._
import leros.uart._
import io._


/*
 * DTU Top level 
 * Some notes on reset strategy:
    * By default Chisel generates synchronous active high reset
    * Remainder of SoC uses an asynchronous active low reset 
    * Therefore DTU top level also expects an asyncronous active low reset
    * A verilog blackbox module is added that generates a synchronous active high reset from an asynchrnous active low reset 
*/

class DtuTop(apbBaseAddr:Int, progROM:String, resetSyncFact:() => ResetSyncBase = () => Module(new ResetSync())) extends Module {
  
  val apbAddrWidth = 32
  val apbDataWidth = 32
  
  val lerosSize = 32
  val lerosMemAddrWidth = 8
  val lerosInstrWidth = 16
  val lerosClockFreq = 100000000
  val lerosUartBaudrate = 115200
  // IO is now mapped to 0x0f00, but wrAddr counts in 32-bit words
  val lerosIoBaseAddr = 0x03c0

  val io = IO(new Bundle {
    // Interface: APB
    val apb = new ApbTargetPort(apbAddrWidth, apbDataWidth)    

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

  val syncReset = RegNext(RegNext(!reset.asBool))

  // All modules instantiated here are reset by synchronous active high reset
  // All registers must be instantiated within this to ensure all have the same reset
  // MS: maybe we should have yet another top level jut for the reset handling to avoid this withReset
  // MS: what is the meaning of the leros value here?
  val leros = withReset((ResetSync.io.resetOut.asBool)) {
    
    val uartRx = Module(new UARTRx(lerosClockFreq, lerosUartBaudrate))
    val uartTx = Module(new BufferedTx(lerosClockFreq, lerosUartBaudrate))
    val registerMap = RegInit(VecInit(Seq.fill(Register_Map_Index.COUNT)(0.U(8.W))))

    registerMap(Register_Map_Index.UART_RX_DATA.U) := uartRx.io.out.bits
    registerMap(Register_Map_Index.UART_RX_VALID) := uartRx.io.out.valid
    uartRx.io.out.ready := registerMap(Register_Map_Index.UART_RX_READY)

    uartTx.io.channel.bits := registerMap(Register_Map_Index.UART_TX_DATA.U)
    uartTx.io.channel.valid := registerMap(Register_Map_Index.UART_TX_VALID)
    registerMap(Register_Map_Index.UART_TX_READY) := uartTx.io.channel.ready

    // interrup not generated
    io.irq1 := false.B

    /* pmod 0
       pin 0: boot select
       pin 1: uart rx
       pin 2: uart tx
       pin 3: unused
    */
    io.pmod0.oe := "b0011".U
    val bootSel = io.pmod0.gpi(0)
    uartRx.io.rxd := io.pmod0.gpi(1)
    io.pmod0.gpo := "b0".U ## uartTx.io.txd ## "b00".U

    // pmod 1 are GPIO pins memory mapped to Leros
    io.pmod1.oe := registerMap(Register_Map_Index.PMOD_OE)
    io.pmod1.gpo := registerMap(Register_Map_Index.PMOD_GPO)
    registerMap(Register_Map_Index.PMOD_GPI) := io.pmod1.gpi

    if (!progROM.isEmpty) {
      val leros = Module(new Leros(prog = "notused", size = lerosSize, memAddrWidth = lerosMemAddrWidth))
      
      val lerosCtrlRegAddr = math.pow(2, lerosMemAddrWidth).toInt + apbBaseAddr
      val apbLoader = Module(new ApbLoader(apbAddrWidth, apbDataWidth, apbBaseAddr, lerosCtrlRegAddr, lerosMemAddrWidth, lerosInstrWidth))
      io.apb <> apbLoader.io.apb
      
      val instrMem = Module(new SramSim(lerosMemAddrWidth, lerosInstrWidth))
      val instrROM = Module(new InstrMem(lerosMemAddrWidth, progROM))


      instrMem.io.wr := apbLoader.io.wr
      instrMem.io.req := apbLoader.io.req
      instrMem.io.wrMask := apbLoader.io.wrMask
      instrMem.io.wrAddr := apbLoader.io.wrAddr
      instrMem.io.wrData := apbLoader.io.wrData

      instrMem.io.rdAddr := leros.imemIO.addr
      instrROM.io.addr := leros.imemIO.addr
    
      leros.imemIO.instr := Mux(bootSel, instrMem.io.rdData, instrROM.io.instr)
      
      leros.reset := apbLoader.io.lerosReset | reset.asBool

      val dataMem = Module(new DataMem(lerosMemAddrWidth, false))
            
      val readIO = leros.dmemIO.rdAddr === lerosIoBaseAddr.U
      val writeIO = (leros.dmemIO.wrAddr === lerosIoBaseAddr.U) &&  leros.dmemIO.wr

      val registerReadIndex = leros.dmemIO.rdAddr - lerosIoBaseAddr.U
      val registerWriteIndex = leros.dmemIO.wrAddr - lerosIoBaseAddr.U
      
      dataMem.io.rdAddr := leros.dmemIO.rdAddr
      leros.dmemIO.rdData := Mux(readIO, registerMap(registerReadIndex), dataMem.io.rdData)
    
      dataMem.io.wr := leros.dmemIO.wr
      dataMem.io.wrAddr := leros.dmemIO.wrAddr
      dataMem.io.wrData := leros.dmemIO.wrData
      dataMem.io.wrMask := leros.dmemIO.wrMask
      when(writeIO) {
        registerMap(registerWriteIndex) := leros.dmemIO.wrData
      }
      

      leros
    } else null.asInstanceOf[Leros]
  }
}

object DtuTop extends App {
  (new ChiselStage).emitSystemVerilog(new DtuTop(Integer.parseInt(args(0), 16), args(1)), Array("--target-dir", args(2)))
}

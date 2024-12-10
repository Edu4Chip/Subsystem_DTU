import chisel3._
import chisel3.util._
import leros._
import config._
import io._

class LerosController(prog: String) extends Module {
  
  val io = IO(new Bundle {
    val bootSel = Input(Bool())
    val instrMemAddr = Output(UInt(LEROS_CONFIG.IMEM_ADDR_WIDTH.W))
    val instr = Input(UInt(LEROS_CONFIG.INSTR_WIDTH.W))

    val ccrApbPort = Flipped(new DualRegPort(APB_CONFIG.N_READ_REGS, APB_CONFIG.N_WRITE_REGS, APB_CONFIG.REGS_WIDTH))

    val pmodGpio = new PmodGpioPort()
    val uartTx = Output(UInt(1.W))
    val uartRx = Input(UInt(1.W))
  })

  val leros = Module(new Leros("notused"))
  val rom = Module(new InstrMem(LEROS_CONFIG.IMEM_ADDR_WIDTH, prog))
  val dmem = Module(new DataMem(LEROS_CONFIG.DMEM_ADDR_WIDTH, false))
  val ccrRegs = Module(new CrossCoreRegs())
  val peripheralsRegs = Module(new Peripherals())
  val lerosMem = Module(new LerosMemorySpace())

  // instruction memory interface
  rom.io.addr := leros.imemIO.addr
  io.instrMemAddr := leros.imemIO.addr
  leros.imemIO.instr := Mux(io.bootSel, io.instr, rom.io.instr)

  // data memory interface
  lerosMem.io.lerosMemIntf <> leros.dmemIO

  // leros to data memory
  dmem.io <> lerosMem.io.dmem
  
  // leros to peripherals
  peripheralsRegs.io.regPort <> lerosMem.io.periphRegs
  
  // leros to CCR
  ccrRegs.io.lerosCCR <> lerosMem.io.lerosCCR

  // APB to CCR
  ccrRegs.io.apbCCR <> io.ccrApbPort

  // Peripheral pins to top level
  io.uartTx := peripheralsRegs.io.uartTx
  peripheralsRegs.io.uartRx := io.uartRx
  io.pmodGpio <> peripheralsRegs.io.pmodGpio

}

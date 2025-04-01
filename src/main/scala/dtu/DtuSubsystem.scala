package dtu

import chisel3._
import chisel3.util.Cat

import leros.Leros
import leros.DataMem
import leros.InstrMem
import leros.util.Assembler

import apb._
import didactic._
import ponte.Ponte
import peripherals.RegBlock
import chisel3.util.log2Ceil
import mem.MemoryFactory

object DtuSubsystem extends App {

  MemoryFactory.use(mem.ChiselSyncMemory.create)

  (new stage.ChiselStage).emitSystemVerilog(
    new DtuSubsystem(DtuSubsystemConfig.default
    .copy(
      romProgramPath = "leros-asm/didactic_rt.s",
      instructionMemorySize = 1 << 11
    )),
    args
  )
}


object IbexCode extends App {
  val code = Assembler.assemble("leros-asm/didactic.s")
  code.grouped(2).zipWithIndex.foreach {
    case (Array(a, b), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$b%04x$a%04x;")
    case (Array(a), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$a%04x;")
  }
}



class DtuSubsystem(conf: DtuSubsystemConfig) extends DidacticSubsystem {

  val io = IO(new DidacticSubsystemIO(conf))
  io.irq := 0.B

  val lerosRx = io.pmod(0).gpi(3)
  val ponteRx = io.pmod(0).gpi(1)

  val sysCtrl =  Module(new SystemControl)
  val ponte = Module(new Ponte(conf.frequency, conf.ponteBaudRate))
  ponte.io.uart.rx := ponteRx

  val leros = Module(new Leros(conf.lerosSize, conf.instructionMemoryAddrWidth))
  leros.reset := reset.asBool || sysCtrl.ctrlPort.lerosReset

  val instrMem = Module(new InstructionMemory(conf.instructionMemorySize))
  val rom =      Module(new InstrMem(conf.instructionMemoryAddrWidth, conf.romProgramPath))
  val regBlock = Module(new peripherals.RegBlock(conf.crossCoreRegisters))
  val gpio =     Module(new peripherals.Gpio)
  val dmem =     Module(new DataMemory(conf.dataMemorySize))
  val uart =     Module(new peripherals.Uart(conf.frequency, conf.lerosBaudRate))
  uart.uartPins.rx := lerosRx

  leros.imemIO <> instrMem.instrPort
  leros.imemIO <> rom.io
  leros.imemIO.instr := Mux(sysCtrl.ctrlPort.lerosBootFromRam, instrMem.instrPort.instr, rom.io.instr)

  ApbMux(ApbArbiter(ponte.io.apb, io.apb))( // 12 bit address space
    instrMem.apbPort -> 0x000,
    regBlock.apbPort -> 0x800,
    sysCtrl.apbPort ->  0xC00,
  )

  DataMemMux(leros.dmemIO)( // 16 bit address space
    dmem.dmemPort ->     0x0000,
    regBlock.dmemPort -> 0x8000,
    gpio.dmemPort ->     0x8100,
    uart.dmemPort ->     0x8110
  )

  io.pmod(1) <> gpio.pmodPort
  io.pmod(0).gpo := Cat(0.B, uart.uartPins.tx, 0.B, ponte.io.uart.tx)
  io.pmod(0).oe := Cat(1.B, 0.B, 1.B, 0.B)
  
}


case class DtuSubsystemConfig(
  romProgramPath: String,
  instructionMemorySize: Int,
  dataMemorySize: Int,
  lerosSize: Int,
  lerosMemAddrWidth: Int,
  crossCoreRegisters: Int,
  frequency: Int,
  lerosBaudRate: Int,
  ponteBaudRate: Int,
  apbAddrWidth: Int,
  apbDataWidth: Int
) extends DidacticConfig {
  val instructionMemoryAddrWidth = log2Ceil(instructionMemorySize)
}
object DtuSubsystemConfig {
  def default = DtuSubsystemConfig(
    romProgramPath = "leros-asm/didactic_rt.s",
    instructionMemorySize = 1 << 11, // 2kB
    dataMemorySize = 1 << 8, // 256 Bytes
    lerosSize = 32, // 32-bit accumulator
    lerosMemAddrWidth = 16, // 16-bit address space
    crossCoreRegisters = 4,
    frequency = 100000000, // 1MHz
    lerosBaudRate = 115200,
    ponteBaudRate = 921600,
    apbAddrWidth = 12,
    apbDataWidth = 32
  )
}

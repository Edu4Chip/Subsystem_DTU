package dtu

import chisel3._
import chisel3.util.Cat

import leros.Leros
import leros.DataMem
import leros.InstrMem
import leros.util.Assembler

import apb._
import didactic._
import peripherals.RegBlock
import chisel3.util.log2Ceil
import mem.MemoryFactory

case class DtuSubsystemConfig(
  romProgramPath: String,
  instructionMemorySize: Int,
  dataMemorySize: Int,
  lerosSize: Int,
  lerosMemAddrWidth: Int,
  crossCoreRegisters: Int,
  frequency: Int,
  uartBaudRate: Int,
  apbAddrWidth: Int,
  apbDataWidth: Int
) extends DidacticConfig {
  val instructionMemoryAddrWidth = log2Ceil(instructionMemorySize)
}
object DtuSubsystemConfig {
  def default = DtuSubsystemConfig(
    romProgramPath = "leros-asm/didactic_rt.s",
    instructionMemorySize = 1 << 11, // 2k words
    dataMemorySize = 1 << 8, // 256 words
    lerosSize = 32, // 32-bit accumulator
    lerosMemAddrWidth = 16, // 16-bit address space
    crossCoreRegisters = 8,
    frequency = 100000000, // 1MHz
    uartBaudRate = 115200,
    apbAddrWidth = 12,
    apbDataWidth = 32
  )
}

class DtuSubsystem(conf: DtuSubsystemConfig) extends DidacticSubsystem {

  val io = IO(new DidacticSubsystemIO(conf))
  io.irq := 0.B

  val bootSelect = io.pmod(0).gpi(0) || io.ssCtrl(0)

  val leros = Module(new Leros(conf.lerosSize, conf.instructionMemoryAddrWidth))
  leros.reset := reset.asBool || io.ssCtrl(1)

  val instrMem = Module(new InstructionMemory(conf.instructionMemorySize))
  val rom = Module(new InstrMem(conf.instructionMemoryAddrWidth, conf.romProgramPath))
  val regBlock = Module(new peripherals.RegBlock(conf.crossCoreRegisters))
  val gpio = Module(new peripherals.Gpio)
  val uart = Module(new peripherals.Uart(conf.frequency, conf.uartBaudRate))
  val dmem = Module(new DataMemory(conf.dataMemorySize))

  leros.imemIO <> instrMem.instrPort
  leros.imemIO <> rom.io
  leros.imemIO.instr := Mux(bootSelect, instrMem.instrPort.instr, rom.io.instr)

  ApbMux(io.apb)( // 12 bit address space
    instrMem.apbPort -> 0x000,
    regBlock.apbPort -> 0x800,
  )

  DataMemMux(leros.dmemIO)( // 16 bit address space
    dmem.dmemPort -> 0x0000,
    regBlock.dmemPort -> 0x8000,
    gpio.dmemPort -> 0x8100,
    uart.dmemPort -> 0x8110
  )

  io.pmod(1) <> gpio.pmodPort
  io.pmod(0).gpo := Cat(0.B, uart.uartPins.tx, 0.B, 0.B)
  io.pmod(0).oe := Cat(0.B, 0.B, 1.B, 1.B)
  uart.uartPins.rx := io.pmod(1).gpi(1)

}

object DtuSubsystem extends App {

  MemoryFactory.use(mem.ChiselSyncMemory.create)

  (new stage.ChiselStage).emitSystemVerilog(
    new DtuSubsystem(DtuSubsystemConfig.default),
    Array("--target-dir", "../src/generated")
  )
}

object Code extends App {
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

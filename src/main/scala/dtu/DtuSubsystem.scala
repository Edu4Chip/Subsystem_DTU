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

object DtuSubsystemConfig {
  val instructionMemorySize = 32 * 4

  val instructionMemoryAddrWidth = log2Ceil(instructionMemorySize)
}

class DtuSubsystem(prog: String) extends DidacticSubsystem {
  import DtuSubsystemConfig._

  val io = IO(new DidacticSubsystemIO(apbAddrWidth = 12, apbDataWidth = 32))
  io.irq := 0.B

  val bootSelect = io.pmod(0).gpi(0) || io.ssCtrl(3)

  val leros = Module(new Leros)
  leros.reset := reset.asBool || io.ssCtrl(2)

  val instrMem = Module(new InstructionMemory(instructionMemorySize))
  val rom = Module(new InstrMem(8, prog))
  val regBlock = Module(new peripherals.RegBlock(4))
  val gpio = Module(new peripherals.Gpio)
  val uart = Module(new peripherals.Uart(4, 20, 5))
  val dmem = Module(new DataMemory(256))

  leros.imemIO <> instrMem.instrPort
  leros.imemIO <> rom.io
  leros.imemIO.instr := Mux(bootSelect, instrMem.instrPort.instr, rom.io.instr)

  ApbMux(io.apb)( // 12 bit address space
    instrMem.apbPort -> 0x000,
    regBlock.apbPort -> 0xff0
  )

  DataMemMux(leros.dmemIO)( // 16 bit address space
    dmem.dmemPort -> 0x0000,
    regBlock.dmemPort -> 0x8000,
    gpio.dmemPort -> 0x8010,
    uart.dmemPort -> 0x8020
  )

  io.pmod(1) <> gpio.pmodPort
  io.pmod(0).gpo := Cat(0.B, uart.uartPins.tx, 0.B, 0.B)
  io.pmod(0).oe := Cat(0.B, 0.B, 1.B, 1.B)
  uart.uartPins.rx := io.pmod(1).gpi(1)

}

object DtuSubsystem extends App {

  MemoryFactory.use(mem.ChiselSyncMemory.create)

  (new stage.ChiselStage).emitSystemVerilog(
    new DtuSubsystem("leros/asm/didactic.s"),
    Array("--target-dir", "../src/generated")
  )
}

object Code extends App {
  val code = Assembler.assemble("leros/asm/didactic.s")
  code.grouped(2).zipWithIndex.foreach {
    case (Array(a, b), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$b%04x$a%04x;")
    case (Array(a), i) =>
      val pointer = 0x01052000 + i * 4
      println(f"*(volatile unsigned int*)(0x$pointer%08x) = 0x$a%04x;")
  }
}

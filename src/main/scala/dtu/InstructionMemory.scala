package dtu

import chisel3._
import chisel3.util.log2Ceil

import leros.InstrMemIO
import apb.ApbPort
import apb.ApbTarget

import mem.MemoryFactory

import misc.Helper.WordToByte
import misc.Helper.UIntRangeCheck
import misc.Helper.BytesToWord
import misc.FormalHelper._

/** This module is an instruction memory for Leros which can be programmed via
  * APB.
  *
  * The memory assumes word addressing on the APB interface.
  *
  * Internally a 32-bit wide memory is used. Leros receives the uppper or lower
  * half-word on the instruction bus.
  *
  * During APB writes the instruction output is undefined.
  *
  * @param noBytes
  *   Number of bytes in the instruction memory
  */
class InstructionMemory(noBytes: Int) extends Module {

  require(noBytes % 4 == 0, "Number of bytes must be a multiple of 4")

  val addrWidth = log2Ceil(noBytes)

  val instrPort = IO(new InstrMemIO(addrWidth))
  val apbPort = IO(ApbPort.targetPort(addrWidth, 32))

  formalProperties {
    apbPort.targetPortProperties("InstructionMemory")
  }

  val mem = MemoryFactory.create(noBytes / 4)

  


  val ackReg = RegInit(0.B)
  when(ackReg) {
    ackReg := 0.B
  }.elsewhen(apbPort.psel) {
    ackReg := 1.B
  }

  apbPort.pready := ackReg

  val rdData = mem.read(Mux(
    apbPort.psel, // are we replying to a apb access?
    apbPort.paddr(addrWidth - 1, 2),  // word address for apb
    instrPort.addr(addrWidth - 2, 1)) // word address for instruction fetch
  )

  apbPort.pready := ackReg
  apbPort.pslverr := 0.B
  apbPort.prdata := rdData

  instrPort.instr := Mux(
    RegNext(instrPort.addr(0)),
    rdData(31, 16),
    rdData(15, 0)
  )

  when(apbPort.psel && apbPort.penable && apbPort.pwrite) {
    mem.write(
      apbPort.paddr(addrWidth - 1, 2),
      apbPort.pwdata,
      apbPort.pstrb
    )
  }

}

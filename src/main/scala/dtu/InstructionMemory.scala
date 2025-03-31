package dtu

import chisel3._
import chisel3.util.log2Ceil

import leros.InstrMemIO
import apb.ApbTargetPort

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
  val apbPort = IO(new ApbTargetPort(addrWidth, 32))

  properties {
    apbPort.targetPortProperties("InstructionMemory")
  }

  val mem = MemoryFactory.create(noBytes / 4)

  apbPort.pready := 0.B
  apbPort.pslverr := 0.B
  apbPort.prdata := DontCare

  instrPort.instr := DontCare

  when(apbPort.psel) {

    apbPort.pready := apbPort.penable

    when(!apbPort.pwrite) {
      apbPort.prdata := mem.read(apbPort.paddr(addrWidth - 1, 2))
    }.elsewhen(apbPort.penable && apbPort.pwrite) {
      mem.write(
        apbPort.paddr(addrWidth - 1, 2),
        apbPort.pwdata,
        apbPort.pstrb
      )
    }

  } otherwise {

    val word = mem.read(instrPort.addr(addrWidth - 2, 1))
    val upr = word(31, 16)
    val lwr = word(15, 0)
    instrPort.instr := Mux(RegNext(instrPort.addr(0)), upr, lwr)
  }
}

package dtu

import chisel3._
import chisel3.util.log2Ceil

import leros.InstrMemIO
import apb.ApbTargetPort

import mem.MemoryFactory

import misc.Helper.WordToByte
import misc.Helper.UIntRangeCheck
import misc.Helper.BytesToWord

class InstructionMemory(noBytes: Int) extends Module {
  require(noBytes % 4 == 0, "Number of bytes must be a multiple of 4")

  val addrWidth = log2Ceil(noBytes)

  val instrPort = IO(new InstrMemIO(addrWidth))
  val apbPort = IO(new ApbTargetPort(addrWidth, 32))

  val mem = MemoryFactory.create(noBytes / 4)

  apbPort.pready := 0.B
  apbPort.pslverr := 0.B
  apbPort.prdata := DontCare

  instrPort.instr := DontCare

  when(apbPort.psel && apbPort.penable) { // transaction for us

    apbPort.pready := 1.B

    val localAddr = apbPort.paddr(addrWidth - 1, 2)

    when(apbPort.pwrite) {
      mem.write(
        localAddr,
        apbPort.pwdata,
        apbPort.pstrb
      )
    } otherwise {
      apbPort.pslverr := 1.B
    }

  } otherwise {

    val word = mem.read(instrPort.addr(addrWidth - 1, 1))
    val upr = word(31, 16)
    val lwr = word(15, 0)
    instrPort.instr := Mux(RegNext(instrPort.addr(0)), upr, lwr)

  }
}

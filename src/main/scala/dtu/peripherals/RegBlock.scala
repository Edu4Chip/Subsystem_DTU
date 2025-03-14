package dtu.peripherals

import chisel3._
import chisel3.util._

import apb.ApbTargetPort

import leros.DataMemIO

class RegBlock(n: Int) extends Module {

  val aw = log2Ceil(n * 4)

  val apbPort = IO(new ApbTargetPort(aw, 32))

  val dmemPort = IO(new DataMemIO(aw - 2))

  val ibexToLerosRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))
  val lerosToIbexRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))

  val apbIndex = apbPort.paddr(aw - 1, 2)

  apbPort.pslverr := 0.B
  apbPort.pready := 0.B
  apbPort.prdata := lerosToIbexRegs(RegNext(apbIndex))

  when(apbPort.psel && apbPort.penable) {
    apbPort.pready := 1.B

    when(apbPort.pwrite) {
      ibexToLerosRegs(apbIndex) := apbPort.pwdata
    }

  }

  dmemPort.rdData := ibexToLerosRegs(RegNext(dmemPort.rdAddr))

  when(dmemPort.wr) {
    lerosToIbexRegs(dmemPort.wrAddr) := dmemPort.wrData
  }

}

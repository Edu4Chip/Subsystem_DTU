package dtu.peripherals

import chisel3._
import chisel3.util._

import apb._

import leros.DataMemIO
import misc.FormalHelper.formalProperties

/** This module contains n 32-bit registers for communication between the Ibex
  * and Leros cores.
  *
  * The Ibex core accesses the registers via APB, while Leros accesses them via
  * the databus.
  *
  * Writes from the ibex core are only visible to the Leros core and vice versa.
  *
  * @param n
  *   Number of 32-bit registers
  */
class RegBlock(n: Int) extends Module {

  // address width
  val aw = log2Ceil(n * 4)

  val apbPort = IO(ApbPort.targetPort(aw, 32))
  val dmemPort = IO(new DataMemIO(aw - 2))

  formalProperties {
    apbPort.targetPortProperties("RegBlock")
  }

  val ibexToLerosRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))
  val lerosToIbexRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))

  // APB access
  val apbAckReg = RegInit(0.B)
  when(apbAckReg) {
    apbAckReg := 0.B
  }.elsewhen(apbPort.psel) {
    apbAckReg := 1.B
  }
  apbPort.pslverr := 0.B
  apbPort.pready := apbAckReg
  val apbIndex = apbPort.paddr(aw - 1, 2)
  apbPort.prdata := RegNext(lerosToIbexRegs(apbIndex))

  when(apbPort.psel && apbPort.penable && apbPort.pwrite) {
    ibexToLerosRegs(apbIndex) := apbPort.pwdata
  }

  // Databus access
  dmemPort.rdData := ibexToLerosRegs(RegNext(dmemPort.rdAddr))
  when(dmemPort.wr) {
    lerosToIbexRegs(dmemPort.wrAddr) := dmemPort.wrData
  }

}

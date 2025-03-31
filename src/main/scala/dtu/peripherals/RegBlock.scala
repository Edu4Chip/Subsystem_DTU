package dtu.peripherals

import chisel3._
import chisel3.util._

import apb.ApbTargetPort

import leros.DataMemIO
import misc.FormalHelper.properties

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

  val apbPort = IO(new ApbTargetPort(aw, 32))
  val dmemPort = IO(new DataMemIO(aw - 2))

  properties {
    apbPort.targetPortProperties("RegBlock")
  }

  val ibexToLerosRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))
  val lerosToIbexRegs = RegInit(VecInit(Seq.fill(n)(0.U(32.W))))

  // APB access
  apbPort.pslverr := 0.B
  apbPort.pready := 0.B
  val apbIndex = apbPort.paddr(aw - 1, 2)
  apbPort.prdata := lerosToIbexRegs(RegNext(apbIndex))

  when(apbPort.psel && apbPort.penable) {
    apbPort.pready := 1.B

    when(apbPort.pwrite) {
      ibexToLerosRegs(apbIndex) := apbPort.pwdata
    }
  }

  // Databus access
  dmemPort.rdData := ibexToLerosRegs(RegNext(dmemPort.rdAddr))
  when(dmemPort.wr) {
    lerosToIbexRegs(dmemPort.wrAddr) := dmemPort.wrData
  }

}

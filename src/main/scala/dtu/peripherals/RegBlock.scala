package dtu.peripherals

import chisel3._
import chisel3.util._

import apb.ApbTargetPort    

import leros.DataMemIO

class RegBlock extends Module {

    val apbPort = IO(new ApbTargetPort(4, 32))

    val dmemPort = IO(new DataMemIO(2))

    val ibexToLerosRegs = RegInit(VecInit(Seq.fill(4)(0.U(32.W))))
    val lerosToIbexRegs = RegInit(VecInit(Seq.fill(4)(0.U(32.W))))

    val apbIndex = apbPort.paddr(3, 2)

    apbPort.pslverr := 0.B
    apbPort.pready := 0.B
    apbPort.prdata := lerosToIbexRegs(apbIndex)

    when(apbPort.psel && apbPort.penable) {
        apbPort.pready := 1.B

        when(apbPort.pwrite) {
            ibexToLerosRegs(apbIndex) := apbPort.pwdata
        }

    }

    dmemPort.rdData := ibexToLerosRegs(dmemPort.rdAddr)

    when(dmemPort.wr) {
        lerosToIbexRegs(dmemPort.wrAddr) := dmemPort.wrData
    }

}

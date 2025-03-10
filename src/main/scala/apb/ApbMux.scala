package apb

import chisel3._
import chisel3.util._


import misc.Helper.UIntRangeCheck

class ApbMux (
    addrWidth: Int,
    dataWidth: Int,
    targetAddressRanges: Seq[Range]
) extends Module {

    val io = IO(new Bundle {
        val master = new ApbTargetPort(addrWidth, dataWidth)
        val targets = Vec(targetAddressRanges.length, Flipped(new ApbTargetPort(addrWidth, dataWidth)))
    })

    io.targets.foreach { t =>
        t <> io.master
        t.psel := 0.B    
    }

    io.targets.zip(targetAddressRanges).foreach { case  (port, range) =>

        val selected = io.master.psel && io.master.paddr.inRange(range)
        
        when(selected) {
            port.psel := 1.B
            io.master.pready := port.pready
            io.master.prdata := port.prdata
            io.master.pslverr := port.pslverr
        }
    }
}

object ApbMux {

    def apply(master: ApbTargetPort)(targets: (ApbTargetPort, Range)*): Unit = {
        val apbMux = Module(new ApbMux(master.addrWidth, master.dataWidth, targets.map(_._2)))
        apbMux.io.master <> master
        apbMux.io.targets.zip(targets.map(_._1)).foreach { case (target, port) => target <> port }
    }

}
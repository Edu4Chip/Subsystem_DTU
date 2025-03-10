package dtu

import chisel3._

import leros.DataMemIO

import misc.Helper.UIntRangeCheck

class DataMemMux(
    addrWidth: Int,
    targetAddressRanges: Seq[Range]
) extends Module {

    val io = IO(new Bundle {
        val master = new DataMemIO(addrWidth)
        val targets = Vec(targetAddressRanges.length, Flipped(new DataMemIO(addrWidth)))
    })

    io.targets.foreach { t =>
        t <> io.master
        t.wr := 0.B    
    }

    io.targets.zip(targetAddressRanges).foreach { case  (port, range) =>

        val rdSelected = io.master.rdAddr.inRange(range)
        
        when(RegNext(rdSelected, 0.B)) {
            io.master.rdData := port.rdData
        }

        val wrSelected = io.master.wr && io.master.wrAddr.inRange(range)
        port.wr := wrSelected
    }

}

object DataMemMux {

    def apply(master: DataMemIO)(targets: (DataMemIO, Range)*): Unit = {
        val dmemMux = Module(new DataMemMux(master.rdAddr.getWidth, targets.map(_._2)))
        dmemMux.io.master <> master
        dmemMux.io.targets.zip(targets.map(_._1)).foreach { case (target, port) => target <> port }
    }

}
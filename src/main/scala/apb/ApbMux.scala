package apb

import circt.stage.ChiselStage

import chisel3._
import chisel3.util._

import chiseltest.formal._

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import misc.FormalHelper._

class ApbMux(
    addrWidth: Int,
    dataWidth: Int,
    targetInfos: Seq[ApbTarget]
) extends Module {

  val io = IO(new Bundle {
    val master = ApbPort.targetPort(addrWidth, dataWidth)
    val targets = Vec(
      targetInfos.length,
      ApbPort.masterPort(addrWidth, dataWidth)
    )
  })

  io.targets.foreach { t =>
    t <> io.master
    t.psel := 0.B
  }

  formalProperties {

    io.master.targetPortProperties("ApbMux.master")
    io.targets.foreach(_.masterPortProperties("ApbMux.target"))

    io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>
      assert(
        (io.master.paddr >= targetInfo.byteAddrRange.start.U
          && io.master.paddr < targetInfo.byteAddrRange.end.U) -> (port.psel === io.master.psel),
        "port psel should be equal to master psel when address is in ports range"
      )
    }
  }

  io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>
    val selected = io.master.psel && io.master.paddr(
      addrWidth - 1,
      targetInfo.byteAddrWidth
    ) === targetInfo.fixedAddrPart.U

    when(selected) {
      port.psel := 1.B
      io.master.pready := port.pready
      io.master.prdata := port.prdata
      io.master.pslverr := port.pslverr
    }
  }
}

object ApbMux {

  def apply(master: ApbPort)(targetTuples: (ApbPort, Int)*): Unit = {

    // byte address ranges for each target
    val targets = targetTuples.map { case (port, base) =>
      val t = ApbTarget(port.toString(), base, port.addrWidth)
      master.addChild(t)
      t
    }

    targets.foreach(_.checkInsideMasterAddrSpace(master))

    // check for overlap of target address ranges
    MemoryMapHelper.findAddressOverlap(
      targets.map(t => t -> t.byteAddrRange)
    ) match {
      case Some((port1, range1, port2, range2)) =>
        throw new IllegalArgumentException(
          s"Address Ranges of DataMem ports ($port1) and ($port2) overlap:\n  $port1: $range1\n  $port2: $range2"
        )
      case None =>
    }

    val apbMux = Module(
      new ApbMux(master.addrWidth, master.dataWidth, targets)
    )
    apbMux.io.master <> master
    apbMux.io.targets.zip(targetTuples.map(_._1)).foreach {
      case (target, port) =>
        target <> port
    }
  }

  def apply(targetTuples: (ApbPort, Int)*): ApbPort = {
    val master = ApbPort.targetPort(
      targetTuples.head._1.addrWidth,
      targetTuples.head._1.dataWidth
    )
    apply(master)(targetTuples: _*)
    master
  }

}

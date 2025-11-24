package wishbone


import chisel3._
import chisel3.util._

import chiseltest.formal._

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import misc.FormalHelper._
import misc.BusTarget


class WishboneMux(
  addrWidth: Int,
  targetInfos: Seq[BusTarget]
) extends Module {


  val io = IO(new Bundle {
    val master = WishbonePort.targetPort(addrWidth)
    val targets = Vec(
      targetInfos.length,
      WishbonePort.masterPort(addrWidth)
    )
  })

  io.targets.foreach { t =>
    t <> io.master
    t.cyc := 0.B
    t.stb := 0.B
  }

  val errorTarget = Module(new WishboneErrorTarget(addrWidth))
  errorTarget.wbPort <> io.master

  formalProperties {

    io.master.targetPortProperties("WishboneMux.master")
    io.targets.zipWithIndex.foreach { case (port, i) =>
      port.masterPortProperties(s"WishboneMux.target[$i]")
    }

    io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>
      assert(
        (io.master.adr >= targetInfo.byteAddrRange.start.U
          && io.master.adr < targetInfo.byteAddrRange.end.U) -> (port.cyc === io.master.cyc),
        "port cyc should be equal to master cyc when address is in ports range"
      )
    }

    assert(
      io.master.cyc -> (io.targets.map(_.cyc) :+ errorTarget.wbPort.cyc).reduce(_ || _),
      "At least one target must be selected when the master asserts cyc"
    )
  }

  io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>

    val selected = io.master.cyc && io.master.adr(
      addrWidth - 1,
      targetInfo.byteAddrWidth
    ) === targetInfo.fixedAddrPart.U

    when(selected) {
      errorTarget.wbPort.cyc := 0.B
      errorTarget.wbPort.stb := 0.B
      port.cyc := 1.B
      port.stb := 1.B
    }

    val wasSelected = RegNext(selected && !port.ack, 0.B)

    when(wasSelected) {
      io.master.ack := port.ack
      io.master.dat_o := port.dat_o
    }
  }


}


object WishboneMux {


  def apply(master: WishbonePort)(targetTuples: (WishbonePort, Int)*): Unit = {

    // byte address ranges for each target
    val targets = targetTuples.map { case (port, base) =>
      val t = BusTarget(port.toString(), master.addrWidth, base, port.addrWidth)
      master.addChild(t)
      port.getTargets().foreach(child => master.addChild(child.copy(baseByteAddr = child.baseByteAddr + base)))
      t
    }

    targets.foreach(_.checkInsideMasterAddrSpace())

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

    val wbMux = Module(
      new WishboneMux(master.addrWidth, targets)
    )
    wbMux.io.master <> master
    wbMux.io.targets.zip(targetTuples.map(_._1)).foreach {
      case (target, port) =>
        target <> port
    }
  }


}
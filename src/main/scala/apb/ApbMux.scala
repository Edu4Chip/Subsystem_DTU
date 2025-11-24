package apb

import circt.stage.ChiselStage

import chisel3._
import chisel3.util._

import chiseltest.formal._

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import misc.FormalHelper._
import misc.BusTarget

class ApbMux(
    addrWidth: Int,
    dataWidth: Int,
    targetInfos: Seq[BusTarget]
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

  val errorTarget = Module(new ApbErrorTarget(addrWidth, dataWidth))
  errorTarget.apbPort <> io.master

  formalProperties {

    io.master.targetPortProperties("ApbMux.master")
    io.targets.zipWithIndex.foreach { case (port, idx) =>
      port.masterPortProperties(s"ApbMux.target[$idx]")
    }

    io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>

      val selected = io.master.paddr >= targetInfo.byteAddrRange.start.U &&
        io.master.paddr < targetInfo.byteAddrRange.end.U

      assert(
        selected -> (port.psel === io.master.psel),
        "port psel should be equal to master psel when address is in ports range"
      )
      assert(
        selected -> (port.paddr === io.master.paddr),
        "port paddr should be equal to master paddr when address is in ports range"
      )
      assert(
        selected -> (port.pwdata === io.master.pwdata),
        "port pwdata should be equal to master pwdata when address is in ports range"
      )
      assert(
        selected -> (port.pwrite === io.master.pwrite),
        "port pwrite should be equal to master pwrite when address is in ports range"
      )
      assert(
        selected -> (port.pstrb === io.master.pstrb),
        "port pstrb should be equal to master pstrb when address is in ports range"
      )
      assert(
        (selected && port.pready) -> (io.master.prdata === port.prdata),
        "when port is selected and ready, master prdata should be equal to port prdata when address is in ports range"
      )
    }

    assert(
      io.master.psel -> (io.targets.map(_.psel) :+ errorTarget.apbPort.psel).reduce(_ || _),
      "At least one target must be selected when the master asserts psel"
    )
  }

  io.targets.lazyZip(targetInfos).foreach { case (port, targetInfo) =>
    val selected = io.master.psel && io.master.paddr(
      addrWidth - 1,
      targetInfo.byteAddrWidth
    ) === targetInfo.fixedAddrPart.U

    when(selected) {
      errorTarget.apbPort.psel := 0.B
      port.psel := 1.B
      io.master.pslverr := 0.B
    }

    val wasSelected = RegNext(selected && !port.pready, false.B)
    when(wasSelected) {
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
      val t = BusTarget(port.toString(), master.addrWidth, base, port.addrWidth)
      master.addChild(t)
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

    val apbMux = Module(
      new ApbMux(master.addrWidth, master.dataWidth, targets)
    )
    apbMux.io.master <> master
    apbMux.io.targets.zip(targetTuples.map(_._1)).foreach {
      case (target, port) =>
        target <> port
    }
  }

}

package apb

import chisel3._
import chisel3.util._

import chiseltest.formal._

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import misc.FormalHelper._
import chisel3.stage.ChiselStage

class ApbMux(
    addrWidth: Int,
    dataWidth: Int,
    targetInfos: Seq[ApbMux.Target]
) extends Module {

  val io = IO(new Bundle {
    val master = new ApbTargetPort(addrWidth, dataWidth)
    val targets = Vec(
      targetInfos.length,
      Flipped(new ApbTargetPort(addrWidth, dataWidth))
    )
  })

  io.targets.foreach { t =>
    t <> io.master
    t.psel := 0.B
  }

  

  properties {

    io.master.targetPortProperties()
    io.targets.foreach(_.masterPortProperties())

    (io.targets, targetInfos).zipped.foreach {
      case (port, targetInfo) =>
        assert(
          (io.master.paddr >= targetInfo.byteAddrRange.start.U 
          && io.master.paddr < targetInfo.byteAddrRange.end.U) -> (port.psel === io.master.psel),
          "port psel should be equal to master psel when address is in ports range"
        )
    }
  }

  (io.targets, targetInfos).zipped.foreach {
    case (port, targetInfo) =>
      val selected = io.master.psel && io.master.paddr(addrWidth - 1, targetInfo.byteAddrWidth) === targetInfo.fixedAddrPart.U

      when(selected) {
        port.psel := 1.B
        io.master.pready := port.pready
        io.master.prdata := port.prdata
        io.master.pslverr := port.pslverr
      }
  }
}

object ApbMux {

  def main(args: Array[String]): Unit = (new ChiselStage).emitSystemVerilog(new ApbMux(10, 32, Seq(
    Target("target1", 0x00, 8),
    Target("target2", 0x100, 8),
    Target("target3", 0x200, 8),
    Target("target4", 0x300, 8)
  )))

  def apply(master: ApbTargetPort)(targetTuples: (ApbTargetPort, Int)*): Unit = {

    // byte address ranges for each target
    val targets = targetTuples.map { case (port, base) =>
      Target(port.toString(), base, port.addrWidth)
    }

    targets.foreach(_.checkInsideMasterAddrSpace(master))

    // check for overlap of target address ranges
    MemoryMapHelper.findAddressOverlap(targets.map(t => t -> t.byteAddrRange)) match {
      case Some((port1, range1, port2, range2)) =>
        throw new IllegalArgumentException(
          s"Address Ranges of DataMem ports ($port1) and ($port2) overlap:\n  $port1: $range1\n  $port2: $range2"
        )
      case None =>
    }

    println(s"ApbMux(${master.addrWidth}.W):")
    targets.foreach { case t =>
      println(
        s"  ${t.portName} (${t.byteAddrWidth}.W): 0x${t.byteAddrRange.start.toHexString} - 0x${t.byteAddrRange.end.toHexString}"
      )
    }

    val apbMux = Module(
      new ApbMux(master.addrWidth, master.dataWidth, targets)
    )
    apbMux.io.master <> master
    apbMux.io.targets.zip(targetTuples.map(_._1)).foreach { case (target, port) =>
      target <> port
    }
  }

  case class Target(portName: String, baseByteAddr: Int, byteAddrWidth: Int) {

    require(
      MemoryMapHelper.isWordAligned(baseByteAddr),
      s"$this: Base address must be word aligned: 0x${baseByteAddr.toHexString}"
    )
    require(
      MemoryMapHelper.isAlignedToAddrWidth(baseByteAddr, byteAddrWidth),
      s"$this: Base address must be aligned to address width (${byteAddrWidth}.W): 0x${baseByteAddr.toHexString}"
    )

    def checkInsideMasterAddrSpace(master: ApbTargetPort): Unit = {
      require(
        MemoryMapHelper.addressRangeInsideAdressSpace(
          byteAddrRange,
          master.addrWidth
        ),
        s"$this: Address range 0x${byteAddrRange.start.toHexString}-0x${byteAddrRange.end.toHexString} "
          + s"is out of range for master port's address range (0x00-0x${(1 << master.addrWidth).toHexString})"
      )
    }

    def byteAddrRange: Range =
      baseByteAddr until (baseByteAddr + (1 << byteAddrWidth))

    def fixedAddrPart = baseByteAddr >> byteAddrWidth

    override def toString(): String = {
      s"Target($portName, 0x${baseByteAddr.toHexString})"
    }
  }

}

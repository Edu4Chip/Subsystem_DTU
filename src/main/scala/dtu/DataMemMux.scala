package dtu

import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._

import leros.DataMemIO

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import apb.ApbTargetPort
import misc.FormalHelper._

/** A Memory Mux for Leros' data bus connecting multiple targets to a single
  * master port.
  *
  * @param addrWidth
  * @param targetDecodePatterns
  */
class DataMemMux(
    addrWidth: Int,
    targetInfos: Seq[DataMemMux.Target]
) extends Module {

  val io = IO(new Bundle {
    val master = new DataMemIO(addrWidth)
    val targets =
      Vec(targetInfos.length, Flipped(new DataMemIO(addrWidth)))
  })

  properties {
    (io.targets, targetInfos).zipped.foreach {
      case (port, targetInfo) =>
        assert(
          (io.master.wrAddr >= targetInfo.wordAddrRange.start.U 
          && io.master.wrAddr < targetInfo.wordAddrRange.end.U) -> (port.wr === io.master.wr),
          "port write enable should be equal to master write enable when address is in ports range"
        )
        assert(
          (io.master.rdAddr >= targetInfo.wordAddrRange.start.U 
          && io.master.rdAddr < targetInfo.wordAddrRange.end.U) |=> (port.rdData === io.master.rdData),
          "port read data should be equal to master read data when address is in ports range"
        )
    }
  }

  io.targets.foreach { t =>
    t <> io.master
    t.wr := 0.B
  }

  (io.targets, targetInfos).zipped.foreach { case (port, targetInfo) =>
    val rdSelected = io.master.rdAddr(addrWidth - 1, targetInfo.wordAddrWidth) === targetInfo.fixedAddrPart.U

    when(RegNext(rdSelected, 0.B)) {
      io.master.rdData := port.rdData
    }

    val wrSelected = io.master.wrAddr(addrWidth - 1, targetInfo.wordAddrWidth) === targetInfo.fixedAddrPart.U
    port.wr := io.master.wr && wrSelected
  }

}

object DataMemMux {
  def apply(master: DataMemIO)(targetTuples: (DataMemIO, Int)*): Unit = {

    val targets = targetTuples.map { case (port, base) =>
      Target(port.toString(), base, port.memAddrWidth)
    }

    targets.foreach(_.checkInsideMasterAddrSpace(master))

    // check for overlap of target address ranges
    MemoryMapHelper.findAddressOverlap(
      targets.map(t => t -> t.byteAddrRange)
    ) match {
      case Some((port1, range1, port2, range2)) =>
        throw new IllegalArgumentException(
          s"Address Ranges of targets $port1 and $port2 overlap:\n  $port1: $range1\n  $port2: $range2"
        )
      case None =>
    }

    println(s"DataMemMux(${master.memAddrWidth}.W):")
    targets.foreach { case t =>
      println(
        s"  ${t.portName} (${t.byteAddrWidth}.W): 0x${t.byteAddrRange.start.toHexString} - 0x${t.byteAddrRange.end.toHexString}"
      )
    }

    val dmemMux = Module(
      new DataMemMux(master.rdAddr.getWidth, targets)
    )
    dmemMux.io.master <> master
    dmemMux.io.targets
      .zip(targetTuples.map(_._1))
      .foreach { case (target, port) =>
        target <> port
      }
  }

  case class Target(portName: String, baseByteAddr: Int, wordAddrWidth: Int) {

    val byteAddrWidth = wordAddrWidth + 2
    val baseWordAddr = baseByteAddr >> 2

    require(
      MemoryMapHelper.isWordAligned(baseByteAddr),
      s"$this: Base address must be word aligned: 0x${baseByteAddr.toHexString}"
    )
    require(
      MemoryMapHelper.isAlignedToAddrWidth(baseByteAddr, byteAddrWidth),
      s"$this: Base address must be aligned to address width (${byteAddrWidth}.W): 0x${baseByteAddr.toHexString}"
    )

    def checkInsideMasterAddrSpace(master: DataMemIO): Unit = {
      require(
        MemoryMapHelper.addressRangeInsideAdressSpace(
          wordAddrRange,
          master.memAddrWidth
        ),
        s"$this: Address range 0x${byteAddrRange.start.toHexString}-0x${byteAddrRange.end.toHexString} "
          + s"is out of range for master port's address range (0x00-0x${(1 << master.memAddrWidth).toHexString})"
      )
    }

    def byteAddrRange: Range =
      baseByteAddr until (baseByteAddr + (1 << byteAddrWidth))
    def wordAddrRange: Range =
      baseWordAddr until (baseWordAddr + (1 << wordAddrWidth))

    def fixedAddrPart = baseWordAddr >> wordAddrWidth

    override def toString(): String = {
      s"Target($portName, 0x${baseByteAddr.toHexString})"
    }
  }

}

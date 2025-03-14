package dtu

import chisel3._
import chisel3.util._

import leros.DataMemIO

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper

class DataMemMux(
    addrWidth: Int,
    targetDecodePatterns: Seq[BitPat]
) extends Module {

  val io = IO(new Bundle {
    val master = new DataMemIO(addrWidth)
    val targets =
      Vec(targetDecodePatterns.length, Flipped(new DataMemIO(addrWidth)))
  })

  io.targets.foreach { t =>
    t <> io.master
    t.wr := 0.B
  }

  (io.targets, targetDecodePatterns).zipped.foreach {
    case (port, targetDecodePattern) =>
      val rdSelected = io.master.rdAddr === targetDecodePattern

      when(RegNext(rdSelected, 0.B)) {
        io.master.rdData := port.rdData
      }

      val wrSelected = io.master.wr && io.master.wrAddr === targetDecodePattern
      port.wr := wrSelected
  }

}

object DataMemMux {

  def apply(master: DataMemIO)(targets: (DataMemIO, Int)*): Unit = {

    // byte address ranges for each target
    val targetRanges = targets.map { case (port, base) =>
      (port, base until (base + (1 << (port.memAddrWidth + 2))))
    }

    targetRanges.foreach { case (port, range) =>
      require( // check for word alignment
        MemoryMapHelper.isWordAligned(range.start),
        s"Port ($port) at 0x${range.start.toHexString}: Base address must be word aligned"
      )

      require( // check for alignment to target address width
        MemoryMapHelper
          .isAlignedToAddrWidth(range.start, port.memAddrWidth + 2),
        s"Port ($port) at 0x${range.start.toHexString}: Base address must be aligned to port's address width"
      )

      require( // check that target address range is inside master address space
        MemoryMapHelper
          .addressRangeInsideAdressSpace(range, master.memAddrWidth + 2),
        s"Port ($port) at 0x${range.start.toHexString}: Address is out of range for master port's address width"
      )

    }

    // check for overlap of target address ranges
    MemoryMapHelper.findAddressOverlap(targetRanges) match {
      case Some((port1, range1, port2, range2)) =>
        throw new IllegalArgumentException(
          s"Address Ranges of DataMem ports ($port1) and ($port2) overlap:\n  $port1: $range1\n  $port2: $range2"
        )
      case None =>
    }

    val patterns = targets.map { case (port, base) =>
      val addressableBits = port.memAddrWidth
      val fixedAddrPart = base >> (addressableBits + 2)
      BitPat(
        "b" + fixedAddrPart.toBinaryString.reverse
          .padTo(master.memAddrWidth - addressableBits, '0')
          .reverse
          .mkString + ("?" * addressableBits)
      )
    }

    println(s"DataMemMux(${master.memAddrWidth}.W):")
    (patterns, targetRanges).zipped.foreach { case (pattern, (port, range)) =>
      println(
        s"  $port (${port.memAddrWidth}.W): 0x${range.start.toHexString} - 0x${range.end.toHexString} -> $pattern"
      )
    }

    val dmemMux = Module(
      new DataMemMux(master.rdAddr.getWidth, patterns)
    )
    dmemMux.io.master <> master
    dmemMux.io.targets
      .zip(targets.map(_._1))
      .foreach { case (target, port) =>
        target <> port
      }
  }

}

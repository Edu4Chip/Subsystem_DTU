package apb

import chisel3._
import chisel3.util._

import chiseltest.formal._

import misc.Helper.UIntRangeCheck
import misc.MemoryMapHelper
import misc.FormalHelper._

class ApbMux(
    addrWidth: Int,
    dataWidth: Int,
    targetDecodePatterns: Seq[BitPat]
) extends Module {

  val io = IO(new Bundle {
    val master = new ApbTargetPort(addrWidth, dataWidth)
    val targets = Vec(
      targetDecodePatterns.length,
      Flipped(new ApbTargetPort(addrWidth, dataWidth))
    )
  })

  io.targets.foreach { t =>
    t <> io.master
    t.psel := 0.B
  }

  io.master.targetPortProperties()
  io.targets.foreach(_.masterPortProperties())

  formalblock {
    (io.targets, targetDecodePatterns).zipped.foreach {
      case (port, targetDecodePattern) =>
        assert(
          (io.master.paddr === targetDecodePattern) -> (port.psel === io.master.psel),
          "port psel should be equal to master psel when address is in ports range"
        )
    }
  }

  (io.targets, targetDecodePatterns).zipped.foreach {
    case (port, targetDecodePattern) =>
      val selected = io.master.psel && io.master.paddr === targetDecodePattern

      when(selected) {
        port.psel := 1.B
        io.master.pready := port.pready
        io.master.prdata := port.prdata
        io.master.pslverr := port.pslverr
      }
  }
}

object ApbMux {

  def apply(master: ApbTargetPort)(targets: (ApbTargetPort, Int)*): Unit = {

    // byte address ranges for each target
    val targetRanges = targets.map { case (port, base) =>
      (port, base until (base + (1 << port.addrWidth)))
    }

    targetRanges.foreach { case (port, range) =>
      require( // check for word alignment
        MemoryMapHelper.isWordAligned(range.start),
        s"Port ($port) at 0x${range.start.toHexString}: Base address must be word aligned"
      )

      require( // check for alignment to target address width
        MemoryMapHelper.isAlignedToAddrWidth(range.start, port.addrWidth),
        s"Port ($port) at 0x${range.start.toHexString}: Base address must be aligned to port's address width"
      )

      require( // check that target address range is inside master address space
        MemoryMapHelper.addressRangeInsideAdressSpace(range, master.addrWidth),
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
      val addressableBits = port.addrWidth
      val fixedAddrPart = base >> addressableBits
      BitPat(
        "b" + fixedAddrPart.toBinaryString.reverse
          .padTo(master.addrWidth - addressableBits, '0')
          .reverse
          .mkString + ("?" * addressableBits)
      )
    }

    println(s"ApbMux(${master.addrWidth}.W):")
    (targetRanges, patterns).zipped.foreach { case ((port, range), pattern) =>
      println(
        s"  $port (${port.addrWidth}.W): 0x${range.start.toHexString} - 0x${range.end.toHexString} -> $pattern"
      )
    }

    val apbMux = Module(
      new ApbMux(master.addrWidth, master.dataWidth, patterns)
    )
    apbMux.io.master <> master
    apbMux.io.targets.zip(targets.map(_._1)).foreach { case (target, port) =>
      target <> port
    }
  }

}

package misc

object MemoryMapHelper {

  def findAddressOverlap[P](
      ranges: Seq[(P, Range)]
  ): Option[(P, Range, P, Range)] = {
    ranges.zipWithIndex.foreach { case ((port, range), i) =>
      ranges.drop(i + 1).foreach { case (otherPort, otherRange) =>
        if (range.intersect(otherRange).nonEmpty)
          return Some((port, range, otherPort, otherRange))
      }
    }
    None
  }

  def isAlignedToAddrWidth(base: Int, addrWidth: Int): Boolean = {
    base % (1 << addrWidth) == 0
  }

  def isWordAligned(base: Int): Boolean = {
    base % 4 == 0
  }

  def addressRangeInsideAdressSpace(
      range: Range,
      memAddrWidth: Int
  ): Boolean = {
    range.end <= (1 << memAddrWidth)
  }

}

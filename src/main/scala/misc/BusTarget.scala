package misc

import chisel3.util.log2Ceil


case class BusTarget(portName: String, masterAddrWidth: Int, baseByteAddr: Int, byteAddrWidth: Int) {
  require(
    MemoryMapHelper.isWordAligned(baseByteAddr),
    s"$this: Base address must be word aligned: 0x${baseByteAddr.toHexString}"
  )
  require(
    MemoryMapHelper.isAlignedToAddrWidth(baseByteAddr, byteAddrWidth),
    s"$this: Base address must be aligned to address width (${byteAddrWidth}.W): 0x${baseByteAddr.toHexString}"
  )

  def checkInsideMasterAddrSpace(): Unit = {
    require(
      MemoryMapHelper.addressRangeInsideAdressSpace(
        byteAddrRange,
        masterAddrWidth
      ),
      s"$this: Address range 0x${byteAddrRange.start.toHexString}-0x${byteAddrRange.end.toHexString} "
        + s"is out of range for master port's address range (0x00-0x${(1 << masterAddrWidth).toHexString})"
    )
  }

  def byteAddrRange: Range =
    baseByteAddr until (baseByteAddr + (1 << byteAddrWidth))

  def fixedAddrPart = baseByteAddr >> byteAddrWidth

  override def toString(): String = {
    val digits = masterAddrWidth / 4 + (if (masterAddrWidth % 4 != 0) 1 else 0)
    s"%15s: 0x%0${digits}x - 0x%0${digits}x".format(
      portName.split(":").head,
      byteAddrRange.start,
      byteAddrRange.end
    )
  }
}
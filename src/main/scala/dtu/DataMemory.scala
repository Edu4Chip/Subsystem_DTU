package dtu

import chisel3._
import chisel3.util._

import leros.DataMemIO
import mem.MemoryFactory

/** A simple data memory module for the DTU.
  *
  * Uses the MemoryFactory to create a memory of the specified size.
  *
  * @param noBytes
  *   the size of the memory in bytes
  */
class DataMemory(noBytes: Int) extends Module {

  val addrWidth = log2Ceil(noBytes)

  val dmemPort = IO(new DataMemIO(addrWidth - 2))

  val mem = MemoryFactory.create(noBytes / 4)

  dmemPort.rdData := mem.read(dmemPort.rdAddr)

  when(dmemPort.wr) {
    mem.write(dmemPort.wrAddr, dmemPort.wrData, dmemPort.wrMask)
  }

}

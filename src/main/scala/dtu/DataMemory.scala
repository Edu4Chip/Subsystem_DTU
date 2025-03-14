package dtu

import chisel3._
import chisel3.util._

import leros.DataMemIO
import mem.MemoryFactory

class DataMemory(noBytes: Int) extends Module {

  val addrWidth = log2Ceil(noBytes)

  val dmemPort = IO(new DataMemIO(addrWidth - 2))

  val mem = MemoryFactory.create(noBytes / 4)

  dmemPort.rdData := mem.read(dmemPort.rdAddr)

  when(dmemPort.wr) {
    mem.write(dmemPort.wrAddr, dmemPort.wrData, dmemPort.wrMask)
  }

}

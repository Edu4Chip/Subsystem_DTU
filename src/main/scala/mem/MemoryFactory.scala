package mem

import chisel3._
import chisel3.util.log2Ceil

import misc.Helper.WordToByte
import misc.Helper.BytesToWord

trait AbstractMemory {
  def read(addr: UInt): UInt
  def write(addr: UInt, data: UInt, strb: UInt): Unit
}

object MemoryFactory {

  private var mem: Int => AbstractMemory = words => ChiselSyncMemory.create(words)

  def create(n: Int): AbstractMemory = mem(n)

  def use(mem: Int => AbstractMemory): Unit = {
    this.mem = mem
  }
}

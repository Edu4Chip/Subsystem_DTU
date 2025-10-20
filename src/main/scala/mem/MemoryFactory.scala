package mem

import chisel3._
import chisel3.util.log2Ceil

import misc.Helper.WordToByte
import misc.Helper.BytesToWord
import scala.util.DynamicVariable

trait AbstractMemory {
  def read(addr: UInt): UInt
  def write(addr: UInt, data: UInt, strb: UInt): Unit
}

trait MemoryFactory {
  def create(n: Int): AbstractMemory
}

object MemoryFactory {

  private val factory: DynamicVariable[MemoryFactory] = new DynamicVariable(ChiselSyncMemory)

  def create(n: Int): AbstractMemory = factory.value.create(n)

  def using[T](f: MemoryFactory)(block: => T): T = {
    factory.withValue(f) {
      block
    }
  }

  private val options = Map(
    "RtlSyncMemory" -> ChiselSyncMemory,
    "RtlRegMemory" -> RegMemory,
    "DffRam" -> DffRam,
    "DidacticSram" -> DidacticSram,
    "OpenRamSky130" -> Sky130Sram,
    "ChipFoundrySram" -> ChipFoundrySram
  )

  def fromString(name: String): MemoryFactory = {
    options.getOrElse(name, throw new Exception(s"Unknown memory type $name"))
  }

  def help(): Unit = {
    println("Available memory types:")
    options.keys.foreach { k =>
      println(s" - $k")
    }
  }
}

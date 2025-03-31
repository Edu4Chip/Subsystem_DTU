package dtu

import leros.util.Assembler
import ponte.PonteSerialDriver

class DtuSerialDriver(portDescriptor: String, baud: Int) {

  private val port = new PonteSerialDriver(portDescriptor, baud)

  private def send(addr: Int, data: Seq[Int]) = {
    assert(addr >= 0 && addr < 0x1000)
    port.send(addr, data)
  }
  private def send(addr: Int, data: Int): Unit = send(addr, Seq(data))

  private def read(addr: Int, words: Int) = {
    assert(addr >= 0 && addr < 0x1000)
    port.read(addr, words)
  }
  private def read(addr: Int): Int = read(addr, 1).head


  private def setSysCtrlBit(bit: Int) = {
    val status = read(0xC00)
    send(0xC00, status | (1 << bit))
  }

  private def clearSysCtrlBit(bit: Int) = {
    val status = read(0xC00)
    send(0xC00, status & ~(1 << bit))
  }

  def resetLerosEnable() = {
    setSysCtrlBit(0)
  }

  def resetLerosDisable() = {
    clearSysCtrlBit(0)
  }

  def resetLeros() = {
    resetLerosEnable()
    resetLerosDisable()
  }

  def selectBootRom() = {
    clearSysCtrlBit(1)
  }

  def selectBootRam() = {
    setSysCtrlBit(1)
  }

  def uploadProgram(path: String) = {
    val blop = Assembler.assemble(path)
    val words = blop.grouped(2).map {
      case Array(a,b) => (b << 16) | a
      case Array(a) => a
    }.toSeq
    loadInstrMem(words)
  }

  def loadInstrMem(data: Seq[Int]) = {
    port.send(0x0000, data)
    port.read(0x0000, data.length).zip(data).foreach {
      case (r, w) => assert(r == w, s"Wrong data")
    }
  }

  def writeCrossCoreReg(reg: Int, data: Int) = {
    port.send(0x800 | (reg * 4), Seq(data))
  }

  def readCrossCoreReg(reg: Int) = {
    port.read(0x800 | (reg * 4), 1).head
  }

  def close() = {
    port.close()
  }

}

package dtu

import basys3.Basys3SerialDriver
import leros.util.Assembler

class DtuSerialDriver(portDescriptor: String, baud: Int) {

  val port = new Basys3SerialDriver(portDescriptor, baud)

  def resetLerosEnable() = {
    port.setSSCtrlBit(1)
  }

  def resetLerosDisable() = {
    port.clearSSCtrlBit(1)
  }

  def resetLeros() = {
    port.setSSCtrlBit(1)
    port.clearSSCtrlBit(1)
  }

  def selectBootRom() = {
    port.clearSSCtrlBit(0)
  }

  def selectBootRam() = {
    port.setSSCtrlBit(0)
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

package basys3

import ponte.PonteSerialDriver

class Basys3SerialDriver(portDescriptor: String, baud: Int) {

  val port = new PonteSerialDriver(portDescriptor, baud)

  def setSSCtrlBit(bit: Int) = {
    val status = port.read(0x0)
    port.send(0x0, status | (1 << bit))
  }

  def clearSSCtrlBit(bit: Int) = {
    val status = port.read(0x0)
    port.send(0x0, status & ~(1 << bit))
  }

  def send(addr: Int, data: Seq[Int]) = {
    assert(addr >= 0 && addr < 0x1000)
    port.send(0x2000 | addr, data)
  }

  def read(addr: Int, words: Int) = {
    assert(addr >= 0 && addr < 0x1000)
    port.read(0x2000 | addr, words)
  }

  def close() = {
    port.close()
  }

}

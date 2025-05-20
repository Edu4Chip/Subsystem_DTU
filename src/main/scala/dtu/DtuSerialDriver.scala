package dtu

import leros.util.Assembler
import ponte.PonteSerialDriver

/** This class implements the DtuInterface using a serial port.
  *
  * It can be used to communicate with the DTU subsystem through a serial
  * interface.
  *
  * @param portDescriptor
  *   the serial port descriptor (e.g., "/dev/ttyUSB0")
  * @param baud
  *   the baud rate for the serial communication
  */
class DtuSerialDriver(portDescriptor: String, baud: Int) extends DtuInterface {

  private val port = new PonteSerialDriver(portDescriptor, baud)

  /** Send a sequence of words through the serial port.
    *
    * @param addr
    *   the start address to send the data to
    * @param data
    *   the sequence of words to send
    */
  def send(addr: Int, data: Seq[Int]) = {
    assert(addr >= 0 && addr < 0x1000)
    port.send(addr, data)
  }

  /** Read a sequence of words from the serial port.
    *
    * @param addr
    *   the start address to read the data from
    * @param words
    *   the number of words to read
    * @return
    *   a sequence of words read from the serial port
    */
  def read(addr: Int, words: Int) = {
    assert(addr >= 0 && addr < 0x1000)
    port.read(addr, words)
  }

  def close() = {
    port.close()
  }

}

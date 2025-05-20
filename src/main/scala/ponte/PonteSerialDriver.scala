package ponte

import com.fazecast.jSerialComm.SerialPort
import scala.collection.mutable.ArrayBuffer

class PonteSerialDriver(portDescriptor: String, baud: Int) {

  val port = SerialPort.getCommPort(portDescriptor)

  port.setComPortParameters(baud, 8, 1, 0)
  port.setComPortTimeouts(
    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
    1000,
    0
  )

  if (port.openPort()) {} else {
    throw new Exception(s"Failed to open port $portDescriptor")
  }

  val out = port.getOutputStream()
  val in = port.getInputStream()

  def toBytes(value: Int, n: Int): Array[Int] = {
    val bytes = new ArrayBuffer[Int]
    for (i <- 0 until n) {
      val byte = (value >> (i * 8) & 0xff)
      if (
        byte == Ponte.START_WR || byte == Ponte.START_RD || byte == Ponte.ESC
      ) {
        bytes += Ponte.ESC
        bytes += byte ^ Ponte.ESC_MASK
      } else {
        bytes += byte
      }
    }
    bytes.toArray
  }

  def send(addr: Int, data: Seq[Int]): Unit = try {

    val frame = ArrayBuffer[Int]()
    frame += Ponte.START_WR
    frame ++= toBytes(addr, 2)

    for (word <- data) {
      frame ++= toBytes(word, 4)
    }

    out.write(frame.map(_.toByte).toArray)
    out.flush()
  } catch {
    case e: Exception =>
      port.closePort()
      throw e
  }
  def send(addr: Int, data: Int): Unit = send(addr, Seq(data))
  def read(addr: Int): Int = read(addr, 1).head

  def read(addr: Int, words: Int): Seq[Int] = try {

    val frame = ArrayBuffer[Int]()
    frame += Ponte.START_RD
    frame += (words - 1)
    frame ++= toBytes(addr, 2)

    out.write(frame.map(_.toByte).toArray)
    out.flush()

    val data = ArrayBuffer[Int]()
    for (_ <- 0 until words) {
      val x = for (_ <- 0 until 4) yield in.read()
      val word = (x(3) << 24) | (x(2) << 16) | (x(1) << 8) | x(0)
      data += word
    }
    data.toSeq
  } catch {
    case e: Exception =>
      port.closePort()
      throw e
  }

  def close(): Unit = {
    port.closePort()
  }

}

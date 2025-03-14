package ponte

import chisel3._
import chiseltest._

class PonteDecoderBfm(p: PonteDecoder) {

  private def sendByteUnescaped(x: Int): Unit = {
    p.io.in.valid.poke(1.B)
    p.io.in.bits.poke(x.U)
    while (!p.io.in.ready.peekBoolean()) p.clock.step()
    p.clock.step()
    p.io.in.valid.poke(0.B)
  }

  private def sendByte(x: Int): Unit = {
    if (x == Ponte.ESC) {
      sendByteUnescaped(Ponte.ESC)
      sendByteUnescaped(x ^ Ponte.ESC_MASK)
    } else {
      sendByteUnescaped(x)
    }
  }

  private def receiveByte(): Int = {
    p.io.out.ready.poke(1.B)
    while (!p.io.out.valid.peekBoolean()) p.clock.step()
    val x = p.io.out.bits.peekInt().toInt
    println(s"Received byte: $x")
    p.clock.step()
    p.io.out.ready.poke(0.B)
    x
  }

  private def receiveWord(): Int = {
    Seq.fill(4)(receiveByte()).foldRight(0)((x, acc) => (acc << 8) | x)
  }

  private def toBytes(x: Int, n: Int): Seq[Int] =
    for (i <- 0 until n) yield (x >> (i * 8)) & 0xff

  def write(baseAddr: Int, xs: Seq[Int]): Unit = {
    sendByteUnescaped(Ponte.START_WR)
    toBytes(baseAddr, 2).foreach(sendByte)
    xs.flatMap(toBytes(_, 4)).foreach(sendByte)
  }

  def read(baseAddr: Int, numberWords: Int): Seq[Int] = {
    sendByteUnescaped(Ponte.START_RD)
    sendByte(numberWords - 1)
    toBytes(baseAddr, 2).foreach(sendByte)
    Seq.fill(numberWords)(receiveWord())
  }

}

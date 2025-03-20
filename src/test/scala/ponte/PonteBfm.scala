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
    if (x == Ponte.ESC || x == Ponte.START_WR || x == Ponte.START_RD) {
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
    p.clock.step()
    p.io.out.ready.poke(0.B)
    x
  }

  private def receiveWord(): BigInt = {
    Seq.fill(4)(receiveByte()).map(BigInt(_)).foldRight(BigInt(0))((x, acc) => (acc << 8) | x)
  }

  private def toBytes(x: BigInt, n: Int): Seq[Int] =
    for (i <- 0 until n) yield ((x >> (i * 8)) & 0xff).toInt

  def write(baseAddr: Int, xs: Seq[BigInt]): Unit = {
    sendByteUnescaped(Ponte.START_WR)
    toBytes(baseAddr, 2).foreach(sendByte)
    xs.flatMap(toBytes(_, 4)).foreach(sendByte)
  }

  def read(baseAddr: Int, numberWords: Int): Seq[BigInt] = {
    sendByteUnescaped(Ponte.START_RD)
    sendByte(numberWords - 1)
    toBytes(baseAddr, 2).foreach(sendByte)
    Seq.fill(numberWords)(receiveWord())
  }

}

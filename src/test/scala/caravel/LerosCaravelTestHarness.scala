package caravel

import dtu.DtuInterface
import wishbone._
import chisel3.Clock


case class LerosCaravelTestHarness(dut: HasWishbonePort, clk: Clock, offset: Int) extends DtuInterface {

  val wbBfm = new WishboneBfm(clk, dut.getWbPort)

  def send(addr: Int, data: Seq[Int]): Unit = {
    assert(addr >= 0 && addr < 0x1000, s"Address 0x${addr.toHexString} out of range (send)")
    data.zipWithIndex.foreach { case (d, i) =>
      wbBfm.write(addr + offset + i * 4, BigInt(d) & 0xFFFFFFFFL)
    }
  }
  

  def read(addr: Int, words: Int): Seq[Int] = {
    assert(addr >= 0 && addr < 0x1000, s"Address 0x${addr.toHexString} out of range (read)")
    (0 until words).map { i =>
      wbBfm.read(addr + offset + i * 4).toInt
    }
  }

}
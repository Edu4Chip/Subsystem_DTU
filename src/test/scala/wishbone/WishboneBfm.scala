package wishbone


import chisel3._

import chiseltest._

import misc.TestingHelper.ClockExtension


class WishboneBfm(clock: Clock, io: WishbonePort) {

  def write(
      addr: BigInt,
      wdata: BigInt,
      mask: Int = 0xf
  ): Unit = {
    // setup tx
    io.adr.poke(addr.U)
    io.we.poke(1.B)
    io.cyc.poke(1.B)
    io.stb.poke(1.B)
    io.dat_i.poke(wdata.U)
    io.sel.poke(mask.U)
    clock.step()
    // wait for ack
    clock.stepUntil(io.ack.peekBoolean)
    clock.step()

    // finish
    io.adr.poke(0.U)
    io.we.poke(0.B)
    io.cyc.poke(0.B)
    io.stb.poke(0.B)
    io.dat_i.poke(0.U)
    io.sel.poke(0.U)
  }

  def read(addr: BigInt): BigInt = {
    // setup tx
    io.adr.poke(addr.U)
    io.we.poke(0.B)
    io.cyc.poke(1.B)
    io.stb.poke(1.B)  
    io.dat_i.poke(0.U)
    clock.step()

    // wait for ack
    clock.stepUntil(io.ack.peekBoolean, 10)
    val rdData = io.dat_o.peekInt()
    clock.step()

    // finish + read out read data
    io.adr.poke(0.U)
    io.we.poke(0.B)
    io.cyc.poke(0.B)
    io.stb.poke(0.B)
    io.dat_i.poke(0.U)

    rdData
  }

}

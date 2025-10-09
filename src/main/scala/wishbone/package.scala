import chisel3._

import chiseltest._
import chiseltest.formal._
import misc.FormalHelper._

package wishbone {

  object WishbonePort {
    def masterPort(addrWidth: Int): WishbonePort = {
      Flipped(new WishbonePort(addrWidth))
    }

    def targetPort(addrWidth: Int): WishbonePort = {
      new WishbonePort(addrWidth)
    }
  }

  class WishbonePort(val addrWidth: Int) extends Bundle {
    val stb = Input(Bool())
    val cyc = Input(Bool())
    val we = Input(Bool())
    val sel = Input(UInt(4.W))
    val dat_i = Input(UInt(32.W))
    val adr = Input(UInt(addrWidth.W))
    val dat_o = Output(UInt(32.W))
    val ack = Output(Bool())


    val MAX_RESP_TIME = 11


    def targetPortProperties(name: String): Unit = {

      val active = RegInit(0.B) // tracks ongoing transaction
      when(active && ack) {
        active := 0.B
      }.elsewhen(cyc && stb) {
        active := 1.B
      }

      // Target Properties
      assert(
        rose(active).within(MAX_RESP_TIME) |=> ack,
        cf"${name}: the target signals ack at least $MAX_RESP_TIME cycles after the beginning of a transaction"
      )

      assert(
        ack -> (cyc && stb),
        cf"${name}: the target only asserts ack when cyc and stb are asserted"
      )

      // Livemess check
      assert(
        active.within(MAX_RESP_TIME) |=> !active,
        cf"${name}: an active transaction should be completed within $MAX_RESP_TIME cycles"
      )


      // Master control properties
      assert(
        stb && !active |=> active,
        cf"${name}: asserting stb while idle leads to the access phase"
      )

      assume(
        active -> cyc,
        cf"${name}: cyc is asserted during an active transaction"
      )
      assume(
        active -> stb,
        cf"${name}: stb is asserted during an active transaction"
      )
      assume(
        stb -> cyc,
        cf"${name}: stb can only be asserted when cyc is asserted"
      )
      assume(
        cyc -> stb,
        cf"${name}: cyc can only be asserted when stb is asserted"
      )

      assume(
        active -> stable(we),
        cf"${name}: we is stable during an active transaction"
      )
      assume(
        active -> stable(sel),
        cf"${name}: sel is stable during an active transaction"
      )
      assume(
        active -> stable(adr),
        cf"${name}: adr is stable during an active transaction"
      )
      assume(
        active -> stable(dat_i),
        cf"${name}: dat_i is stable during an active transaction"
      )




    }


  }


}
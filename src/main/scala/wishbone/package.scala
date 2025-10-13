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

      val wbTxActive = RegInit(0.B) // tracks ongoing transaction
      when(wbTxActive && ack) {
        wbTxActive := 0.B
      }.elsewhen(cyc && stb) {
        wbTxActive := 1.B
      }

      // Target Properties
      assert(
        rose(wbTxActive).within(MAX_RESP_TIME) |=> ack,
        cf"${name}: the target signals ack at least $MAX_RESP_TIME cycles after the beginning of a transaction"
      )

      assert(
        ack -> (cyc && stb),
        cf"${name}: the target only asserts ack when cyc and stb are asserted"
      )

      // Livemess check
      assert(
        wbTxActive.within(MAX_RESP_TIME) |=> !wbTxActive,
        cf"${name}: an active transaction should be completed within $MAX_RESP_TIME cycles"
      )


      // Master control properties
      assert(
        stb && !wbTxActive |=> wbTxActive,
        cf"${name}: asserting stb while idle leads to the access phase"
      )

      assume(
        wbTxActive -> cyc,
        cf"${name}: cyc is asserted during an active transaction"
      )
      assume(
        wbTxActive -> stb,
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
        wbTxActive -> stable(we),
        cf"${name}: we is stable during an active transaction"
      )
      assume(
        wbTxActive -> stable(sel),
        cf"${name}: sel is stable during an active transaction"
      )
      assume(
        wbTxActive -> stable(adr),
        cf"${name}: adr is stable during an active transaction"
      )
      assume(
        wbTxActive -> stable(dat_i),
        cf"${name}: dat_i is stable during an active transaction"
      )




    }


  }


}
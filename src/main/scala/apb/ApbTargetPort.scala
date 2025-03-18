package apb

import chisel3._
import chisel3.util._

import chiseltest._
import chiseltest.formal._
import misc.FormalHelper._

class ApbTargetPort(
    val addrWidth: Int,
    val dataWidth: Int
) extends Bundle {
  val paddr = Input(UInt(addrWidth.W))
  val psel = Input(Bool())
  val penable = Input(Bool())
  val pwrite = Input(Bool())
  val pstrb = Input(UInt((dataWidth / 8).W))
  val pwdata = Input(UInt(dataWidth.W))
  val pready = Output(Bool())
  val prdata = Output(UInt(dataWidth.W))
  val pslverr = Output(Bool())

  val MAX_RESP_TIME = 10

  def targetPortProperties(): Unit = formalblock {

    val active = RegInit(0.B) // tracks ongoing transaction
    when(active && pready) {
      active := 0.B
    }.elsewhen(psel) {
      active := 1.B
    }

    // Target Properties
    assert(rose(active) |MAX_RESP_TIME|=> pready, 
      cf"the target signals ready at least $MAX_RESP_TIME cycles after"
    )
    assert(pslverr -> pready, 
      "the target signals an error when it is ready"
    )
    assert(pready -> (psel && penable),
      "pready is only asserted in the access phase"
    )

    // Liveness Check
    assert(active |MAX_RESP_TIME|=> !active,
      cf"an active transaction should be completed within $MAX_RESP_TIME cycles"
    )


    // Master control properties
    assert(psel && !active |=> active,
      "asserting psel while idle leads to the access phase"
    )
    assume((psel && !active) -> !penable,
      "penable has to be low during the setup phase"
    )
    assume(active -> psel, 
      "psel has to be asserted during the access phase"
    )
    assume(active -> penable, 
      "penable has to be asserted during the access phase"
    )

    // Master data stability properties
    assume(active -> stable(paddr), 
      "paddr should be stable during the setup and access phases"
    )
    assume(active -> stable(pwrite), 
      "pwrite should be stable during the setup and access phases"
    )
    assume(active -> stable(pstrb), 
      "pstrb should be stable during the setup and access phases"
    )
    assume(active -> stable(pwdata), 
      "pwdata should be stable during the setup and access phases"
    )
  }


  def masterPortProperties(): Unit = formalblock {

    val active = RegInit(0.B) // tracks ongoing transaction
    when(active && pready) {
      active := 0.B
    }.elsewhen(psel) {
      active := 1.B
    }

    // Target Assumptions
    assume(rose(active) |MAX_RESP_TIME|=> pready, 
      "the target signals ready at least 4 cycles after"
    )
    assume(pslverr -> pready, 
      "the target signals an error when it is ready"
    )
    assume(pready -> (psel && penable),
      "pready is only asserted in the access phase"
    )

    // Liveness Check
    assert(active |MAX_RESP_TIME|=> !active,
      "an active transaction should be completed within 5 cycles"
    )

    // Master control properties
    assert(psel && !active |=> active,
      "asserting psel while idle leads to the access phase"
    )
    assert((psel && !active) -> !penable,
      "penable has to be low during the setup phase"
    )
    assert(active -> psel, 
      "psel has to be asserted during the access phase"
    )
    assert(active -> penable, 
      "penable has to be asserted during the access phase"
    )


    // Master data stability properties
    assert(active -> stable(paddr), 
      "paddr should be stable during the setup and access phases"
    )
    assert(active -> stable(pwrite), 
      "pwrite should be stable during the setup and access phases"
    )
    assert(active -> stable(pstrb), 
      "pstrb should be stable during the setup and access phases"
    )
    assert(active -> stable(pwdata), 
      "pwdata should be stable during the setup and access phases"
    )
  }
  
}

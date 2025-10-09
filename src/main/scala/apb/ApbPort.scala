package apb

import chisel3._
import chisel3.util._

import scala.collection.mutable

import chiseltest._
import chiseltest.formal._
import misc.FormalHelper._

object ApbPort {
  def targetPort(addrWidth: Int, dataWidth: Int): ApbPort = {
    new ApbPort(addrWidth, dataWidth)
  }

  def masterPort(addrWidth: Int, dataWidth: Int): ApbPort = {
    Flipped(new ApbPort(addrWidth, dataWidth))
  }
}

class ApbPort(
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

  val children = mutable.ListBuffer[ApbTarget]()
  var addChild: ApbTarget => Unit = child => {
    children += child
  }

  def getTargets(): Seq[ApbTarget] = {
    children.toSeq
  }

  val MAX_RESP_TIME = 10

  def targetPortProperties(name: String): Unit = {

    val active = RegInit(0.B) // tracks ongoing transaction
    when(active && pready) {
      active := 0.B
    }.elsewhen(psel) {
      active := 1.B
    }

    // Target Properties
    assert(
      rose(active).within(MAX_RESP_TIME) |=> pready,
      cf"${name}: the target signals ready at least $MAX_RESP_TIME cycles after"
    )
    assert(
      (psel && pslverr) -> pready,
      cf"${name}: the target signals an error at the end of the access phase"
    )
    assert(
      pready -> (psel && penable),
      cf"${name}: pready is only asserted in the access phase"
    )

    // Liveness Check
    assert(
      active.within(MAX_RESP_TIME) |=> !active,
      cf"${name}: an active transaction should be completed within $MAX_RESP_TIME cycles"
    )

    // Master control properties
    assert(
      psel && !active |=> active,
      cf"${name}: asserting psel while idle leads to the access phase"
    )
    assume(
      (psel && !active) -> !penable,
      cf"${name}: penable has to be low during the setup phase"
    )
    assume(
      active -> psel,
      cf"${name}: psel has to be asserted during the access phase"
    )
    assume(
      active -> penable,
      cf"${name}: penable has to be asserted during the access phase"
    )

    // Master data stability properties
    assume(
      active -> stable(paddr),
      cf"${name}: paddr should be stable during the setup and access phases"
    )
    assume(
      active -> stable(pwrite),
      cf"${name}: pwrite should be stable during the setup and access phases"
    )
    assume(
      active -> stable(pstrb),
      cf"${name}: pstrb should be stable during the setup and access phases"
    )
    assume(
      active -> stable(pwdata),
      cf"${name}: pwdata should be stable during the setup and access phases"
    )
  }

  def masterPortProperties(name: String): Unit = {

    val active = RegInit(0.B) // tracks ongoing transaction
    when(active && pready) {
      active := 0.B
    }.elsewhen(psel) {
      active := 1.B
    }

    // Target Assumptions
    assume(
      rose(active).within(MAX_RESP_TIME) |=> pready,
      cf"${name}: the target signals ready at least ${MAX_RESP_TIME} cycles after"
    )
    assume(
      (psel && pslverr) -> pready,
      cf"${name}: the target signals an error at the end of the access phase"
    )
    assume(
      pready -> (psel && penable),
      cf"${name}: pready is only asserted in the access phase"
    )

    // Liveness Check
    assert(
      active.within(MAX_RESP_TIME) |=> !active,
      cf"${name}: an active transaction should be completed within ${MAX_RESP_TIME} cycles"
    )

    // Master control properties
    assert(
      psel && !active |=> active,
      cf"${name}: asserting psel while idle leads to the access phase"
    )
    assert(
      (psel && !active) -> !penable,
      cf"${name}: penable has to be low during the setup phase"
    )
    assert(
      active -> psel,
      cf"${name}: psel has to be asserted during the access phase"
    )
    assert(
      active -> penable,
      cf"${name}: penable has to be asserted during the access phase"
    )

    // Master data stability properties
    assert(
      active -> stable(paddr),
      cf"${name}: paddr should be stable during the setup and access phases"
    )
    assert(
      active -> stable(pwrite),
      cf"${name}: pwrite should be stable during the setup and access phases"
    )
    assert(
      active -> stable(pstrb),
      cf"${name}: pstrb should be stable during the setup and access phases"
    )
    assert(
      active -> stable(pwdata),
      cf"${name}: pwdata should be stable during the setup and access phases"
    )
  }

}

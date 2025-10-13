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

    val apbTxActive = RegInit(0.B) // tracks ongoing transaction
    when(apbTxActive && pready) {
      apbTxActive := 0.B
    }.elsewhen(psel) {
      apbTxActive := 1.B
    }

    // Target Properties
    assert(
      rose(apbTxActive).within(MAX_RESP_TIME) |=> pready,
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
      apbTxActive.within(MAX_RESP_TIME) |=> !apbTxActive,
      cf"${name}: an active transaction should be completed within $MAX_RESP_TIME cycles"
    )

    // Master control properties
    assert(
      psel && !apbTxActive |=> apbTxActive,
      cf"${name}: asserting psel while idle leads to the access phase"
    )
    assume(
      (psel && !apbTxActive) -> !penable,
      cf"${name}: penable has to be low during the setup phase"
    )
    assume(
      apbTxActive -> psel,
      cf"${name}: psel has to be asserted during the access phase"
    )
    assume(
      apbTxActive -> penable,
      cf"${name}: penable has to be asserted during the access phase"
    )

    // Master data stability properties
    assume(
      apbTxActive -> stable(paddr),
      cf"${name}: paddr should be stable during the setup and access phases"
    )
    assume(
      apbTxActive -> stable(pwrite),
      cf"${name}: pwrite should be stable during the setup and access phases"
    )
    assume(
      apbTxActive -> stable(pstrb),
      cf"${name}: pstrb should be stable during the setup and access phases"
    )
    assume(
      apbTxActive -> stable(pwdata),
      cf"${name}: pwdata should be stable during the setup and access phases"
    )
  }

  def masterPortProperties(name: String): Unit = {

    val apbTxActive = RegInit(0.B) // tracks ongoing transaction
    when(apbTxActive && pready) {
      apbTxActive := 0.B
    }.elsewhen(psel) {
      apbTxActive := 1.B
    }

    // Target Assumptions
    assume(
      rose(apbTxActive).within(MAX_RESP_TIME) |=> pready,
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
      apbTxActive.within(MAX_RESP_TIME + 1) |=> !apbTxActive,
      cf"${name}: an active transaction should be completed within ${MAX_RESP_TIME} cycles"
    )

    // Master control properties
    assert(
      psel && !apbTxActive |=> apbTxActive,
      cf"${name}: asserting psel while idle leads to the access phase"
    )
    assert(
      (psel && !apbTxActive) -> !penable,
      cf"${name}: penable has to be low during the setup phase"
    )
    assert(
      apbTxActive -> psel,
      cf"${name}: psel has to be asserted during the access phase"
    )
    assert(
      apbTxActive -> penable,
      cf"${name}: penable has to be asserted during the access phase"
    )

    // Master data stability properties
    assert(
      apbTxActive -> stable(paddr),
      cf"${name}: paddr should be stable during the setup and access phases"
    )
    assert(
      apbTxActive -> stable(pwrite),
      cf"${name}: pwrite should be stable during the setup and access phases"
    )
    assert(
      apbTxActive -> stable(pstrb),
      cf"${name}: pstrb should be stable during the setup and access phases"
    )
    assert(
      apbTxActive -> stable(pwdata),
      cf"${name}: pwdata should be stable during the setup and access phases"
    )
  }

}

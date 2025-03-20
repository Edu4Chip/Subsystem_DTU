package dtu.peripherals

import chisel3._
import chisel3.util._

import apb.ApbTargetPort
import leros.DataMemIO
import io.PmodPins

/** Memory mapped interface for a single 6-pin (4 data) PMOD port.
  *
  * Address 0: read and write output enable (active low)
  *
  * Address 1: read and write output state Address 2: read input state
  */
class Gpio extends Module {

  val dmemPort = IO(new DataMemIO(2))
  val pmodPort = IO(new PmodPins)

  val oes = RegInit(0.U(4.W))
  val gpos = RegInit(0.U(4.W))
  val gpis = pmodPort.gpi

  dmemPort.rdData := MuxLookup(
    RegNext(dmemPort.rdAddr),
    DontCare,
    Seq(
      0.U -> oes,
      1.U -> gpos,
      2.U -> gpis
    )
  )

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) {
        oes := dmemPort.wrData
      }
      is(1.U) {
        gpos := dmemPort.wrData
      }
    }
  }

  pmodPort.gpo := gpos
  pmodPort.oe := oes

}

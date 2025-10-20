package dtu.peripherals

import chisel3._
import chisel3.util._

import leros.DataMemIO
import io.GpioPins

/** Memory mapped interface for a single 6-pin (4 data) PMOD port.
  *
  * Address 0: read and write output enable (active low)
  *
  * Address 1: read and write output state
  *
  * Address 2: read input state
  */
class Gpio(pins: Int) extends Module {

  val dmemPort = IO(new DataMemIO(2))
  val gpioPort = IO(new GpioPins(pins))

  val outputEnables = RegInit(0.U(pins.W))
  val outputs = RegInit(0.U(pins.W))
  val inputs = gpioPort.in

  dmemPort.rdData := MuxLookup(RegNext(dmemPort.rdAddr), outputEnables)(
    Seq(
      0.U -> outputEnables,
      1.U -> outputs,
      2.U -> inputs
    )
  )

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) {
        outputEnables := dmemPort.wrData
      }
      is(1.U) {
        outputs := dmemPort.wrData
      }
    }
  }

  gpioPort.out := outputs
  gpioPort.oe := outputEnables

}

import chisel3._

package object io {
  class GpioPins(w: Int) extends Bundle() {
    val in = Input(UInt(w.W))
    val out = Output(UInt(w.W))
    /** output enable active low */
    val oe = Output(UInt(w.W))
  }
  class UartPins extends Bundle {
    val tx = Output(Bool())
    val rx = Input(Bool())
  }
}

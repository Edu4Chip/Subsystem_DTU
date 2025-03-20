import chisel3._

package object io {
  class PmodPins() extends Bundle() {
    val gpi = Input(UInt(4.W))
    val gpo = Output(UInt(4.W))
    val oe = Output(UInt(4.W)) // output enable active low
  }
  class UartPins extends Bundle {
    val tx = Output(Bool())
    val rx = Input(Bool())
  }
}

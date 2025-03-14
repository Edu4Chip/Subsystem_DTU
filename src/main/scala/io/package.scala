import chisel3._

package object io {
  class UartPins extends Bundle {
    val tx = Output(Bool())
    val rx = Input(Bool())
  }
}

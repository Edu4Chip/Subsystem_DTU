import chisel3._
import chisel3.util._

class PmodGpioPort() extends Bundle() {
    val gpi = Input(UInt(4.W))
    val gpo = Output(UInt(4.W))
    val oe = Output(UInt(4.W)) // output enable active low
}

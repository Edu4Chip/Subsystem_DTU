import chisel3._
import chisel3.util._

class ResetSync extends HasBlackBoxResource {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val resetIn = Input(Bool())
        val resetOut = Output(Bool())
    })
    addResource("ResetSync.sv")
}
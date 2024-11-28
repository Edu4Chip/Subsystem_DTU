import chisel3._
import chisel3.util._

trait ResetSyncBase {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val resetIn = Input(Bool())
        val resetOut = Output(Bool())
    })
}

class ResetSync extends HasBlackBoxResource with ResetSyncBase {
    addResource("ResetSync.sv")
}


class ResetSyncTest extends Module with ResetSyncBase {
    io.resetOut := io.resetIn
}
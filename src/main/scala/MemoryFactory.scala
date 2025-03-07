import chisel3._
import chisel3.util.log2Ceil

import Helper.WordToByte
import Helper.BytesToWord

trait AbstractMemory {
    def read(addr: UInt): UInt
    def write(addr: UInt, data: UInt, strb: UInt): Unit
}

object MemoryFactory {

    private var mem: Int => AbstractMemory = n => RegMemory.create(n)

    def create(n: Int): AbstractMemory = mem(n)

    def use(mem: Int => AbstractMemory): Unit = {
        this.mem = mem
    }
}


object RegMemory {
    def create(n: Int): AbstractMemory = {
        val m = Module(new RegMemory(n))
        m.io.wordAddr := 0.U
        m.io.write := 0.B
        m.io.wrData := 0.U
        m.io.mask := 0.U
        m
    }
}
class RegMemory(words: Int) extends Module with AbstractMemory {
    val addrWidth = log2Ceil(words)

    val io = IO(new Bundle {
        val wordAddr = Input(UInt(addrWidth.W))
        val write = Input(Bool())
        val wrData = Input(UInt(32.W))
        val rdData = Output(UInt(32.W))
        val mask = Input(UInt(4.W))
    })

    
    val mem = RegInit(VecInit.fill(words, 4)(0.U(8.W)))

    val addrReg = RegNext(io.wordAddr)
    io.rdData := mem(addrReg).toWord

    when(io.write) {
        val data = io.wrData.toBytes(4)
        val mask = io.mask.asBools
        (mem(io.wordAddr), data, mask).zipped.foreach { (m, d, msk) =>
            when(msk) {
                m := d
            }
        }
    }

    def read(addr: UInt): UInt = {
        io.wordAddr := addr
        io.rdData
    }

    def write(addr: UInt, data: UInt, strb: UInt): Unit = {
        io.wordAddr := addr
        io.wrData := data
        io.mask := strb
        io.write := 1.B
    }

}


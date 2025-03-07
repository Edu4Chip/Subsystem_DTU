import chisel3._


object Helper {


    implicit class WordToByte(word: UInt) {
        def toBytes(n: Int): Vec[UInt] = {
            val bytes = for (i <- 0 until n) yield word(i * 8 + 7, i * 8)
            VecInit(bytes)
        }
    }


    implicit class UIntRangeCheck(value: UInt) {
        def inRange(min: UInt, max: UInt): Bool = {
            value >= min && value < max
        }
    }

    implicit class BytesToWord(bytes: Vec[UInt]) {
        def toWord: UInt = {
            bytes.foldRight(0.U) { (byte, acc) => acc << 8 | byte }
        }
    }

}

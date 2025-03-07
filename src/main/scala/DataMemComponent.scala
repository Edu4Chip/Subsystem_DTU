import chisel3._

import leros.DataMemIO



trait DataMemTarget {

    val dmemIO: DataMemIO

}



class DataMemTree(width: Int, map: Seq[(BigInt,BigInt)]) extends Module {

    val master = IO(Flipped(new DataMemIO(width)))

    val targets = IO(Vec(map.length, new DataMemIO(width)))

    targets.foreach { t => t <> master }

}
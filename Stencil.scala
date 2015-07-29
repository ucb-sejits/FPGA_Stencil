package Stencil
import Chisel._
import param._
import ChiselFloat._
import math.{ceil, pow, log}



//Define a stallable, parameterizable shift register.
object EnableShiftRegister
{
  def apply[T <: Data](in: T, n: Int, en: Bool = Bool(true)): T =
  {
    if (n == 1) {
      RegEnable(in, en)
    } else if (n != 0) {
      RegEnable(apply(in, n-1, en), en)
    } else {
      in
    }
  }
}


class ExposedEnableSR(val len: Int) extends Module {
  val io = new Bundle {
    val in = UInt(INPUT, 32)
    val en = Bool(INPUT)
    val outVec = Vec.fill(len){UInt(OUTPUT, 32)}
  }
  val regVec = Vec.fill(len){Reg(UInt(32))}
  when (io.en) {
    for (i <- len - 1 to 1 by -1) {
      regVec(i) := regVec(i - 1)
    }
    regVec(0) := io.in
  }
  for (i <- 0 to len - 1) {
    io.outVec(i) := regVec(i)
  }
}


object VecReverser {
  def apply (in: Vec[UInt], n: Int): Vec[UInt] = {
    val reverseVec = Vec.fill(n){UInt()}
    for (i <- 0 to n - 1) {
      reverseVec(i) := in(n - 1 - i)
    }
    reverseVec
  }
}






class DelayGrid(val window_width: Int, val grid_width: Int) extends Module {
  val window_size = pow(window_width, 2).toInt
  val wireVec_size = window_width
  val regVec_size = window_size - wireVec_size

  val io = new Bundle {
    val in = UInt(INPUT, 32)
    val en = Bool(INPUT)
    val outVec = Vec.fill(window_size){UInt(OUTPUT, 32)}
  }

  val wireVec = Vec.fill(wireVec_size){UInt()}
  val regVec = Vec.fill(regVec_size){Reg(UInt(32))}

  // Assign registers inputs
  when (io.en) {
    for (i <- 0 to regVec_size - 1) {
      if (i % (window_width - 1) == 0) {
        // Get input from wireVec
        regVec(i) := wireVec(i / (window_width - 1))
      } else {
        // Get input from regVec
        regVec(i) := regVec(i - 1)
      }
    }
  }

  // Assign wires inputs (including instantiation of shift registers)
  wireVec(0) := io.in
  for (i <- 1 to wireVec_size - 1) {
    wireVec(i) := EnableShiftRegister(regVec(i * (window_width - 1) - 1), grid_width - (window_width - 1), io.en)
  }


  // Merge wire and register outputs into output vector
  for (i <- 0 to window_width - 1) {
    // First output of each row should be from a wire
    io.outVec(i * window_width) := wireVec(i)
    // Remaining window_width - 1 output of each row should be from registers
    for (j <- 1 to window_width - 1) {
      io.outVec(i * window_width + j) := regVec(i * (window_width - 1) + j - 1)
    }
  }

}




object FPMult32 {
  def apply (a: UInt, b: UInt, en: Bool) = {
    val multiplier = Module(new FPMult32())
    multiplier.io.a := a
    multiplier.io.b := b
    multiplier.io.en := en
    multiplier.io.res
  }
}


object FPAdd32 {
  def apply (a: UInt, b: UInt, en: Bool) = {
    val adder = Module(new FPAdd32())
    adder.io.a := a
    adder.io.b := b
    adder.io.en := en
    adder.io.res
  }
}

object FPAdd32Dummy {
  def apply (a: UInt, en: Bool) = {
    val adder = Module(new FPAdd32Dummy())
    adder.io.a := a
    adder.io.en := en
    adder.io.res
  }
}



class FPAdd32Dummy extends Module {
  val io = new Bundle {
      val a = Bits(INPUT, 32)
      val res = Bits(OUTPUT, 32)
      val en = Bool(INPUT)
  }

  val delay_1 = Reg(UInt(32))
  val delay_2 = Reg(UInt(32))
  val delay_3 = Reg(UInt(32))
  val delay_4 = Reg(UInt(32))

  when (io.en) {
    delay_1 := io.a
    delay_2 := delay_1
    delay_3 := delay_2
    delay_4 := delay_3
  }

  io.res := delay_4
}




object FPAdd32_N {
  def apply (in: Vec[UInt], en: Bool, n: Int): UInt = {
    if (n == 2) {
      FPAdd32(in(0), in(1), en)
    } else {
      val numAdds = n / 2
      val numDummies = n % 2
      val totalResults = numAdds + numDummies
      val resultVec = Vec.fill(totalResults){UInt()}
      for (i <- 0 to numAdds - 1) {
        resultVec(i) := FPAdd32(in(i * 2), in(i * 2 + 1), en)
      }
      if (numDummies != 0) {
        resultVec(numAdds) := FPAdd32Dummy(in(numAdds * 2), en)
      }
      apply(resultVec, en, totalResults)
    }
  }
}


object FPMult32withCoeff_N {
  def apply (in: Vec[UInt], coeff: Vec[UInt], en: Bool, n: Int): Vec[UInt] = {
    val resultVec = Vec.fill(n){UInt()}
    for (i <- 0 to n - 1) {
      resultVec(i) := FPMult32(in(i), coeff(i), en)
    }
    resultVec
  }
}






class InboundFifoIO(n: Int) extends Bundle {
  val empty = Bool(INPUT)
  val rd_en = Bool(OUTPUT)
  val data = Bits(INPUT, n)
}

class OutboundFifoIO(n: Int) extends Bundle {
  val full = Bool(INPUT)
  val wr_en = Bool(OUTPUT)
  val data = Bits(OUTPUT, n)
}




// Pull from inbound fifo and push to ready-valid interface
class InFifoRV(n: Int) extends Module {
  val io = new Bundle {
    val in = new InboundFifoIO(n)
    val out = Decoupled(Bits(n))
  }

  /* Data registers to hold output of fifo while waiting for a ready-valid transaction
  Two registers are required because there is a cycle delay in issuing a read enable and getting
  a result. If one register were used then there would have to be a one cycle delay where the 
  register is invalidated in between every ready-valid transaction. Two registers enable one to be
  filled and validated by the fifo while the other is consumed and invalidated by the ready-valid
  interface. */
  val dataReg0 = Reg(Bits(n))
  val validReg0 = Reg(init = Bool(false))
  val dataReg1 = Reg(Bits(n))
  val validReg1 = Reg(init = Bool(false))

  /* Because two valid entries could be stored, a register has to be maintained to determine the
  order in which they should be consumed by the ready valid interface */
  val newestIs0 = Reg(init = Bool(true))

  val lastReadEnable = Reg(init = Bool(false))

  // Issue a read when the fifo is not empty and there is a vacancy in one of the data registers.
  // !!! Above doesn't seem to be true, majority circuit with last read enable ???
  val minority = (!validReg0 && !validReg1) || (!validReg0 && !lastReadEnable) || (!validReg1 && !lastReadEnable)
  val readEnable = !io.in.empty && (minority || io.out.ready)

  io.in.rd_en := readEnable
  lastReadEnable := readEnable

  /* When a read was issued last cycle, store the data and validate it in whichever space is
  invalid (one must exist in order for the read to have been issued last cycle). */
  when (lastReadEnable) {
    // If the first space is valid then the second space must be invalid.
    when (validReg0) {
      dataReg1 := io.in.data
      validReg1 := Bool(true)
      newestIs0 := Bool(false)
    } .otherwise {
      dataReg0 := io.in.data
      validReg0 := Bool(true)
      newestIs0 := Bool(true)
    }
  }

  // When either slot has valid data, the modules output should be valid.
  val valid = validReg0 || validReg1
  io.out.valid := valid

  /* Need to determine which space to present to the output, as well as which to invalidate
  upon the completion of a reay-valid transaction. */
  val presentedIs0 = Bool()

  /* Present space 0 when it is the only valid space or when both are valid but space 0 is 
  the oldest. This means that space 1 will be unconditionally presented when space 0 is invalid,
  but if both spaces are invalid then the the overall module will be invalid anyways. */
  when (validReg0 && (!validReg1 || !newestIs0)) {
    presentedIs0 := Bool(true)
  } .otherwise {
    presentedIs0 := Bool(false)
  }

  // Present the appropriate data register to the ready-valid interface's bit field.
  when(presentedIs0) {
    io.out.bits := dataReg0
  } .otherwise {
    io.out.bits := dataReg1
  }

  // When a ready-valid transaction occurs, invalidate the now vacant space.
  when (valid && io.out.ready) {
    when (presentedIs0) {
      validReg0 := Bool(false)
    } .otherwise {
      validReg1 := Bool(false)
    }
  }

}







class Stencil extends Module {
  val DataWidth = 32

  val io = new Bundle { 
    val in = new InboundFifoIO(DataWidth)
    val out = new OutboundFifoIO(DataWidth)
    val coeff = new InboundFifoIO(DataWidth)
    val done = Bool(OUTPUT)
  }



  
  /*
  val in_fifo_ready = Bool()
  val in_fifo_valid = Reg(init = Bool(false))
  // Dont think it should be reg
  val in_fifo_data = UInt()
  */


  /*
  val in_fetch = (!io.in.empty && (in_fifo_ready || !in_fifo_valid))
  io.in.rd_en := in_fetch
  // Valid is a flip flop so 1 cycle delay between fetching and valid being asserted matches
  // the one cycle delay between rd_en being asserted and valid data being presented.
  in_fifo_valid := in_fetch
  in_fifo_data := io.in.data
  */



  val inFifoModule = Module(new InFifoRV(32))
  inFifoModule.io.in <> io.in
  val in_fifo_ready = inFifoModule.io.out.ready
  val in_fifo_valid = inFifoModule.io.out.valid
  val in_fifo_data = inFifoModule.io.out.bits
  



  io.coeff.rd_en := Bool(false)
  val last_coeff_rd_en = Reg(Bool())
  last_coeff_rd_en := Bool(false)

  when (!io.coeff.empty) {
    io.coeff.rd_en := Bool(true)
    last_coeff_rd_en := Bool(true)
  }



  val coeffSR = Module(new ExposedEnableSR(pow(window, 2).toInt))
  coeffSR.io.in := io.coeff.data
  coeffSR.io.en := last_coeff_rd_en
  val coeffVec = coeffSR.io.outVec






  // Delay grid ready valid interface
  val delay_grid_ready = Bool()
  // Should it be a register???
  val delay_grid_valid = Bool()

  // Actually delay_grid_data is a lot of wires for now. So just keep as i# for now.
  //val delay_grid_data = UInt(32)


  // Number of elements piped into the delay grid, counts 0's used for padding at the end.
  val d_g_data_counter = Reg(init = UInt(0, 32))

  val s_read :: s_read_write :: s_write :: s_done :: Nil = Enum(UInt(), 4)

  val d_g_state = Reg(init = s_read)

  when (d_g_data_counter < UInt(((window / 2) * (gridx + 1)) - 1)) {
    d_g_state := s_read
  } .elsewhen (d_g_data_counter < UInt((gridx * gridy)-1)) {
    d_g_state := s_read_write
  } .elsewhen (d_g_data_counter < UInt((gridx * gridy + (window / 2) * (gridx + 1)) - 1)) {
    d_g_state := s_write
  } .otherwise {
    d_g_state := s_done
  }

  val update_d_g = Bool()
  // Dont think it should be reg.
  val d_g_input = UInt()


  // Set default of signals which may be overwritten

  in_fifo_ready := Bool(false)
  update_d_g := Bool(false)
  d_g_input := UInt(0, 32)
  delay_grid_valid := Bool(false)


  switch(d_g_state) {
    is (s_read) {
      in_fifo_ready := Bool(true)
      when (in_fifo_valid) {
        d_g_input := in_fifo_data
        update_d_g := Bool(true)
      }
    }
    is(s_read_write) {
      when (delay_grid_ready) {
        in_fifo_ready := Bool(true)
        when (in_fifo_valid){
          d_g_input := in_fifo_data
          update_d_g := Bool(true)
          delay_grid_valid := Bool(true)
        }
      }
    }
    is(s_write) {
      when (delay_grid_ready) {
        update_d_g := Bool(true)
        delay_grid_valid := Bool(true)
      }
    }
  }



  val delay_grid = Module(new DelayGrid(window, gridx))

  delay_grid.io.in := d_g_input
  delay_grid.io.en := update_d_g
  val dataVec = delay_grid.io.outVec

  when (update_d_g) {
    d_g_data_counter := d_g_data_counter + UInt(1)
  }









  



  // Calculation tree ready valid interface
  val calc_tree_valid = Bool()
  // Dont think it should be reg
  val calc_tree_data = UInt()
  val calc_tree_ready = Bool()

  // Defaults
  calc_tree_valid := Bool(false)

  val c_t_state = Reg(init = s_read)

  val c_t_enable = Bool()

  delay_grid_ready := Bool(false)

  io.done := Bool(false)

  // Default to stalling the pipeline
  c_t_enable := Bool(false)
  val c_t_data_counter = Reg(init = UInt(0, 32))
  val c_t_advance = Bool()
  c_t_advance := Bool(false)

  // Total latency due to 1 cycle multiplication delay and 4 cycle add delays.
  // latency(window) = 1 + 4(ceil(log_2(window^2)))
  val latency = 1 + 4 * ceil(log(pow(window, 2))/log(2)).toInt

  when (c_t_data_counter < UInt((latency) - 1,32)) {
    c_t_state := s_read
  } .elsewhen (c_t_data_counter < UInt((gridx * gridy) - 1, 32)) {
    c_t_state := s_read_write
  } .elsewhen (c_t_data_counter < UInt((gridx * gridy + latency) - 1, 32)) {
    c_t_state := s_write
  } .otherwise {
    c_t_state := s_done
  }



  switch(c_t_state) {
    is(s_read) {
      delay_grid_ready := Bool(true)
      when (delay_grid_valid) {
        c_t_advance := Bool(true)
      }
    }
    is(s_read_write) {
      when(calc_tree_ready) {
        delay_grid_ready := Bool(true)
        when (delay_grid_valid) {
          calc_tree_valid := Bool(true)
          c_t_advance := Bool(true)
        }
      }
    }
    is(s_write) {
      when(calc_tree_ready) {
        calc_tree_valid := Bool(true)
        c_t_advance := Bool(true)
      }
    }
    is(s_done) {
      io.done := Bool(true)
    }
  }



  when(c_t_advance) {
    c_t_enable := Bool(true)
    c_t_data_counter := c_t_data_counter + UInt(1, 32)
  }


 
  

  val mulResults = FPMult32withCoeff_N(dataVec, VecReverser(coeffVec, pow(window, 2).toInt), c_t_enable, pow(window, 2).toInt)

  calc_tree_data := FPAdd32_N(mulResults, c_t_enable, pow(window, 2).toInt)

  

  



  // Outbound Fifo interface

  // Wothout this delay I get a timing violation during synthesis
  /*
  val c_t_data_pipe = Reg(UInt())
  val c_t_valid_pipe = Reg(Bool())
  c_t_data_pipe := calc_tree_data
  c_t_valid_pipe := calc_tree_valid

  io.out.data := c_t_data_pipe
  io.out.wr_en := c_t_valid_pipe
  */
  io.out.data := calc_tree_data
  io.out.wr_en := calc_tree_valid
  
  calc_tree_ready := !io.out.full
}


class StencilTests(c: Stencil) extends Tester(c) {
  //val stream_in = 42
  //poke(c.io.stream_in, stream_in)
  //step(1)
  //expect(c.io.stream_out, 42)
}

object Stencil {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    chiselMainTest(tutArgs, () => Module(new Stencil())) {
      c => new StencilTests(c) }
  }
}
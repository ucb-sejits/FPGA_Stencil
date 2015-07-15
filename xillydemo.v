`include "Stencil.v"
module xillydemo
  (
  input  clk_100,
  input  otg_oc,   
  inout [55:0] PS_GPIO,
  output [3:0] GPIO_LED,
  output [3:0] vga4_blue,
  output [3:0] vga4_green,
  output [3:0] vga4_red,
  output  vga_hsync,
  output  vga_vsync,

  output  audio_mclk,
  output  audio_dac,
  input   audio_adc,
  input   audio_bclk,
  input   audio_lrclk,

  output smb_sclk,
  inout  smb_sdata,
  output [1:0] smbus_addr

  ); 

  // Clock and quiesce
  wire  bus_clk;
  wire  quiesce;

  // Memory arrays
  reg [7:0] litearray0[0:31];
  reg [7:0] litearray1[0:31];
  reg [7:0] litearray2[0:31];
  reg [7:0] litearray3[0:31];

  // Wires related to /dev/xillybus_coeff_write
  wire  user_w_coeff_write_wren;
  wire  user_w_coeff_write_full;
  wire [31:0] user_w_coeff_write_data;
  wire  user_w_coeff_write_open;

  // Wires related to /dev/xillybus_data_read
  wire  user_r_data_read_rden;
  wire  user_r_data_read_empty;
  wire [31:0] user_r_data_read_data;
  wire  user_r_data_read_eof;
  wire  user_r_data_read_open;

  // Wires related to /dev/xillybus_data_write
  wire  user_w_data_write_wren;
  wire  user_w_data_write_full;
  wire [31:0] user_w_data_write_data;
  wire  user_w_data_write_open;

  // Wires related to Xillybus Lite
  wire  user_clk;
  wire  user_wren;
  wire  user_rden;
  wire [3:0] user_wstrb;
  wire [31:0] user_addr;
  reg [31:0] user_rd_data;
  wire [31:0] user_wr_data;
  wire  user_irq;

  // Wires related to the Stencil module
  wire io_in_rd_en;
  wire [31:0] io_in_data;
  wire io_in_empty;
  wire io_out_wr_en;
  wire [31:0] io_out_data;
  wire io_out_full;
  wire io_coeff_rd_en;
  wire [31:0] io_coeff_data;
  wire io_coeff_empty;

   // Note that none of the ARM processor's direct connections to pads is
   // attached in the instantion below. Normally, they should be connected as
   // toplevel ports here, but that confuses Vivado 2013.4 to think that
   // some of these ports are real I/Os, causing an implementation failure.
   // This detachment results in a lot of warnings during synthesis and
   // implementation, but has no practical significance, as these pads are
   // completely unrelated to the FPGA bitstream.

  xillybus xillybus_ins (

    // Ports related to /dev/xillybus_coeff_write
    // CPU to FPGA signals:
    .user_w_coeff_write_wren(user_w_coeff_write_wren),
    .user_w_coeff_write_full(user_w_coeff_write_full),
    .user_w_coeff_write_data(user_w_coeff_write_data),
    .user_w_coeff_write_open(user_w_coeff_write_open),

    // Ports related to /dev/xillybus_data_read
    // FPGA to CPU signals:
    .user_r_data_read_rden(user_r_data_read_rden),
    .user_r_data_read_empty(user_r_data_read_empty),
    .user_r_data_read_data(user_r_data_read_data),
    .user_r_data_read_eof(user_r_data_read_eof),
    .user_r_data_read_open(user_r_data_read_open),


    // Ports related to /dev/xillybus_data_write
    // CPU to FPGA signals:
    .user_w_data_write_wren(user_w_data_write_wren),
    .user_w_data_write_full(user_w_data_write_full),
    .user_w_data_write_data(user_w_data_write_data),
    .user_w_data_write_open(user_w_data_write_open),


    // Ports related to Xillybus Lite
    .user_clk(user_clk),
    .user_wren(user_wren),
    .user_rden(user_rden),
    .user_wstrb(user_wstrb),
    .user_addr(user_addr),
    .user_rd_data(user_rd_data),
    .user_wr_data(user_wr_data),
    .user_irq(user_irq),


    // General signals
    .PS_CLK(PS_CLK),
    .PS_PORB(PS_PORB),
    .PS_SRSTB(PS_SRSTB),
    .clk_100(clk_100),
    .otg_oc(otg_oc),
    .DDR_Addr(DDR_Addr),
    .DDR_BankAddr(DDR_BankAddr),
    .DDR_CAS_n(DDR_CAS_n),
    .DDR_CKE(DDR_CKE),
    .DDR_CS_n(DDR_CS_n),
    .DDR_Clk(DDR_Clk),
    .DDR_Clk_n(DDR_Clk_n),
    .DDR_DM(DDR_DM),
    .DDR_DQ(DDR_DQ),
    .DDR_DQS(DDR_DQS),
    .DDR_DQS_n(DDR_DQS_n),
    .DDR_DRSTB(DDR_DRSTB),
    .DDR_ODT(DDR_ODT),
    .DDR_RAS_n(DDR_RAS_n),
    .DDR_VRN(DDR_VRN),
    .DDR_VRP(DDR_VRP),
    .MIO(MIO),
    .PS_GPIO(PS_GPIO),
    .DDR_WEB(DDR_WEB),
    .GPIO_LED(GPIO_LED),
    .bus_clk(bus_clk),
    .quiesce(quiesce),
    .vga4_blue(vga4_blue),
    .vga4_green(vga4_green),
    .vga4_red(vga4_red),
    .vga_hsync(vga_hsync),
    .vga_vsync(vga_vsync)
  );

   assign      user_irq = 0; // No interrupts for now
   
   always @(posedge user_clk)
     begin
	if (user_wstrb[0])
	  litearray0[user_addr[6:2]] <= user_wr_data[7:0];

	if (user_wstrb[1])
	  litearray1[user_addr[6:2]] <= user_wr_data[15:8];

	if (user_wstrb[2])
	  litearray2[user_addr[6:2]] <= user_wr_data[23:16];

	if (user_wstrb[3])
	  litearray3[user_addr[6:2]] <= user_wr_data[31:24];
	
	if (user_rden)
	  user_rd_data <= { litearray3[user_addr[6:2]],
			    litearray2[user_addr[6:2]],
			    litearray1[user_addr[6:2]],
			    litearray0[user_addr[6:2]] };
     end




   fifo_32x512 fifo_32_in
     (
      .clk(bus_clk),
      .srst(!user_w_data_write_open),
      .din(user_w_data_write_data),
      .wr_en(user_w_data_write_wren),
      .rd_en(io_in_rd_en),
      .dout(io_in_data),
      .full(user_w_data_write_full),
      .empty(io_in_empty)
      );

   fifo_32x512 fifo_32_out
     (
      .clk(bus_clk),
      .srst(!user_r_data_read_open),
      .din(io_out_data),
      .wr_en(io_out_wr_en),
      .rd_en(user_r_data_read_rden),
      .dout(user_r_data_read_data),
      .full(io_out_full),
      .empty(user_r_data_read_empty)
      );

    fifo_32x512 fifo_32_coeff
     (
      .clk(bus_clk),
      .srst(!user_w_coeff_write_open),
      .din(user_w_coeff_write_data),
      .wr_en(user_w_coeff_write_wren),
      .rd_en(io_coeff_rd_en),
      .dout(io_coeff_data),
      .full(user_w_coeff_write_full),
      .empty(io_coeff_empty)
      );



   Stencil test_stencil
    (
      .clk(bus_clk),
      .io_in_rd_en(io_in_rd_en),
      .io_in_data(io_in_data),
      .io_in_empty(io_in_empty),
      .io_out_wr_en(io_out_wr_en),
      .io_out_data(io_out_data),
      .io_out_full(io_out_full),
      .io_coeff_rd_en(io_coeff_rd_en),
      .io_coeff_data(io_coeff_data),
      .io_coeff_empty(io_coeff_empty)
      );

   assign  user_r_data_read_eof = 0;


   


endmodule
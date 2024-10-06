module ResetGen(
    input clock,
    input reset_in,
    output reset_out
);

reg sync_reg1;
reg sync_reg2;

always @(posedge clock or negedge reset_in) begin
    if(~reset_in) begin
        sync_reg1 <= 1'b1;
        sync_reg2 <= 1'b1;
    end 
    else begin
        sync_reg1 <= 1'b0;
        sync_reg2 <= sync_reg1;
    end

end

assign reset_out = sync_reg2;

endmodule

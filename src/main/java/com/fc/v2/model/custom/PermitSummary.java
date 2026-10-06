package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 签批页首页数回话：页首那个「还没封住的张数」与逐段累上来的「各段在签人数」
 * 出自同一次计算（同一批在签单＋同一批逐笔签署），不许两处各算各的。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PermitSummary", description = "签批页首页数（张数与逐段人数同源一次算）")
public class PermitSummary implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 还没封住的张数（止住的也算没封住）：随立单加一、随封存减一 */
    @ApiModelProperty(value = "还没封住的张数")
    private int openBillCount;

    /** 各段在签人数：只数在签单当前趟里仍算数的笔（逐笔累，不取屏显） */
    @ApiModelProperty(value = "各段在签人数（段一/二/三）")
    private int[] arrivalsByNode = new int[3];

    public int getOpenBillCount() {
        return openBillCount;
    }

    public void setOpenBillCount(int openBillCount) {
        this.openBillCount = openBillCount;
    }

    public int[] getArrivalsByNode() {
        return arrivalsByNode;
    }

    public void setArrivalsByNode(int[] arrivalsByNode) {
        this.arrivalsByNode = arrivalsByNode;
    }
}

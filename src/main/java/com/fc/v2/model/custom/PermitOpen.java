package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 签批单立单报文。
 *
 * 单子只许从点位底册的记录里带出来：报文只给点位码，区册代号由底册带出，
 * 段次一律起在段一，前端没有自己新建的口子、也不接受报文自带段次。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PermitOpen", description = "签批单立单报文（只认点位底册带出）")
public class PermitOpen implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 点位码（底册里点得到才立得起；一处点位一次夜间连续施工只摊上一张在签的单） */
    @ApiModelProperty(value = "点位码（从点位底册带出）")
    private String pointNo;

    /** 备注（可空） */
    @ApiModelProperty(value = "备注")
    private String remark;

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

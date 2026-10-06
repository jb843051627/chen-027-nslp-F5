package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 夜间连续施工作业许可签批单对象 t_nslp_permit_bill
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_nslp_permit_bill")
@ApiModel(value = "TNslpPermitBill", description = "夜间连续施工作业许可签批单")
public class TNslpPermitBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 夜间连续施工作业许可签批单 */
    @TableField("bill_no")
    @ApiModelProperty(value = "夜间连续施工作业许可签批单")
    private String billNo;

    /** 点位码（只从点位底册带出，一处点位一次夜间连续施工只摊上一张） */
    @TableField("point_no")
    @ApiModelProperty(value = "点位码（只从点位底册带出）")
    private String pointNo;

    /** 区册代号（随点位底册带出，以底册为准） */
    @TableField("zone_no")
    @ApiModelProperty(value = "区册代号（随点位底册带出）")
    private String zoneNo;

    /** 当前所在段 0..2（窗口受理/区局核定/市局备案；段次由后端数，报文自带的不采信） */
    @TableField("node_no")
    @ApiModelProperty(value = "当前所在段 0..2（窗口受理/区局核定/市局备案）")
    private Integer nodeNo;

    /** 本段算齐办法 0一名即过 1先落笔那一方为准 2两名一并认可 */
    @TableField("sign_mode")
    @ApiModelProperty(value = "本段算齐办法 0一名即过 1先落笔那一方为准 2两名一并认可")
    private Integer signMode;

    /** 本关应签人数 */
    @TableField("need_count")
    @ApiModelProperty(value = "本关应签人数")
    private Integer needCount;

    /** 本关已到人数 */
    @TableField("sign_count")
    @ApiModelProperty(value = "本关已到人数")
    private Integer signCount;

    /** 签批情形 0在签（含压回重走） 1已点齐封住 */
    @TableField("status")
    @ApiModelProperty(value = "签批情形 0在签 1已点齐封住")
    private Integer status;

    /** 走到第几趟（压回本段重走为新一趟，更早各段旧笔一笔不动） */
    @TableField("round_no")
    @ApiModelProperty(value = "走到第几趟（压回重走的段本轮加一）")
    private Integer roundNo;

    /** 止住 0在走 1已止住（有一方落不可即止住，冻结不接笔） */
    @TableField("stop_flag")
    @ApiModelProperty(value = "止住 0在走 1已止住")
    private Integer stopFlag;

    /** 点齐封住那一刻（走满三段写回本行） */
    @TableField("seal_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "点齐封住那一刻")
    private Date sealTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSignMode() {
        return signMode;
    }

    public void setSignMode(Integer signMode) {
        this.signMode = signMode;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(Integer stopFlag) {
        this.stopFlag = stopFlag;
    }

    public Date getSealTime() {
        return sealTime;
    }

    public void setSealTime(Date sealTime) {
        this.sealTime = sealTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

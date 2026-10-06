package com.fc.v2.model.auto;

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
 * 点位夜间施工档变更履历对象 t_nslp_point_permit_hist
 *
 * 签批单走满三段点齐那一刻，点位主档夜间施工档由未准改为准许；本履历与主档改动
 * 出自同一回处理（同事务、同一次计算）。对不住时凭 bill_id/bill_no 指名报出对应单据。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_nslp_point_permit_hist")
@ApiModel(value = "TNslpPointPermitHist", description = "点位夜间施工档变更履历")
public class TNslpPointPermitHist implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所随点位 */
    @TableField("point_id")
    @ApiModelProperty(value = "所随点位 t_nslp_point.id")
    private Long pointId;

    /** 点位码（冗余自点位行） */
    @TableField("point_no")
    @ApiModelProperty(value = "点位码")
    private String pointNo;

    /** 依据哪一张签批单 */
    @TableField("bill_id")
    @ApiModelProperty(value = "依据哪一张签批单")
    private Long billId;

    /** 依据的签批单单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "依据的签批单单号")
    private String billNo;

    /** 这一改由谁落（封住那一笔的落笔人） */
    @TableField("operator")
    @ApiModelProperty(value = "这一改由谁落")
    private String operator;

    /** 从哪一档 0夜间施工未准 */
    @TableField("from_flag")
    @ApiModelProperty(value = "从哪一档")
    private Integer fromFlag;

    /** 改到哪一档 1夜间连续施工已准 */
    @TableField("to_flag")
    @ApiModelProperty(value = "改到哪一档")
    private Integer toFlag;

    /** 改口那一刻（与主档更新、单据封住同一刻） */
    @TableField("change_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "改口那一刻")
    private Date changeTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField("create_by")
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPointId() {
        return pointId;
    }

    public void setPointId(Long pointId) {
        this.pointId = pointId;
    }

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getFromFlag() {
        return fromFlag;
    }

    public void setFromFlag(Integer fromFlag) {
        this.fromFlag = fromFlag;
    }

    public Integer getToFlag() {
        return toFlag;
    }

    public void setToFlag(Integer toFlag) {
        this.toFlag = toFlag;
    }

    public Date getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(Date changeTime) {
        this.changeTime = changeTime;
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
}

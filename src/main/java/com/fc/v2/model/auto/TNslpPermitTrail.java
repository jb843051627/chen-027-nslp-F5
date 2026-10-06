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
 * 签批单流转留痕对象 t_nslp_permit_trail
 *
 * 立单、落笔、挪前、压回、止住、分歧挂起、点齐各留一笔，按时序压在单身后。
 * 主档段次改口与留痕出自同一回处理；当前段只由这一串放出来，捋账只取这一串。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_nslp_permit_trail")
@ApiModel(value = "TNslpPermitTrail", description = "签批单流转留痕")
public class TNslpPermitTrail implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所随签批单 */
    @TableField("bill_id")
    @ApiModelProperty(value = "所随签批单")
    private Long billId;

    /** 落笔那一刻单子所处段 0..2 */
    @TableField("node_no")
    @ApiModelProperty(value = "落笔那一刻所处段 0..2")
    private Integer nodeNo;

    /** 落笔那一刻走到的趟次 */
    @TableField("round_no")
    @ApiModelProperty(value = "落笔那一刻走到的趟次")
    private Integer roundNo;

    /** 动作 立单/落笔/挪前/压回/止住/分歧挂起/点齐 */
    @TableField("action")
    @ApiModelProperty(value = "动作")
    private String action;

    /** 这一笔留下的话（原话或动作按语） */
    @TableField("note")
    @ApiModelProperty(value = "这一笔留下的话")
    private String note;

    /** 提交那一刻（同刻两笔各记各的先后） */
    @TableField("trail_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "提交那一刻")
    private Date trailTime;

    /** 同刻先后次序（trail_time 并列时以本列断先后） */
    @TableField("seq")
    @ApiModelProperty(value = "同刻先后次序")
    private Integer seq;

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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getTrailTime() {
        return trailTime;
    }

    public void setTrailTime(Date trailTime) {
        this.trailTime = trailTime;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
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

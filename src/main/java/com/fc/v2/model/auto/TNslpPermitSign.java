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
 * 签批单逐笔签署对象 t_nslp_permit_sign
 *
 * 一人一笔，各记各的落单时刻；「已到几人」只由这一串逐笔累出来，
 * 屏上只是回显，任何角色手里都没有改它的位置。压回只把本段本轮的笔腾掉
 *（active_flag 置 0），更早各段与旧趟的旧笔一笔不动，照旧一条一条查得着。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_nslp_permit_sign")
@ApiModel(value = "TNslpPermitSign", description = "签批单逐笔签署")
public class TNslpPermitSign implements Serializable {
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

    /** 落笔那一刻服务端数出的段 0..2（报文写的段不采信） */
    @TableField("node_no")
    @ApiModelProperty(value = "落笔那一刻所处段 0..2")
    private Integer nodeNo;

    /** 这一笔属第几趟 */
    @TableField("round_no")
    @ApiModelProperty(value = "这一笔属第几趟")
    private Integer roundNo;

    /** 落笔人（两名各落各的，同一个抵不得两笔） */
    @TableField("signer")
    @ApiModelProperty(value = "落笔人")
    private String signer;

    /** 意见原话 可／不可 */
    @TableField("verdict")
    @ApiModelProperty(value = "意见原话 可／不可")
    private String verdict;

    /** 这一笔的原话（两方文本都进库，屏上两行都摆得出来） */
    @TableField("note")
    @ApiModelProperty(value = "这一笔的原话")
    private String note;

    /** 落单那一刻（同刻两笔各记各的，按本列排先后不许并成一笔） */
    @TableField("sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落单那一刻")
    private Date signTime;

    /** 同刻先后次序（sign_time 并列时以本列断先后） */
    @TableField("seq")
    @ApiModelProperty(value = "同刻先后次序")
    private Integer seq;

    /** 本轮有效 1本轮算数 0已被压回腾掉（行仍在，旧笔查得着，不计已到人数） */
    @TableField("active_flag")
    @ApiModelProperty(value = "本轮有效 1算数 0已被压回腾掉")
    private Integer activeFlag;

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

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(Integer activeFlag) {
        this.activeFlag = activeFlag;
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

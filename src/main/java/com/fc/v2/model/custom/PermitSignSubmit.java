package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 签批落笔报文（一次落笔只有这一个入口；另起单、改旧笔、追补签署三类旁路一概不开）。
 *
 * 报文写了第几段只作参考：落库采信与否由服务端按当下段点，提前递、跳段递的笔一律丢掉。
 * 已到几人由服务端逐笔累加，报文不带人数、屏上也没有改人数的位置。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PermitSignSubmit", description = "签批落笔报文（唯一落笔入口）")
public class PermitSignSubmit implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 签批单主键 */
    @ApiModelProperty(value = "签批单主键")
    private Long id;

    /** 报文自称递到的段：只作参考，与服务端当下段不符这笔不采信 */
    @ApiModelProperty(value = "报文自称的段次（不采信，只用来对一眼）")
    private Integer reportNode;

    /** 落笔人（空着由服务端按当前登录岗补） */
    @ApiModelProperty(value = "落笔人")
    private String signer;

    /** 意见原话：只认「可」「不可」，其余写法按没递上来 */
    @ApiModelProperty(value = "意见 可／不可")
    private String verdict;

    /** 这一笔的原话（随意见一起进库留底，屏上摆得出来） */
    @ApiModelProperty(value = "这一笔的原话")
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getReportNode() {
        return reportNode;
    }

    public void setReportNode(Integer reportNode) {
        this.reportNode = reportNode;
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
}

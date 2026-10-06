package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TNslpPermitSign;
import com.fc.v2.model.auto.TNslpPermitTrail;
import com.fc.v2.service.support.PermitBillRules;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 签批追溯回话：一张单身后按时序压着的那一串，加上逐段逐趟的落笔账。
 *
 * 当前段不听行面缓存、不听报文序号，只由留痕这一串当场放一遍；每段「已到几人」
 * 不取屏显，只取逐笔签署按（段、趟）累出来的那一回。压回腾掉的本轮旧笔标成失效，
 * 行仍在、原话仍在，旧趟与本趟分两处摆。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PermitTrace", description = "签批追溯（留痕放段＋逐笔累人）")
public class PermitTrace implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 签批单主键 */
    @ApiModelProperty(value = "签批单主键")
    private Long billId;

    /** 签批单单号 */
    @ApiModelProperty(value = "签批单单号")
    private String billNo;

    /** 从留痕放出来的当前段（不另算一套去凑行面） */
    @ApiModelProperty(value = "从留痕放出来的当前段")
    private int replayNode;

    /** 段名回话 */
    @ApiModelProperty(value = "段名回话")
    private String replayNodeText;

    /** 从留痕放出来是否已点齐封住 */
    @ApiModelProperty(value = "是否已点齐封住")
    private boolean replaySealed;

    /** 从留痕放出来是否已止住 */
    @ApiModelProperty(value = "是否已止住")
    private boolean replayStopped;

    /** 按时序排好的一笔一笔 */
    @ApiModelProperty(value = "按时序的流转留痕明细")
    private List<Entry> entries = new ArrayList<Entry>();

    /** 逐段的落笔账（含旧趟；屏上两行都摆得出来，本轮与上轮分两处） */
    @ApiModelProperty(value = "逐段落笔账")
    private List<NodeBook> nodes = new ArrayList<NodeBook>();

    /** 唯一装配口：当前段由留痕放，已到人数由逐笔签押（段、趟）累 */
    public static PermitTrace of(Long billId, String billNo, List<TNslpPermitTrail> trails,
                                 List<TNslpPermitSign> signs) {
        PermitTrace t = new PermitTrace();
        t.billId = billId;
        t.billNo = billNo;
        List<PermitBillRules.Event> events = new ArrayList<PermitBillRules.Event>();
        if (trails != null) {
            for (TNslpPermitTrail tr : trails) {
                Entry e = new Entry();
                int n = tr.getNodeNo() == null ? 0 : tr.getNodeNo().intValue();
                e.node = n;
                e.nodeText = PermitBillRules.nodeName(Integer.valueOf(n));
                e.roundNo = tr.getRoundNo();
                e.action = tr.getAction();
                e.note = tr.getNote();
                e.trailTime = tr.getTrailTime();
                e.seq = tr.getSeq() == null ? 0 : tr.getSeq().intValue();
                t.entries.add(e);
                events.add(new PermitBillRules.Event(tr.getAction(), n));
            }
        }
        PermitBillRules.ReplayResult replay = PermitBillRules.replay(events);
        t.replayNode = replay.getNode();
        t.replayNodeText = PermitBillRules.nodeName(Integer.valueOf(t.replayNode));
        t.replaySealed = replay.isSealed();
        t.replayStopped = replay.isStopped();

            // 「已到几人」逐笔累的累法只认口径那一处：最新一趟里仍算数（active=1）的笔
            for (int node = 0; node < PermitBillRules.NODE_COUNT; node++) {
                NodeBook nb = new NodeBook();
                nb.node = node;
                nb.nodeText = PermitBillRules.nodeName(Integer.valueOf(node));
                nb.mode = PermitBillRules.signMode(node);
                nb.need = PermitBillRules.needCount(node);
                List<PermitBillRules.RoundSign> roundSigns =
                        new ArrayList<PermitBillRules.RoundSign>();
                for (TNslpPermitSign s : signs) {
                    int sn = s.getNodeNo() == null ? 0 : s.getNodeNo().intValue();
                    if (sn != node) {
                        continue;
                    }
                    SignRow row = new SignRow();
                    row.signer = s.getSigner();
                    row.verdict = s.getVerdict();
                    row.note = s.getNote();
                    row.roundNo = s.getRoundNo();
                    row.signTime = s.getSignTime();
                    row.seq = s.getSeq() == null ? 0 : s.getSeq().intValue();
                    row.active = s.getActiveFlag() == null || s.getActiveFlag().intValue() == 1;
                    nb.rows.add(row);
                    roundSigns.add(new PermitBillRules.RoundSign(
                            s.getRoundNo() == null ? 1 : s.getRoundNo().intValue(), row.active));
                }
                nb.latestRound = PermitBillRules.latestActiveRound(roundSigns);
                nb.arrivals = PermitBillRules.activeArrivals(roundSigns, nb.latestRound);
                // 没到过的段没有算数趟：别把旧趟/0 趟的笔算到没发生的本趟头上
                if (roundSigns.isEmpty()) {
                    nb.arrivals = 0;
                }
                t.nodes.add(nb);
            }
        return t;
    }

    /** 流转留痕里的一笔 */
    @ApiModel(value = "PermitTraceEntry", description = "流转留痕一笔")
    public static class Entry implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private int node;
        private String nodeText;
        private Integer roundNo;
        private String action;
        private String note;
        private java.util.Date trailTime;
        private int seq;

        public int getNode() {
            return node;
        }

        public String getNodeText() {
            return nodeText;
        }

        public Integer getRoundNo() {
            return roundNo;
        }

        public String getAction() {
            return action;
        }

        public String getNote() {
            return note;
        }

        public java.util.Date getTrailTime() {
            return trailTime;
        }

        public int getSeq() {
            return seq;
        }
    }

    /** 一段的落笔账：应签、数法、最新趟、该趟已到几人、各趟原话两行 */
    @ApiModel(value = "PermitNodeBook", description = "一段落笔账")
    public static class NodeBook implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private int node;
        private String nodeText;
        private int mode;
        private int need;
        private int latestRound;
        private int arrivals;
        private List<SignRow> rows = new ArrayList<SignRow>();

        public int getNode() {
            return node;
        }

        public String getNodeText() {
            return nodeText;
        }

        public int getMode() {
            return mode;
        }

        public int getNeed() {
            return need;
        }

        public int getLatestRound() {
            return latestRound;
        }

        public int getArrivals() {
            return arrivals;
        }

        public List<SignRow> getRows() {
            return rows;
        }
    }

    /** 一笔签署：谁、哪一句、可不可、第几趟、还算不算数 */
    @ApiModel(value = "PermitSignRow", description = "逐笔签署一行")
    public static class SignRow implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private String signer;
        private String verdict;
        private String note;
        private Integer roundNo;
        private java.util.Date signTime;
        private int seq;
        private boolean active;

        public String getSigner() {
            return signer;
        }

        public String getVerdict() {
            return verdict;
        }

        public String getNote() {
            return note;
        }

        public Integer getRoundNo() {
            return roundNo;
        }

        public java.util.Date getSignTime() {
            return signTime;
        }

        public int getSeq() {
            return seq;
        }

        public boolean isActive() {
            return active;
        }
    }

    public Long getBillId() {
        return billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public int getReplayNode() {
        return replayNode;
    }

    public String getReplayNodeText() {
        return replayNodeText;
    }

    public boolean isReplaySealed() {
        return replaySealed;
    }

    public boolean isReplayStopped() {
        return replayStopped;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<NodeBook> getNodes() {
        return nodes;
    }
}

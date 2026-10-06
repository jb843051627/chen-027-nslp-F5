package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TNslpPermitSign;
import com.fc.v2.model.auto.TNslpPermitTrail;
import com.fc.v2.service.support.PermitBillRules;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 受理簿对账回话：把一张单从末段倒着数回头一段，段数与当日受理簿上的笔数
 * 一笔一笔合得上；只抽其中任意一段单独核，也自洽。
 *
 * 受理簿记的是「笔」：挪前、压回各一笔，同刻两人各送一笔也各记各的。
 * 数全部回到留痕里数，不许另起一套；逐段的人数回到逐笔签署里数。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PermitLedger", description = "签批受理簿对账（段数对得上笔数，逐段自洽）")
public class PermitLedger implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 签批单主键 */
    @ApiModelProperty(value = "签批单主键")
    private Long billId;

    /** 对账那一一日的线（只数这一天落笔的）；空着数全串 */
    @ApiModelProperty(value = "对账那一一日")
    private Date day;

    /** 当日留痕总笔数 */
    @ApiModelProperty(value = "当日留痕总笔数")
    private int totalCount;

    /** 当日挪前笔数 */
    @ApiModelProperty(value = "当日挪前笔数")
    private int forwardCount;

    /** 当日压回笔数 */
    @ApiModelProperty(value = "当日压回笔数")
    private int backCount;

    /** 从整串放出来的当前段 */
    @ApiModelProperty(value = "从整串放出来的当前段")
    private int replayNode;

    /** 段名回话 */
    @ApiModelProperty(value = "段名回话")
    private String replayNodeText;

    /** 是否点齐封住 */
    @ApiModelProperty(value = "是否点齐封住")
    private boolean replaySealed;

    /** 是否止住 */
    @ApiModelProperty(value = "是否止住")
    private boolean replayStopped;

    /** 逐段进出轧平（每段进来的笔＝出去的笔，唯当前停着的那段多压一笔） */
    @ApiModelProperty(value = "逐段进出轧平与否")
    private boolean stageBalanced;

    /** 轧不平的段（点名） */
    @ApiModelProperty(value = "轧不平的段")
    private List<Integer> unbalancedNodes = new ArrayList<Integer>();

    /** 逐段自洽核 */
    @ApiModelProperty(value = "逐段自洽核")
    private List<NodeLedger> nodes = new ArrayList<NodeLedger>();

    /**
     * 唯一装配口：进出笔数全回到留痕里数，已到几人全回到逐笔签署里（段、趟）数；
     * 屏上那一个数、行面缓存一概不喂。day 不空只数这一天落笔的，当前段始终由整串放。
     */
    public static PermitLedger of(Long billId, Date day, List<TNslpPermitTrail> allTrails,
                                  List<TNslpPermitSign> allSigns) {
        PermitLedger l = new PermitLedger();
        l.billId = billId;
        l.day = day;

        int[] enters = new int[PermitBillRules.NODE_COUNT];
        int[] exits = new int[PermitBillRules.NODE_COUNT];
        List<PermitBillRules.Event> dayEvents = new ArrayList<PermitBillRules.Event>();
        List<PermitBillRules.Event> allEvents = new ArrayList<PermitBillRules.Event>();
        if (allTrails != null) {
            for (TNslpPermitTrail tr : allTrails) {
                int n = tr.getNodeNo() == null ? 0 : tr.getNodeNo().intValue();
                String a = tr.getAction();
                allEvents.add(new PermitBillRules.Event(a, n));
                if (day != null && !sameDay(tr.getTrailTime(), day)) {
                    continue;
                }
                dayEvents.add(new PermitBillRules.Event(a, n));
                if (PermitBillRules.ACT_OPEN.equals(a)) {
                    if (inBounds(n)) {
                        enters[n]++;
                    }
                } else if (PermitBillRules.ACT_FORWARD.equals(a)) {
                    // 落笔那一刻单已在新段：挪前记目的段
                    if (inBounds(n)) {
                        enters[n]++;
                    }
                    if (n - 1 >= 0) {
                        exits[n - 1]++;
                    }
                } else if (PermitBillRules.ACT_BACK.equals(a)) {
                    // 压回同样记目的段：从后一段离开、落回前一段
                    if (inBounds(n)) {
                        enters[n]++;
                    }
                    if (n + 1 < PermitBillRules.NODE_COUNT) {
                        exits[n + 1]++;
                    }
                }
            }
        }

        PermitBillRules.ReplayResult dayReplay = PermitBillRules.replay(dayEvents);
        PermitBillRules.ReplayResult fullReplay = PermitBillRules.replay(allEvents);
        l.totalCount = dayEvents.size();
        l.forwardCount = dayReplay.getForwardCount();
        l.backCount = dayReplay.getBackCount();
        l.replayNode = fullReplay.getNode();
        l.replayNodeText = PermitBillRules.nodeName(Integer.valueOf(l.replayNode));
        l.replaySealed = fullReplay.isSealed();
        l.replayStopped = fullReplay.isStopped();

        boolean balanced = true;
        for (int n = 0; n < PermitBillRules.NODE_COUNT; n++) {
            // 当前停着的那段多压一笔（进来还没出去）；封住落在段三、止住停在落定段，同理
            int expect = n == l.replayNode ? 1 : 0;
            boolean nb = enters[n] - exits[n] == expect;
            if (!nb) {
                balanced = false;
                l.unbalancedNodes.add(Integer.valueOf(n));
            }
            l.nodes.add(NodeLedger.of(n, enters[n], exits[n], nb, signsOf(allSigns, n)));
        }
        l.stageBalanced = balanced;
        return l;
    }

    /** 一段的自洽核：进出轧平＋本轮已到几人不超应签＋人数仍由逐笔累 */
    @ApiModel(value = "PermitNodeLedger", description = "受理簿逐段核")
    public static class NodeLedger implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private int node;
        private String nodeText;
        private int mode;
        private int need;
        private int enterCount;
        private int exitCount;
        private boolean flowBalanced;
        private int latestRound;
        private int activeArrivals;
        private int signTotal;
        private boolean arrivalsWithinNeed;
        private boolean selfConsistent;
        private List<RoundCount> rounds = new ArrayList<RoundCount>();

        static NodeLedger of(int node, int enters, int exits, boolean flowBalanced,
                             List<TNslpPermitSign> nodeSigns) {
            NodeLedger nb = new NodeLedger();
            nb.node = node;
            nb.nodeText = PermitBillRules.nodeName(Integer.valueOf(node));
            nb.mode = PermitBillRules.signMode(node);
            nb.need = PermitBillRules.needCount(node);
            nb.enterCount = enters;
            nb.exitCount = exits;
            nb.flowBalanced = flowBalanced;

            // 已到几人、分趟账的累法只认口径那一处，这里不另写第二遍
            List<PermitBillRules.RoundSign> roundSigns =
                    new java.util.ArrayList<PermitBillRules.RoundSign>();
            int total = 0;
            if (nodeSigns != null) {
                for (TNslpPermitSign s : nodeSigns) {
                    total++;
                    roundSigns.add(new PermitBillRules.RoundSign(
                            s.getRoundNo() == null ? 1 : s.getRoundNo().intValue(),
                            s.getActiveFlag() == null || s.getActiveFlag().intValue() == 1));
                }
            }
            for (PermitBillRules.RoundSign rs : roundSigns) {
                boolean seen = false;
                for (RoundCount rc : nb.rounds) {
                    if (rc.roundNo == rs.getRound()) {
                        seen = true;
                        if (rs.isActive()) {
                            rc.arrivals++;
                        }
                        break;
                    }
                }
                if (!seen) {
                    RoundCount rc = new RoundCount();
                    rc.roundNo = rs.getRound();
                    rc.arrivals = rs.isActive() ? 1 : 0;
                    nb.rounds.add(rc);
                }
            }
            nb.latestRound = PermitBillRules.latestActiveRound(roundSigns);
            nb.activeArrivals = roundSigns.isEmpty() ? 0
                    : PermitBillRules.activeArrivals(roundSigns, nb.latestRound);
            nb.signTotal = total;
            // 逐笔累上来的本轮人数不许越过应签（越了就是有旁路在塞笔）
            nb.arrivalsWithinNeed = nb.activeArrivals <= nb.need;
            // 抽这一段单核：进出轧平、人数不超应签；两样本源不同，合起来才算自洽
            nb.selfConsistent = flowBalanced && nb.arrivalsWithinNeed;
            return nb;
        }

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

        public int getEnterCount() {
            return enterCount;
        }

        public int getExitCount() {
            return exitCount;
        }

        public boolean isFlowBalanced() {
            return flowBalanced;
        }

        public int getLatestRound() {
            return latestRound;
        }

        public int getActiveArrivals() {
            return activeArrivals;
        }

        public int getSignTotal() {
            return signTotal;
        }

        public boolean isArrivalsWithinNeed() {
            return arrivalsWithinNeed;
        }

        public boolean isSelfConsistent() {
            return selfConsistent;
        }

        public List<RoundCount> getRounds() {
            return rounds;
        }
    }

    /** 一段一趟的已到人数（压回重走为新一趟，旧趟照样列得出） */
    @ApiModel(value = "PermitRoundCount", description = "一段一趟已到人数")
    public static class RoundCount implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private int roundNo;
        private int arrivals;

        public int getRoundNo() {
            return roundNo;
        }

        public int getArrivals() {
            return arrivals;
        }
    }

    private static List<TNslpPermitSign> signsOf(List<TNslpPermitSign> all, int node) {
        List<TNslpPermitSign> out = new ArrayList<TNslpPermitSign>();
        if (all != null) {
            for (TNslpPermitSign s : all) {
                int sn = s.getNodeNo() == null ? 0 : s.getNodeNo().intValue();
                if (sn == node) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static boolean inBounds(int node) {
        return node >= 0 && node < PermitBillRules.NODE_COUNT;
    }

    private static boolean sameDay(Date a, Date b) {
        if (a == null) {
            return false;
        }
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    public Long getBillId() {
        return billId;
    }

    public Date getDay() {
        return day;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getForwardCount() {
        return forwardCount;
    }

    public int getBackCount() {
        return backCount;
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

    public boolean isStageBalanced() {
        return stageBalanced;
    }

    public List<Integer> getUnbalancedNodes() {
        return unbalancedNodes;
    }

    public List<NodeLedger> getNodes() {
        return nodes;
    }
}

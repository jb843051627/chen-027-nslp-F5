package com.fc.v2.service.support;

import java.util.List;

/**
 * 夜间连续施工作业许可签批单纯口径（不碰库、不沾 Spring）。
 *
 * 三道人手、三种数法钉死在这一处，谁也不套谁：
 * <ol>
 *   <li>段一·区里收件席，应签 1 人，{@link #MODE_SOLO}：那一人的意见一落单，这一段就算齐。</li>
 *   <li>段二·区局认定席，应签 2 人，{@link #MODE_FIRST_WINS}：按「先落笔那一方为准」判——
 *       头一个落下的说法就是这一段的结论；第二个仍然要交，人数照常累，两方原话都进库、
 *       屏上两行都摆得出来。</li>
 *   <li>段三·市局存备席，应签 2 人，{@link #MODE_ALL_AGREE}：两条一并认可才算齐；
 *       一条认可一条不认可，整单停在这一段等，既不往前推、也不就此止住（等压回重走）。</li>
 * </ol>
 * 段次只许一级一级走；整单当前在哪一段、每段应签几人、按什么数法算齐，一律只问这一份。
 * 报文里写的第几段、屏上回显的已签人数，都不作数。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class PermitBillRules {

    /** 段一：区里收件席（立单落在此段） */
    public static final int NODE_RECEIVE = 0;
    /** 段二：区局认定席 */
    public static final int NODE_VERIFY = 1;
    /** 段三：市局存备席（走满此段即点齐封住） */
    public static final int NODE_RECORD = 2;
    /** 段只有三道，先后钉死，不许添、不许改名、不许跳 */
    public static final int NODE_COUNT = 3;

    /** 算齐办法：一名即过（段一） */
    public static final int MODE_SOLO = 0;
    /** 算齐办法：先落笔那一方为准（段二） */
    public static final int MODE_FIRST_WINS = 1;
    /** 算齐办法：两名一并认可才算齐（段三） */
    public static final int MODE_ALL_AGREE = 2;

    /** 签批情形：在签（压回重走的也仍在签） */
    public static final int STATUS_SIGNING = 0;
    /** 签批情形：已点齐封住（封住之后再递进来的人一律不接） */
    public static final int STATUS_SEALED = 1;

    /** 意见原话：认可 */
    public static final String VERDICT_OK = "可";
    /** 意见原话：不可（段一落此即止住；段二收齐后按首笔论；段三作分歧挂起处理） */
    public static final String VERDICT_NO = "不可";

    /** 留痕动作：立单（从点位底册带出来） */
    public static final String ACT_OPEN = "立单";
    /** 留痕动作：落笔（一人一笔，各记各的时刻） */
    public static final String ACT_SIGN = "落笔";
    /** 留痕动作：往前挪紧邻的下一段 */
    public static final String ACT_FORWARD = "挪前";
    /** 留痕动作：压回一关（只清这一段本轮的笔，更早各段旧笔一笔不动） */
    public static final String ACT_BACK = "压回";
    /** 留痕动作：段二两笔收齐、首笔为不可，整张止住 */
    public static final String ACT_STOP = "止住";
    /** 留痕动作：段三一条认可一条不认可，停在此段等压回（不推进、不止住） */
    public static final String ACT_HOLD = "分歧挂起";
    /** 留痕动作：走满三段点齐，整张封住 */
    public static final String ACT_SEAL = "点齐";

    /** 三道的名字，段数与名字都只此一处出处 */
    private static final String[] NODE_NAMES = {"区里收件席", "区局认定席", "市局存备席"};

    private PermitBillRules() {
    }

    /** 段名回话：0..2 出本名；其余只有「段次不明」四个字，不许替它预设 */
    public static String nodeName(Integer node) {
        if (node != null && node.intValue() >= 0 && node.intValue() < NODE_COUNT) {
            return NODE_NAMES[node.intValue()];
        }
        return "段次不明";
    }

    /** 这一段应签几人：段一 1，段二、段三各 2，只此一处出处 */
    public static int needCount(int node) {
        return node == NODE_RECEIVE ? 1 : 2;
    }

    /** 这一段按什么算法算齐，只此一处出处；段次不明不给数法 */
    public static int signMode(int node) {
        if (node == NODE_RECEIVE) {
            return MODE_SOLO;
        }
        if (node == NODE_VERIFY) {
            return MODE_FIRST_WINS;
        }
        if (node == NODE_RECORD) {
            return MODE_ALL_AGREE;
        }
        throw new IllegalArgumentException("段次不明，点不出算齐办法：" + node);
    }

    /** 意见只认「可」「不可」这两句原话；空着或写成别的，按没递上来，不落库 */
    public static String normalizeVerdict(String verdict) {
        if (verdict == null) {
            return null;
        }
        String v = verdict.trim();
        return VERDICT_OK.equals(v) || VERDICT_NO.equals(v) ? v : null;
    }

    // ------------------------------------------------------------------
    // 算齐：三段的算法各是各的，各数各的笔，谁也不套用谁
    // ------------------------------------------------------------------

    /**
     * 收齐这一段后怎么落，按这一段自己的数法判：
     * <ul>
     *   <li>{@link Outcome#WAIT} 人还没到齐，整单留在这一段；</li>
     *   <li>{@link Outcome#ADVANCE} 这一段齐了，往前挪（末段则由调用方点齐封住）；</li>
     *   <li>{@link Outcome#STOP} 有「不可」落定，整张当即止住；</li>
     *   <li>{@link Outcome#HOLD} 仅段三：一条可一条不可，停在此段等压回，不推、不止。</li>
     * </ul>
     *
     * @param node        当前段（0..2）
     * @param arrivals    本轮已落笔数（只由逐笔累加数出来，不许取屏显）
     * @param okSeq       本轮各笔按落单先后排好的原话（true=可 false=不可）
     */
    public static Outcome decide(int node, int arrivals, List<Boolean> okSeq) {
        int need = needCount(node);
        if (arrivals < need) {
            return Outcome.WAIT;
        }
        int mode = signMode(node);
        if (mode == MODE_SOLO) {
            // 段一：那一人的意见一落单就算齐；落「不可」即止住
            return first(okSeq) ? Outcome.ADVANCE : Outcome.STOP;
        }
        if (mode == MODE_FIRST_WINS) {
            // 段二：头一个落下的说法就是这一段的结论；第二笔照常收，数照常累，不影响结论
            return first(okSeq) ? Outcome.ADVANCE : Outcome.STOP;
        }
        // 段三：两条一并认可才算齐；一可一不可（或两条皆不可）挂在这一段等，不推也不止
        boolean allOk = true;
        for (int i = 0; i < need; i++) {
            if (i >= okSeq.size() || !Boolean.TRUE.equals(okSeq.get(i))) {
                allOk = false;
                break;
            }
        }
        return allOk ? Outcome.ADVANCE : Outcome.HOLD;
    }

    /** 收齐后的去向 */
    public enum Outcome {
        /** 人没到齐，留在本段 */
        WAIT,
        /** 本段算齐，往前挪（末段由调用方改判点齐封住） */
        ADVANCE,
        /** 有「不可」落定，整张止住 */
        STOP,
        /** 段三分歧：停在此段等压回，不推进、不止住 */
        HOLD
    }

    // ------------------------------------------------------------------
    // 段次：只许一级一级往上走；压回的粒度是「一段」，退到哪一段口径定死
    // ------------------------------------------------------------------

    /** 已在头一段，没什么可再压回 */
    public static boolean canRollback(int node) {
        return node > NODE_RECEIVE && node < NODE_COUNT;
    }

    /**
     * 压回只退一关，目标段由这一处钉死，不由递报文的人点：
     * 段二压回段一、段三压回段二。头一段压无可压。
     */
    public static int rollbackTarget(int node) {
        if (!canRollback(node)) {
            throw new IllegalStateException("已经站在头一段，没什么可再压回：" + node);
        }
        return node - 1;
    }

    /** 往前只许紧邻的下一段；站在末段的「往前」就是点齐封住，不再有更下一段 */
    public static boolean canForward(int node) {
        return node >= NODE_RECEIVE && node < NODE_RECORD;
    }

    /** 报文递到的段次与服务端数出的当前段对不上：不采信（提前递、跳段递的笔一律丢掉） */
    public static boolean acceptsAt(int currentNode, Integer reportedNode) {
        return reportedNode != null && reportedNode.intValue() == currentNode;
    }

    // ------------------------------------------------------------------
    // 止住 / 封住：止住之后任何一段再落的笔都不采信；封住之后人一律不接
    // ------------------------------------------------------------------

    /** 已止住（有「不可」落定）：只许看，不许动 */
    public static boolean isStopped(Integer stopFlag) {
        return stopFlag != null && stopFlag.intValue() == 1;
    }

    /** 已点齐封住：只许看，不许动 */
    public static boolean isSealed(Integer status) {
        return status != null && status.intValue() == STATUS_SEALED;
    }

    /** 止住或封住：整单冻结，再递进来的笔一概不采信 */
    public static boolean isFrozen(Integer status, Integer stopFlag) {
        return isSealed(status) || isStopped(stopFlag);
    }

    // ------------------------------------------------------------------
    // 捋：照受理簿那串从头放一遍，当前段只由这一串放出来
    // ------------------------------------------------------------------

    /**
     * 把留痕按时序一笔一笔放回去：立单起在段一，挪前只认紧邻的一下，
     * 压回只退一段，止住／点齐到此冻结。落笔与分歧挂起不动段。
     * 段次全由递进来的这一串动作放出来，不拿行面缓存、也不拿报文序号。
     */
    public static ReplayResult replay(List<Event> events) {
        int node = NODE_RECEIVE;
        boolean frozen = false;
        boolean sealed = false;
        boolean stopped = false;
        int forwardCount = 0;
        int backCount = 0;
        if (events != null) {
            for (Event e : events) {
                if (e == null || e.action == null) {
                    continue;
                }
                String a = e.action;
                if (ACT_OPEN.equals(a)) {
                    node = NODE_RECEIVE;
                } else if (ACT_FORWARD.equals(a)) {
                    if (!frozen && canForward(node)) {
                        node++;
                        forwardCount++;
                    }
                } else if (ACT_BACK.equals(a)) {
                    if (!frozen && canRollback(node)) {
                        node--;
                        backCount++;
                    }
                } else if (ACT_STOP.equals(a)) {
                    if (!frozen) {
                        stopped = true;
                        frozen = true;
                    }
                } else if (ACT_SEAL.equals(a)) {
                    sealed = true;
                    frozen = true;
                }
                // ACT_SIGN、ACT_HOLD 不动段
            }
        }
        return new ReplayResult(node, sealed, stopped, forwardCount, backCount);
    }

    /** 留痕放回去的结果：当前段、封没封住、止没止住，以及放出来的挪前／压回笔数 */
    public static final class ReplayResult {
        private final int node;
        private final boolean sealed;
        private final boolean stopped;
        private final int forwardCount;
        private final int backCount;

        public ReplayResult(int node, boolean sealed, boolean stopped, int forwardCount, int backCount) {
            this.node = node;
            this.sealed = sealed;
            this.stopped = stopped;
            this.forwardCount = forwardCount;
            this.backCount = backCount;
        }

        public int getNode() {
            return node;
        }

        public boolean isSealed() {
            return sealed;
        }

        public boolean isStopped() {
            return stopped;
        }

        public int getForwardCount() {
            return forwardCount;
        }

        public int getBackCount() {
            return backCount;
        }
    }

    /** 留痕里的一笔（动作＋落笔时所处段）；捋的时候只吃这一串 */
    public static final class Event {
        private final String action;
        private final int node;

        public Event(String action, int node) {
            this.action = action;
            this.node = node;
        }

        public String getAction() {
            return action;
        }

        public int getNode() {
            return node;
        }
    }

    // ------------------------------------------------------------------
    // 已到几人：只由逐笔签署按（段、趟）累，只此一处累法
    // ------------------------------------------------------------------

    /** 逐笔签署喂进来的一行：属第几趟、还算不算数（压回腾掉的为 false） */
    public static final class RoundSign {
        private final int round;
        private final boolean active;

        public RoundSign(int round, boolean active) {
            this.round = round;
            this.active = active;
        }

        public int getRound() {
            return round;
        }

        public boolean isActive() {
            return active;
        }
    }

    /**
     * 一段里最新的算数趟：只在仍算数（active）的笔里挑趟次最大的；一笔算数的都没有回 0。
     * 更早各趟的旧笔不抹、行还在，但不进「已到几人」。
     */
    public static int latestActiveRound(List<RoundSign> signs) {
        int latest = 0;
        boolean has = false;
        if (signs != null) {
            for (RoundSign s : signs) {
                if (s.active && (!has || s.round >= latest)) {
                    latest = s.round;
                    has = true;
                }
            }
        }
        return latest;
    }

    /** 某一段某一趟里仍算数的落笔有几笔（已到几人只认这一回逐笔累出来的数） */
    public static int activeArrivals(List<RoundSign> signs, int round) {
        int n = 0;
        if (signs != null) {
            for (RoundSign s : signs) {
                if (s.active && s.round == round) {
                    n++;
                }
            }
        }
        return n;
    }

    private static boolean first(List<Boolean> okSeq) {
        return okSeq != null && !okSeq.isEmpty() && Boolean.TRUE.equals(okSeq.get(0));
    }
}

package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpPermitBillMapper;
import com.fc.v2.mapper.auto.TNslpPermitSignMapper;
import com.fc.v2.mapper.auto.TNslpPermitTrailMapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.mapper.auto.TNslpPointPermitHistMapper;
import com.fc.v2.model.auto.TNslpPermitBill;
import com.fc.v2.model.auto.TNslpPermitSign;
import com.fc.v2.model.auto.TNslpPermitTrail;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.auto.TNslpPointPermitHist;
import com.fc.v2.model.custom.PermitLedger;
import com.fc.v2.model.custom.PermitOpen;
import com.fc.v2.model.custom.PermitSignSubmit;
import com.fc.v2.model.custom.PermitSummary;
import com.fc.v2.model.custom.PermitTrace;
import com.fc.v2.service.ITNslpPermitBillService;
import com.fc.v2.service.support.PermitBillRules;

/**
 * 夜间连续施工作业许可签批单 Service业务层处理（approval-chain 形状：三段签批＋册面联动）。
 *
 * 三段的事只在本处办——
 * 立（只从点位底册带出，一处点位一张在签的单，口头招呼不算立单）、
 * 签（唯一落笔入口；段次、应签、已到几人、算齐数法全由 {@link PermitBillRules} 这一处点，
 *     报文写的段、屏显的人数都不作数；一人一笔各记各的时刻）、
 * 回（压回粒度一段：只退一段、只抹本段本轮的笔，旧趟旧段一笔不动，重走分两处）、
 * 封（段三两条一并认可才点齐；点齐同一回里改口点位主档并附变更履历）、
 * 捋（当前段只由留痕放，已到几人只由逐笔累，受理簿笔数对得上，抽任一段也自洽）。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class TNslpPermitBillServiceImpl implements ITNslpPermitBillService {

    /** 点位主档夜间施工档：未准 */
    private static final int POINT_NOT_PERMITTED = 0;
    /** 点位主档夜间施工档：已准（三道签齐才准动） */
    private static final int POINT_PERMITTED = 1;

    @Resource
    private TNslpPermitBillMapper nslpPermitBillMapper;

    @Resource
    private TNslpPermitSignMapper nslpPermitSignMapper;

    @Resource
    private TNslpPermitTrailMapper nslpPermitTrailMapper;

    @Resource
    private TNslpPointMapper nslpPointMapper;

    @Resource
    private TNslpPointPermitHistMapper nslpPointPermitHistMapper;

    @Override
    public TNslpPermitBill selectTNslpPermitBillById(Long id) {
        return this.nslpPermitBillMapper.selectById(id);
    }

    // ------------------------------------------------------------------
    // 立：只从点位底册带出
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPermitBill open(PermitOpen submit) {
        if (submit == null || !notEmpty(submit.getPointNo())) {
            throw new IllegalArgumentException("立单只认点位底册带出来的点位码：这一栏空着，单立不起来"
                    + "（作业单位口头说一声不算立单，前端也没有自己新建的口子）");
        }
        String pointNo = submit.getPointNo().trim();
        TNslpPoint point = this.nslpPointMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("point_no", pointNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (point == null) {
            throw new IllegalArgumentException("点位底册里点不出这一处，单带不出来：" + pointNo);
        }
        // 一处点位、一次夜间连续施工只摊上一张：前一张还没封住（在签、止住、分歧挂起都算没封住），
        // 第二张开不出来；封住的不占名额，下一次施工另开新张。
        Integer openNum = this.nslpPermitBillMapper.selectCount(new QueryWrapper<TNslpPermitBill>()
                .eq("point_no", pointNo)
                .eq("status", PermitBillRules.STATUS_SIGNING)
                .eq("del_flag", 0));
        if (openNum != null && openNum.intValue() > 0) {
            throw new IllegalStateException("这一处点位已有一张没封住的签批单，第二张开不出来：" + pointNo);
        }

        Date now = new Date();
        TNslpPermitBill bill = new TNslpPermitBill();
        bill.setBillNo(resolveBillNo(pointNo));
        bill.setPointNo(pointNo);
        // 区册代号随点位底册带出，以底册为准；报文不带、前端不录
        bill.setZoneNo(point.getZoneNo());
        // 段次一律由后端数：起在段一，报文就算写了段也不采信
        bill.setNodeNo(Integer.valueOf(PermitBillRules.NODE_RECEIVE));
        applyNodeShape(bill, PermitBillRules.NODE_RECEIVE);
        bill.setSignCount(Integer.valueOf(0));
        bill.setStatus(Integer.valueOf(PermitBillRules.STATUS_SIGNING));
        bill.setRoundNo(Integer.valueOf(1));
        bill.setStopFlag(Integer.valueOf(0));
        bill.setDelFlag(Integer.valueOf(0));
        bill.setRemark(submit.getRemark());
        this.nslpPermitBillMapper.insert(bill);
        addTrail(bill.getId(), PermitBillRules.NODE_RECEIVE, 1,
                PermitBillRules.ACT_OPEN, "立单：点位" + pointNo + "，区册" + point.getZoneNo(),
                bill.getBillNo(), now);
        return bill;
    }

    // ------------------------------------------------------------------
    // 签：一次落笔只有这一个入口
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPermitBill sign(PermitSignSubmit submit) {
        if (submit == null || submit.getId() == null) {
            throw new IllegalArgumentException("落笔报文没指到单，这笔落不下来");
        }
        TNslpPermitBill bill = mustGet(submit.getId());
        if (PermitBillRules.isFrozen(bill.getStatus(), bill.getStopFlag())) {
            // 止住之后任何一段再落的笔都不采信；封住之后再递进来的人一律不接
            throw new IllegalStateException(PermitBillRules.isSealed(bill.getStatus())
                    ? "这一张已经点齐封住，再递进来的人一律不接：" + bill.getId()
                    : "这一张已止住，此后任何一段再落的笔都不采信：" + bill.getId());
        }
        int node = nodeOf(bill);
        // 递进来的报文写了第几段，落库未必采信：段次以服务端当下点的为准。
        // 提前递到下一段的不采信；跳过段二由段三递上来的丢掉，整单还留在段二。
        if (!PermitBillRules.acceptsAt(node, submit.getReportNode())) {
            throw new IllegalStateException("这笔自称落在" + PermitBillRules.nodeName(submit.getReportNode())
                    + "，本单当下停在" + PermitBillRules.nodeName(Integer.valueOf(node))
                    + "——段次只认服务端数出来的，这笔不采信、丢掉，整单仍留在"
                    + PermitBillRules.nodeName(Integer.valueOf(node)) + "：" + bill.getId());
        }
        if (!notEmpty(submit.getSigner())) {
            throw new IllegalArgumentException("落笔人这一栏空着，复核点不出是哪一位，这笔按没办完不落库");
        }
        String signer = submit.getSigner().trim();
        // 意见只认「可」「不可」两句原话；空着、写成别的，按没递上来，不落库
        String verdict = PermitBillRules.normalizeVerdict(submit.getVerdict());
        if (verdict == null) {
            throw new IllegalArgumentException("意见只认「可」「不可」这两句原话，递上来的是「"
                    + submit.getVerdict() + "」，这笔不落库");
        }
        int round = roundOf(bill);
        int arrivals = activeArrivals(bill.getId(), node, round);
        if (arrivals >= PermitBillRules.needCount(node)) {
            throw new IllegalStateException(PermitBillRules.nodeName(Integer.valueOf(node))
                    + "应签的人数已经齐了，多递进来的笔不采信：" + bill.getId());
        }
        List<TNslpPermitSign> nodeSigns = activeSigns(bill.getId(), node, round);
        for (TNslpPermitSign s : nodeSigns) {
            // 同一个人同一段本趟再递：段上留头一次那一句，不覆盖、不另存一份（再落两回幂等）
            if (signer.equals(s.getSigner())) {
                return bill;
            }
        }

        Date now = new Date();
        TNslpPermitSign row = new TNslpPermitSign();
        row.setBillId(bill.getId());
        row.setNodeNo(Integer.valueOf(node));
        row.setRoundNo(Integer.valueOf(round));
        row.setSigner(signer);
        row.setVerdict(verdict);
        row.setNote(notEmpty(submit.getNote()) ? submit.getNote().trim() : null);
        row.setSignTime(now);
        // 同一刻两人各自提交：按各自那一刻分别记账，同刻以 seq 断先后，不许并成一笔
        row.setSeq(Integer.valueOf(nextSignSeq(bill.getId(), now)));
        row.setActiveFlag(Integer.valueOf(1));
        row.setDelFlag(Integer.valueOf(0));
        row.setCreateBy(signer);
        row.setCreateTime(now);
        this.nslpPermitSignMapper.insert(row);
        addTrail(bill.getId(), node, round, PermitBillRules.ACT_SIGN,
                "落笔：" + signer + "「" + verdict + "」"
                        + (notEmpty(row.getNote()) ? "（" + row.getNote() + "）" : ""),
                signer, now);

        // 已到几人只由这一串逐笔累出来（按落单先后），回写行面只是回显
        List<TNslpPermitSign> all = activeSigns(bill.getId(), node, round);
        List<Boolean> okSeq = new ArrayList<Boolean>();
        for (TNslpPermitSign s : all) {
            okSeq.add(Boolean.valueOf(PermitBillRules.VERDICT_OK.equals(s.getVerdict())));
        }
        int nowArrivals = all.size();
        bill.setSignCount(Integer.valueOf(nowArrivals));
        bill.setUpdateBy(signer);
        bill.setUpdateTime(now);

        PermitBillRules.Outcome outcome = PermitBillRules.decide(node, nowArrivals, okSeq);
        if (outcome == PermitBillRules.Outcome.WAIT) {
            // 还差着人：整单留在这一段，段次、数法都不换
            this.nslpPermitBillMapper.updateById(bill);
            return bill;
        }
        if (outcome == PermitBillRules.Outcome.STOP) {
            // 有一方落「不可」：整张当即止住，冻结在落定的这一段
            bill.setStopFlag(Integer.valueOf(1));
            this.nslpPermitBillMapper.updateById(bill);
            addTrail(bill.getId(), node, round, PermitBillRules.ACT_STOP,
                    PermitBillRules.nodeName(Integer.valueOf(node)) + "收齐，首笔落「不可」，整张止住",
                    signer, now);
            return bill;
        }
        if (outcome == PermitBillRules.Outcome.HOLD) {
            // 仅段三：一条可一条不可——停在这一段等压回，既不往前推，也不就此止住
            this.nslpPermitBillMapper.updateById(bill);
            addTrail(bill.getId(), node, round, PermitBillRules.ACT_HOLD,
                    "市局存备席两条未一并认可：一条「可」一条「不可」，停在此段等压回", signer, now);
            return bill;
        }

        // 本段算齐：段三齐即点齐封住；其余只许挪紧邻的下一段
        if (node == PermitBillRules.NODE_RECORD) {
            sealBill(bill, signer, now);
        } else {
            int to = node + 1;
            bill.setNodeNo(Integer.valueOf(to));
            applyNodeShape(bill, to);
            // 新段还没人落笔：人数仍由逐笔累，起手是 0
            bill.setSignCount(Integer.valueOf(0));
            this.nslpPermitBillMapper.updateById(bill);
            addTrail(bill.getId(), to, round, PermitBillRules.ACT_FORWARD,
                    PermitBillRules.nodeName(Integer.valueOf(node)) + "算齐，挪到"
                            + PermitBillRules.nodeName(Integer.valueOf(to)), signer, now);
        }
        return bill;
    }

    // ------------------------------------------------------------------
    // 压回：粒度是「一段」，退到哪一段口径定死
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPermitBill rollback(Long id, String operator, String note) {
        TNslpPermitBill bill = mustGet(id);
        if (PermitBillRules.isFrozen(bill.getStatus(), bill.getStopFlag())) {
            throw new IllegalStateException(PermitBillRules.isSealed(bill.getStatus())
                    ? "这一张已经点齐封住，压不回来：" + id
                    : "这一张已经止住，不压回、不推进，只能看：" + id);
        }
        int node = nodeOf(bill);
        // 退到哪一段不由递报文的人点，口径里钉死：只退一段，头一段压无可压
        int target = PermitBillRules.rollbackTarget(node);
        int oldRound = roundOf(bill);
        int newRound = oldRound + 1;
        Date now = new Date();

        // 只抹这一段本轮落下的笔：置失效，行仍在、原话仍在，旧笔一笔不删；
        // 更早各段、更早各趟留下的旧笔更不动。重走这一段时本轮文本与上轮分两处存放。
        for (TNslpPermitSign stale : activeSigns(bill.getId(), node, oldRound)) {
            stale.setActiveFlag(Integer.valueOf(0));
            this.nslpPermitSignMapper.updateById(stale);
        }
        bill.setNodeNo(Integer.valueOf(target));
        applyNodeShape(bill, target);
        // 退回段上的人数只数它自己最新一趟的笔；旧趟的笔不进这个数
        bill.setRoundNo(Integer.valueOf(newRound));
        bill.setSignCount(Integer.valueOf(activeArrivals(bill.getId(), target, newRound)));
        bill.setUpdateBy(notEmpty(operator) ? operator.trim() : bill.getUpdateBy());
        bill.setUpdateTime(now);
        this.nslpPermitBillMapper.updateById(bill);
        addTrail(bill.getId(), target, newRound, PermitBillRules.ACT_BACK,
                "从" + PermitBillRules.nodeName(Integer.valueOf(node)) + "压回一关到"
                        + PermitBillRules.nodeName(Integer.valueOf(target))
                        + (notEmpty(note) ? "：" + note.trim() : ""),
                bill.getUpdateBy(), now);
        return bill;
    }

    // ------------------------------------------------------------------
    // 封：走满三段即点齐；同一回里改口点位主档并附履历
    // ------------------------------------------------------------------

    private void sealBill(TNslpPermitBill bill, String operator, Date now) {
        bill.setStatus(Integer.valueOf(PermitBillRules.STATUS_SEALED));
        bill.setSealTime(now);
        this.nslpPermitBillMapper.updateById(bill);

        // 册面与流转那本联动，同一回处理、同一次计算：主档改口与履历追加谁也不单独成回。
        TNslpPoint point = this.nslpPointMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("point_no", bill.getPointNo())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (point == null) {
            // 立单时点得出点位，封住却点不出：两本对不住，指名报出对应单据，整回回滚
            throw new IllegalStateException("封住时点位底册点不出这一行，册面与流转对不住，单据："
                    + bill.getBillNo() + "（" + bill.getPointNo() + "）");
        }
        int from = point.getNightPermit() == null ? POINT_NOT_PERMITTED : point.getNightPermit().intValue();
        if (from != POINT_PERMITTED) {
            point.setNightPermit(Integer.valueOf(POINT_PERMITTED));
            point.setUpdateBy(operator);
            point.setUpdateTime(now);
            this.nslpPointMapper.updateById(point);
            TNslpPointPermitHist hist = new TNslpPointPermitHist();
            hist.setPointId(point.getId());
            hist.setPointNo(point.getPointNo());
            // 履历钉住依据的是哪一张单：日后对不住，凭这一格指名报得出来
            hist.setBillId(bill.getId());
            hist.setBillNo(bill.getBillNo());
            hist.setOperator(operator);
            hist.setFromFlag(Integer.valueOf(from));
            hist.setToFlag(Integer.valueOf(POINT_PERMITTED));
            hist.setChangeTime(now);
            hist.setDelFlag(Integer.valueOf(0));
            hist.setCreateBy(operator);
            hist.setCreateTime(now);
            this.nslpPointPermitHistMapper.insert(hist);
        }
        addTrail(bill.getId(), PermitBillRules.NODE_RECORD, roundOf(bill),
                PermitBillRules.ACT_SEAL, "三段签齐点齐封住，点位" + point.getPointNo()
                        + "夜间施工档改为已准（单据" + bill.getBillNo() + "）", operator, now);
    }

    // ------------------------------------------------------------------
    // 捋：当前段由留痕放，已到几人由逐笔累
    // ------------------------------------------------------------------

    @Override
    public PermitTrace trace(Long id) {
        TNslpPermitBill bill = mustGet(id);
        return PermitTrace.of(bill.getId(), bill.getBillNo(), listTrails(id), listSigns(id));
    }

    @Override
    public PermitLedger ledger(Long id, Date day) {
        TNslpPermitBill bill = mustGet(id);
        return PermitLedger.of(bill.getId(), day, listTrails(id), listSigns(id));
    }

    @Override
    public int countOpen() {
        // 与逐段人数出自同一次计算，不另查第二遍
        return summary().getOpenBillCount();
    }

    @Override
    public PermitSummary summary() {
        // 页首张数与逐段在签人数同一次算：同一批在签单、同一套逐笔累数条件，两处不许各算各的
        List<TNslpPermitBill> openBills = this.nslpPermitBillMapper.selectList(
                new QueryWrapper<TNslpPermitBill>()
                        .eq("status", PermitBillRules.STATUS_SIGNING)
                        .eq("del_flag", 0));
        PermitSummary s = new PermitSummary();
        s.setOpenBillCount(openBills.size());
        int[] arrivalsByNode = new int[PermitBillRules.NODE_COUNT];
        for (TNslpPermitBill bill : openBills) {
            arrivalsByNode[nodeOf(bill)] += activeArrivals(bill.getId(), nodeOf(bill), roundOf(bill));
        }
        s.setArrivalsByNode(arrivalsByNode);
        return s;
    }

    @Override
    public List<TNslpPointPermitHist> listPointHist(Long pointId) {
        if (pointId == null) {
            throw new IllegalArgumentException("点位主键没给，履历翻不出来");
        }
        return this.nslpPointPermitHistMapper.selectList(new QueryWrapper<TNslpPointPermitHist>()
                .eq("point_id", pointId)
                .eq("del_flag", 0)
                .orderByAsc("change_time", "id"));
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private TNslpPermitBill mustGet(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("单主键没给，找不到这一张");
        }
        TNslpPermitBill bill = this.nslpPermitBillMapper.selectById(id);
        if (bill == null || (bill.getDelFlag() != null && bill.getDelFlag().intValue() == 1)) {
            throw new IllegalArgumentException("主档里没有这一张签批单：" + id);
        }
        return bill;
    }

    private int nodeOf(TNslpPermitBill bill) {
        return bill.getNodeNo() == null ? PermitBillRules.NODE_RECEIVE : bill.getNodeNo().intValue();
    }

    private int roundOf(TNslpPermitBill bill) {
        return bill.getRoundNo() == null ? 1 : bill.getRoundNo().intValue();
    }

    /** 每段应签几人、按什么算法算齐：一律问口径这一处，行面只回显 */
    private static void applyNodeShape(TNslpPermitBill bill, int node) {
        bill.setSignMode(Integer.valueOf(PermitBillRules.signMode(node)));
        bill.setNeedCount(Integer.valueOf(PermitBillRules.needCount(node)));
    }

    private List<TNslpPermitSign> activeSigns(Long billId, int node, int round) {
        return this.nslpPermitSignMapper.selectList(new QueryWrapper<TNslpPermitSign>()
                .eq("bill_id", billId)
                .eq("node_no", node)
                .eq("round_no", round)
                .eq("active_flag", 1)
                .eq("del_flag", 0)
                .orderByAsc("sign_time", "seq", "id"));
    }

    /** 已到几人只认逐笔累加的这一回：本趟仍算数的落笔有几笔 */
    private int activeArrivals(Long billId, int node, int round) {
        Integer n = this.nslpPermitSignMapper.selectCount(new QueryWrapper<TNslpPermitSign>()
                .eq("bill_id", billId)
                .eq("node_no", node)
                .eq("round_no", round)
                .eq("active_flag", 1)
                .eq("del_flag", 0));
        return n == null ? 0 : n.intValue();
    }

    private List<TNslpPermitTrail> listTrails(Long billId) {
        return this.nslpPermitTrailMapper.selectList(new QueryWrapper<TNslpPermitTrail>()
                .eq("bill_id", billId)
                .eq("del_flag", 0)
                .orderByAsc("trail_time", "seq", "id"));
    }

    private List<TNslpPermitSign> listSigns(Long billId) {
        return this.nslpPermitSignMapper.selectList(new QueryWrapper<TNslpPermitSign>()
                .eq("bill_id", billId)
                .eq("del_flag", 0)
                .orderByAsc("node_no", "round_no", "sign_time", "seq", "id"));
    }

    private int addTrail(Long billId, int node, int round, String action, String note,
                         String operator, Date at) {
        TNslpPermitTrail t = new TNslpPermitTrail();
        t.setBillId(billId);
        t.setNodeNo(Integer.valueOf(node));
        t.setRoundNo(Integer.valueOf(round));
        t.setAction(action);
        t.setNote(note);
        t.setTrailTime(at);
        t.setSeq(Integer.valueOf(nextTrailSeq(billId, at)));
        t.setDelFlag(Integer.valueOf(0));
        t.setCreateBy(operator);
        t.setCreateTime(at);
        return this.nslpPermitTrailMapper.insert(t);
    }

    /** 同刻先后：trail_time 并列时以本列断先后，两人同刻各记各的、不并成一笔 */
    private int nextTrailSeq(Long billId, Date at) {
        Integer n = this.nslpPermitTrailMapper.selectCount(new QueryWrapper<TNslpPermitTrail>()
                .eq("bill_id", billId)
                .eq("trail_time", at)
                .eq("del_flag", 0));
        return n == null ? 0 : n.intValue();
    }

    private int nextSignSeq(Long billId, Date at) {
        Integer n = this.nslpPermitSignMapper.selectCount(new QueryWrapper<TNslpPermitSign>()
                .eq("bill_id", billId)
                .eq("sign_time", at)
                .eq("del_flag", 0));
        return n == null ? 0 : n.intValue();
    }

    private String resolveBillNo(String pointNo) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        return "YP" + pointNo + String.format("%04d%02d%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

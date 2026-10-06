package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import com.fc.v2.model.custom.PermitTrace;
import com.fc.v2.service.support.PermitBillRules;

/**
 * 夜间连续施工许可签批单测试：立・签・回・封・捋五件事照口径走一遍。
 *
 * 五个 mapper 用内存账顶替，条件按 QueryWrapper 片段里的列名逐个过滤；
 * 改口主档、追加履历、落笔留痕在同一方法调用内接连落账（事务由 Spring 管）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TNslpPermitBillServiceImplTest {

    @Mock
    private TNslpPermitBillMapper billMapper;
    @Mock
    private TNslpPermitSignMapper signMapper;
    @Mock
    private TNslpPermitTrailMapper trailMapper;
    @Mock
    private TNslpPointMapper pointMapper;
    @Mock
    private TNslpPointPermitHistMapper histMapper;
    @InjectMocks
    private TNslpPermitBillServiceImpl service;

    private List<TNslpPermitBill> billStore;
    private List<TNslpPermitSign> signStore;
    private List<TNslpPermitTrail> trailStore;
    private List<TNslpPoint> pointStore;
    private List<TNslpPointPermitHist> histStore;
    private long idSeq;

    private static final Pattern CLAUSE = Pattern.compile(
            "(\\w+)\\s*(<=|>=|=|<|>)\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    private static boolean eqv(Object field, Object val) {
        if (field == null || val == null) {
            return field == val;
        }
        return field.equals(val) || String.valueOf(field).equals(String.valueOf(val));
    }

    private static List<String[]> clauses(QueryWrapper<?> w) {
        w.getSqlSegment();
        Map<String, Object> params = w.getParamNameValuePairs();
        List<String[]> out = new ArrayList<String[]>();
        Matcher m = CLAUSE.matcher(w.getSqlSegment() == null ? "" : w.getSqlSegment());
        while (m.find()) {
            out.add(new String[]{m.group(1), m.group(2), String.valueOf(params.get(m.group(3)))});
        }
        return out;
    }

    private boolean billMatches(TNslpPermitBill r, List<String[]> cs) {
        for (String[] c : cs) {
            Object v;
            switch (c[0]) {
                case "point_no": v = r.getPointNo(); break;
                case "status": v = r.getStatus(); break;
                case "stop_flag": v = r.getStopFlag(); break;
                case "del_flag": v = r.getDelFlag(); break;
                case "id": v = r.getId(); break;
                default: v = null;
            }
            if (!eqv(v, c[2])) {
                return false;
            }
        }
        return true;
    }

    private boolean signMatches(TNslpPermitSign s, List<String[]> cs) {
        for (String[] c : cs) {
            Object v;
            switch (c[0]) {
                case "bill_id": v = s.getBillId(); break;
                case "node_no": v = s.getNodeNo(); break;
                case "round_no": v = s.getRoundNo(); break;
                case "active_flag": v = s.getActiveFlag(); break;
                case "del_flag": v = s.getDelFlag(); break;
                case "sign_time": v = s.getSignTime(); break;
                default: v = null;
            }
            if (!eqv(v, c[2])) {
                return false;
            }
        }
        return true;
    }

    private boolean trailMatches(TNslpPermitTrail t, List<String[]> cs) {
        for (String[] c : cs) {
            Object v;
            switch (c[0]) {
                case "bill_id": v = t.getBillId(); break;
                case "trail_time": v = t.getTrailTime(); break;
                case "del_flag": v = t.getDelFlag(); break;
                default: v = null;
            }
            if (!eqv(v, c[2])) {
                return false;
            }
        }
        return true;
    }

    private boolean pointMatches(TNslpPoint p, List<String[]> cs) {
        for (String[] c : cs) {
            Object v = "point_no".equals(c[0]) ? p.getPointNo()
                    : "del_flag".equals(c[0]) ? p.getDelFlag()
                    : "id".equals(c[0]) ? p.getId() : null;
            if (!eqv(v, c[2])) {
                return false;
            }
        }
        return true;
    }

    private boolean histMatches(TNslpPointPermitHist h, List<String[]> cs) {
        for (String[] c : cs) {
            Object v = "point_id".equals(c[0]) ? h.getPointId()
                    : "del_flag".equals(c[0]) ? h.getDelFlag() : null;
            if (!eqv(v, c[2])) {
                return false;
            }
        }
        return true;
    }

    private TNslpPermitBill findBill(Long id) {
        for (TNslpPermitBill r : billStore) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    @BeforeEach
    public void setUp() {
        billStore = new ArrayList<TNslpPermitBill>();
        signStore = new ArrayList<TNslpPermitSign>();
        trailStore = new ArrayList<TNslpPermitTrail>();
        pointStore = new ArrayList<TNslpPoint>();
        histStore = new ArrayList<TNslpPointPermitHist>();
        idSeq = 0;

        when(billMapper.insert(any(TNslpPermitBill.class))).thenAnswer(inv -> {
            TNslpPermitBill r = inv.getArgument(0);
            r.setId(Long.valueOf(++idSeq));
            billStore.add(r);
            return 1;
        });
        when(billMapper.selectById(any())).thenAnswer(inv -> findBill(inv.getArgument(0)));
        when(billMapper.selectList(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            List<TNslpPermitBill> out = new ArrayList<TNslpPermitBill>();
            for (TNslpPermitBill r : billStore) {
                if (billMatches(r, cs)) {
                    out.add(r);
                }
            }
            return out;
        });
        when(billMapper.selectCount(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            int n = 0;
            for (TNslpPermitBill r : billStore) {
                if (billMatches(r, cs)) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });
        when(billMapper.updateById(any(TNslpPermitBill.class))).thenAnswer(inv -> {
            TNslpPermitBill u = inv.getArgument(0);
            TNslpPermitBill r = findBill(u.getId());
            if (r == null) {
                return 0;
            }
            if (u.getBillNo() != null) r.setBillNo(u.getBillNo());
            if (u.getPointNo() != null) r.setPointNo(u.getPointNo());
            if (u.getZoneNo() != null) r.setZoneNo(u.getZoneNo());
            if (u.getNodeNo() != null) r.setNodeNo(u.getNodeNo());
            if (u.getSignMode() != null) r.setSignMode(u.getSignMode());
            if (u.getNeedCount() != null) r.setNeedCount(u.getNeedCount());
            if (u.getSignCount() != null) r.setSignCount(u.getSignCount());
            if (u.getStatus() != null) r.setStatus(u.getStatus());
            if (u.getRoundNo() != null) r.setRoundNo(u.getRoundNo());
            if (u.getStopFlag() != null) r.setStopFlag(u.getStopFlag());
            if (u.getSealTime() != null) r.setSealTime(u.getSealTime());
            if (u.getDelFlag() != null) r.setDelFlag(u.getDelFlag());
            if (u.getUpdateBy() != null) r.setUpdateBy(u.getUpdateBy());
            if (u.getUpdateTime() != null) r.setUpdateTime(u.getUpdateTime());
            if (u.getRemark() != null) r.setRemark(u.getRemark());
            return 1;
        });

        when(signMapper.insert(any(TNslpPermitSign.class))).thenAnswer(inv -> {
            TNslpPermitSign s = inv.getArgument(0);
            s.setId(Long.valueOf(++idSeq));
            signStore.add(s);
            return 1;
        });
        when(signMapper.selectList(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            List<TNslpPermitSign> out = new ArrayList<TNslpPermitSign>();
            for (TNslpPermitSign s : signStore) {
                if (signMatches(s, cs)) {
                    out.add(s);
                }
            }
            return out;
        });
        when(signMapper.selectCount(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            int n = 0;
            for (TNslpPermitSign s : signStore) {
                if (signMatches(s, cs)) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });
        when(signMapper.updateById(any(TNslpPermitSign.class))).thenAnswer(inv -> {
            TNslpPermitSign u = inv.getArgument(0);
            for (TNslpPermitSign s : signStore) {
                if (s.getId().equals(u.getId())) {
                    s.setActiveFlag(u.getActiveFlag());
                    return 1;
                }
            }
            return 0;
        });

        when(trailMapper.insert(any(TNslpPermitTrail.class))).thenAnswer(inv -> {
            TNslpPermitTrail t = inv.getArgument(0);
            t.setId(Long.valueOf(++idSeq));
            trailStore.add(t);
            return 1;
        });
        when(trailMapper.selectList(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            List<TNslpPermitTrail> out = new ArrayList<TNslpPermitTrail>();
            for (TNslpPermitTrail t : trailStore) {
                if (trailMatches(t, cs)) {
                    out.add(t);
                }
            }
            return out;
        });
        when(trailMapper.selectCount(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            int n = 0;
            for (TNslpPermitTrail t : trailStore) {
                if (trailMatches(t, cs)) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });

        when(pointMapper.selectOne(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            for (TNslpPoint p : pointStore) {
                if (pointMatches(p, cs)) {
                    return p;
                }
            }
            return null;
        });
        when(pointMapper.updateById(any(TNslpPoint.class))).thenAnswer(inv -> {
            TNslpPoint u = inv.getArgument(0);
            for (TNslpPoint p : pointStore) {
                if (p.getId().equals(u.getId())) {
                    p.setNightPermit(u.getNightPermit());
                    p.setUpdateBy(u.getUpdateBy());
                    p.setUpdateTime(u.getUpdateTime());
                    return 1;
                }
            }
            return 0;
        });

        when(histMapper.insert(any(TNslpPointPermitHist.class))).thenAnswer(inv -> {
            TNslpPointPermitHist h = inv.getArgument(0);
            h.setId(Long.valueOf(++idSeq));
            histStore.add(h);
            return 1;
        });
        when(histMapper.selectList(any())).thenAnswer(inv -> {
            List<String[]> cs = clauses(inv.getArgument(0));
            List<TNslpPointPermitHist> out = new ArrayList<TNslpPointPermitHist>();
            for (TNslpPointPermitHist h : histStore) {
                if (histMatches(h, cs)) {
                    out.add(h);
                }
            }
            return out;
        });

        pointStore.add(point(1L, "PT0001", "QN00"));
        pointStore.add(point(2L, "PT0002", "QN00"));
    }

    private static TNslpPoint point(Long id, String no, String zoneNo) {
        TNslpPoint p = new TNslpPoint();
        p.setId(id);
        p.setPointNo(no);
        p.setZoneNo(zoneNo);
        p.setNightPermit(0);
        p.setDelFlag(0);
        return p;
    }

    private static PermitOpen openReq(String pointNo) {
        PermitOpen o = new PermitOpen();
        o.setPointNo(pointNo);
        return o;
    }

    private static PermitSignSubmit signReq(Long id, Integer reportNode, String signer, String verdict) {
        PermitSignSubmit s = new PermitSignSubmit();
        s.setId(id);
        s.setReportNode(reportNode);
        s.setSigner(signer);
        s.setVerdict(verdict);
        s.setNote(verdict + "的原话");
        return s;
    }

    private List<TNslpPermitTrail> trailsOf(Long id) {
        List<TNslpPermitTrail> out = new ArrayList<TNslpPermitTrail>();
        for (TNslpPermitTrail t : trailStore) {
            if (t.getBillId().equals(id)) {
                out.add(t);
            }
        }
        return out;
    }

    private List<TNslpPermitSign> signsOf(Long id) {
        List<TNslpPermitSign> out = new ArrayList<TNslpPermitSign>();
        for (TNslpPermitSign s : signStore) {
            if (s.getBillId().equals(id)) {
                out.add(s);
            }
        }
        return out;
    }

    private int countAction(Long id, String action) {
        int n = 0;
        for (TNslpPermitTrail t : trailsOf(id)) {
            if (action.equals(t.getAction())) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // 立
    // ------------------------------------------------------------------

    @Test
    public void openComesOnlyFromPointLedger() {
        TNslpPermitBill bill = service.open(openReq("PT0001"));
        assertEquals("PT0001", bill.getPointNo());
        assertEquals("QN00", bill.getZoneNo());
        assertEquals(Integer.valueOf(0), bill.getNodeNo());
        assertEquals(Integer.valueOf(0), bill.getSignMode());
        assertEquals(Integer.valueOf(1), bill.getNeedCount());
        assertEquals(Integer.valueOf(0), bill.getSignCount());
        assertEquals(Integer.valueOf(1), bill.getRoundNo());
        assertEquals(Integer.valueOf(PermitBillRules.STATUS_SIGNING), bill.getStatus());
        assertEquals(PermitBillRules.ACT_OPEN, trailsOf(bill.getId()).get(0).getAction());
        assertEquals(1, service.countOpen());

        // 口头招呼、空报文、空点位码都不立单
        assertThrows(IllegalArgumentException.class, () -> service.open(null));
        assertThrows(IllegalArgumentException.class, () -> service.open(openReq("  ")));
        // 底册点不出：带不出来
        assertThrows(IllegalArgumentException.class, () -> service.open(openReq("PT404")));
        assertEquals(1, billStore.size());
    }

    @Test
    public void oneOpenBillPerPoint() {
        TNslpPermitBill a = service.open(openReq("PT0001"));
        // 前一张没封住（哪怕止住）：第二张开不出来
        service.sign(signReq(a.getId(), 0, "收件甲", "不可"));
        assertThrows(IllegalStateException.class, () -> service.open(openReq("PT0001")));
        // 另一个点位照常立
        assertNotNull(service.open(openReq("PT0002")));
        assertEquals(2, service.countOpen());
    }

    // ------------------------------------------------------------------
    // 段一：1 人即过
    // ------------------------------------------------------------------

    @Test
    public void stageOneSoloSignPassesOrStops() {
        TNslpPermitBill bill = service.open(openReq("PT0001"));
        TNslpPermitBill now = service.sign(signReq(bill.getId(), 0, "收件甲", "可"));
        // 一人落单即齐，挪到段二；段二形状 2 人、先落为准
        assertEquals(Integer.valueOf(1), now.getNodeNo());
        assertEquals(Integer.valueOf(1), now.getSignMode());
        assertEquals(Integer.valueOf(2), now.getNeedCount());
        assertEquals(Integer.valueOf(0), now.getSignCount());
        assertEquals(1, countAction(bill.getId(), PermitBillRules.ACT_FORWARD));

        TNslpPermitBill vetoed = service.open(openReq("PT0002"));
        service.sign(signReq(vetoed.getId(), 0, "收件甲", "不可"));
        TNslpPermitBill frozen = service.selectTNslpPermitBillById(vetoed.getId());
        assertEquals(Integer.valueOf(1), frozen.getStopFlag());
        // 止住后任何一段再落的笔都不采信
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(vetoed.getId(), 0, "收件乙", "可")));
        assertThrows(IllegalStateException.class,
                () -> service.rollback(vetoed.getId(), "op", null));
        // 止住仍算没封住：占名额、进页首数
        assertEquals(2, service.countOpen());
    }

    // ------------------------------------------------------------------
    // 段二：先落笔那一方为准
    // ------------------------------------------------------------------

    @Test
    public void stageTwoFirstWinsSecondStillRecorded() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 1);
        // 头一个落「可」：结论就是可；第二个落「不可」仍要交，人数累、原话进库、屏上两行都在
        service.sign(signReq(bill.getId(), 1, "认定甲", "可"));
        TNslpPermitBill second = service.sign(signReq(bill.getId(), 1, "认定乙", "不可"));
        // 首笔为准：两人交齐后照样推进，不被第二笔的不可止住
        assertEquals(Integer.valueOf(2), second.getNodeNo());
        assertEquals(Integer.valueOf(0), second.getStopFlag());
        int node1Signs = 0;
        for (TNslpPermitSign s : signsOf(bill.getId())) {
            if (s.getNodeNo().intValue() == 1) {
                node1Signs++;
            }
        }
        assertEquals(2, node1Signs);

        List<TNslpPermitSign> n2 = new ArrayList<TNslpPermitSign>();
        for (TNslpPermitSign s : signsOf(bill.getId())) {
            if (s.getNodeNo().intValue() == 1) {
                n2.add(s);
            }
        }
        assertEquals(2, n2.size());
        assertEquals("可", n2.get(0).getVerdict());
        assertEquals("不可", n2.get(1).getVerdict());
        assertNotNull(n2.get(0).getNote());
        assertNotNull(n2.get(1).getNote());
    }

    @Test
    public void stageTwoFirstNoStopsAfterQuota() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 1);
        service.sign(signReq(bill.getId(), 1, "认定甲", "不可"));
        // 只来一人还没齐，整单留在段二，不止住
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
        assertEquals(Integer.valueOf(0), service.selectTNslpPermitBillById(bill.getId()).getStopFlag());
        // 第二笔收齐，首笔为不可：止住
        service.sign(signReq(bill.getId(), 1, "认定乙", "可"));
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getStopFlag());
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
        assertEquals(1, countAction(bill.getId(), PermitBillRules.ACT_STOP));
    }

    // ------------------------------------------------------------------
    // 段三：两条一并认可
    // ------------------------------------------------------------------

    @Test
    public void stageThreeNeedsBothAgreeThenSealsAndFlipsLedger() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 2);
        service.sign(signReq(bill.getId(), 2, "存备甲", "可"));
        // 只到一人：留在段三等
        assertEquals(Integer.valueOf(2), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getSignCount());

        // 第二笔也可：点齐封住
        TNslpPermitBill sealed = service.sign(signReq(bill.getId(), 2, "存备乙", "可"));
        assertEquals(Integer.valueOf(PermitBillRules.STATUS_SEALED), sealed.getStatus());
        assertNotNull(sealed.getSealTime());
        assertEquals(1, countAction(bill.getId(), PermitBillRules.ACT_SEAL));
        // 封住后再递不接
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(bill.getId(), 2, "存备丙", "可")));
        // 页首数减一
        assertEquals(0, service.countOpen());

        // 点位主档同一回改口，且追加一条变更履历：谁落、哪张单、从哪档到哪档、哪一刻
        assertEquals(Integer.valueOf(1), pointStore.get(0).getNightPermit());
        assertEquals("存备乙", pointStore.get(0).getUpdateBy());
        assertEquals(1, histStore.size());
        TNslpPointPermitHist h = histStore.get(0);
        assertEquals(Long.valueOf(1L), h.getPointId());
        assertEquals("PT0001", h.getPointNo());
        assertEquals(bill.getId(), h.getBillId());
        assertEquals(sealed.getBillNo(), h.getBillNo());
        assertEquals("存备乙", h.getOperator());
        assertEquals(Integer.valueOf(0), h.getFromFlag());
        assertEquals(Integer.valueOf(1), h.getToFlag());
        assertEquals(sealed.getSealTime(), h.getChangeTime());
        assertEquals(1, service.listPointHist(1L).size());

        // 封住的点位可就下一次施工另开新张（不占名额）
        TNslpPermitBill again = service.open(openReq("PT0001"));
        assertNotNull(again);
        assertEquals(1, service.countOpen());
    }

    @Test
    public void stageThreeSplitHoldsNeitherAdvancesNorStops() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 2);
        service.sign(signReq(bill.getId(), 2, "存备甲", "可"));
        TNslpPermitBill split = service.sign(signReq(bill.getId(), 2, "存备乙", "不可"));
        // 一可一不可：停在段三等，既不往前推、也不就此止住
        assertEquals(Integer.valueOf(2), split.getNodeNo());
        assertEquals(Integer.valueOf(0), split.getStopFlag());
        assertEquals(PermitBillRules.STATUS_SIGNING, split.getStatus().intValue());
        assertEquals(1, countAction(bill.getId(), PermitBillRules.ACT_HOLD));
        // 人已齐，再递不采信
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(bill.getId(), 2, "存备丙", "可")));
        // 等压回：可以压，旧笔腾掉
        TNslpPermitBill back = service.rollback(bill.getId(), "存备乙", "分歧，补材料");
        assertEquals(Integer.valueOf(1), back.getNodeNo());
    }

    // ------------------------------------------------------------------
    // 段次：报文写了不算，提前递/跳段递的笔丢掉
    // ------------------------------------------------------------------

    @Test
    public void reportedStageIsNotTrusted() {
        TNslpPermitBill bill = service.open(openReq("PT0001"));
        // 单还在段一，提前递到段二：不采信，整单留段一
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(bill.getId(), 1, "认定甲", "可")));
        assertEquals(Integer.valueOf(0), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
        assertEquals(0, countAction(bill.getId(), PermitBillRules.ACT_SIGN));

        // 推到段二后，跳过段二直接由段三递：丢掉，整单还留段二
        service.sign(signReq(bill.getId(), 0, "收件甲", "可"));
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(bill.getId(), 2, "存备甲", "可")));
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
    }

    @Test
    public void sameSignerTwiceIsIdempotentAndQuotaRejectsExtra() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 1);
        // 段二首笔落「可」后仍等第二个交：首笔定结论，人数照旧要累到 2
        TNslpPermitBill first = service.sign(signReq(bill.getId(), 1, "认定甲", "可"));
        assertEquals(Integer.valueOf(1), first.getNodeNo());
        assertEquals(Integer.valueOf(1), first.getSignCount());
        // 同一段同一人第二次递文本：段上留头一次那句，不覆盖、不另存一份（再落两回幂等挡回）
        TNslpPermitBill repeat = service.sign(signReq(bill.getId(), 1, "认定甲", "不可"));
        assertEquals(Integer.valueOf(1), repeat.getSignCount());
        List<TNslpPermitSign> n2 = new ArrayList<TNslpPermitSign>();
        for (TNslpPermitSign z : signsOf(bill.getId())) {
            if (z.getNodeNo().intValue() == 1) {
                n2.add(z);
            }
        }
        assertEquals(1, n2.size());
        assertEquals("可", n2.get(0).getVerdict());
        // 第二名交齐，按首笔「可」推进，第二笔「不可」不止住
        service.sign(signReq(bill.getId(), 1, "认定乙", "不可"));
        assertEquals(Integer.valueOf(2), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
        assertEquals(Integer.valueOf(0), service.selectTNslpPermitBillById(bill.getId()).getStopFlag());
        // 段三两名齐后封住，再多递的人不采信
        service.sign(signReq(bill.getId(), 2, "存备甲", "可"));
        service.sign(signReq(bill.getId(), 2, "存备乙", "可"));
        assertEquals(Integer.valueOf(PermitBillRules.STATUS_SEALED),
                service.selectTNslpPermitBillById(bill.getId()).getStatus());
        assertThrows(IllegalStateException.class,
                () -> service.sign(signReq(bill.getId(), 2, "存备丙", "可")));
        // 段一1＋段二2＋段三2＝5 笔；重复那回没添笔
        assertEquals(5, signsOf(bill.getId()).size());
    }

    @Test
    public void sameInstantTwoSignsKeptSeparatelyWithSeq() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 1);
        Date same = new Date();
        TNslpPermitSign a = new TNslpPermitSign();
        a.setBillId(bill.getId());
        a.setNodeNo(1);
        a.setRoundNo(1);
        a.setSigner("认定甲");
        a.setVerdict("可");
        a.setSignTime(same);
        a.setSeq(0);
        a.setActiveFlag(1);
        a.setDelFlag(0);
        TNslpPermitSign b = new TNslpPermitSign();
        b.setBillId(bill.getId());
        b.setNodeNo(1);
        b.setRoundNo(1);
        b.setSigner("认定乙");
        b.setVerdict("可");
        b.setSignTime(same);
        b.setSeq(1);
        b.setActiveFlag(1);
        b.setDelFlag(0);
        signMapper.insert(a);
        signMapper.insert(b);
        // 同刻两笔各记各的，不并成一笔；按 time,seq 排先后
        PermitTrace trace = service.trace(bill.getId());
        assertEquals(2, trace.getNodes().get(1).getArrivals());
        assertEquals("认定甲", trace.getNodes().get(1).getRows().get(0).getSigner());
        assertEquals("认定乙", trace.getNodes().get(1).getRows().get(1).getSigner());
        assertEquals(0, trace.getNodes().get(1).getRows().get(0).getSeq());
        assertEquals(1, trace.getNodes().get(1).getRows().get(1).getSeq());
    }

    // ------------------------------------------------------------------
    // 压回：只退一关、只抹本轮；重走分两趟
    // ------------------------------------------------------------------

    @Test
    public void rollbackClearsOnlyCurrentRoundKeepsOlder() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 2);
        int signsBefore = signsOf(bill.getId()).size(); // 段一1＋段二2＝3 笔旧账
        service.sign(signReq(bill.getId(), 2, "存备甲", "可"));
        service.sign(signReq(bill.getId(), 2, "存备乙", "不可")); // 分歧挂起段三
        assertEquals(1, countAction(bill.getId(), PermitBillRules.ACT_HOLD));

        // 从段三压回段二：只退一段
        TNslpPermitBill back = service.rollback(bill.getId(), "存备乙", "退回重认");
        assertEquals(Integer.valueOf(1), back.getNodeNo());
        assertEquals(Integer.valueOf(2), back.getRoundNo());
        assertEquals(Integer.valueOf(0), back.getSignCount()); // 段二新趟还没人
        assertEquals(PermitBillRules.MODE_FIRST_WINS, back.getSignMode().intValue());

        // 段三本轮两笔置失效但行还在、原话还查得着；更早各段旧笔一笔不动
        for (TNslpPermitSign s : signsOf(bill.getId())) {
            if (s.getNodeNo().intValue() == 2) {
                assertEquals(Integer.valueOf(0), s.getActiveFlag());
                assertNotNull(s.getNote());
            } else {
                assertEquals(Integer.valueOf(1), s.getActiveFlag());
            }
        }
        assertEquals(signsBefore + 2, signsOf(bill.getId()).size());

        // 段二重走（第二趟）：本轮与上轮分两处；首笔结论重新数——上趟首笔可，这趟首笔不可
        service.sign(signReq(bill.getId(), 1, "认定丙", "不可"));
        assertEquals(Integer.valueOf(1),
                service.selectTNslpPermitBillById(bill.getId()).getSignCount());
        service.sign(signReq(bill.getId(), 1, "认定丁", "可"));
        // 新趟首笔为不可：止住；上趟旧结论不套用
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getStopFlag());
        assertEquals(Integer.valueOf(1), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());

        // 头一段压无可压
        TNslpPermitBill fresh = service.open(openReq("PT0002"));
        assertThrows(IllegalStateException.class, () -> service.rollback(fresh.getId(), "op", null));
    }

    // ------------------------------------------------------------------
    // 捋：段由留痕放、人由逐笔累；受理簿对得上、抽一段自洽
    // ------------------------------------------------------------------

    @Test
    public void traceStageComesFromTrailAndCountsFromSigns() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 2);
        PermitTrace t = service.trace(bill.getId());
        assertEquals(2, t.getReplayNode());
        assertEquals("市局存备席", t.getReplayNodeText());
        assertFalse(t.isReplaySealed());
        // 三段的应签、数法各是各的
        assertEquals(1, t.getNodes().get(0).getNeed());
        assertEquals(PermitBillRules.MODE_SOLO, t.getNodes().get(0).getMode());
        assertEquals(2, t.getNodes().get(1).getNeed());
        assertEquals(PermitBillRules.MODE_FIRST_WINS, t.getNodes().get(1).getMode());
        assertEquals(2, t.getNodes().get(2).getNeed());
        assertEquals(PermitBillRules.MODE_ALL_AGREE, t.getNodes().get(2).getMode());
        // 已到几人不取屏显：段一、段二各累出 1、2，段三 0
        assertEquals(1, t.getNodes().get(0).getArrivals());
        assertEquals(2, t.getNodes().get(1).getArrivals());
        assertEquals(0, t.getNodes().get(2).getArrivals());
        // 立单 1、落笔 3（段一1＋段二2）、挪前 2
        assertEquals(6, t.getEntries().size());
    }

    @Test
    public void ledgerBalancesStageByStage() {
        TNslpPermitBill bill = reachNode(service.open(openReq("PT0001")).getId(), 2);
        service.sign(signReq(bill.getId(), 2, "存备甲", "可"));
        service.sign(signReq(bill.getId(), 2, "存备乙", "不可")); // 挂起
        service.rollback(bill.getId(), "存备乙", null);             // 压回段二
        service.sign(signReq(bill.getId(), 1, "认定丙", "可"));
        service.sign(signReq(bill.getId(), 1, "认定丁", "可"));  // 重走到段三
        // 此刻停在段三
        assertEquals(Integer.valueOf(2),
                service.selectTNslpPermitBillById(bill.getId()).getNodeNo());

        PermitLedger l = service.ledger(bill.getId(), null);
        // 挪前 3 笔（段一→二、二→三、压回后重走 二→三）、压回 1 笔；点齐不走挪前
        assertEquals(3, l.getForwardCount());
        assertEquals(1, l.getBackCount());
        assertEquals(2, l.getReplayNode());
        assertTrue(l.isStageBalanced(), "逐段进出该轧平：" + l.getUnbalancedNodes());
        for (PermitLedger.NodeLedger nb : l.getNodes()) {
            assertTrue(nb.isSelfConsistent(), "抽" + nb.getNodeText() + "单核也得自洽");
            assertTrue(nb.isArrivalsWithinNeed());
        }
        // 段三上趟两笔被压回腾掉：逐段账上仍列得出旧趟（activeArrivals 只数本趟）
        PermitLedger.NodeLedger n3 = l.getNodes().get(2);
        assertEquals(0, n3.getActiveArrivals());
        assertEquals(2, n3.getSignTotal());
        assertEquals(1, n3.getRounds().size());
        // 段二重走在第二趟，逐段账分趟列
        PermitLedger.NodeLedger n2 = l.getNodes().get(1);
        assertEquals(2, n2.getLatestRound());
        assertEquals(2, n2.getActiveArrivals());

        // 点齐封住后：末段多压一笔（封住落在段三），进出仍轧平；段三新旧两趟共四笔
        service.sign(signReq(bill.getId(), 2, "存备戊", "可"));
        service.sign(signReq(bill.getId(), 2, "存备己", "可"));
        PermitLedger sealed = service.ledger(bill.getId(), null);
        assertEquals(2, sealed.getReplayNode());
        assertTrue(sealed.isStageBalanced());
        assertTrue(sealed.isReplaySealed());
        PermitLedger.NodeLedger sealedNode3 = sealed.getNodes().get(2);
        assertEquals(2, sealedNode3.getActiveArrivals());
        assertEquals(4, sealedNode3.getSignTotal());
        assertEquals(2, sealedNode3.getRounds().size());
        assertThrows(IllegalArgumentException.class, () -> service.ledger(999L, null));
    }

    @Test
    public void badVerdictAndBlankSignerRejected() {
        TNslpPermitBill bill = service.open(openReq("PT0001"));
        PermitSignSubmit bad = signReq(bill.getId(), 0, "收件甲", "同意");
        assertThrows(IllegalArgumentException.class, () -> service.sign(bad));
        PermitSignSubmit nobody = signReq(bill.getId(), 0, "  ", "可");
        assertThrows(IllegalArgumentException.class, () -> service.sign(nobody));
        assertEquals(0, signsOf(bill.getId()).size());
        assertEquals(Integer.valueOf(0), service.selectTNslpPermitBillById(bill.getId()).getNodeNo());
    }

    @Test
    public void summaryCountsBillsAndArrivalsFromOneComputation() {
        TNslpPermitBill a = service.open(openReq("PT0001"));
        service.sign(signReq(a.getId(), 0, "收件甲", "可"));   // a 挪到段二，新段 0 人
        TNslpPermitBill b = service.open(openReq("PT0002"));
        service.sign(signReq(b.getId(), 0, "收件乙", "可"));
        service.sign(signReq(b.getId(), 1, "认定丙", "可"));   // b 在段二，新段已 1 人

        // 两张在签：张数与逐段在签人数同一次算；countOpen 取的是同一回
        com.fc.v2.model.custom.PermitSummary s = service.summary();
        assertEquals(service.countOpen(), s.getOpenBillCount());
        assertEquals(2, s.getOpenBillCount());
        assertEquals(0, s.getArrivalsByNode()[0]);
        assertEquals(1, s.getArrivalsByNode()[1]);
        assertEquals(0, s.getArrivalsByNode()[2]);
    }

    // ------------------------------------------------------------------
    // 帮衬：把单开到指定段（各段都递「可」）
    // ------------------------------------------------------------------

    private TNslpPermitBill reachNode(Long id, int node) {
        String[][] seats = {{"收件甲"}, {"认定甲", "认定乙"}, {"存备甲", "存备乙"}};
        int cur = service.selectTNslpPermitBillById(id).getNodeNo().intValue();
        while (cur < node) {
            for (String who : seats[cur]) {
                service.sign(signReq(id, cur, who, "可"));
            }
            cur++;
        }
        return service.selectTNslpPermitBillById(id);
    }
}

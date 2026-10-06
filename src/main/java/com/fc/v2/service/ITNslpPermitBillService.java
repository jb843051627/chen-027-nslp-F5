package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TNslpPermitBill;
import com.fc.v2.model.auto.TNslpPointPermitHist;
import com.fc.v2.model.custom.PermitLedger;
import com.fc.v2.model.custom.PermitOpen;
import com.fc.v2.model.custom.PermitSignSubmit;
import com.fc.v2.model.custom.PermitSummary;
import com.fc.v2.model.custom.PermitTrace;

/**
 * 夜间连续施工作业许可签批单 Service接口（approval-chain 形状：三段签批，无新建/改笔/追补旁路）。
 *
 * 一次落笔只有一个入口；段次、应签、已到几人、算齐数法全由后端这一处点，
 * 报文写的段、屏显的人数都不作依据。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface ITNslpPermitBillService {

    /** 按主键回查单据 */
    TNslpPermitBill selectTNslpPermitBillById(Long id);

    /**
     * 立单：只从点位底册带出来。一处点位、一次夜间连续施工只摊上一张在签的单；
     * 口头说一声不算立单，前端没有自己新建的口子。
     */
    TNslpPermitBill open(PermitOpen submit);

    /**
     * 落笔（唯一入口）：一人一笔，逐笔记账。报文自称的段与服务端当下段不符，笔不采信。
     * 同一落笔人在同一段本趟再递，不并成一笔、也不另存一份（幂等挡回）。
     */
    TNslpPermitBill sign(PermitSignSubmit submit);

    /**
     * 压回一关：目标段由口径定死（只退一段）；只抹目标段本轮的笔（置失效，行留底），
     * 趟次加一，重走时本轮与上轮分两处。止住／封住的单压不动。
     */
    TNslpPermitBill rollback(Long id, String operator, String note);

    /** 追溯：当前段由留痕放，已到几人由逐笔签署累，屏显不取 */
    PermitTrace trace(Long id);

    /** 受理簿对账：倒着数回头一段，段数与当日笔数一笔一笔合得上，抽任一段也自洽 */
    PermitLedger ledger(Long id, Date day);

    /**
     * 页首一回装配：还没封住的张数与逐段在签人数基于同一次计算。
     * 人数由在签单当前趟的逐笔签署累；行面 sign_count 是同一累法写回的回显，
     * 对不住时指名报出对应单据。
     */
    PermitSummary summary();

    /** 还没封住的张数（止住的仍算没封住）：立单加一、封存减一，只认一次查出来的数 */
    int countOpen();

    /** 点位改口履历：封住改口那一笔起，一条一条查得着，对不住时凭单据号指名 */
    List<TNslpPointPermitHist> listPointHist(Long pointId);
}

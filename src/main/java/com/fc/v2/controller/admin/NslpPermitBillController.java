package com.fc.v2.controller.admin;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.PermitLedger;
import com.fc.v2.model.custom.PermitOpen;
import com.fc.v2.model.custom.PermitSignSubmit;
import com.fc.v2.model.custom.PermitSummary;
import com.fc.v2.model.custom.PermitTrace;
import com.fc.v2.service.ITNslpPermitBillService;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.DateUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 夜间连续施工作业许可签批单 Controller（approval-chain 形状：三段签批唯一入口）。
 *
 * 前端只把字敲进来、把数回显出来：停在哪一段、每段应签几人、已到几人全由后端数。
 * 没有新建单据的口子（立单只从点位底册带出），也没有改旧笔、追补签署的旁路。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "夜间连续施工作业许可签批单")
@Controller
@RequestMapping("/nslpPermitBill")
public class NslpPermitBillController extends BaseController {

    private final String prefix = "admin/nslpPermitBill";

    @Autowired
    private ITNslpPermitBillService nslpPermitBillService;

    @ApiOperation(value = "签批台账跳转", notes = "签批台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("nslpPermitBill:view")
    public String view() {
        return prefix + "/list";
    }

    @ApiOperation(value = "回查一张单", notes = "段次、人数都是服务端数出来的回显")
    @GetMapping("/get")
    @RequiresPermissions("nslpPermitBill:view")
    @ResponseBody
    public AjaxResult get(Long id) {
        return AjaxResult.successData(200, nslpPermitBillService.selectTNslpPermitBillById(id));
    }

    @Log(title = "签批单立单（从点位底册带出）", action = "open")
    @ApiOperation(value = "立单", notes = "只认点位底册；一处点位一张在签的单；口头招呼、前端自建都不开口")
    @PostMapping("/open")
    @RequiresPermissions("nslpPermitBill:open")
    @ResponseBody
    public AjaxResult open(@RequestBody PermitOpen submit) {
        return AjaxResult.successData(200, nslpPermitBillService.open(submit));
    }

    @Log(title = "签批落笔（唯一入口）", action = "sign")
    @ApiOperation(value = "落笔", notes = "一人一笔逐笔记；报文段次不采信；同段同人再递幂等挡回")
    @PostMapping("/sign")
    @RequiresPermissions("nslpPermitBill:sign")
    @ResponseBody
    public AjaxResult sign(@RequestBody PermitSignSubmit submit) {
        // 落笔人只认登录这一位；报文没带由服务端补，复核点得出人
        if (submit.getSigner() == null || submit.getSigner().trim().isEmpty()) {
            submit.setSigner(ShiroUtils.getLoginName());
        }
        return AjaxResult.successData(200, nslpPermitBillService.sign(submit));
    }

    @Log(title = "签批单压回一关", action = "rollback")
    @ApiOperation(value = "压回一关", notes = "只退一段；只抹本段本轮的笔，旧趟旧段一笔不动；止住/封住压不动")
    @PostMapping("/rollback")
    @RequiresPermissions("nslpPermitBill:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String note) {
        return AjaxResult.successData(200,
                nslpPermitBillService.rollback(id, ShiroUtils.getLoginName(), note));
    }

    @ApiOperation(value = "签批追溯", notes = "当前段由留痕放；已到几人由逐笔签押累；新旧两趟分两处")
    @GetMapping("/trace")
    @RequiresPermissions("nslpPermitBill:trace")
    @ResponseBody
    public AjaxResult trace(Long id) {
        PermitTrace t = nslpPermitBillService.trace(id);
        return AjaxResult.successData(200, t);
    }

    @ApiOperation(value = "受理簿对账", notes = "倒着数回头一段，段数与当日笔数对得上，抽任一段也自洽")
    @GetMapping("/ledger")
    @RequiresPermissions("nslpPermitBill:trace")
    @ResponseBody
    public AjaxResult ledger(Long id, String day) {
        PermitLedger l = nslpPermitBillService.ledger(id, DateUtils.parseDate(day));
        return AjaxResult.successData(200, l);
    }

    @ApiOperation(value = "页首页数（与逐段人数同源）", notes = "张数随立单加一、封存减一；各段在签人数与张数同一次算")
    @GetMapping("/summary")
    @RequiresPermissions("nslpPermitBill:view")
    @ResponseBody
    public AjaxResult summary() {
        return AjaxResult.successData(200, nslpPermitBillService.summary());
    }

    @ApiOperation(value = "还没封住的张数", notes = "立单加一、封存减一；止住的也算没封住；只认一次算的数")
    @GetMapping("/countOpen")
    @RequiresPermissions("nslpPermitBill:view")
    @ResponseBody
    public AjaxResult countOpen() {
        return AjaxResult.successData(200, nslpPermitBillService.countOpen());
    }

    @ApiOperation(value = "点位改口履历", notes = "封住改口那一笔起；谁落、依据哪张单、从哪档到哪档、哪一刻")
    @GetMapping("/pointHist")
    @RequiresPermissions("nslpPermitBill:trace")
    @ResponseBody
    public AjaxResult pointHist(Long pointId) {
        return AjaxResult.successData(200, nslpPermitBillService.listPointHist(pointId));
    }
}

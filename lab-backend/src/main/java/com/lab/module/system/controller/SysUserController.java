package com.lab.module.system.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.module.system.entity.SysUser;
import com.lab.module.system.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService userService;
    public SysUserController(SysUserService s) { this.userService = s; }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1")  int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(pageNum, pageSize, keyword, deptId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<SysUser> detail(@PathVariable Long id) { return Result.ok(userService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        SysUser u = mapTo(body);
        @SuppressWarnings("unchecked") List<Long> roleIds = (List<Long>) body.get("roleIds");
        return Result.ok(userService.create(u, roleIds));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<?> update(@RequestBody Map<String, Object> body) {
        SysUser u = mapTo(body);
        u.setId(((Number) body.get("id")).longValue());
        @SuppressWarnings("unchecked") List<Long> roleIds = (List<Long>) body.get("roleIds");
        userService.update(u, roleIds);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<?> delete(@PathVariable Long id) { userService.delete(id); return Result.ok(); }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<?> status(@PathVariable Long id, @PathVariable Integer status) {
        userService.changeStatus(id, status); return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<?> resetPwd(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password); return Result.ok();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('system:user:list')")
    public void export(HttpServletResponse resp) throws IOException {
        List<SysUser> users = userService.listAll();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("用户列表");
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"ID", "用户名", "真实姓名", "手机", "邮箱", "部门", "状态", "创建时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SysUser u : users) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(u.getId());
            row.createCell(1).setCellValue(u.getUsername());
            row.createCell(2).setCellValue(u.getRealName());
            row.createCell(3).setCellValue(u.getPhone());
            row.createCell(4).setCellValue(u.getEmail());
            row.createCell(5).setCellValue(u.getDeptName());
            row.createCell(6).setCellValue(u.getStatus() == 1 ? "正常" : "禁用");
            row.createCell(7).setCellValue(u.getCreateTime() != null ? u.getCreateTime().toString() : "");
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        String filename = URLEncoder.encode("用户列表", StandardCharsets.UTF_8) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + filename);
        wb.write(resp.getOutputStream());
        wb.close();
    }

    @SuppressWarnings("unchecked")
    private SysUser mapTo(Map<String, Object> body) {
        SysUser u = new SysUser();
        u.setUsername((String) body.get("username"));
        u.setPassword((String) body.get("password"));
        u.setRealName((String) body.get("realName"));
        u.setPhone((String) body.get("phone"));
        u.setEmail((String) body.get("email"));
        if (body.get("gender") != null) u.setGender(((Number) body.get("gender")).intValue());
        if (body.get("status") != null) u.setStatus(((Number) body.get("status")).intValue());
        if (body.get("deptId") != null) u.setDeptId(((Number) body.get("deptId")).longValue());
        u.setRemark((String) body.get("remark"));
        return u;
    }
}

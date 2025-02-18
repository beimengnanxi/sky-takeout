package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.OrderService;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1.获取起止时间
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        // 2.调用方法获取概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 3.通过POI将数据写入到excel文件中

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            // 基于模板创建一个新的excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充数据
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateBegin);
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());


            // 填充明细数据
            for(int i = 0; i < 30; i++){
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }
            // 通过输出流将excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            // 关闭
            excel.close();
            out.close();

        }catch (IOException e){
            e.printStackTrace();
        }



    }

    @Override
    public SalesTop10ReportVO getOrderSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getOrderSalesTop10(beginTime,endTime);

        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(nameList,","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 获取日期集合
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dataList.add(begin);
        }
        List<Integer> newOrders = new ArrayList<>();
        List<Integer> newValidOrders = new ArrayList<>();
        Map mp = new HashMap();
        Integer totalOrderCount = 0;
        Integer totalValidOrderCount = 0;

        //
        for(LocalDate date : dataList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("endTime",endTime);
            map.put("beginTime",beginTime);
            Integer newOrder = orderMapper.countByMap(map);
            map.put("status", Orders.COMPLETED);
            Integer newValidOrder = orderMapper.countByMap(map);
            totalOrderCount += newOrder;
            totalValidOrderCount += newValidOrder;
            newOrders.add(newOrder);
            newValidOrders.add(newValidOrder);
        }
        Double orderCompletionRate = 0.0;
        if(totalOrderCount > 0){
            orderCompletionRate = (double)totalValidOrderCount / (double)totalOrderCount;
        }
        return OrderReportVO.builder().dateList(StringUtils.join(dataList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCountList(StringUtils.join(newOrders,","))
                .validOrderCountList(StringUtils.join(newValidOrders,","))
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 获取日期集合
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dataList.add(begin);
        }
        // 获取总用户统计
        List<Integer> newUsers = new ArrayList<>();
        List<Integer> totalUsers = new ArrayList<>();
        for(LocalDate date : dataList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            // 先查当前总用户量
            map.put("endTime",endTime);
            Integer newUser = userMapper.countByMap(map);
            // 再查新增用户
            Integer totalUser = userMapper.countByMap(map);
            newUsers.add(newUser);
            totalUsers.add(totalUser);
        }
        return UserReportVO.builder().dateList(StringUtils.join(dataList,","))
                .totalUserList(StringUtils.join(totalUsers,","))
                .newUserList(StringUtils.join(newUsers,","))
                .build();
    }

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 获取日期集合
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dataList.add(begin);
        }

        //获取每日营业额
        List<Double> turnoverStatistics = new ArrayList<>();
        for(LocalDate date : dataList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("beginTime",beginTime);
            map.put("endTime",endTime);
            map.put("status",Orders.COMPLETED);
            Double turnover = orderMapper.sumByOrderTimeAndStatus(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverStatistics.add(turnover);
        }
        return TurnoverReportVO.builder().dateList(StringUtils.join(dataList,",")).turnoverList(StringUtils.join(turnoverStatistics,",")).build();
    }
}

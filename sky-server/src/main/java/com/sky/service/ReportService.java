package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@Service
public interface ReportService {

    void exportBusinessData(HttpServletResponse response);

    SalesTop10ReportVO getOrderSalesTop10(LocalDate begin, LocalDate end);

    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);
}

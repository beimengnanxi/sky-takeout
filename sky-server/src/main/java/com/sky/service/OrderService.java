package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO getStatistics();

    OrderVO getDetails(Long id);

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    void cancel(OrdersCancelDTO ordersCancelDTO);

    void complete(Long id);

    void delivery(Long id);

    PageResult getHistoryOrders(int page, int pageSize, Integer status);

    void reminder(Long id);

    void repetition(Long id);

    void userCancel(Long id);


}

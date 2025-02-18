package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;



    private Orders orders;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 先查询有默认地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        // 再查询购物车是否为空
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 先插入订单数据
        User user = userMapper.getById(userId);
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPhone(addressBook.getPhone());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserName(user.getName());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //自定义代码，用作微信支付
        this.orders = orders;


        orderMapper.insert(orders);

        // 再插入订单明细数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart sc : list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(sc, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        // 清空购物车数据
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        shoppingCartMapper.delete(cart);

        // 返回VO数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());
        // 用户支付成功时触发来单提醒
        Map map = new HashMap();
        map.put("type",1); // 1:来单提醒
        map.put("orderId",orders.getId());
        map.put("content","订单号：" + ordersPaymentDTO.getOrderNumber());
        String jsonString = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);


    }

    /**
     * 订单分页 查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if(page != null && !page.isEmpty()){
            list = getOrderVoList(page);
        }

        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询各状态订单数量
     * @return
     */
    @Override
    public OrderStatisticsVO getStatistics() {
        Integer toBeConfirmed = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.getCountByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 查询
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getDetails(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> list = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(list);
       return orderVO;
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 用户拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 先查询当前订单状态，只有待接单状态下才可拒单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(orders == null || !Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders order = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .build();

        // 如果用户已经付款，需退款(这里不做其他处理，仅将支付状态修改为已退款)
        if(!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)){
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(order);

    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        // 如果用户已经付款，需退款(这里不做其他处理，仅将支付状态修改为已退款)
        if(!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)){
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);

    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 商家派单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null || !Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 用户端获取历史订单
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult getHistoryOrders(int pageNum, int pageSize, Integer status) {
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setPage(pageNum);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        return conditionSearch(ordersPageQueryDTO);
    }

    @Override
    public void reminder(Long id) {
            Orders orderDB = orderMapper.getById(id);
            if(orderDB == null){
                throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
            Map map = new HashMap();
            map.put("type",2);// 2表示催单
            map.put("orderId",orderDB.getId());
            map.put("content","订单号：" + orderDB.getNumber());
            String json = JSONObject.toJSONString(map);
            webSocketServer.sendToAllClient(json);
    }

    @Override
    public void repetition(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> list = orderDetailMapper.getByOrderId(orders.getId());
        if(list != null){
            List<ShoppingCart> shoppingCarts = new ArrayList<>();
            for(OrderDetail orderDetail : list){
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(orderDetail, shoppingCart);
                shoppingCart.setUserId(orders.getUserId());
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCarts.add(shoppingCart);
            }
            shoppingCartMapper.insertBatch(shoppingCarts);
        }
    }

    @Override
    public void userCancel(Long id) {
        Orders orders = orderMapper.getById(id);
        // 校验订单
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        // 如果用户已经付款，需退款(这里不做其他处理，仅将支付状态修改为已退款)
        if(!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)){
            order.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(order);
    }










    /**
     * 返回信息中需添加菜品信息字符串
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVoList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page.getResult()) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
            orderVO.setOrderDetailList(orderDetails);
            orderVO.setOrderDishes(getOrderDishesStr(orders));
            orderVOList.add(orderVO);
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishes = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetails) {
            String orderDishesStr = orderDetail.getName() + "*" + orderDetail.getNumber() + ";" ;
            orderDishes.add(orderDishesStr);
        }
        return String.join("",orderDishes);
    }

}

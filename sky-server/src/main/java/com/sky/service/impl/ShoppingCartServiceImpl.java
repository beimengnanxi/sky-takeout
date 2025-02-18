package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long id = BaseContext.getCurrentId();
        shoppingCart.setId(id);
        // 先查询菜品或套餐是否已经存在购物车中
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {
            // 如果存在，只需将数量加1
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }else{
            // 不存在则插入
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);
            shoppingCart.setUserId(id);
            // 先判断是套餐还是菜品
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setName(dish.getName());
            }else{
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setName(setmeal.getName());

            }
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> list() {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        return list;
    }

    @Override
    public void clean(Long userId) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long id = BaseContext.getCurrentId();
        shoppingCart.setId(id);
        // 先查询菜品或套餐的数量大于等于1
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list.get(0).getNumber() > 1) {
            // 如果大于1，只需将数量减1
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.updateNumberById(cart);
        }else{
            // 等于1则直接删除

            // 先判断是套餐还是菜品
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                shoppingCart.setDishId(dishId);
                shoppingCartMapper.delete(shoppingCart);
            }else{
               Long setmealId = shoppingCartDTO.getSetmealId();
               shoppingCart.setSetmealId(setmealId);
               shoppingCartMapper.delete(shoppingCart);

            }
        }
    }
}

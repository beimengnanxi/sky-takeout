package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void saveWithFlavour(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        List<DishFlavor> dishFlavors= dishDTO.getFlavors();
        if(dishFlavors!=null&&dishFlavors.size()>0){
            for (DishFlavor df : dishFlavors) {
                df.setDishId(dish.getId());
            }
            dishFlavorMapper.insertBatch(dishFlavors);
        }


    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Transactional
    @Override
    public void deleteByIds(List<Long> ids) {
        for(Long dishId:ids){
            Dish dish = dishMapper.getById(dishId);
            // 旗手状态不可被删除
            if(Objects.equals(dish.getStatus(), StatusConstant.ENABLE)){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            // 套餐中含有该菜品不能被删除
        }
        List<Long>setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        for(Long dishId:ids){
//            dishFlavorMapper.deleteByDishId(dishId);
//            dishMapper.deleteById(dishId);
//        }
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);
    }

    @Override
    public DishVO getByIdWithFlavor(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.getById(id);
        BeanUtils.copyProperties(dish, dishVO);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        // 先删
        Long dishId = dishDTO.getId();
        dishFlavorMapper.deleteByDishId(dishId);
        // 后重新插入
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if(dishFlavors!=null&&dishFlavors.size()>0){
            for (DishFlavor df : dishFlavors) {
                df.setDishId(dish.getId());
            }
            dishFlavorMapper.insertBatch(dishFlavors);
        }


    }

    @Override
    @AutoFill(value = OperationType.UPDATE)
    public void updateStatus(Integer status, Long id){
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        dishMapper.update(dish);
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> list = dishMapper.getByCategoryId(categoryId);
        return list;
    }

    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        // 从redis中查询缓存数据，如果没有再查询数据库并存入redis中
        String key = "dish_" + dish.getCategoryId().toString();
        List<DishVO> dishVOS = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if(dishVOS!=null&& !dishVOS.isEmpty()){
            return dishVOS;
        }
        dishVOS = new ArrayList<>();
        List<Dish> dishList = dishMapper.list(dish);
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(dishFlavors);
            dishVOS.add(dishVO);
        }
        redisTemplate.opsForValue().set(key,dishVOS);
        return dishVOS;
    }

}

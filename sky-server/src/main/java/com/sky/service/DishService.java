package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface DishService {

    @Transactional
    void saveWithFlavour(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteByIds(List<Long> ids);

    DishVO getByIdWithFlavor(Long id);

    void update(DishDTO dishDTO);

    void updateStatus(Integer status, Long id);

    List<Dish> getByCategoryId(Long categoryId);

    List<DishVO> listWithFlavor(Dish dish);
}

package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;



    @PostMapping
    public Result saveWithFlavour(@RequestBody DishDTO dishDTO) {

        dishService.saveWithFlavour(dishDTO);
        cleanCache("dish_" + dishDTO.getCategoryId());
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    @DeleteMapping
    public Result deleteBatch(@RequestParam List<Long> ids) {
        dishService.deleteByIds(ids);
        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<DishVO> show(@PathVariable Long id) {
        DishVO dishVO =  dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        dishService.update(dishDTO);
        cleanCache("dish_*");
        return Result.success();
    }

    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status,Long id) {
        dishService.updateStatus(status, id);
        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Dish>> getByCategoryId(Long categoryId) {
        List<Dish> list = dishService.getByCategoryId(categoryId);
        return Result.success(list);
    }


    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }


}

package com.tinyhis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinyhis.entity.CheckItem;
import com.tinyhis.mapper.CheckItemMapper;
import com.tinyhis.service.CheckItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckItemServiceImpl implements CheckItemService {

    private final CheckItemMapper checkItemMapper;

    @Override
    public List<CheckItem> getAllCheckItems() {
        return checkItemMapper.selectList(new LambdaQueryWrapper<CheckItem>()
                .eq(CheckItem::getStatus, 1) // Only active items
                .orderByAsc(CheckItem::getCategory)
                .orderByAsc(CheckItem::getItemId));
    }

    @Override
    public List<CheckItem> searchCheckItems(String keyword) {
        LambdaQueryWrapper<CheckItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckItem::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CheckItem::getItemName, keyword)
                    .or().like(CheckItem::getItemCode, keyword));
        }
        wrapper.orderByAsc(CheckItem::getCategory);
        return checkItemMapper.selectList(wrapper);
    }
}

package com.tinyhis.service;

import com.tinyhis.entity.CheckItem;
import java.util.List;

public interface CheckItemService {
    List<CheckItem> getAllCheckItems();
    List<CheckItem> searchCheckItems(String keyword);
}

package com.dzdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzdp.dto.Result;
import com.dzdp.entity.ShopType;


public interface IShopTypeService extends IService<ShopType> {

    Result getTypeList();
}

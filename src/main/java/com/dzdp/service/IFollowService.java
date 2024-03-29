package com.dzdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzdp.dto.Result;
import com.dzdp.entity.Follow;


public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);


    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}

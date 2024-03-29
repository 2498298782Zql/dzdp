package com.dzdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzdp.dto.Result;
import com.dzdp.entity.Blog;


public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Result queryBlogLikesById(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}

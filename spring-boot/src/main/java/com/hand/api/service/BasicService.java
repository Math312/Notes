package com.hand.api.service;

import com.github.pagehelper.PageInfo;

import java.util.List;

public interface BasicService<T>
{
    T selectById(Long id);

    T insertInstance(T instance);

    void deleteById(Long id);

    PageInfo<T> list(int page, int num);

}

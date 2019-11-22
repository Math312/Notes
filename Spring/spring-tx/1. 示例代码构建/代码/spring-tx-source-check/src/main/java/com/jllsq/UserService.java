package com.jllsq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserDao userdao;

    public void insertUser() {
        userdao.insert();
        System.out.println("userdao.insert()执行完成");
        int i = 10/0;
    }

}

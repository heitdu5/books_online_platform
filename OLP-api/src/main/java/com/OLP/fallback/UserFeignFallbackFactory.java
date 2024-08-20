package com.OLP.fallback;

import com.OLP.api.UserFeignService;
import com.OLP.common.entity.R;
import com.OLP.common.exception.DataException;
import com.OLP.common.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Slf4j
public class UserFeignFallbackFactory  implements FallbackFactory<UserFeignService> {
    @Override
    public UserFeignService create(Throwable cause) {

        return new UserFeignService() {
            @Override
            public R<List<User>> FreindsList(String username) {
                log.error("未查询到好友用户");
                return R.success(Collections.emptyList());
            }

            @Override
            public User getById(Long id) {
                throw new DataException("未查询到该用户!");
            }

            @Override
            public List<User> getListByCondition(List<String> reactUsers) {
                return Collections.emptyList();
            }

            @Override
            public User getOneByCondition(String name) {
                throw new DataException("未查询到该用户!");
            }

            @Override
            public void setproof(String s) {
                throw new DataException("上传图片失败!");
            }

        };

    }
}

package com.dzdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzdp.dto.LoginFormDTO;
import com.dzdp.dto.Result;
import com.dzdp.dto.UserDTO;
import com.dzdp.entity.User;
import com.dzdp.mapper.UserMapper;
import com.dzdp.service.IUserService;
import com.dzdp.utils.RegexUtils;
import com.dzdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dzdp.utils.RedisConstants.*;
import static com.dzdp.utils.SystemConstants.*;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Override
    public Result sendMailCode(String email) {
        if (RegexUtils.isEmailInvalid(email)) {
            //手机号不符合
            return Result.fail("邮箱格式错误");
        }

        // 60s内只可以发一次
        Boolean keyIsExist = stringRedisTemplate.hasKey(LOGIN_MAIL_KEY + email);
        if (keyIsExist) {
            Long expireTime = stringRedisTemplate.getExpire(LOGIN_MAIL_KEY + email);
            return Result.fail("请" + expireTime + "秒后重试");
        }


        //判断是否在限制条件内
        Boolean oneLevelLimit = stringRedisTemplate.opsForSet().isMember(ONE_LEVER_LIMIT_KEY + email, "1");
        if (oneLevelLimit != null && oneLevelLimit) {
            return Result.fail("5分钟内您已经发送了多次，请稍后重试");
        }

        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        long count_fiveMinute = stringRedisTemplate.opsForZSet().count(SET_COUNT + email, fiveMinutesAgo, System.currentTimeMillis());
        if (count_fiveMinute > 4) {
            stringRedisTemplate.opsForSet().add(ONE_LEVER_LIMIT_KEY + email, "1");
            stringRedisTemplate.expire(ONE_LEVER_LIMIT_KEY + email, 5, TimeUnit.MINUTES);
            Result.fail("5分钟内您已经发送了多次，请稍后重试");
        }
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("验证码邮件");
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_MAIL_KEY + email, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("发送登录验证码：{}", code);
        mailMessage.setText("您收到的验证码是: " + code);
        mailMessage.setTo(email);
        mailMessage.setFrom(emailFrom);
        mailSender.send(mailMessage);
        stringRedisTemplate.opsForZSet().add(SET_COUNT + email, System.currentTimeMillis() + "", System.currentTimeMillis());
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isEmailInvalid(phone)) {
            //手机号不符合
            return Result.fail("邮箱格式错误");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_MAIL_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            //不一致 报错
            return Result.fail("验证码错误");
        }
        //一致 根据手机号查询用户
        User user = baseMapper
                .selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone));
        //判断用户是否存在
        if (user == null) {
            //不存在 创建新用户
            user = createUserWithPhone(phone);
        }

        String token = UUID.randomUUID().toString(true);
        //userDTO转map
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>()
                , CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor(
                                (name, value) -> value.toString()
                        ));

        //保存用户信息到redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
        //设置过期时间
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.delete(LOGIN_MAIL_KEY + phone);
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY + yyyyMM + id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //写了redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取当前登陆用户
        Long id = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        //拼接key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY + yyyyMM + id;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //获取截至本月今天的所有签到记录
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key
                , BitFieldSubCommands
                        .create()
                        .get(BitFieldSubCommands.BitFieldType
                                .unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        //转二进制字符串
        String binaryString = Long.toBinaryString(num);
        //计算连续签到天数
        int count = 0;
        for (int i = binaryString.length() - 1; i >= 0; i--) {
            if (binaryString.charAt(i) == '1') {
                count++;
            } else {
                break;
            }
        }
        //返回
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        //生成随机昵称
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        baseMapper.insert(user);
        return user;
    }
}
